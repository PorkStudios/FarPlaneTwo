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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.fp2.client.VanillaRenderabilityTracker;
import net.daporkchop.fp2.client.gl.shader.ComputeShaderBuilder;
import net.daporkchop.fp2.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.mode.voxel.VoxelDirectPosAccess;
import net.daporkchop.fp2.mode.voxel.VoxelPos;

/**
 * Implementation of {@link ICullingStrategy} for {@link VoxelPos}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VoxelCullingStrategy implements ICullingStrategy<VoxelPos> {
    public static final VoxelCullingStrategy INSTANCE = new VoxelCullingStrategy();

    @Override
    public ComputeShaderBuilder cullShaderBuilder() {
        return VoxelShaders.CULL_SHADER;
    }

    @Override
    public boolean blockedByVanilla(@NonNull VanillaRenderabilityTracker tracker, long pos) {
        return tracker.vanillaBlocksFP2RenderingAtLevel0(VoxelDirectPosAccess._x(pos), VoxelDirectPosAccess._y(pos), VoxelDirectPosAccess._z(pos));
    }
}
