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

package net.daporkchop.fp2.util.datastructure;

import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;
import net.daporkchop.lib.primitive.lambda.IntIntConsumer;
import net.daporkchop.lib.primitive.lambda.IntIntIntConsumer;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.util.Objects.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A set of N-dimensional points, where each point consists of N {@code int} coordinates.
 *
 * @author DaPorkchop_
 */
public interface NDimensionalIntSet extends IDatastructure<NDimensionalIntSet> {
    /**
     * @return this set's dimensionality
     */
    int dimensions();

    /**
     * @return the number of points in this set
     */
    long count();

    /**
     * @return whether or not this set is empty
     */
    default boolean isEmpty() {
        return this.count() == 0L;
    }

    /**
     * Removes every point in this set.
     */
    void clear();

    //
    // generic methods
    //

    /**
     * Adds the given point to this set.
     *
     * @param point the point to add
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to the dimensionality of the given point
     */
    boolean add(@NonNull int... point);

    /**
     * Removes the given point from this set.
     *
     * @param point the point to remove
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to the dimensionality of the given point
     */
    boolean remove(@NonNull int... point);

    /**
     * Checks whether or not this set contains the given point.
     *
     * @param point the point to check for
     * @return whether or not this set contains the given point
     * @throws IllegalArgumentException if this set's dimensionality is not equal to the dimensionality of the given point
     */
    boolean contains(@NonNull int... point);

    /**
     * Runs the given callback function on every point in this set.
     *
     * @param callback the callback function
     */
    void forEach(@NonNull Consumer<int[]> callback);

    //
    // special cases
    //

    /**
     * Adds the given 1D point to this set.
     *
     * @param x the point's X coordinate
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 1
     * @see #add(int...)
     */
    default boolean add(int x) {
        return this.add(new int[]{ x });
    }

    /**
     * Adds the given 2D point to this set.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     * @see #add(int...)
     */
    default boolean add(int x, int y) {
        return this.add(new int[]{ x, y });
    }

    /**
     * Adds the given 3D point to this set.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @param z the point's Z coordinate
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 3
     * @see #add(int...)
     */
    default boolean add(int x, int y, int z) {
        return this.add(new int[]{ x, y, z });
    }

    /**
     * Removes the given 1D point from this set.
     *
     * @param x the point's X coordinate
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 1
     */
    default boolean remove(int x) {
        return this.remove(new int[]{ x });
    }

    /**
     * Removes the given 2D point from this set.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     */
    default boolean remove(int x, int y) {
        return this.remove(new int[]{ x, y });
    }

    /**
     * Removes the given 3D point from this set.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @param z the point's Z coordinate
     * @return whether or not this set was modified as a result of this change
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 3
     */
    default boolean remove(int x, int y, int z) {
        return this.remove(new int[]{ x, y, z });
    }

    /**
     * Checks whether or not this set contains the given 1D point.
     *
     * @param x the point's X coordinate
     * @return whether or not this set contains the given point
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 1
     */
    default boolean contains(int x) {
        return this.contains(new int[]{ x });
    }

    /**
     * Checks whether or not this set contains the given 2D point.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @return whether or not this set contains the given point
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     */
    default boolean contains(int x, int y) {
        return this.contains(new int[]{ x, y });
    }

    /**
     * Checks whether or not this set contains the given 3D point.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @param z the point's Z coordinate
     * @return whether or not this set contains the given point
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 3
     */
    default boolean contains(int x, int y, int z) {
        return this.contains(new int[]{ x, y, z });
    }

    /**
     * Runs the given callback function on every 1D point in this set.
     *
     * @param callback the callback function
     */
    default void forEach1D(@NonNull IntConsumer callback) {
        this.forEach(coords -> {
            checkArg(coords.length == 1, "1D callback for %dD set!", coords.length);
            callback.accept(coords[0]);
        });
    }

    /**
     * Runs the given callback function on every 2D point in this set.
     *
     * @param callback the callback function
     */
    default void forEach2D(@NonNull IntIntConsumer callback) {
        this.forEach(coords -> {
            checkArg(coords.length == 2, "2D callback for %dD set!", coords.length);
            callback.accept(coords[0], coords[1]);
        });
    }

    /**
     * Runs the given callback function on every 3D point in this set.
     *
     * @param callback the callback function
     */
    default void forEach3D(@NonNull IntIntIntConsumer callback) {
        this.forEach(coords -> {
            checkArg(coords.length == 3, "3D callback for %dD set!", coords.length);
            callback.accept(coords[0], coords[1], coords[2]);
        });
    }

    @Override
    int refCnt();

    @Override
    NDimensionalIntSet retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;

    @Setter
    abstract class Builder extends IDatastructure.Builder<Builder, NDimensionalIntSet> {
        @NonNull
        protected Integer dimensions;

        @Override
        protected void validate() {
            positive(requireNonNull(this.dimensions, "dimensions must be set!"), "dimensions");
        }
    }
}
