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
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderTree;
import net.daporkchop.fp2.mode.voxel.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderTree extends AbstractFarRenderTree<VoxelPos, VoxelPiece> {
    public VoxelRenderTree(@NonNull VoxelRenderCache cache) {
        super(cache, 3);
    }

    @Override
    protected void storePos(long pos, @NonNull VoxelPos toStore) {
        PUnsafe.putInt(pos + 0 * 4L, toStore.x());
        PUnsafe.putInt(pos + 1 * 4L, toStore.y());
        PUnsafe.putInt(pos + 2 * 4L, toStore.z());
    }

    @Override
    protected boolean isPosEqual(int aLevel, long aPos, @NonNull VoxelPos b) {
        return aLevel == b.level()
               && PUnsafe.getInt(aPos + 0 * 4L) == b.x()
               && PUnsafe.getInt(aPos + 1 * 4L) == b.y()
               && PUnsafe.getInt(aPos + 2 * 4L) == b.z();
    }

    @Override
    protected int childIndex(int level, @NonNull VoxelPos pos) {
        int shift = level - pos.level() - 1;
        return (((pos.x() >> shift) & 1) << 2) | (((pos.y() >> shift) & 1) << 1) | ((pos.z() >> shift) & 1);
    }
}
