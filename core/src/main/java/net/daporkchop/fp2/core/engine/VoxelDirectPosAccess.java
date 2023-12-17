/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.engine;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.core.engine.util.VoxelPosArrayList;
import net.daporkchop.fp2.core.engine.util.VoxelPosHashSet;
import net.daporkchop.fp2.core.util.math.geometry.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.engine.VoxelConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link IFarDirectPosAccess} for {@link VoxelPos}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class VoxelDirectPosAccess implements IFarDirectPosAccess<VoxelPos> {
    public static final VoxelDirectPosAccess INSTANCE = new VoxelDirectPosAccess();

    /*
     * struct Pos { // 16 bytes
     *   int x;
     *   int y;
     *   int z;
     *   int level;
     * };
     */

    public static final long _X_OFFSET = 0L;
    public static final long _Y_OFFSET = _X_OFFSET + INT_SIZE;
    public static final long _Z_OFFSET = _Y_OFFSET + INT_SIZE;
    public static final long _LEVEL_OFFSET = _Z_OFFSET + INT_SIZE;

    public static final long _SIZE = _LEVEL_OFFSET + INT_SIZE;

    public static int _x(long pos) {
        return PUnsafe.getInt(pos + _X_OFFSET);
    }

    public static int _x(Object base, long pos) {
        return PUnsafe.getInt(base, pos + _X_OFFSET);
    }

    public static void _x(long pos, int x) {
        PUnsafe.putInt(pos + _X_OFFSET, x);
    }

    public static void _x(Object base, long pos, int x) {
        PUnsafe.putInt(base, pos + _X_OFFSET, x);
    }

    public static int _y(long pos) {
        return PUnsafe.getInt(pos + _Y_OFFSET);
    }

    public static int _y(Object base, long pos) {
        return PUnsafe.getInt(base, pos + _Y_OFFSET);
    }

    public static void _y(long pos, int y) {
        PUnsafe.putInt(pos + _Y_OFFSET, y);
    }

    public static void _y(Object base, long pos, int y) {
        PUnsafe.putInt(base, pos + _Y_OFFSET, y);
    }

    public static int _z(long pos) {
        return PUnsafe.getInt(pos + _Z_OFFSET);
    }

    public static int _z(Object base, long pos) {
        return PUnsafe.getInt(base, pos + _Z_OFFSET);
    }

    public static void _z(long pos, int z) {
        PUnsafe.putInt(pos + _Z_OFFSET, z);
    }

    public static void _z(Object base, long pos, int z) {
        PUnsafe.putInt(base, pos + _Z_OFFSET, z);
    }

    public static int _level(long pos) {
        return PUnsafe.getInt(pos + _LEVEL_OFFSET);
    }

    public static int _level(Object base, long pos) {
        return PUnsafe.getInt(base, pos + _LEVEL_OFFSET);
    }

    public static void _level(long pos, int level) {
        PUnsafe.putInt(pos + _LEVEL_OFFSET, level);
    }

    public static void _level(Object base, long pos, int level) {
        PUnsafe.putInt(base, pos + _LEVEL_OFFSET, level);
    }

    @Override
    public int axisCount() {
        return 3;
    }

    @Override
    public long size() {
        return _SIZE;
    }

    @Override
    public void store(VoxelPos pos, long addr) {
        _x(addr, pos.x());
        _y(addr, pos.y());
        _z(addr, pos.z());
        _level(addr, pos.level());
    }

    @Override
    public void store(VoxelPos pos, Object base, long offset) {
        _x(base, offset, pos.x());
        _y(base, offset, pos.y());
        _z(base, offset, pos.z());
        _level(base, offset, pos.level());
    }

    @Override
    public VoxelPos load(long addr) {
        return new VoxelPos(_level(addr), _x(addr), _y(addr), _z(addr));
    }

    @Override
    public VoxelPos load(Object base, long offset) {
        return new VoxelPos(_level(base, offset), _x(base, offset), _y(base, offset), _z(base, offset));
    }

    @Override
    public int getAxisHeap(@NonNull VoxelPos pos, int axis) {
        switch (axis) {
            case 0:
                return pos.x();
            case 1:
                return pos.y();
            case 2:
                return pos.z();
            default:
                throw new IllegalArgumentException("invalid axis number: " + axis);
        }
    }

    @Override
    public int getAxisDirect(long addr, int axis) {
        return PUnsafe.getInt(addr + (long) checkIndex(3, axis) * INT_SIZE);
    }

    @Override
    public boolean equalsPos(long addr1, long addr2) {
        return _x(addr1) == _x(addr2)
               && _y(addr1) == _y(addr2)
               && _z(addr1) == _z(addr2)
               && _level(addr1) == _level(addr2);
    }

    @Override
    public int hashPos(long addr) {
        return _x(addr) * 1317194159 + _y(addr) * 1964379643 + _z(addr) * 1656858407 + _level(addr);
    }

    @Override
    public long localHashPos(long addr) {
        return interleaveBits(_x(addr), _y(addr), _z(addr));
    }

    @Override
    public boolean intersects(long addr, @NonNull Volume volume) {
        double x = _x(addr);
        double y = _y(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * VT_VOXELS;
        return volume.intersects(x * f, y * f, z * f, (x + 1.0d) * f + d, (y + 1.0d) * f + d, (z + 1.0d) * f + d);
    }

    @Override
    public boolean containedBy(long addr, @NonNull Volume volume) {
        double x = _x(addr);
        double y = _y(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * VT_VOXELS;
        return volume.contains(x * f, y * f, z * f, (x + 1.0d) * f + d, (y + 1.0d) * f + d, (z + 1.0d) * f + d);
    }

    @Override
    public boolean inFrustum(long addr, @NonNull IFrustum frustum) {
        double x = _x(addr);
        double y = _y(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * VT_VOXELS;
        return frustum.intersectsBB(x * f, y * f, z * f, (x + 1.0d) * f + d, (y + 1.0d) * f + d, (z + 1.0d) * f + d);
    }

    @Override
    public Set<VoxelPos> newPositionSet() {
        return new VoxelPosHashSet();
    }

    @Override
    public Set<VoxelPos> clonePositionsAsSet(@NonNull Collection<VoxelPos> src) {
        return new VoxelPosHashSet(src);
    }

    @Override
    public List<VoxelPos> newPositionList() {
        return new VoxelPosArrayList();
    }

    @Override
    public List<VoxelPos> newPositionList(int initialCapacity) {
        return new VoxelPosArrayList(initialCapacity);
    }

    @Override
    public List<VoxelPos> clonePositionsAsList(@NonNull Collection<VoxelPos> src) {
        return new VoxelPosArrayList(src);
    }
}
