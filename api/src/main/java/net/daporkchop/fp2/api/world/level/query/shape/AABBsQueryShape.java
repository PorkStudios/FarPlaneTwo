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
 */

package net.daporkchop.fp2.api.world.level.query.shape;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Describes the shape of a query consisting of multiple {@link IntAxisAlignedBB axis-aligned bounding boxes}.
 * <p>
 * A query shape consists of a sequence of {@link IntAxisAlignedBB axis-aligned bounding boxes}, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive).
 *
 * @author DaPorkchop_
 */
public interface AABBsQueryShape {
    /**
     * Ensures that this query's state is valid, throwing an exception if not.
     * <p>
     * If this method is not called and the shape's state is invalid, the behavior of all other methods is undefined.
     * <p>
     * It is recommended to call this once per method body before using a shape instance, as it could allow the JVM to optimize the code more aggressively.
     *
     * @throws RuntimeException if the shape's state is invalid
     */
    default void validate() throws RuntimeException {
        //no-op
    }

    /**
     * @return the number of voxels which will be read by this query
     */
    int count();

    /**
     * Gets the minimum X coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's minimum X coordinate
     */
    int minX(int index);

    /**
     * Gets the minimum Y coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's minimum Y coordinate
     */
    int minY(int index);

    /**
     * Gets the minimum Z coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's minimum Z coordinate
     */
    int minZ(int index);

    /**
     * Gets the maximum X coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's maximum X coordinate
     */
    int maxX(int index);

    /**
     * Gets the maximum Y coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's maximum Y coordinate
     */
    int maxY(int index);

    /**
     * Gets the maximum Z coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's maximum Z coordinate
     */
    int maxZ(int index);

    /**
     * Gets the {@code index}th bounding box.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * checkRangeLen(dst.length, 0, 6);
     * dst[0] = this.minX(index);
     * dst[1] = this.minY(index);
     * dst[2] = this.minZ(index);
     * dst[3] = this.maxX(index);
     * dst[4] = this.maxY(index);
     * dst[5] = this.maxZ(index);
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param index the bounding box's index
     * @param dst   an {@code int[]} to which the voxel position's minX,minY,minZ,maxX,maxY,maxZ coordinates will be written at indices {@code 0} through {@code 5},
     *              respectively
     * @throws IndexOutOfBoundsException if the length of {@code dst} is less than 6
     */
    default void aabb(int index, @NonNull int[] dst) {
        checkRangeLen(dst.length, 0, 6);
        dst[0] = this.minX(index);
        dst[1] = this.minY(index);
        dst[2] = this.minZ(index);
        dst[3] = this.maxX(index);
        dst[4] = this.maxY(index);
        dst[5] = this.maxZ(index);
    }

    /**
     * Gets the {@code index}th bounding box.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * return new IntAxisAlignedBB(
     *         this.minX(index), this.minY(index), this.minZ(index),
     *         this.maxX(index), this.maxY(index), this.maxZ(index));
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param index the bounding box's index
     * @return the bounding box
     */
    default IntAxisAlignedBB aabb(int index) {
        return new IntAxisAlignedBB(
                this.minX(index), this.minY(index), this.minZ(index),
                this.maxX(index), this.maxY(index), this.maxZ(index));
    }

    /**
     * Performs the given action for each bounding box in this shape until all bounding boxes have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each bounding box
     */
    default <T extends Throwable> void forEach(@NonNull BoundingBoxConsumer<T> action) throws T {
        int[] position = new int[6];
        for (int index = 0, count = this.count(); index < count; index++) {
            this.aabb(index, position);
            action.accept(index, position[0], position[1], position[2], position[3], position[4], position[5]);
        }
    }

    /**
     * Represents an operation that accepts a single voxel position (both its index and coordinate values) and returns no result.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface BoundingBoxConsumer<T extends Throwable> {
        /**
         * Accepts a single voxel position.
         *
         * @param index the voxel position index
         * @param minX  the voxel position's minimum X coordinate
         * @param minY  the voxel position's minimum Y coordinate
         * @param minZ  the voxel position's minimum Z coordinate
         * @param maxX  the voxel position's maximum X coordinate
         * @param maxY  the voxel position's maximum Y coordinate
         * @param maxZ  the voxel position's maximum Z coordinate
         */
        void accept(int index, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) throws T;
    }

    /**
     * A simple {@link AABBsQueryShape} which consists of a single bounding box.
     *
     * @author DaPorkchop_
     */
    @Data
    final class Single implements AABBsQueryShape {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        @Override
        public int count() {
            return 1;
        }

        @Override
        public int minX(int index) {
            checkIndex(1, index);
            return this.minX;
        }

        @Override
        public int minY(int index) {
            checkIndex(1, index);
            return this.minY;
        }

        @Override
        public int minZ(int index) {
            checkIndex(1, index);
            return this.minZ;
        }

        @Override
        public int maxX(int index) {
            checkIndex(1, index);
            return this.maxX;
        }

        @Override
        public int maxY(int index) {
            checkIndex(1, index);
            return this.maxY;
        }

        @Override
        public int maxZ(int index) {
            checkIndex(1, index);
            return this.maxZ;
        }

        @Override
        public void aabb(int index, @NonNull int[] dst) {
            checkIndex(1, index);
            checkRangeLen(dst.length, 0, 6);
            dst[0] = this.minX;
            dst[1] = this.minY;
            dst[2] = this.minZ;
            dst[3] = this.maxX;
            dst[4] = this.maxY;
            dst[5] = this.maxZ;
        }

        @Override
        public IntAxisAlignedBB aabb(int index) {
            checkIndex(1, index);
            return new IntAxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        }

        @Override
        public <T extends Throwable> void forEach(@NonNull BoundingBoxConsumer<T> action) throws T {
            action.accept(0, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        }
    }

    /**
     * A simple {@link AABBsQueryShape} which consists of multiple bounding boxes. Bounding boxes are defined by six user-provided arrays, two for each axis (min/max).
     * <p>
     * Bounding boxes are returned in the order they are present in the arrays.
     *
     * @author DaPorkchop_
     */
    @Data
    final class Multi implements AABBsQueryShape {
        @NonNull
        private final int[] minX;
        private final int minXOffset;
        private final int minXStride;

        @NonNull
        private final int[] minY;
        private final int minYOffset;
        private final int minYStride;

        @NonNull
        private final int[] minZ;
        private final int minZOffset;
        private final int minZStride;

        @NonNull
        private final int[] maxX;
        private final int maxXOffset;
        private final int maxXStride;

        @NonNull
        private final int[] maxY;
        private final int maxYOffset;
        private final int maxYStride;

        @NonNull
        private final int[] maxZ;
        private final int maxZOffset;
        private final int maxZStride;

        private final int count;

        @Override
        public void validate() throws RuntimeException {
            //make sure count is valid
            notNegative(this.count, "count");

            //make sure all the indices fit within the given arrays for the provided offset and stride
            if (this.count != 0) {
                checkRangeLen(this.minX.length, this.minXOffset, multiplyExact(positive(this.minXStride, "minXStride"), this.count) - this.minXOffset);
                checkRangeLen(this.minY.length, this.minYOffset, multiplyExact(positive(this.minYStride, "minYStride"), this.count) - this.minYOffset);
                checkRangeLen(this.minZ.length, this.minZOffset, multiplyExact(positive(this.minZStride, "minZStride"), this.count) - this.minZOffset);
                checkRangeLen(this.maxX.length, this.maxXOffset, multiplyExact(positive(this.maxXStride, "maxXStride"), this.count) - this.maxXOffset);
                checkRangeLen(this.maxY.length, this.maxYOffset, multiplyExact(positive(this.maxYStride, "maxYStride"), this.count) - this.maxYOffset);
                checkRangeLen(this.maxZ.length, this.maxZOffset, multiplyExact(positive(this.maxZStride, "maxZStride"), this.count) - this.maxZOffset);
            }
        }

        @Override
        public int minX(int index) {
            checkIndex(this.count, index);
            //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
            return this.minX[this.minXOffset + index * this.minXStride];
        }

        @Override
        public int minY(int index) {
            checkIndex(this.count, index);
            return this.minY[this.minYOffset + index * this.minYStride];
        }

        @Override
        public int minZ(int index) {
            checkIndex(this.count, index);
            return this.minZ[this.minZOffset + index * this.minZStride];
        }

        @Override
        public int maxX(int index) {
            checkIndex(this.count, index);
            return this.maxX[this.maxXOffset + index * this.maxXStride];
        }

        @Override
        public int maxY(int index) {
            checkIndex(this.count, index);
            return this.maxY[this.maxYOffset + index * this.maxYStride];
        }

        @Override
        public int maxZ(int index) {
            checkIndex(this.count, index);
            return this.maxZ[this.maxZOffset + index * this.maxZStride];
        }

        @Override
        public <T extends Throwable> void forEach(@NonNull BoundingBoxConsumer<T> action) throws T {
            this.validate();

            //iterate over every position
            for (int index = 0, minXIndex = this.minXOffset, minYIndex = this.minYOffset, minZIndex = this.minZOffset, maxXIndex = this.maxXOffset, maxYIndex = this.maxYOffset, maxZIndex = this.maxZOffset;
                 index < this.count;
                 index++, minXIndex += this.minXStride, minYIndex += this.minYStride, minZIndex += this.minZStride, maxXIndex += this.maxXStride, maxYIndex += this.maxYStride, maxZIndex += this.maxZStride) {
                action.accept(index, this.minX[minXIndex], this.minY[minYIndex], this.minZ[minZIndex], this.maxX[maxXIndex], this.maxY[maxYIndex], this.maxZ[maxZIndex]);
            }
        }
    }
}
