/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.mode.voxel.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.mode.common.client.IFarRenderBaker;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.minecraft.client.multiplayer.WorldClient;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderer extends AbstractFarRenderer<VoxelPos, VoxelPiece> {
    public VoxelRenderer(@NonNull WorldClient world) {
        super(world);
    }

    @Override
    protected IFarRenderStrategy<VoxelPos, VoxelPiece> createStrategy() {
        return new MultidrawVoxelRenderStrategy();
    }

    @Override
    protected VoxelRenderCache createCache() {
        return new VoxelRenderCache(this);
    }

    /*@Override
    protected void render0(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull IFrustum frustum, @NonNull FarRenderIndex index) {
        try (DrawMode drawMode = FP2Config.compatibility.drawMode.start(index, this.drawCommandBuffer, this)) {
            for (int i = 0; i < RenderPass.COUNT; i++) {
                drawMode.draw(RenderPass.fromOrdinal(i), i);
            }
        }
    }

    @Override
    public ShaderProgram getAndUseShader(@NonNull DrawMode mode, @NonNull RenderPass pass, boolean stencil) {
        switch (mode) {
            case MULTIDRAW:
                return (stencil ? STENCIL_SHADER : SOLID_SHADER).use();
        }
        throw new IllegalArgumentException("mode=" + mode + ", pass=" + pass + ", stencil=" + stencil);
    }*/

    @Override
    public RenderMode mode() {
        return RenderMode.VOXEL;
    }
}
