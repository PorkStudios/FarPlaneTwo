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
import net.daporkchop.fp2.api.world.level.query.DataQueryBatchOutput;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Describes a query's shape.
 * <p>
 * A query shape consists of a sequence of voxel positions, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). It describes the positions of the voxels which
 * the query will access, as well as the order in which the query's results will be written to the {@link DataQueryBatchOutput output}.
 *
 * @author DaPorkchop_
 */
public interface PointsQueryShape {
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
     * Gets the X coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's X coordinate
     */
    int x(int index);

    /**
     * Gets the Y coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's Y coordinate
     */
    int y(int index);

    /**
     * Gets the Z coordinate of the {@code index}th voxel position.
     *
     * @param index the voxel position's index
     * @return the voxel position's Z coordinate
     */
    int z(int index);

    /**
     * Gets the coordinates of the {@code index}th voxel position.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * PValidation.checkRangeLen(dst.length, 0, 3);
     * dst[0] = this.x(index);
     * dst[1] = this.y(index);
     * dst[2] = this.z(index);
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param index the voxel position's index
     * @param dst   an {@code int[]} to which the voxel position's X,Y,Z coordinates will be written at indices {@code 0}, {@code 1} and {@code 2}, respectively
     * @throws IndexOutOfBoundsException if the length of {@code dst} is less than 3
     */
    default void position(int index, @NonNull int[] dst) {
        checkRangeLen(dst.length, 0, 3);
        dst[0] = this.x(index);
        dst[1] = this.y(index);
        dst[2] = this.z(index);
    }

    /**
     * Performs the given action for each voxel position in this shape until all positions have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each voxel position
     */
    default <T extends Throwable> void forEach(@NonNull VoxelPositionConsumer<T> action) throws T {
        int[] position = new int[3];
        for (int index = 0, count = this.count(); index < count; index++) {
            this.position(index, position);
            action.accept(index, position[0], position[1], position[2]);
        }
    }

    /**
     * Represents an operation that accepts a single voxel position (both its index and coordinate values) and returns no result.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface VoxelPositionConsumer<T extends Throwable> {
        /**
         * Accepts a single voxel position.
         *
         * @param index the voxel position index
         * @param x     the voxel position's X coordinate
         * @param y     the voxel position's Y coordinate
         * @param z     the voxel position's Z coordinate
         */
        void accept(int index, int x, int y, int z) throws T;
    }

    /**
     * A simple {@link PointsQueryShape} which consists of a single position.
     *
     * @author DaPorkchop_
     */
    @Data
    final class Single implements PointsQueryShape {
        private final int x;
        private final int y;
        private final int z;

        @Override
        public int count() {
            return 1;
        }

        @Override
        public int x(int index) {
            checkIndex(1, index);
            return this.x;
        }

        @Override
        public int y(int index) {
            checkIndex(1, index);
            return this.y;
        }

        @Override
        public int z(int index) {
            checkIndex(1, index);
            return this.z;
        }

        @Override
        public void position(int index, @NonNull int[] dst) {
            checkIndex(1, index);
            checkRangeLen(dst.length, 0, 3);
            dst[0] = this.x;
            dst[1] = this.y;
            dst[2] = this.z;
        }

        @Override
        public <T extends Throwable> void forEach(@NonNull VoxelPositionConsumer<T> action) throws T {
            action.accept(0, this.x, this.y, this.z);
        }
    }

    /**
     * A simple {@link PointsQueryShape} which consists of multiple positions. Positions are defined by three user-provided arrays, one for each axis.
     * <p>
     * Positions are returned in the order they are present in the arrays.
     *
     * @author DaPorkchop_
     */
    @Data
    final class Multi implements PointsQueryShape {
        @NonNull
        private final int[] x;
        private final int xOffset;
        private final int xStride;

        @NonNull
        private final int[] y;
        private final int yOffset;
        private final int yStride;

        @NonNull
        private final int[] z;
        private final int zOffset;
        private final int zStride;

        private final int count;

        @Override
        public void validate() throws RuntimeException {
            //make sure count is valid
            notNegative(this.count, "count");

            //make sure all the indices fit within the given arrays for the provided offset and stride
            if (this.count != 0) {
                checkRangeLen(this.x.length, this.xOffset, multiplyExact(positive(this.xStride, "xStride"), this.count) - this.xOffset);
                checkRangeLen(this.y.length, this.yOffset, multiplyExact(positive(this.yStride, "yStride"), this.count) - this.yOffset);
                checkRangeLen(this.z.length, this.zOffset, multiplyExact(positive(this.zStride, "zStride"), this.count) - this.zOffset);
            }
        }

        @Override
        public int x(int index) {
            checkIndex(this.count, index);
            //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
            return this.x[this.xOffset + index * this.xStride];
        }

        @Override
        public int y(int index) {
            checkIndex(this.count, index);
            //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
            return this.y[this.yOffset + index * this.yStride];
        }

        @Override
        public int z(int index) {
            checkIndex(this.count, index);
            //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
            return this.z[this.zOffset + index * this.zStride];
        }

        @Override
        public <T extends Throwable> void forEach(@NonNull VoxelPositionConsumer<T> action) throws T {
            this.validate();

            //iterate over every position
            for (int index = 0, xIndex = this.xOffset, yIndex = this.yOffset, zIndex = this.zOffset;
                 index < this.count;
                 index++, xIndex += this.xStride, yIndex += this.yStride, zIndex += this.zStride) {
                action.accept(index, this.x[xIndex], this.y[yIndex], this.z[zIndex]);
            }
        }
    }

    /**
     * A simple {@link PointsQueryShape} which consists of an origin position, and per-axis sample counts and strides.
     * <p>
     * Positions are returned in XYZ order.
     *
     * @author DaPorkchop_
     */
    @Data
    final class OriginSizeStride implements PointsQueryShape {
        private final int originX;
        private final int originY;
        private final int originZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final int strideX;
        private final int strideY;
        private final int strideZ;

        @Override
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public void validate() throws RuntimeException {
            //make sure the sizes can be multiplied together to form an exclusive upper index bound without overflow
            multiplyExact(multiplyExact(notNegative(this.sizeX, "sizeX"), notNegative(this.sizeY, "sizeY")), notNegative(this.sizeZ, "sizeZ"));

            //make sure there will be no overflow when computing the actual voxel positions
            addExact(this.originX, multiplyExact(notNegative(this.strideX, "strideX"), this.sizeX));
            addExact(this.originY, multiplyExact(notNegative(this.strideY, "strideY"), this.sizeY));
            addExact(this.originZ, multiplyExact(notNegative(this.strideZ, "strideZ"), this.sizeZ));
        }

        @Override
        public int count() {
            //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
            return this.sizeX * this.sizeY * this.sizeZ;
        }

        @Override
        public int x(int index) {
            checkIndex(this.count(), index);
            return this.originX + (index / (this.sizeY * this.sizeZ)) * this.strideX;
        }

        @Override
        public int y(int index) {
            checkIndex(this.count(), index);
            return this.originY + (index / this.sizeZ % this.sizeY) * this.strideY;
        }

        @Override
        public int z(int index) {
            checkIndex(this.count(), index);
            return this.originZ + (index % this.sizeZ) * this.strideZ;
        }

        @Override
        public <T extends Throwable> void forEach(@NonNull VoxelPositionConsumer<T> action) throws T {
            this.validate();

            //iterate over every position!
            // we don't have to worry about overflows: validate() would have thrown an exception if it were possible to overflow
            for (int index = 0, dx = 0; dx < this.sizeX; dx++) {
                for (int dy = 0; dy < this.sizeY; dy++) {
                    for (int dz = 0; dz < this.sizeZ; dz++, index++) {
                        action.accept(index, this.originX + dx * this.strideX, this.originY + dy * this.strideY, this.originZ + dz * this.strideZ);
                    }
                }
            }
        }
    }
}
