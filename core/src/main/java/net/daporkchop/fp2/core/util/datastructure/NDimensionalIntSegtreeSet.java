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

package net.daporkchop.fp2.core.util.datastructure;

import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.lib.common.function.throwing.ESupplier;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.stream.Stream;

import static java.util.Objects.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An extension of {@link NDimensionalIntSet} which is implemented using a segment tree in order to allow efficient queries over arbitrary bounding volumes.
 *
 * @author DaPorkchop_
 */
public interface NDimensionalIntSegtreeSet extends NDimensionalIntSet {
    //
    // generic methods
    //

    /**
     * Checks whether or not this set contains any points in the AABB defined by the two given points.
     * <p>
     * Both points are inclusive.
     *
     * @param a one of the bounding box's corners
     * @param b one of the bounding box's corners
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to the dimensionality of either of the given points
     */
    boolean containsAny(@NonNull int[] a, @NonNull int[] b);

    /**
     * Checks whether or not this set contains any points in the AABB defined by the given point left-shifted by the given amount (inclusive), and
     * the given point, incremented by 1 and then left-shifted by the given amount (exclusive).
     *
     * @param shift the number of bits to shift by
     * @param point the origin point
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to the dimensionality of the given point
     */
    boolean containsAny(int shift, @NonNull int... point);

    //
    // special cases
    //

    /**
     * Checks whether or not this set contains any points in the AABB defined by the given point left-shifted by the given amount (inclusive), and
     * the given point, incremented by 1 and then left-shifted by the given amount (exclusive).
     *
     * @param shift the number of bits to shift by
     * @param x     the origin point's X coordinate
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 1
     */
    default boolean containsAny(int shift, int x) {
        return this.containsAny(shift, new int[]{ x });
    }

    /**
     * Checks whether or not this set contains any points in the AABB defined by the given point left-shifted by the given amount (inclusive), and
     * the given point, incremented by 1 and then left-shifted by the given amount (exclusive).
     *
     * @param shift the number of bits to shift by
     * @param x     the origin point's X coordinate
     * @param y     the origin point's Y coordinate
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     */
    default boolean containsAny(int shift, int x, int y) {
        return this.containsAny(shift, new int[]{ x, y });
    }

    /**
     * Checks whether or not this set contains any points in the AABB defined by the given point left-shifted by the given amount (inclusive), and
     * the given point, incremented by 1 and then left-shifted by the given amount (exclusive).
     *
     * @param shift the number of bits to shift by
     * @param x the origin point's X coordinate
     * @param y the origin point's Y coordinate
     * @param z the origin point's Z coordinate
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 3
     */
    default boolean containsAny(int shift, int x, int y, int z) {
        return this.containsAny(shift, new int[]{ x, y, z });
    }

    @Override
    int refCnt();

    @Override
    NDimensionalIntSegtreeSet retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;

    @Setter
    abstract class Builder extends IDatastructure.Builder<Builder, NDimensionalIntSegtreeSet> {
        @NonNull
        protected Integer dimensions;
        protected ESupplier<Stream<int[]>> initialPoints;

        @Override
        protected void validate() {
            positive(requireNonNull(this.dimensions, "dimensions must be set"), "dimensions");
        }
    }
}
