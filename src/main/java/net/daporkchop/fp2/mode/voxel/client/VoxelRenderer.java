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

package net.daporkchop.fp2.mode.voxel.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.mode.common.client.FarRenderIndex;
import net.daporkchop.fp2.mode.common.client.IFarRenderBaker;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderer extends AbstractFarRenderer<VoxelPos, VoxelPiece> {
    public static final ShaderProgram SOLID_SHADER = ShaderManager.get("voxel/solid");
    public static final ShaderProgram TRANSPARENT_STENCIL_SHADER = ShaderManager.get("voxel/transparent_stencil");
    public static final ShaderProgram TRANSPARENT_SHADER = ShaderManager.get("voxel/transparent");

    public VoxelRenderer(@NonNull WorldClient world) {
        super(world);
    }

    @Override
    protected VoxelRenderCache createCache() {
        return new VoxelRenderCache(this);
    }

    @Override
    public IFarRenderBaker<VoxelPos, VoxelPiece> baker() {
        return new VoxelRenderBaker();
    }

    @Override
    protected void render0(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull IFrustum frustum, @NonNull FarRenderIndex index) {
        try (ShaderProgram shader = SOLID_SHADER.use()) {
            for (int i = 0; i < VoxelRenderPass.COUNT; i++) {
                int size = index.upload(i);
                if (size > 0) {
                    VoxelRenderPass.VALUES[i].render(mc, size);
                }
            }
        }
    }

    @Override
    public RenderMode mode() {
        return RenderMode.VOXEL;
    }
}
