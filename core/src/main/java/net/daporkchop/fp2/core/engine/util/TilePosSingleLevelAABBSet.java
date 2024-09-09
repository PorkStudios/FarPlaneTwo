/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.util;

import lombok.NonNull;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.util.datastructure.simple.SimpleSet;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Read-only implementation of {@link Set} which can store an AABB of {@link TilePos}.
 *
 * @author DaPorkchop_
 */
public final class TilePosSingleLevelAABBSet extends SimpleSet<TilePos> {
    private final int level;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    /**
     * Creates a new {@link TilePosSingleLevelAABBSet} from the AABB defined by the given corner positions.
     *
     * @param min the minimum position (inclusive)
     * @param max the maximum position (exclusive)
     * @throws IllegalArgumentException if the given tile positions are not at the same level
     * @throws IllegalArgumentException if any of the minimum coordinates are greater than the corresponding maximum coordinates
     */
    public static TilePosSingleLevelAABBSet createExclusive(@NonNull TilePos min, @NonNull TilePos max) {
        checkArg(min.level() == max.level(), "mismatched levels (min=%s, max=%s)", min, max);
        checkArg(min.x() <= max.x() && min.y() <= max.y() && min.z() <= max.z(), "min (%s) may not be greater than max (%s)", min, max);

        return new TilePosSingleLevelAABBSet(min.level(),
                min.x(), min.y(), min.z(),
                max.x(), max.y(), max.z());
    }

    /**
     * Creates a new {@link TilePosSingleLevelAABBSet} from the AABB defined by the given corner positions.
     * <p>
     * Note that this does not permit empty AABBs.
     *
     * @param min the minimum position (inclusive)
     * @param max the maximum position (inclusive)
     * @throws IllegalArgumentException if the given tile positions are not at the same level
     * @throws IllegalArgumentException if any of the minimum coordinates are greater than the corresponding maximum coordinates
     */
    public static TilePosSingleLevelAABBSet createInclusive(@NonNull TilePos min, @NonNull TilePos max) {
        checkArg(min.level() == max.level(), "mismatched levels (min=%s, max=%s)", min, max);
        checkArg(min.x() <= max.x() && min.y() <= max.y() && min.z() <= max.z(), "min (%s) may not be greater than max (%s)", min, max);

        return new TilePosSingleLevelAABBSet(min.level(),
                min.x(), min.y(), min.z(),
                Math.incrementExact(max.x()), Math.incrementExact(max.y()), Math.incrementExact(max.z()));
    }

    private TilePosSingleLevelAABBSet(int level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        //sanity check: this should already be verified above but i want to make sure
        assert minX <= maxX && minY <= maxY && minZ <= maxZ
                : "min (" + minX + ", " + minY + ", " + minZ + ") may not be greater than max (" + maxX + ", " + maxY + ", " + maxZ + ')';

        //this will throw an exception if the size would overflow
        int sizeX = Math.subtractExact(maxX, minX);
        int sizeY = Math.subtractExact(maxY, minY);
        int sizeZ = Math.subtractExact(maxZ, minZ);
        int ignored = Math.multiplyExact(Math.multiplyExact(sizeX, sizeY), sizeZ);

        this.level = level;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Override
    public int size() {
        return (this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ);
    }

    @Override
    public boolean isEmpty() {
        return this.minX == this.maxX || this.minY == this.maxY || this.minZ == this.maxZ;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(TilePos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object value) {
        if (value instanceof TilePos) {
            TilePos pos = (TilePos) value;

            return pos.level() == this.level
                   && this.minX <= pos.x() && pos.x() < this.maxX
                   && this.minY <= pos.y() && pos.y() < this.maxY
                   && this.minZ <= pos.z() && pos.z() < this.maxZ;
        } else {
            return false;
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super TilePos> callback) {
        int level = this.level;
        int minX = this.minX;
        int minY = this.minY;
        int minZ = this.minZ;
        int maxX = this.maxX;
        int maxY = this.maxY;
        int maxZ = this.maxZ;
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    callback.accept(new TilePos(level, x, y, z));
                }
            }
        }
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        //TODO
        return super.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends TilePos> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }
}
