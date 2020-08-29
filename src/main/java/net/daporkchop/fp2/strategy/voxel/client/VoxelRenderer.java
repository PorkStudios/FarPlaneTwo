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

package net.daporkchop.fp2.strategy.voxel.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.ElementArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.base.client.AbstractFarRenderer;
import net.daporkchop.fp2.strategy.base.client.IFarRenderBaker;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;
import net.daporkchop.fp2.util.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderer extends AbstractFarRenderer<VoxelPos, VoxelPiece, VoxelRenderTile> {
    public static final ShaderProgram TERRAIN_SHADER = ShaderManager.get("voxel/terrain");

    public final ElementArrayObject mesh = new ElementArrayObject();
    public final int vertexCount = T_VOXELS * T_VOXELS * T_VOXELS;

    public final VertexBufferObject coords = new VertexBufferObject();
    public final VertexArrayObject vao = new VertexArrayObject();

    public VoxelRenderer(@NonNull WorldClient world) {
        super(world);

        {
            ShortBuffer meshData = Constants.createShortBuffer(T_VOXELS * T_VOXELS * T_VOXELS);
            for (int x = 0; x < T_VOXELS; x++) {
                for (int y = 0; y < T_VOXELS; y++) {
                    for (int z = 0; z < T_VOXELS; z++) {
                        meshData.put((short) ((x * T_VOXELS + y) * T_VOXELS + z));
                    }
                }
            }
            meshData.flip();

            try (ElementArrayObject mesh = this.mesh.bind()) {
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, meshData, GL_STATIC_DRAW);
            }
        }

        {
            ByteBuffer coordsData = Constants.createByteBuffer(T_VOXELS * T_VOXELS * T_VOXELS * 5);
            for (int x = 0; x < T_VOXELS; x++) {
                for (int y = 0; y < T_VOXELS; y++) {
                    for (int z = 0; z < T_VOXELS; z++) {
                        coordsData.put((byte) x).put((byte) y).put((byte) z)
                                .putShort((short) ((x * T_VOXELS + y) * T_VOXELS + z));
                    }
                }
            }
            coordsData.flip();

            try (VertexBufferObject coords = this.coords.bind()) {
                glBufferData(GL_ARRAY_BUFFER, coordsData, GL_STATIC_DRAW);
            }
        }

        try (VertexArrayObject vao = this.vao.bind()) {
            for (int i = 0; i <= 1; i++) {
                glEnableVertexAttribArray(i);
            }

            try (VertexBufferObject vbo = this.coords.bind()) {
                glVertexAttribIPointer(0, 3, GL_UNSIGNED_BYTE, 5, 0L);
                glVertexAttribIPointer(1, 1, GL_UNSIGNED_SHORT, 5, 3L);

                for (int i = 0; i <= 1; i++) {
                    vao.putDependency(i, vbo);
                }
            }

            vao.putElementArray(this.mesh.bind());
        } finally {
            for (int i = 0; i <= 1; i++) {
                glDisableVertexAttribArray(i);
            }

            this.mesh.close();
        }
    }

    @Override
    protected void createRenderData() {
    }

    @Override
    protected VoxelRenderCache createCache() {
        return new VoxelRenderCache(this);
    }

    @Override
    public IFarRenderBaker<VoxelPos, VoxelPiece> baker() {
        return null; //TODO
    }

    @Override
    protected void render0(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum, int count, IntBuffer commands) {
        try (VertexArrayObject vao = this.vao.bind()) {
            try (ShaderProgram shader = TERRAIN_SHADER.use()) {
                GlStateManager.disableAlpha();

                glDrawElementsInstanced(GL_POINTS, this.vertexCount, GL_UNSIGNED_SHORT, 0L, count);

                GlStateManager.enableAlpha();
            }
        }
    }
}
