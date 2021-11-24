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

package net.daporkchop.fp2.core.mode.api;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;

import java.util.stream.Stream;

/**
 * An identifier for a {@link IFarTile}.
 *
 * @author DaPorkchop_
 */
public interface IFarPos extends Comparable<IFarPos> {
    /**
     * @return the level of detail at this position
     */
    int level();

    /**
     * Gets the {@link IFarPos} containing this position at the given lower level of detail.
     *
     * @param targetLevel the level of detail to go up to
     * @return the {@link IFarPos} containing this position at the given lower level of detail
     */
    IFarPos upTo(int targetLevel);

    /**
     * @return the {@link IFarPos} containing this position at a lower level of detail
     */
    default IFarPos up() {
        return this.upTo(this.level() + 1);
    }

    /**
     * Gets the {@link IFarPos} containing this position at the given higher level of detail.
     *
     * @param targetLevel the level of detail to go down to
     * @return the {@link IFarPos} containing this position at the given higher level of detail
     */
    IFarPos downTo(int targetLevel);

    /**
     * @return the {@link IFarPos} containing this position at a higher level of detail
     */
    default IFarPos down() {
        return this.downTo(this.level() - 1);
    }

    /**
     * Writes this position to the given {@link ByteBuf}.
     * <p>
     * The written data must be deserializable by this position's render mode's {@link IFarRenderMode#readPos(ByteBuf)} method.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    void writePos(@NonNull ByteBuf dst);

    /**
     * Converts this position to a {@code byte[]}.
     * <p>
     * The resulting {@code byte[]}'s contents are identical to the data written by {@link #writePos(ByteBuf)}.
     *
     * @return the encoded position
     */
    byte[] toBytes();

    /**
     * Checks whether or not this position contains the given {@link IFarPos}.
     *
     * @param posIn the {@link IFarPos} to check
     * @return whether or not this position contains the given {@link IFarPos}
     */
    boolean contains(@NonNull IFarPos posIn);

    /**
     * Checks whether or not this tile position is contained by the given tile coordinate limits.
     *
     * @param coordLimits the {@link IntAxisAlignedBB} representing the tile coordinate limits
     * @return whether or not this tile position is contained by the given tile coordinate limits
     */
    boolean containedBy(@NonNull IntAxisAlignedBB coordLimits);

    /**
     * Checks whether or not this tile position is contained by the given tile coordinate limits.
     *
     * @param coordLimits the {@link IntAxisAlignedBB}s representing the tile coordinate limits, indexed by detail level
     * @return whether or not this tile position is contained by the given tile coordinate limits
     * @throws ArrayIndexOutOfBoundsException if this position's {@link #level()} is not a valid index in the given {@code coordLimits} array
     */
    default boolean containedBy(@NonNull IntAxisAlignedBB[] coordLimits) {
        return this.containedBy(coordLimits[this.level()]);
    }

    /**
     * Gets a {@link Stream} containing all the unique positions in the a bounding box originating at this position.
     * <p>
     * The bounding box's corners are defined by adding/subtracting the given max/min offsets to this position's coordinates, respectively.
     * <p>
     * Both corners are inclusive.
     */
    Stream<? extends IFarPos> allPositionsInBB(int offsetMin, int offsetMax);

    /**
     * Gets the Manhattan distance to the given {@link IFarPos}.
     * <p>
     * If the positions are at different detail levels, the distance is approximate.
     * <p>
     * Note that the distance may not be returned in blocks, but rather in arbitrary units. The only constraint is that it must remain consistent across
     * levels (such that the distance between any two positions at the same level is always twice the distance between the positions with the same axis
     * values one level lower).
     *
     * @param posIn the {@link IFarPos} to get the distance to
     * @return the Manhattan distance to the given {@link IFarPos}
     */
    int manhattanDistance(@NonNull IFarPos posIn);

    /**
     * @return a locality-sensitive hash of this position
     */
    long localHash();

    /**
     * Compares two positions in some arbitrary manner.
     * <p>
     * The function may be implemented in any way, but must be consistent and must only return {@code 0} for positions that are also considered
     * identical by {@link #equals(Object)}.
     * <p>
     * Failure to implement this correctly will result in a deadlock of the rendering threads!
     *
     * @see Comparable#compareTo(Object)
     */
    @Override
    int compareTo(IFarPos o);
}
