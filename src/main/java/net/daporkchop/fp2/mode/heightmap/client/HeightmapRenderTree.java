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

package net.daporkchop.fp2.mode.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderTree;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderTree extends AbstractFarRenderTree<HeightmapPos, HeightmapPiece> {
    public HeightmapRenderTree(@NonNull HeightmapRenderCache cache) {
        super(cache, 2);
    }

    @Override
    protected void storePos(long pos, @NonNull HeightmapPos toStore) {
        PUnsafe.putInt(pos + 0 * 4L, toStore.x());
        PUnsafe.putInt(pos + 1 * 4L, toStore.z());
    }

    @Override
    protected boolean isPosEqual(long a, @NonNull HeightmapPos b) {
        return aLevel == b.level()
               && PUnsafe.getInt(a + 0 * 4L) == b.x()
               && PUnsafe.getInt(a + 1 * 4L) == b.z();
    }

    @Override
    protected int childIndex(int level, @NonNull HeightmapPos pos) {
        int shift = level - pos.level() - 1;
        return (((pos.x() >>> shift) & 1) << 1) | ((pos.z() >>> shift) & 1);
    }

    @Override
    protected boolean intersects(long pos, @NonNull Volume volume) {
        int x = PUnsafe.getInt(pos + this.pos + 0 * 4L);
        int z = PUnsafe.getInt(pos + this.pos + 1 * 4L);
        int shift = level + T_SHIFT;
        return volume.intersects(x << shift, Integer.MIN_VALUE, z << shift, (x + 1) << shift, Integer.MAX_VALUE, (z + 1) << shift);
    }

    @Override
    protected boolean isNodeInFrustum(int level, long node, @NonNull IFrustum frustum) {
        int x = PUnsafe.getInt(node + this.pos + 0 * 4L);
        int z = PUnsafe.getInt(node + this.pos + 1 * 4L);
        int shift = level + T_SHIFT;
        return frustum.intersectsBB(x << shift, Integer.MIN_VALUE, z << shift, (x + 1) << shift, Integer.MAX_VALUE, (z + 1) << shift);
    }

    @Override
    protected boolean isVanillaRenderable(long pos) {
        return false; //TODO
    }

    @Override
    protected void putNodePosForIndex(int level, long node, @NonNull IntBuffer dst) {
        int x = PUnsafe.getInt(node + this.pos + 0 * 4L);
        int z = PUnsafe.getInt(node + this.pos + 1 * 4L);
        dst.put(x).put(0).put(z).put(level);
    }
}
