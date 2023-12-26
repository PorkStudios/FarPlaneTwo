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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@ToString
public class TilePos implements Comparable<TilePos> {
    protected final int level;
    protected final int x;
    protected final int y;
    protected final int z;

    /**
     * @return the level of detail at this position
     */
    public final int level() {
        return this.level;
    }

    public final int x() {
        return this.x;
    }

    public final int y() {
        return this.y;
    }

    public final int z() {
        return this.z;
    }

    /**
     * @return whether or not this position's level is valid
     */
    public final boolean isLevelValid() {
        return this.level >= 0 && this.level < MAX_LODS;
    }

    public final int blockX() {
        return this.x * T_VOXELS << this.level;
    }

    public final int blockY() {
        return this.y * T_VOXELS << this.level;
    }

    public final int blockZ() {
        return this.z * T_VOXELS << this.level;
    }

    public final int sideLength() {
        return T_VOXELS << this.level;
    }

    public final int flooredChunkX() {
        return this.blockX() >> 4;
    }

    public final int flooredChunkY() {
        return this.blockY() >> 4;
    }

    public final int flooredChunkZ() {
        return this.blockZ() >> 4;
    }

    public final IntAxisAlignedBB bb() {
        return new IntAxisAlignedBB(
                this.x << (T_SHIFT + this.level), this.y << (T_SHIFT + this.level), this.z << (T_SHIFT + this.level),
                (this.x + 1) << (T_SHIFT + this.level), (this.y + 1) << (T_SHIFT + this.level), (this.z + 1) << (T_SHIFT + this.level));
    }

    /**
     * Gets the {@link TilePos} containing this position at the given lower level of detail.
     *
     * @param targetLevel the level of detail to go up to
     * @return the {@link TilePos} containing this position at the given lower level of detail
     */
    public final TilePos upTo(int targetLevel) {
        if (targetLevel == this.level) {
            return this;
        }
        checkArg(targetLevel > this.level, "targetLevel (%d) must be greater than current level (%d)", targetLevel, this.level);

        int shift = targetLevel - this.level;
        return new TilePos(targetLevel, this.x >> shift, this.y >> shift, this.z >> shift);
    }

    /**
     * @return the {@link TilePos} containing this position at a lower level of detail
     */
    public final TilePos up() {
        return new TilePos(this.level + 1, this.x >> 1, this.y >> 1, this.z >> 1);
    }

    /**
     * Gets the {@link TilePos} containing this position at the given higher level of detail.
     *
     * @param targetLevel the level of detail to go down to
     * @return the {@link TilePos} containing this position at the given higher level of detail
     */
    public final TilePos downTo(int targetLevel) {
        if (targetLevel == this.level) {
            return this;
        }
        checkArg(targetLevel < this.level, "targetLevel (%d) must be less than current level (%d)", targetLevel, this.level);

        int shift = this.level - targetLevel;
        return new TilePos(targetLevel, this.x << shift, this.y << shift, this.z << shift);
    }

    /**
     * @return the {@link TilePos} containing this position at a higher level of detail
     */
    public final TilePos down() {
        return new TilePos(this.level - 1, this.x << 1, this.y << 1, this.z << 1);
    }

    /**
     * Checks whether or not this position contains the given {@link TilePos}.
     *
     * @param pos the {@link TilePos} to check
     * @return whether or not this position contains the given {@link TilePos}
     */
    public final boolean contains(@NonNull TilePos pos) {
        int d = this.level - pos.level;
        return d > 0
               && (this.x << d) >= pos.x && ((this.x + 1) << d) <= pos.x
               && (this.y << d) >= pos.y && ((this.y + 1) << d) <= pos.y
               && (this.z << d) >= pos.z && ((this.z + 1) << d) <= pos.z;
    }

    /**
     * Gets a {@link Stream} containing all the unique positions in the a bounding box originating at this position.
     * <p>
     * The bounding box's corners are defined by adding/subtracting the given max/min offsets to this position's coordinates, respectively.
     * <p>
     * Both corners are inclusive.
     */
    public final Stream<TilePos> allPositionsInBB(int offsetMin, int offsetMax) {
        notNegative(offsetMin, "offsetMin");
        notNegative(offsetMax, "offsetMax");

        if (cb((long) offsetMin + offsetMax + 1L) < 256L) { //fast-track: fill an array and create a simple stream over that
            TilePos[] arr = new TilePos[cb(offsetMin + offsetMax + 1)];
            for (int i = 0, dx = -offsetMin; dx <= offsetMax; dx++) {
                for (int dy = -offsetMin; dy <= offsetMax; dy++) {
                    for (int dz = -offsetMin; dz <= offsetMax; dz++) {
                        arr[i++] = new TilePos(this.level, this.x + dx, this.y + dy, this.z + dz);
                    }
                }
            }
            return Stream.of(arr);
        } else { //slower fallback: dynamically computed stream
            return IntStream.rangeClosed(this.x - offsetMin, this.x + offsetMax)
                    .mapToObj(x -> IntStream.rangeClosed(this.y - offsetMin, this.y + offsetMax)
                            .mapToObj(y -> IntStream.rangeClosed(this.z - offsetMin, this.z + offsetMax)
                                    .mapToObj(z -> new TilePos(this.level, x, y, z)))
                            .flatMap(Function.identity()))
                    .flatMap(Function.identity());
        }
    }

    /**
     * Gets the Manhattan distance to the given {@link TilePos}.
     * <p>
     * If the positions are at different detail levels, the distance is approximate.
     * <p>
     * Note that the distance may not be returned in blocks, but rather in arbitrary units. The only constraint is that it must remain consistent across
     * levels (such that the distance between any two positions at the same level is always twice the distance between the positions with the same axis
     * values one level lower).
     *
     * @param pos the {@link TilePos} to get the distance to
     * @return the Manhattan distance to the given {@link TilePos}
     */
    public final int manhattanDistance(@NonNull TilePos pos) {
        if (this.level == pos.level) {
            return abs(this.x - pos.x) + abs(this.y - pos.y) + abs(this.z - pos.z);
        } else {
            int l0 = this.level;
            int l1 = pos.level;
            int s0 = max(l1 - l0, 0);
            int s1 = max(l0 - l1, 0);
            int s2 = max(s0, s1);
            return (abs((this.x >> s0) - (pos.x >> s1)) << s2)
                   + (abs((this.y >> s0) - (pos.y >> s1)) << s2)
                   + (abs((this.z >> s0) - (pos.z >> s1)) << s2);
        }
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof TilePos) {
            TilePos pos = (TilePos) obj;
            return this.x == pos.x && this.y == pos.y && this.z == pos.z && this.level == pos.level;
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return this.x * 1317194159 + this.y * 1964379643 + this.z * 1656858407 + this.level;
    }

    /**
     * @return a locality-sensitive hash of this position
     */
    public final long localHash() {
        return interleaveBits(this.x, this.y, this.z);
    }

    /**
     * Compares two positions in some arbitrary manner.
     * <p>
     * The function may be implemented in any way, but must be consistent and must only return {@code 0} for positions that are also considered
     * identical by {@link Object#equals(Object)}.
     * <p>
     * Failure to implement this correctly will result in a deadlock of the rendering threads!
     *
     * @see Comparable#compareTo(Object)
     */
    public final int compareTo(TilePos pos) {
        int d = Integer.compare(this.level, pos.level());
        if (d == 0 && (d = Integer.compare(this.x, pos.x)) == 0 && (d = Integer.compare(this.z, pos.z)) == 0) {
            d = Integer.compare(this.y, pos.y);
        }
        return d;
    }
}
