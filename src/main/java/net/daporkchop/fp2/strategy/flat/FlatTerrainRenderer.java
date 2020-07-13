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

package net.daporkchop.fp2.strategy.flat;

import lombok.NonNull;
import net.daporkchop.fp2.client.GlobalInfo;
import net.daporkchop.fp2.client.RenderPass;
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
import net.daporkchop.fp2.client.ClientConstants;
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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

import static net.daporkchop.fp2.client.GlobalInfo.*;
import static net.daporkchop.fp2.strategy.flat.FlatConstants.*;
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

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class FlatTerrainRenderer extends TerrainRenderer {
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

    public static final VertexBufferObject COORDS = new VertexBufferObject();

    static {
        ShortBuffer meshData = BufferUtils.createShortBuffer(FLAT_VERTS * FLAT_VERTS * 6 + 1);
        MESH_VERTEX_COUNT = genMesh(FLAT_VERTS, FLAT_VERTS, meshData);

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

    static {
        FloatBuffer coordsData = BufferUtils.createFloatBuffer(FLAT_VERTS * FLAT_VERTS * 2);
        for (int x = 0; x < FLAT_VERTS; x++) {
            for (int z = 0; z < FLAT_VERTS; z++) {
                coordsData.put(x).put(z);
            }
        }

        try (VertexBufferObject coords = COORDS.bind()) {
            GL15.glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) coordsData.flip(), GL_STATIC_DRAW);
        }
    }

    protected final Map<FlatPiecePos, VertexArrayObject> pieces = new HashMap<>();
    protected IntBuffer renderableChunksMask;

    public FlatTerrainRenderer(@NonNull WorldClient world) {
    }

    @Override
    public void receivePiece(@NonNull IFarPiece pieceIn) {
        checkArg(pieceIn instanceof FlatPiece, pieceIn);
        FlatPiece piece = (FlatPiece) pieceIn;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try (VertexArrayObject vao = new VertexArrayObject().bind()) {
                glEnableVertexAttribArray(0);
                glEnableVertexAttribArray(1);
                glEnableVertexAttribArray(2);
                glEnableVertexAttribArray(3);
                glEnableVertexAttribArray(4);
                glEnableVertexAttribArray(5);

                try (VertexBufferObject coords = COORDS.bind()) {
                    glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
                    vao.putDependency(0, coords);
                }
                try (VertexBufferObject heights = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, piece.height(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(1, 1, GL_INT, 0, 0L);
                    vao.putDependency(1, heights);
                }
                try (VertexBufferObject colors = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, piece.color(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(2, 1, GL_INT, 0, 0L);
                    vao.putDependency(2, colors);
                }
                try (VertexBufferObject biomes = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, piece.biome(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(3, 1, GL_UNSIGNED_BYTE, 0, 0L);
                    vao.putDependency(3, biomes);
                }
                try (VertexBufferObject blocks = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, piece.block(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(4, 1, GL_UNSIGNED_SHORT, 0, 0L);
                    vao.putDependency(4, blocks);
                }
                try (VertexBufferObject light = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, piece.light(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(5, 1, GL_INT, 0, 0L);
                    vao.putDependency(5, light);
                }

                vao.putElementArray(MESH.bind());

                this.pieces.put(piece.pos(), vao);
            } finally {
                glDisableVertexAttribArray(0);
                glDisableVertexAttribArray(1);
                glDisableVertexAttribArray(2);
                glDisableVertexAttribArray(3);
                glDisableVertexAttribArray(4);
                glDisableVertexAttribArray(5);

                MESH.close();
            }
        });
    }

    @Override
    public void unloadPiece(@NonNull IFarPiecePos pos) {
        checkArg(pos instanceof FlatPiecePos, pos);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            this.pieces.remove(pos);
        });
    }

    @Override
    public void render(RenderPass pass, float partialTicks, WorldClient world, Minecraft mc) {
        if (pass == RenderPass.PRE) {
            super.render(pass, partialTicks, world, mc);

            try (ShaderStorageBuffer loadedBuffer = new ShaderStorageBuffer().bind()) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, this.renderableChunksMask = ClientConstants.renderableChunksMask(mc, this.renderableChunksMask), GL_STATIC_DRAW);
                loadedBuffer.bindingIndex(0);
            }
            GLOBAL_INFO.bindingIndex(1);

            GlStateManager.disableCull();
            GlStateManager.enableAlpha();

            GlStateManager.enableBlend();
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            glPushMatrix();
            glTranslated(-this.cameraX, -this.cameraY, -this.cameraZ);

            this.modelView = MatrixHelper.getMATRIX(GL_MODELVIEW_MATRIX, this.modelView);
            this.proj = MatrixHelper.getMATRIX(GL_PROJECTION_MATRIX, this.proj);

            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

            mc.entityRenderer.enableLightmap();

            try {
                try (ShaderProgram shader = HEIGHT_SHADER.use()) {
                    ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
                    ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);

                    this.pieces.forEach((pos, o) -> {
                        glUniform2d(shader.uniformLocation("camera_offset"), pos.x() * FLAT_VOXELS + .5d, pos.z() * FLAT_VOXELS + .5d);

                        try (VertexArrayObject vao = o.bind()) {
                            glDrawElements(GL_TRIANGLES, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                        }
                    });
                }

                //glTranslated(0.0d, this.seaLevel, 0.0d);

                //this.modelView = MatrixHelper.getMATRIX(GL_MODELVIEW_MATRIX, this.modelView);

                try (ShaderProgram shader = WATER_SHADER.use()) {
                    ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
                    ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);
                    glUniform1f(shader.uniformLocation("seaLevel"), 63f);

                    this.pieces.forEach((pos, o) -> {
                        glUniform2d(shader.uniformLocation("camera_offset"), pos.x() * FLAT_VOXELS + .5d, pos.z() * FLAT_VOXELS + .5d);

                        try (VertexArrayObject vao = o.bind()) {
                            glDrawElements(GL_TRIANGLES, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                        }
                    });
                }
            } finally {
                mc.entityRenderer.disableLightmap();

                glPopMatrix();

                GlStateManager.disableBlend();
                GlStateManager.disableAlpha();
                GlStateManager.enableCull();
            }
        } else if (pass == RenderPass.TRANSLUCENT) {
        }
    }
}
