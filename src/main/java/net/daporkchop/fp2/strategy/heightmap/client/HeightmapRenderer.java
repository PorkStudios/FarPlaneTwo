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

package net.daporkchop.fp2.strategy.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.DrawIndirectBuffer;
import net.daporkchop.fp2.client.gl.object.ElementArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.base.client.AbstractFarRenderer;
import net.daporkchop.fp2.strategy.base.client.IFarRenderBaker;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class HeightmapRenderer extends AbstractFarRenderer<HeightmapPos, HeightmapPiece, HeightmapRenderTile> {
    public static final ShaderProgram TERRAIN_SHADER = ShaderManager.get("heightmap/terrain");
    //public static final ShaderProgram WATER_SHADER = ShaderManager.get("heightmap/water");

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

    public final ElementArrayObject mesh = new ElementArrayObject();
    public final int vertexCount;

    public final VertexBufferObject coords = new VertexBufferObject();
    public final VertexArrayObject vao = new VertexArrayObject();

    public HeightmapRenderer(@NonNull WorldClient world) {
        super(world);

        {
            ShortBuffer meshData = Constants.createShortBuffer(T_VERTS * T_VERTS * 6 + 1);
            this.vertexCount = genMesh(T_VERTS, T_VERTS, meshData);
            meshData.flip();

            try (ElementArrayObject mesh = this.mesh.bind()) {
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, meshData, GL_STATIC_DRAW);
            }
        }

        {
            ByteBuffer coordsData = Constants.createByteBuffer(T_VERTS * T_VERTS * 5);
            for (int x = 0; x < T_VERTS; x++) {
                for (int z = 0; z < T_VERTS; z++) {
                    coordsData.put((byte) x).put((byte) z)
                            .put((byte) (x & T_MASK)).put((byte) (z & T_MASK))
                            .put((byte) ((x & T_MASK) * T_VOXELS + (z & T_MASK)));
                }
            }
            coordsData.flip();

            try (VertexBufferObject coords = this.coords.bind()) {
                glBufferData(GL_ARRAY_BUFFER, coordsData, GL_STATIC_DRAW);
            }
        }

        try (VertexArrayObject vao = this.vao.bind()) {
            for (int i = 0; i <= 2; i++) {
                glEnableVertexAttribArray(i);
            }

            try (VertexBufferObject vbo = this.coords.bind()) {
                glVertexAttribIPointer(0, 2, GL_UNSIGNED_BYTE, 5, 0L);
                glVertexAttribIPointer(1, 2, GL_UNSIGNED_BYTE, 5, 2L);
                glVertexAttribIPointer(2, 1, GL_UNSIGNED_BYTE, 5, 4L);

                for (int i = 0; i <= 2; i++) {
                    vao.putDependency(i, vbo);
                }
            }

            vao.putElementArray(this.mesh.bind());
        } finally {
            for (int i = 0; i <= 2; i++) {
                glDisableVertexAttribArray(i);
            }

            this.mesh.close();
        }
    }

    @Override
    protected void createRenderData() {
    }

    @Override
    protected HeightmapRenderCache createCache() {
        return new HeightmapRenderCache(this);
    }

    @Override
    public IFarRenderBaker<HeightmapPos, HeightmapPiece> baker() {
        return new HeightmapRenderBaker();
    }

    @Override
    protected void render0(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum, int count, IntBuffer commands) {
        try (VertexArrayObject vao = this.cache.vao().bind();
             DrawIndirectBuffer drawCommandBuffer = this.cache.drawCommandBuffer().bind()) {
            try (ShaderProgram shader = TERRAIN_SHADER.use()) {
                GlStateManager.disableAlpha();

                glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, 0L, count, 0);

                GlStateManager.enableAlpha();
            }
            /*try (ShaderProgram shader = WATER_SHADER.use()) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glUniform1f(shader.uniformLocation("seaLevel"), 63.0f);

                glDrawElementsInstanced(GL_TRIANGLES, this.vertexCount, GL_UNSIGNED_SHORT, 0L, count);

                GlStateManager.disableBlend();
            }*/
        }
    }
}
