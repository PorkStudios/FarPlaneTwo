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

package net.daporkchop.fp2.core.util.datastructure;

import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.lib.common.function.throwing.ESupplier;

import java.util.stream.Stream;

import static java.util.Objects.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An extension of {@link NDimensionalIntSet} which is implemented using a segment tree in order to allow efficient queries over arbitrary bounding volumes.
 *
 * @author DaPorkchop_
 */
public interface NDimensionalIntSegtreeSet extends NDimensionalIntSet {
    /**
     * @return a clone of this set
     */
    @Override
    NDimensionalIntSegtreeSet clone();

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

    //
    // special cases
    //

    /**
     * Checks whether or not this set contains any points in the AABB defined by the two given points.
     * <p>
     * Both points are inclusive.
     *
     * @param x0 the X coordinate of one of the bounding box's corners
     * @param x1 the X coordinate of one of the bounding box's corners
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 1
     */
    default boolean containsAny(int x0, int x1) {
        return this.containsAny(new int[]{ x0 }, new int[]{ x1 });
    }

    /**
     * Checks whether or not this set contains any points in the AABB defined by the two given points.
     * <p>
     * Both points are inclusive.
     *
     * @param x0 the X coordinate of one of the bounding box's corners
     * @param y0 the Y coordinate of one of the bounding box's corners
     * @param x1 the X coordinate of one of the bounding box's corners
     * @param y1 the Y coordinate of one of the bounding box's corners
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     */
    default boolean containsAny(int x0, int y0, int x1, int y1) {
        return this.containsAny(new int[]{ x0, y0 }, new int[]{ x1, y1 });
    }

    /**
     * Checks whether or not this set contains any points in the AABB defined by the two given points.
     * <p>
     * Both points are inclusive.
     *
     * @param x0 the X coordinate of one of the bounding box's corners
     * @param y0 the Y coordinate of one of the bounding box's corners
     * @param z0 the Z coordinate of one of the bounding box's corners
     * @param x1 the X coordinate of one of the bounding box's corners
     * @param y1 the Y coordinate of one of the bounding box's corners
     * @param z1 the Z coordinate of one of the bounding box's corners
     * @return whether or not this set contains any points in the given AABB
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     */
    default boolean containsAny(int x0, int y0, int z0, int x1, int y1, int z1) {
        return this.containsAny(new int[]{ x0, y0, z0 }, new int[]{ x1, y1, z1 });
    }

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
