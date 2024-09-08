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

package net.daporkchop.fp2.core.util.datastructure;

import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.core.util.BreakOutOfLambdaException;
import net.daporkchop.lib.primitive.lambda.IntIntConsumer;
import net.daporkchop.lib.primitive.lambda.IntIntIntConsumer;

import java.util.Arrays;
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
    int size();

    /**
     * @return whether or not this set is empty
     */
    default boolean isEmpty() {
        return this.size() == 0;
    }

    /**
     * Removes every point in this set.
     */
    void clear();

    /**
     * @return a clone of this set
     */
    NDimensionalIntSet clone();

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

    /**
     * Counts the number of elements in the given coordinate range.
     *
     * @param begin the minimum coordinates to search (inclusive)
     * @param end   the maximum coordinates to search (exclusive)
     * @return the number of elements in the given coordinate range
     */
    default int countInRange(@NonNull int[] begin, @NonNull int[] end) {
        int dimensions = this.dimensions();
        {
            checkArg(dimensions == begin.length, "mismatched dimension count (this: %dD, begin: %dD)", dimensions, begin.length);
            checkArg(dimensions == end.length, "mismatched dimension count (this: %dD, end: %dD)", dimensions, end.length);
        }

        for (int i = 0; i < dimensions; i++) {
            checkArg(begin[i] <= end[i], "begin#%s (%s) may not be greater than end#%s (%s)", i, begin[i], i, end[i]);
        }

        for (int i = 0; i < dimensions; i++) { //break out early if the range is empty
            if (begin[i] == end[i]) {
                return 0;
            }
        }

        int result = 0;
        int[] point = begin.clone();
        while (true) { //keep iterating until we reach the end point
            if (this.contains(point)) {
                result++;
            }

            //advance to the next point
            INCREMENT:
            {
                for (int i = 0; i < dimensions - 1; i++) {
                    if (++point[i] < end[i]) {
                        break INCREMENT;
                    } else {
                        point[i] = begin[i];
                    }
                }

                if (++point[dimensions - 1] >= end[dimensions - 1]) { //special handling for last coordinate, since rather than wrapping around it should terminate the loop when reached
                    return result;
                }
            }
        }
    }

    /**
     * Adds all of the elements in the given coordinate range to this set.
     *
     * @param begin the minimum coordinates to add (inclusive)
     * @param end   the maximum coordinates to add (exclusive)
     * @return the number of elements which were newly added
     */
    default int addAllInRange(@NonNull int[] begin, @NonNull int[] end) {
        int dimensions = this.dimensions();
        {
            checkArg(dimensions == begin.length, "mismatched dimension count (this: %dD, begin: %dD)", dimensions, begin.length);
            checkArg(dimensions == end.length, "mismatched dimension count (this: %dD, end: %dD)", dimensions, end.length);
        }

        for (int i = 0; i < dimensions; i++) {
            checkArg(begin[i] <= end[i], "begin#%s (%s) may not be greater than end#%s (%s)", i, begin[i], i, end[i]);
        }

        for (int i = 0; i < dimensions; i++) { //break out early if the range is empty
            if (begin[i] == end[i]) {
                return 0;
            }
        }

        int result = 0;
        int[] point = begin.clone();
        while (true) { //keep iterating until we reach the end point
            if (this.add(point)) {
                result++;
            }

            //advance to the next point
            INCREMENT:
            {
                for (int i = 0; i < dimensions - 1; i++) {
                    if (++point[i] < end[i]) {
                        break INCREMENT;
                    } else {
                        point[i] = begin[i];
                    }
                }

                if (++point[dimensions - 1] >= end[dimensions - 1]) { //special handling for last coordinate, since rather than wrapping around it should terminate the loop when reached
                    return result;
                }
            }
        }
    }

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

    /**
     * Counts the number of elements in the given coordinate range.
     *
     * @param beginX the minimum X coordinate to search (inclusive)
     * @param endX   the maximum X coordinate to search (exclusive)
     * @return the number of elements in the given coordinate range
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 1
     */
    default int countInRange(int beginX, int endX) {
        return this.countInRange(new int[]{ beginX }, new int[]{ endX });
    }

    /**
     * Counts the number of elements in the given coordinate range.
     *
     * @param beginX the minimum X coordinate to search (inclusive)
     * @param beginY the minimum Y coordinate to search (inclusive)
     * @param endX   the maximum X coordinate to search (exclusive)
     * @param endY   the maximum Y coordinate to search (exclusive)
     * @return the number of elements in the given coordinate range
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     */
    default int countInRange(int beginX, int beginY, int endX, int endY) {
        return this.countInRange(new int[]{ beginX, beginY }, new int[]{ endX, endY });
    }

    /**
     * Counts the number of elements in the given coordinate range.
     *
     * @param beginX the minimum X coordinate to search (inclusive)
     * @param beginY the minimum Y coordinate to search (inclusive)
     * @param beginZ the minimum Z coordinate to search (inclusive)
     * @param endX   the maximum X coordinate to search (exclusive)
     * @param endY   the maximum Y coordinate to search (exclusive)
     * @param endZ   the maximum Z coordinate to search (exclusive)
     * @return the number of elements in the given coordinate range
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 3
     */
    default int countInRange(int beginX, int beginY, int beginZ, int endX, int endY, int endZ) {
        return this.countInRange(new int[]{ beginX, beginY, beginZ }, new int[]{ endX, endY, endZ });
    }

    /**
     * Adds all of the elements in the given coordinate range to this set.
     *
     * @param beginX the minimum X coordinate to add (inclusive)
     * @param endX   the maximum X coordinate to add (exclusive)
     * @return the number of elements which were newly added
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 1
     */
    default int addAllInRange(int beginX, int endX) {
        return this.addAllInRange(new int[]{ beginX }, new int[]{ endX });
    }

    /**
     * Adds all of the elements in the given coordinate range to this set.
     *
     * @param beginX the minimum X coordinate to add (inclusive)
     * @param beginY the minimum Y coordinate to add (inclusive)
     * @param endX   the maximum X coordinate to add (exclusive)
     * @param endY   the maximum Y coordinate to add (exclusive)
     * @return the number of elements which were newly added
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 2
     */
    default int addAllInRange(int beginX, int beginY, int endX, int endY) {
        return this.addAllInRange(new int[]{ beginX, beginY }, new int[]{ endX, endY });
    }

    /**
     * Adds all of the elements in the given coordinate range to this set.
     *
     * @param beginX the minimum X coordinate to add (inclusive)
     * @param beginY the minimum Y coordinate to add (inclusive)
     * @param beginZ the minimum Z coordinate to add (inclusive)
     * @param endX   the maximum X coordinate to add (exclusive)
     * @param endY   the maximum Y coordinate to add (exclusive)
     * @param endZ   the maximum Z coordinate to add (exclusive)
     * @return the number of elements which were newly added
     * @throws IllegalArgumentException if this set's dimensionality is not equal to 3
     */
    default int addAllInRange(int beginX, int beginY, int beginZ, int endX, int endY, int endZ) {
        return this.addAllInRange(new int[]{ beginX, beginY, beginZ }, new int[]{ endX, endY, endZ });
    }

    //
    // bulk operations
    //

    /**
     * Checks whether or not this set contains every point in the given {@link NDimensionalIntSet}
     *
     * @param set the {@link NDimensionalIntSet} containing the points to check for
     * @return whether or not this set contains every point in the given {@link NDimensionalIntSet}
     */
    default boolean containsAll(@NonNull NDimensionalIntSet set) {
        {
            int thisDimensions = this.dimensions();
            int otherDimensions = set.dimensions();
            checkArg(thisDimensions == otherDimensions, "mismatched dimension count (this: %dD, set: %dD)", thisDimensions, otherDimensions);
        }

        try {
            //check every point
            set.forEach(point -> {
                if (!this.contains(point)) {
                    throw BreakOutOfLambdaException.get();
                }
            });

            //every point was contained
            return true;
        } catch (BreakOutOfLambdaException e) {
            //a point wasn't contained, return false
            return false;
        }
    }

    /**
     * Adds every point in the given {@link NDimensionalIntSet} to this set.
     *
     * @param set the {@link NDimensionalIntSet} containing the points to add
     * @return whether or not this set was modified as a result of this operation (i.e. whether or not any points were added)
     */
    default boolean addAll(@NonNull NDimensionalIntSet set) {
        {
            int thisDimensions = this.dimensions();
            int otherDimensions = set.dimensions();
            checkArg(thisDimensions == otherDimensions, "mismatched dimension count (this: %dD, set: %dD)", thisDimensions, otherDimensions);
        }

        //local class contains the return value without having to allocate a second object to get the return value
        class State implements Consumer<int[]> {
            boolean modified = false;

            @Override
            public void accept(int[] point) {
                //try to add each point and update the "modified" flag if successful
                if (NDimensionalIntSet.this.add(point)) {
                    this.modified = true;
                }
            }
        }

        State state = new State();
        set.forEach(state);
        return state.modified;
    }

    /**
     * Removes every point in the given {@link NDimensionalIntSet} from this set.
     *
     * @param set the {@link NDimensionalIntSet} containing the points to remove
     * @return whether or not this set was modified as a result of this operation (i.e. whether or not any points were removed)
     */
    default boolean removeAll(@NonNull NDimensionalIntSet set) {
        {
            int thisDimensions = this.dimensions();
            int otherDimensions = set.dimensions();
            checkArg(thisDimensions == otherDimensions, "mismatched dimension count (this: %dD, set: %dD)", thisDimensions, otherDimensions);
        }

        //local class contains the return value without having to allocate a second object to get the return value
        class State implements Consumer<int[]> {
            boolean modified = false;

            @Override
            public void accept(int[] point) {
                //try to remove each point and update the "modified" flag if successful
                if (NDimensionalIntSet.this.remove(point)) {
                    this.modified = true;
                }
            }
        }

        State state = new State();
        set.forEach(state);
        return state.modified;
    }

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
