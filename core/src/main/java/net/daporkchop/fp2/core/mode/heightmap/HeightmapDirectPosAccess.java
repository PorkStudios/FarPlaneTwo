/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.heightmap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.core.mode.heightmap.util.HeightmapPosArrayList;
import net.daporkchop.fp2.core.mode.heightmap.util.HeightmapPosHashSet;
import net.daporkchop.fp2.core.util.math.geometry.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link IFarDirectPosAccess} for {@link HeightmapPos}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HeightmapDirectPosAccess implements IFarDirectPosAccess<HeightmapPos> {
    public static final HeightmapDirectPosAccess INSTANCE = new HeightmapDirectPosAccess();

    /*
     * struct Pos { // 12 bytes
     *   int x;
     *   int z;
     *   int level;
     * };
     */

    public static final long _X_OFFSET = 0L;
    public static final long _Z_OFFSET = _X_OFFSET + INT_SIZE;
    public static final long _LEVEL_OFFSET = _Z_OFFSET + INT_SIZE;

    public static final long _SIZE = _LEVEL_OFFSET + INT_SIZE;

    public static int _x(long pos) {
        return PUnsafe.getInt(pos + _X_OFFSET);
    }

    public static void _x(long pos, int x) {
        PUnsafe.putInt(pos + _X_OFFSET, x);
    }

    public static int _z(long pos) {
        return PUnsafe.getInt(pos + _Z_OFFSET);
    }

    public static void _z(long pos, int z) {
        PUnsafe.putInt(pos + _Z_OFFSET, z);
    }

    public static int _level(long pos) {
        return PUnsafe.getInt(pos + _LEVEL_OFFSET);
    }

    public static void _level(long pos, int level) {
        PUnsafe.putInt(pos + _LEVEL_OFFSET, level);
    }

    @Override
    public int axisCount() {
        return 2;
    }

    @Override
    public long posSize() {
        return _SIZE;
    }

    @Override
    public void storePos(@NonNull HeightmapPos pos, long addr) {
        _x(addr, pos.x());
        _z(addr, pos.z());
        _level(addr, pos.level());
    }

    @Override
    public HeightmapPos loadPos(long addr) {
        return new HeightmapPos(_level(addr), _x(addr), _z(addr));
    }

    @Override
    public int getAxisHeap(@NonNull HeightmapPos pos, int axis) {
        switch (axis) {
            case 0:
                return pos.x();
            case 1:
                return pos.z();
            default:
                throw new IllegalArgumentException("invalid axis number: " + axis);
        }
    }

    @Override
    public int getAxisDirect(long addr, int axis) {
        return PUnsafe.getInt(addr + (long) checkIndex(2, axis) * INT_SIZE);
    }

    @Override
    public boolean equalsPos(long addr1, long addr2) {
        return _x(addr1) == _x(addr2)
               && _z(addr1) == _z(addr2)
               && _level(addr1) == _level(addr2);
    }

    @Override
    public int hashPos(long addr) {
        return _x(addr) * 1317194159 + _z(addr) * 1656858407 + _level(addr);
    }

    @Override
    public long localHashPos(long addr) {
        return interleaveBits(_x(addr), _z(addr));
    }

    @Override
    public boolean intersects(long addr, @NonNull Volume volume) {
        double x = _x(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * HT_VOXELS;
        return volume.intersects(x * f, Integer.MIN_VALUE, z * f, (x + 1.0d) * f + d, Integer.MAX_VALUE, (z + 1.0d) * f + d);
    }

    @Override
    public boolean containedBy(long addr, @NonNull Volume volume) {
        double x = _x(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * HT_VOXELS;
        return volume.contains(x * f, Integer.MIN_VALUE, z * f, (x + 1.0d) * f + d, Integer.MAX_VALUE, (z + 1.0d) * f + d);
    }

    @Override
    public boolean inFrustum(long addr, @NonNull IFrustum frustum) {
        double x = _x(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * HT_VOXELS;
        return frustum.intersectsBB(x * f, Integer.MIN_VALUE, z * f, (x + 1.0d) * f + d, Integer.MAX_VALUE, (z + 1.0d) * f + d);
    }

    @Override
    public Set<HeightmapPos> newPositionSet() {
        return new HeightmapPosHashSet();
    }

    @Override
    public Set<HeightmapPos> clonePositionsAsSet(@NonNull Collection<HeightmapPos> src) {
        return new HeightmapPosHashSet(src);
    }

    @Override
    public List<HeightmapPos> newPositionList() {
        return new HeightmapPosArrayList();
    }

    @Override
    public List<HeightmapPos> newPositionList(int initialCapacity) {
        return new HeightmapPosArrayList(initialCapacity);
    }

    @Override
    public List<HeightmapPos> clonePositionsAsList(@NonNull Collection<HeightmapPos> src) {
        return new HeightmapPosArrayList(src);
    }
}
