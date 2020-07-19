/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.strategy.heightmap.render;

import lombok.NonNull;
import net.daporkchop.fp2.client.ClientConstants;
import net.daporkchop.fp2.client.GlobalInfo;
import net.daporkchop.fp2.client.gl.MatrixHelper;
import net.daporkchop.fp2.client.gl.object.ElementArrayObject;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPiecePos;
import net.daporkchop.fp2.strategy.common.TerrainRenderer;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiecePos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL15;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

import static net.daporkchop.fp2.client.GlobalInfo.*;
import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class HeightmapTerrainRenderer extends TerrainRenderer {
    public static ShaderProgram HEIGHT_SHADER = ShaderManager.get("heightmap");
    public static ShaderProgram WATER_SHADER = ShaderManager.get("heightmap_water");

    public static void reloadHeightShader() {
        ShaderProgram shader = HEIGHT_SHADER;
        ShaderProgram shader2 = WATER_SHADER;
        try {
            HEIGHT_SHADER = ShaderManager.get("heightmap");
            WATER_SHADER = ShaderManager.get("heightmap_water");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (HEIGHT_SHADER == shader || WATER_SHADER == shader2) {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§cheightmap shader reload failed (check console)."));
            } else {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§aheightmap shader successfully reloaded."));
            }
        }
        GlobalInfo.init();
        GlobalInfo.reloadUVs();
    }

    public static final ElementArrayObject MESH = new ElementArrayObject();
    public static final int MESH_VERTEX_COUNT;

    /*static {
        ShortBuffer meshData = BufferUtils.createShortBuffer(HEIGHTMAP_VOXELS * HEIGHTMAP_VOXELS);
        MESH_VERTEX_COUNT = meshData.capacity();

        for (int i = 0; i < MESH_VERTEX_COUNT; i++) {
            meshData.put((short) i);
        }

        try (ElementArrayObject mesh = MESH.bind()) {
            GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer) meshData.flip(), GL_STATIC_DRAW);
        }
    }*/

    static {
        ShortBuffer meshData = BufferUtils.createShortBuffer(HEIGHTMAP_VOXELS * HEIGHTMAP_VOXELS * 6 + 1);
        MESH_VERTEX_COUNT = genMesh(HEIGHTMAP_VOXELS, HEIGHTMAP_VOXELS, meshData);

        try (ElementArrayObject mesh = MESH.bind()) {
            GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer) meshData.flip(), GL_STATIC_DRAW);
        }
    }

    private static int genMesh(int size, int edge, ShortBuffer out) {
        int verts = 0;
        for (int x = 0; x < size - 1; x++) {
            for (int z = 0; z < size - 1; z++) {
                out.put((short) ((x + 1) * edge + z))
                        .put((short) ((x + 1) * edge + (z + 1)))
                        .put((short) (x * edge + z));
                out.put((short) (x * edge + (z + 1)))
                        .put((short) ((x + 1) * edge + (z + 1)))
                        .put((short) (x * edge + z));
                verts += 6;
            }
        }
        return verts;
    }

    protected final Map<HeightmapPiecePos, VertexArrayObject> pieces = new HashMap<>();
    protected IntBuffer renderableChunksMask;

    public HeightmapTerrainRenderer(@NonNull WorldClient world) {
    }

    @Override
    public void receivePiece(@NonNull IFarPiece pieceIn) {
        checkArg(pieceIn instanceof HeightmapPiece, pieceIn);
        HeightmapPiece piece = (HeightmapPiece) pieceIn;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try (VertexArrayObject vao = new VertexArrayObject().bind()) {
                glEnableVertexAttribArray(0);
                glEnableVertexAttribArray(1);
                glEnableVertexAttribArray(2);
                glEnableVertexAttribArray(3);
                glEnableVertexAttribArray(4);

                try (VertexBufferObject vbo = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, piece.data(), GL_STATIC_DRAW);

                    glVertexAttribIPointer(0, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.HEIGHT_OFFSET);
                    vao.putDependency(0, vbo);

                    glVertexAttribIPointer(1, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.BLOCK_OFFSET);
                    vao.putDependency(1, vbo);

                    glVertexAttribIPointer(2, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.ATTRS_OFFSET);
                    vao.putDependency(2, vbo);
                }

                vao.putElementArray(MESH.bind());

                this.pieces.put(piece.pos(), vao);
            } finally {
                glDisableVertexAttribArray(0);
                glDisableVertexAttribArray(1);
                glDisableVertexAttribArray(2);
                glDisableVertexAttribArray(3);
                glDisableVertexAttribArray(4);

                MESH.close();
            }
        });
    }

    @Override
    public void unloadPiece(@NonNull IFarPiecePos pos) {
        checkArg(pos instanceof HeightmapPiecePos, pos);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            this.pieces.remove(pos);
        });
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        super.render(partialTicks, world, mc);

        try (ShaderStorageBuffer loadedBuffer = new ShaderStorageBuffer().bind()) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, this.renderableChunksMask = ClientConstants.renderableChunksMask(mc, this.renderableChunksMask), GL_STATIC_DRAW);
            loadedBuffer.bindSSBO(0);
        }
        GLOBAL_INFO.bindSSBO(1);

        GlStateManager.disableCull();
        GlStateManager.enableAlpha();

        GlStateManager.enableBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        MatrixHelper.reversedZ(mc.entityRenderer.getFOVModifier(partialTicks, true), (float) mc.displayWidth / (float) mc.displayHeight, 0.05f);

        GlStateManager.depthFunc(GL_GREATER);

        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);

        GlStateManager.clearDepth(0.0d);
        GlStateManager.clear(GL_DEPTH_BUFFER_BIT); //TODO: it might be better to use a separate depth buffer (maybe)

        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();

        glTranslated(-this.cameraX, -this.cameraY, -this.cameraZ);

        this.modelView = MatrixHelper.getMatrix(GL_MODELVIEW_MATRIX, this.modelView);
        this.proj = MatrixHelper.getMatrix(GL_PROJECTION_MATRIX, this.proj);

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        mc.entityRenderer.enableLightmap();

        try {
            try (ShaderProgram shader = HEIGHT_SHADER.use()) {
                ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
                ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);

                this.pieces.forEach((pos, o) -> {
                    glUniform2d(shader.uniformLocation("camera_offset"), pos.x() * HEIGHTMAP_VOXELS + .5d, pos.z() * HEIGHTMAP_VOXELS + .5d);

                    try (VertexArrayObject vao = o.bind()) {
                        glDrawElements(GL_TRIANGLES, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                    }
                });
            }

            /*try (ShaderProgram shader = WATER_SHADER.use()) {
                ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
                ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);
                glUniform1f(shader.uniformLocation("seaLevel"), 63f);

                this.pieces.forEach((pos, o) -> {
                    glUniform2d(shader.uniformLocation("camera_offset"), pos.x() * HEIGHTMAP_VOXELS + .5d, pos.z() * HEIGHTMAP_VOXELS + .5d);

                    try (VertexArrayObject vao = o.bind()) {
                        glDrawElements(GL_TRIANGLES, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                    }
                });
            }*/
        } finally {
            mc.entityRenderer.disableLightmap();

            glClipControl(GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE);
            GlStateManager.depthFunc(GL_LEQUAL);

            GlStateManager.clearDepth(1.0d);
            GlStateManager.clear(GL_DEPTH_BUFFER_BIT);

            GlStateManager.matrixMode(GL_PROJECTION);
            GlStateManager.popMatrix();

            GlStateManager.matrixMode(GL_MODELVIEW);
            GlStateManager.popMatrix();

            GlStateManager.disableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.enableCull();
        }
    }
}
