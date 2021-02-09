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
import net.daporkchop.fp2.client.ClientConstants;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderCache;
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderTree;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderTree extends AbstractFarRenderTree<VoxelPos, VoxelPiece> {
    public VoxelRenderTree(@NonNull AbstractFarRenderCache<VoxelPos, VoxelPiece> cache, @NonNull IFarRenderStrategy<VoxelPos, VoxelPiece> strategy, int d) {
        super(strategy, d, );
    }

    @Override
    protected boolean isPosEqual(long a, @NonNull VoxelPos b) {
        return PUnsafe.getInt(a + 0 * INT_SIZE) == b.x()
               && PUnsafe.getInt(a + 1 * INT_SIZE) == b.y()
               && PUnsafe.getInt(a + 2 * INT_SIZE) == b.z()
               && PUnsafe.getInt(a + 3 * INT_SIZE) == b.level();
    }

    @Override
    protected int childIndex(int level, @NonNull VoxelPos pos) {
        int shift = level - pos.level() - 1;
        return (((pos.x() >>> shift) & 1) << 2) | (((pos.y() >>> shift) & 1) << 1) | ((pos.z() >>> shift) & 1);
    }

    @Override
    protected boolean intersects(long pos, @NonNull Volume volume) {
        int x = PUnsafe.getInt(pos + 0 * INT_SIZE);
        int y = PUnsafe.getInt(pos + 1 * INT_SIZE);
        int z = PUnsafe.getInt(pos + 2 * INT_SIZE);
        int shift = PUnsafe.getInt(pos + 3 * INT_SIZE) + T_SHIFT;
        return volume.intersects(x << shift, y << shift, z << shift, (x + 1) << shift, (y + 1) << shift, (z + 1) << shift);
    }

    @Override
    protected boolean containedBy(long pos, @NonNull Volume volume) {
        int x = PUnsafe.getInt(pos + 0 * INT_SIZE);
        int y = PUnsafe.getInt(pos + 1 * INT_SIZE);
        int z = PUnsafe.getInt(pos + 2 * INT_SIZE);
        int shift = PUnsafe.getInt(pos + 3 * INT_SIZE) + T_SHIFT;
        return volume.contains(x << shift, y << shift, z << shift, (x + 1) << shift, (y + 1) << shift, (z + 1) << shift);
    }

    @Override
    protected boolean isNodeInFrustum(long pos, @NonNull IFrustum frustum) {
        int x = PUnsafe.getInt(pos + 0 * INT_SIZE);
        int y = PUnsafe.getInt(pos + 1 * INT_SIZE);
        int z = PUnsafe.getInt(pos + 2 * INT_SIZE);
        int shift = PUnsafe.getInt(pos + 3 * INT_SIZE) + T_SHIFT;
        return frustum.intersectsBB(x << shift, y << shift, z << shift, (x + 1) << shift, (y + 1) << shift, (z + 1) << shift);
    }

    @Override
    protected boolean isVanillaRenderable(long pos) {
        int x = PUnsafe.getInt(pos + 0 * INT_SIZE);
        int y = PUnsafe.getInt(pos + 1 * INT_SIZE);
        int z = PUnsafe.getInt(pos + 2 * INT_SIZE);
        return ClientConstants.isChunkRenderable(x, y, z);
    }
}
