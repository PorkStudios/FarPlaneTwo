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
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;
import net.daporkchop.fp2.util.math.Volume;

import static net.daporkchop.fp2.mode.heightmap.client.HeightmapRenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderTree extends AbstractFarRenderTree<HeightmapPos, HeightmapPiece> {
    public HeightmapRenderTree(@NonNull IFarRenderStrategy<HeightmapPos, HeightmapPiece> strategy, int maxLevel) {
        super(strategy, 2, maxLevel);
    }

    @Override
    protected boolean isPosEqual(long a, @NonNull HeightmapPos b) {
        return _pos_tileX(a) == b.x()
               && _pos_tileZ(a) == b.z()
               && _pos_level(a) == b.level();
    }

    @Override
    protected int childIndex(int level, @NonNull HeightmapPos pos) {
        int shift = level - pos.level() - 1;
        return (((pos.x() >>> shift) & 1) << 1) | ((pos.z() >>> shift) & 1);
    }

    @Override
    protected boolean intersects(long pos, @NonNull Volume volume) {
        int x = _pos_tileX(pos);
        int z = _pos_tileZ(pos);
        int shift = _pos_level(pos) + T_SHIFT;
        return volume.intersects(x << shift, Integer.MIN_VALUE, z << shift, (x + 1) << shift, Integer.MAX_VALUE, (z + 1) << shift);
    }

    @Override
    protected boolean containedBy(long pos, @NonNull Volume volume) {
        int x = _pos_tileX(pos);
        int z = _pos_tileZ(pos);
        int shift = _pos_level(pos) + T_SHIFT;
        return volume.contains(x << shift, Integer.MIN_VALUE, z << shift, (x + 1) << shift, Integer.MAX_VALUE, (z + 1) << shift);
    }

    @Override
    protected boolean isNodeInFrustum(long pos, @NonNull IFrustum frustum) {
        int x = _pos_tileX(pos);
        int z = _pos_tileZ(pos);
        int shift = _pos_level(pos) + T_SHIFT;
        return frustum.intersectsBB(x << shift, Integer.MIN_VALUE, z << shift, (x + 1) << shift, Integer.MAX_VALUE, (z + 1) << shift);
    }

    @Override
    protected boolean isVanillaRenderable(long pos) {
        return false; //TODO
    }
}
