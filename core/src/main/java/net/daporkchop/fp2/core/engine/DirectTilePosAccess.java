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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.engine.util.TilePosArrayList;
import net.daporkchop.fp2.core.engine.util.TilePosHashSet;
import net.daporkchop.fp2.core.util.math.geometry.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;

/**
 * Helper class for storing {@link TilePos} instances off-heap.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class DirectTilePosAccess {
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

    /**
     * @return the off-heap size of a position, in bytes
     */
    public static long size() {
        return _SIZE;
    }

    /**
     * Stores a position off-heap at the given memory address.
     *
     * @param pos  the position
     * @param addr the memory address
     */
    public static void store(TilePos pos, long addr) {
        _x(addr, pos.x());
        _y(addr, pos.y());
        _z(addr, pos.z());
        _level(addr, pos.level());
    }

    public static void store(TilePos pos, Object base, long offset) {
        _x(base, offset, pos.x());
        _y(base, offset, pos.y());
        _z(base, offset, pos.z());
        _level(base, offset, pos.level());
    }

    /**
     * Loads the position at the give memory address onto the Java heap.
     *
     * @param addr the memory address
     * @return the position
     */
    public static TilePos load(long addr) {
        return new TilePos(_level(addr), _x(addr), _y(addr), _z(addr));
    }

    public static TilePos load(Object base, long offset) {
        return new TilePos(_level(base, offset), _x(base, offset), _y(base, offset), _z(base, offset));
    }

    /**
     * Checks the positions at the given memory addresses for equality.
     * <p>
     * Functionally identical to {@code loadPos(addr1).equals(loadPos(addr2))}.
     *
     * @return whether or not the two positions are equal
     */
    public static boolean equalsPos(long addr1, long addr2) {
        return _x(addr1) == _x(addr2)
               && _y(addr1) == _y(addr2)
               && _z(addr1) == _z(addr2)
               && _level(addr1) == _level(addr2);
    }

    /**
     * Hashes the position at the given memory address.
     * <p>
     * Functionally identical to {@code loadPos(addr).localHash()}.
     *
     * @param addr the memory address
     * @return the position's hash
     */
    public static int hashPos(long addr) {
        return _x(addr) * 1317194159 + _y(addr) * 1964379643 + _z(addr) * 1656858407 + _level(addr);
    }

    /**
     * Hashes the position at the given memory address.
     * <p>
     * Functionally identical to {@code loadPos(addr).hashCode()}.
     *
     * @param addr the memory address
     * @return the position's locality-sensitive hash
     */
    public static long localHashPos(long addr) {
        return interleaveBits(_x(addr), _y(addr), _z(addr));
    }

    /**
     * Checks whether or not the tile at the given position intersects the given volume.
     *
     * @param addr   the memory address of the off-heap position
     * @param volume the volume
     * @return whether or not the tile at the given position intersects the given volume
     */
    public static boolean intersects(long addr, @NonNull Volume volume) {
        double x = _x(addr);
        double y = _y(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * T_VOXELS;
        return volume.intersects(x * f, y * f, z * f, (x + 1.0d) * f + d, (y + 1.0d) * f + d, (z + 1.0d) * f + d);
    }

    /**
     * Checks whether or not the tile at the given position is contained by the given volume.
     *
     * @param addr   the memory address of the off-heap position
     * @param volume the volume
     * @return whether or not the tile at the given position is contained by the given volume
     */
    public static boolean containedBy(long addr, @NonNull Volume volume) {
        double x = _x(addr);
        double y = _y(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * T_VOXELS;
        return volume.contains(x * f, y * f, z * f, (x + 1.0d) * f + d, (y + 1.0d) * f + d, (z + 1.0d) * f + d);
    }

    /**
     * Checks whether or not the tile at the given position is in the given frustum.
     *
     * @param addr    the memory address of the off-heap position
     * @param frustum the frustum
     * @return whether or not the tile at the given position is in the given frustum
     */
    public static boolean inFrustum(long addr, @NonNull IFrustum frustum) {
        double x = _x(addr);
        double y = _y(addr);
        double z = _z(addr);

        double d = 1 << _level(addr);
        double f = d * T_VOXELS;
        return frustum.intersectsBB(x * f, y * f, z * f, (x + 1.0d) * f + d, (y + 1.0d) * f + d, (z + 1.0d) * f + d);
    }

    /**
     * @return a new {@link Set} which can store positions of type {@link TilePos}
     */
    public static Set<TilePos> newPositionSet() {
        return new TilePosHashSet();
    }

    /**
     * Creates a new {@link Set} which can store positions of type {@link TilePos} and adds all the positions in the given {@link Collection} to it.
     *
     * @param src the {@link Collection} to clone
     * @return a new {@link Set} which can store positions of type {@link TilePos} and contains all the positions from the given {@link Collection}
     */
    public static Set<TilePos> clonePositionsAsSet(@NonNull Collection<TilePos> src) {
        return new TilePosHashSet(src);
    }

    /**
     * @return a new {@link List} which can store positions of type {@link TilePos}
     */
    public static List<TilePos> newPositionList() {
        return new TilePosArrayList();
    }

    /**
     * @param initialCapacity the initial size of the list
     * @return a new {@link List} which can store positions of type {@link TilePos}
     */
    public static List<TilePos> newPositionList(int initialCapacity) {
        return new TilePosArrayList(initialCapacity);
    }

    /**
     * Creates a new {@link List} which can store positions of type {@link TilePos} and adds all the positions in the given {@link Collection} to it.
     *
     * @param src the {@link Collection} to clone
     * @return a new {@link List} which can store positions of type {@link TilePos} and contains all the positions from the given {@link Collection}
     */
    public static List<TilePos> clonePositionsAsList(@NonNull Collection<TilePos> src) {
        return new TilePosArrayList(src);
    }

    /**
     * @param <V> the type of value to store in the {@link Map}
     * @return a new {@link Map} which uses {@link TilePos} as a key
     */
    public static <V> Map<TilePos, V> newPositionKeyedMap() {
        return new Object2ObjectOpenHashMap<>();
    }

    /**
     * @param initialCapacity the initial capacity of the map
     * @param <V> the type of value to store in the {@link Map}
     * @return a new {@link Map} which uses {@link TilePos} as a key
     */
    public static <V> Map<TilePos, V> newPositionKeyedMap(int initialCapacity) {
        return new Object2ObjectOpenHashMap<>(initialCapacity);
    }

    /**
     * @param <V> the type of value to store in the {@link ConcurrentMap}
     * @return a new {@link ConcurrentMap} which uses {@link TilePos} as a key
     */
    public static <V> ConcurrentMap<TilePos, V> newPositionKeyedConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * @param initialCapacity the initial capacity of the map
     * @param <V> the type of value to store in the {@link ConcurrentMap}
     * @return a new {@link ConcurrentMap} which uses {@link TilePos} as a key
     */
    public static <V> ConcurrentMap<TilePos, V> newPositionKeyedConcurrentMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }
}
