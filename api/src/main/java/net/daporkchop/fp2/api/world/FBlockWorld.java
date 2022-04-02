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

package net.daporkchop.fp2.api.world;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;

import static java.lang.Math.*;
import static java.util.Objects.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A read-only world consisting of voxels at integer coordinates.
 * <p>
 * Implementations <strong>must</strong> be thread-safe.
 * <p>
 * <h2>Voxel Data and Bands</h2>
 * <p>
 * Each voxel contains multiple independent data values, stored in separate channels ("bands"). The following bands are used:<br>
 * <table>
 *     <tr><th>Name</th><th>Type</th><th>Description</th></tr>
 *     <tr><td>{@link BlockWorldConstants#BAND_ORDINAL_STATES State}</td><td>{@code int}</td><td>A state ID, as returned by the corresponding methods in {@link FGameRegistry}</td></tr>
 *     <tr><td>{@link BlockWorldConstants#BAND_ORDINAL_BIOMES Biome}</td><td>{@code int}</td><td>A biome ID, as returned by the corresponding methods in {@link FGameRegistry}</td></tr>
 *     <tr><td>{@link BlockWorldConstants#BAND_ORDINAL_LIGHT Light}</td><td>{@code byte}</td><td>Block+sky light levels, as returned by {@link BlockWorldConstants#packLight(int, int)}</td></tr>
 * </table><br>
 * More bands may be added in the future as necessary.
 * <p>
 * <h2>Data Availability</h2>
 * <p>
 * Data at a given voxel position is in one of three possible availability states:<br>
 * <ul>
 *     <li><strong>Exists</strong>: the data at the given position exists and is accessible.</li>
 *     <li><strong>Ungenerated</strong>: the data at the given position has not yet been generated, and does not exist. Depending on whether or not the world
 *     {@link #generationAllowed() allows generation}, attempting to access ungenerated data will either cause the data to be generated (and therefore transition to
 *     <i>Exists</i> for future queries), or will cause {@link GenerationNotAllowedException} to be thrown.</li>
 *     <li><strong>Out-of-Bounds</strong>: the given position is outside of the world's {@link #dataLimits() coordinate limits} and cannot exist in any case. Attempts
 *     to access out-of-bounds data will always succeed, but will return some predetermined value (possibly dependent on the position) rather than "real" data. However,
 *     even though out-of-bounds data is always accessible, it is not considered by methods such as {@link #containsAnyData}, as they report <i>existence</i> of data
 *     rather than <i>accessibility</i> of data.</li>
 * </ul>
 * <p>
 * Data availability is per-position and not specific to individual bands.
 * <p>
 * <h2>Bulk Operations</h2>
 * <p>
 * All bulk operations write their output to arrays. 2-dimensional queries operate over a square area and write their output in {@code X,Z} coordinate order, while
 * 3-dimensional queries operate over a cubic volume and write their output in {@code X,Y,Z} coordinate order.
 * <p>
 * Additionally, bulk queries accept a {@code stride} parameter, which defines the spacing between sampled voxels. There are three distinct cases for this value:<br>
 * <ul>
 *     <li>All strides {@code <= 0} are invalid, and produce undefined behavior.</li>
 *     <li>A stride of exactly {@code 1} indicates that the voxels to be sampled are tightly packed. Implementations are required to return the exact information which would
 *     exist at the given position.</li>
 *     <li>All strides {@code > 1} indicate that voxels to be sampled have the given spacing between each voxel. Samples are taken every {@code stride} voxels, where each sample
 *     consists of a value which most accurately represents all of the voxels in the {@code stride²} area (for the 2-dimensional case) or {@code stride³} volume (for the 3-dimensional case).</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
public interface FBlockWorld extends AutoCloseable {
    /**
     * Closes this world, immediately releasing any internally allocated resources.
     * <p>
     * Once closed, all of this instance's methods will produce undefined behavior when called.
     * <p>
     * If not manually closed, a world will be implicitly closed when the instance is garbage-collected.
     */
    @Override
    void close();

    //
    // STATIC WORLD INFORMATION
    //

    /**
     * @return the {@link FGameRegistry} instance used by this world
     */
    FGameRegistry registry();

    /**
     * @return whether or not this world allows generating data which is not known
     */
    boolean generationAllowed();

    /**
     * Gets the AABB in which this world contains data.
     * <p>
     * This is not the volume in which queries may be made, but rather the volume in which valid data may be returned. Queries may be made at any valid 32-bit integer coordinates,
     * however all queries outside of the data limits will return some predetermined value.
     *
     * @return the AABB in which this world contains data
     */
    IntAxisAlignedBB dataLimits();

    //
    // DATA AVAILABILITY
    //

    /**
     * Checks whether or not <strong>any</strong> data in the given AABB exists.
     * <p>
     * Note that this checks for data which <i>exists</i>, not merely <i>accessible</i>. Voxel positions which are out-of-bounds or whose data could be generated if requested are
     * not considered by this.
     * <p>
     * Implementations <i>may</i> choose to allow false positives to be returned, but must <i>never</i> return false negatives.
     *
     * @param minX the minimum X coordinate (inclusive)
     * @param minY the minimum Y coordinate (inclusive)
     * @param minZ the minimum Z coordinate (inclusive)
     * @param maxX the maximum X coordinate (exclusive)
     * @param maxY the maximum Y coordinate (exclusive)
     * @param maxZ the maximum Z coordinate (exclusive)
     * @return whether or not any block data in the given AABB is known
     */
    boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    /**
     * Checks whether or not <strong>any</strong> data in the given AABB is known.
     *
     * @see #containsAnyData(int, int, int, int, int, int)
     */
    default boolean containsAnyData(@NonNull IntAxisAlignedBB bb) {
        return this.containsAnyData(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
    }

    /**
     * Gets the AABB in which block data is guaranteed to be accessible if all the block data in the given AABB is accessible.
     * <p>
     * In other words, if generation is disallowed and a query to every point in the given AABB would succeed, it is safe to assume that querying any point in the AABB(s)
     * returned by this method would be successful.
     * <p>
     * If the world is broken up into smaller pieces (chunks/cubes/columns/shards/whatever-you-want-to-call-them), accessing block data requires these pieces to be loaded
     * into memory. Depending on the size of pieces, the size of the query, and the query's alignment relative to the pieces it would load, much of the loaded data could
     * end up unused. This method provides a way for users to retrieve the volume of data which would actually be accessible for a given query, therefore allowing users to
     * increase their query size to minimize wasted resources. Since a single block in a piece being generated means that all of the data in the piece has been generated,
     * it is obviously safe to issue a query anywhere in the piece without causing issues.
     * <p>
     * Note that the AABB(s) returned by this method may extend beyond the world's {@link #dataLimits() data limits}.
     *
     * @param minX the minimum X coordinate (inclusive)
     * @param minY the minimum Y coordinate (inclusive)
     * @param minZ the minimum Z coordinate (inclusive)
     * @param maxX the maximum X coordinate (exclusive)
     * @param maxY the maximum Y coordinate (exclusive)
     * @param maxZ the maximum Z coordinate (exclusive)
     * @return the volume intersecting the given AABB which are guaranteed to be known if all the block data in the given AABB is known
     */
    default IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        //default: make no assumptions about internal data representation, return exactly the queried bounding box
        return new IntAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Gets the AABB in which block data is guaranteed to be accessible if all the block data in the given AABB is accessible.
     *
     * @see #guaranteedDataAvailableVolume(int, int, int, int, int, int)
     */
    default IntAxisAlignedBB guaranteedDataAvailableVolume(@NonNull IntAxisAlignedBB bb) {
        return this.guaranteedDataAvailableVolume(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
    }

    //
    // INDIVIDUAL DATA QUERIES
    //

    /**
     * Gets the state at the given voxel position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the state
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this world doesn't allow generation
     */
    int getState(int x, int y, int z) throws GenerationNotAllowedException;

    /**
     * Gets the biome at the given voxel position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the biome
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this world doesn't allow generation
     */
    int getBiome(int x, int y, int z) throws GenerationNotAllowedException;

    /**
     * Gets the packed block/sky light at the given voxel position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the packed light levels
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this world doesn't allow generation
     */
    byte getLight(int x, int y, int z) throws GenerationNotAllowedException;

    //
    // BULK DATA QUERIES
    //

    /**
     * @deprecated legacy API, use {@link #query}
     */
    @Deprecated
    default void getData(
            int[] states, int statesOffset, int statesStride,
            int[] biomes, int biomesOffset, int biomesStride,
            byte[] light, int lightOffset, int lightStride,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) throws GenerationNotAllowedException {
        //emulate AABB query as OriginSizeStrideQueryShape with stride 1
        this.getData(
                states, statesOffset, statesStride,
                biomes, biomesOffset, biomesStride,
                light, lightOffset, lightStride,
                minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ, 1, 1, 1);
    }

    /**
     * @deprecated legacy API, use {@link #query}
     */
    @Deprecated
    default void getData(
            int[] states, int statesOffset, int statesStride,
            int[] biomes, int biomesOffset, int biomesStride,
            byte[] light, int lightOffset, int lightStride,
            int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) throws GenerationNotAllowedException {
        //translate to new query api
        this.query(Query.of(
                new OriginSizeStrideQueryShape(x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ),
                new BandArraysQueryOutput(states, statesOffset, statesStride, biomes, biomesOffset, biomesStride, light, lightOffset, lightStride, sizeX * sizeY * sizeZ)));
    }

    /**
     * @deprecated legacy API, use {@link #query}
     */
    @Deprecated
    default void getData(
            int[] states, int statesOffset, int statesStride,
            int[] biomes, int biomesOffset, int biomesStride,
            byte[] light, int lightOffset, int lightStride,
            @NonNull int[] xs, int xOff, int xStride,
            @NonNull int[] ys, int yOff, int yStride,
            @NonNull int[] zs, int zOff, int zStride,
            int count) throws GenerationNotAllowedException {
        //translate to new query api
        this.query(Query.of(
                new MultiPointsQueryShape(xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count),
                new BandArraysQueryOutput(states, statesOffset, statesStride, biomes, biomesOffset, biomesStride, light, lightOffset, lightStride, count)));
    }

    /**
     * Issues a bulk data retrieval query.
     *
     * @param query the {@link Query query} which describes the position(s) to query the data from, which data bands should be accessed, and where the retrieved data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this world doesn't allow generation
     */
    default void query(@NonNull Query query) throws GenerationNotAllowedException {
        this.query(new Query[]{ query }); //delegate to bulk implementation
    }

    /**
     * Issues multiple bulk data retrieval queries.
     *
     * @param queries an array of {@link Query queries} which describe the position(s) to query the data from, which data bands should be accessed, and where the retrieved data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this world doesn't allow generation
     */
    default void query(@NonNull Query... queries) throws GenerationNotAllowedException {
        //ensure all queries are valid
        for (Query query : queries) {
            requireNonNull(query, "query").validate();
        }

        //dispatch queries
        for (Query query : queries) {
            query.validate(); //validate again to potentially help JIT

            QueryShape shape = query.shape();
            QueryOutput output = query.output();

            int enabledBands = output.enabledBands();
            shape.forEach((index, x, y, z) -> {
                //initialize variables to throwaway values (they won't be used if the band is disabled)
                int state = 0;
                int biome = 0;
                byte light = 0;

                //read values if corresponding bands are enabled
                if (BlockWorldConstants.isBandEnabled(enabledBands, BlockWorldConstants.BAND_ORDINAL_STATES)) {
                    state = this.getState(x, y, z);
                }
                if (BlockWorldConstants.isBandEnabled(enabledBands, BlockWorldConstants.BAND_ORDINAL_BIOMES)) {
                    biome = this.getBiome(x, y, z);
                }
                if (BlockWorldConstants.isBandEnabled(enabledBands, BlockWorldConstants.BAND_ORDINAL_LIGHT)) {
                    light = this.getLight(x, y, z);
                }

                //store values in query output
                output.setStateBiomesLight(index, state, biome, light);
            });
        }
    }

    /**
     * Describes a query's shape.
     * <p>
     * A query shape consists of a sequence of voxel positions, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). It describes the positions of the voxels which
     * the query will access, as well as the order in which the query's results will be written to the {@link QueryOutput output}.
     *
     * @author DaPorkchop_
     */
    interface QueryShape {
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
    }

    /**
     * A simple {@link QueryShape} which consists of a single position.
     *
     * @author DaPorkchop_
     */
    @Data
    final class SinglePointQueryShape implements QueryShape {
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
     * A simple {@link QueryShape} which consists of multiple positions. Positions are defined by three user-provided arrays, one for each axis.
     * <p>
     * Positions are returned in the order they are present in the arrays.
     *
     * @author DaPorkchop_
     */
    @Data
    final class MultiPointsQueryShape implements QueryShape {
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
            for (int index = 0, xIndex = this.xOffset, yIndex = this.yOffset, zIndex = this.zOffset; index < this.count; index++, xIndex += this.xStride, yIndex += this.yStride, zIndex += this.zStride) {
                action.accept(index, this.x[xIndex], this.y[yIndex], this.z[zIndex]);
            }
        }
    }

    /**
     * A simple {@link QueryShape} which consists of an origin position, and per-axis sample counts and strides.
     * <p>
     * Positions are returned in XYZ order.
     *
     * @author DaPorkchop_
     */
    @Data
    final class OriginSizeStrideQueryShape implements QueryShape {
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

    /**
     * Describes target for a query's result to be written to.
     * <p>
     * A query shape consists of a sequence of per-voxel data values, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). Each value is broken up into multiple
     * "bands", each of which contain a separate category of information. Bands may be enabled or disabled individually, allowing users to query only the data they're interested
     * in without wasting processing time reading unneeded values or allocating throwaway buffers.
     *
     * @author DaPorkchop_
     */
    interface QueryOutput {
        /**
         * Ensures that this output's state is valid, throwing an exception if not.
         * <p>
         * If this method is not called and the output's state is invalid, the behavior of all other methods is undefined.
         * <p>
         * It is recommended to call this once per method body before using an output instance, as it could allow the JVM to optimize the code more aggressively.
         *
         * @throws RuntimeException if the output's state is invalid
         */
        void validate() throws RuntimeException;

        /**
         * @return a bitfield indicating which bands are enabled for this output
         * @see BlockWorldConstants#isBandEnabled(int, int)
         */
        int enabledBands();

        /**
         * @return the number of output slots which will be read by this query
         */
        int count();

        /**
         * Sets the state value at the given output index.
         * <p>
         * If this output does not have the {@link BlockWorldConstants#BAND_ORDINAL_STATES "states" band} enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param state the new state value
         */
        void setState(int index, int state);

        /**
         * Sets the biome value at the given output index.
         * <p>
         * If this output does not have the {@link BlockWorldConstants#BAND_ORDINAL_BIOMES "biome" band} enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param biome the new biome value
         */
        void setBiome(int index, int biome);

        /**
         * Sets the light value at the given output index.
         * <p>
         * If this output does not have the {@link BlockWorldConstants#BAND_ORDINAL_LIGHT "light" band} enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param light the new light value
         */
        void setLight(int index, byte light);

        /**
         * Sets the values in multiple bands at the given output index.
         * <p>
         * Conceptually implemented by
         * <blockquote><pre>{@code
         * this.setState(index, state);
         * this.setBiome(index, biome);
         * this.setLight(index, light);
         * }</pre></blockquote>
         * except the implementation has the opportunity to optimize this beyond what the user could write.
         *
         * @param index the output index
         * @param state the new state value
         * @param biome the new biome value
         * @param light the new light value
         */
        default void setStateBiomesLight(int index, int state, int biome, byte light) {
            this.setState(index, state);
            this.setBiome(index, biome);
            this.setLight(index, light);
        }
    }

    /**
     * A simple {@link QueryOutput} consisting of a separate array for each band.
     * <p>
     * Each band array is described by the following:
     * <ul>
     *     <li>the array to which output is to be written, or {@code null} if the band is to be disabled</li>
     *     <li>the offset at which to begin writing values to the array. Should be {@code 0} to begin writing at the beginning of the array</li>
     *     <li>the stride between values written to the array. Should be {@code 1} for tightly-packed output</li>
     * </ul>
     *
     * @author DaPorkchop_
     */
    @Data
    final class BandArraysQueryOutput implements QueryOutput {
        private final int[] statesArray;
        private final int statesOffset;
        private final int statesStride;

        private final int[] biomesArray;
        private final int biomesOffset;
        private final int biomesStride;

        private final byte[] lightArray;
        private final int lightOffset;
        private final int lightStride;

        private final int count;

        @Override
        public void validate() throws RuntimeException {
            //make sure count is valid
            notNegative(this.count, "count");

            //make sure all the indices fit within the given arrays for the provided offset and stride
            if (this.count != 0) {
                if (this.statesArray != null) {
                    checkRangeLen(this.statesArray.length, this.statesOffset, multiplyExact(positive(this.statesStride, "statesStride"), this.count) - this.statesOffset);
                }
                if (this.biomesArray != null) {
                    checkRangeLen(this.biomesArray.length, this.biomesOffset, multiplyExact(positive(this.biomesStride, "biomesStride"), this.count) - this.biomesOffset);
                }
                if (this.lightArray != null) {
                    checkRangeLen(this.lightArray.length, this.lightOffset, multiplyExact(positive(this.lightStride, "lightStride"), this.count) - this.lightOffset);
                }
            }
        }

        @Override
        public int enabledBands() {
            int bands = 0;
            if (this.statesArray != null) {
                bands |= BlockWorldConstants.bandFlag(BlockWorldConstants.BAND_ORDINAL_STATES);
            }
            if (this.biomesArray != null) {
                bands |= BlockWorldConstants.bandFlag(BlockWorldConstants.BAND_ORDINAL_BIOMES);
            }
            if (this.lightArray != null) {
                bands |= BlockWorldConstants.bandFlag(BlockWorldConstants.BAND_ORDINAL_LIGHT);
            }
            return bands;
        }

        @Override
        public void setState(int index, int state) {
            checkIndex(this.count, index);
            if (this.statesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.statesArray[this.statesOffset + this.statesStride * index] = state;
            }
        }

        @Override
        public void setBiome(int index, int biome) {
            notNegative(index, "index");
            if (this.biomesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.biomesArray[this.biomesOffset + this.biomesStride * index] = biome;
            }
        }

        @Override
        public void setLight(int index, byte light) {
            notNegative(index, "index");
            if (this.lightArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.lightArray[this.lightOffset + this.lightStride * index] = light;
            }
        }

        @Override
        public void setStateBiomesLight(int index, int state, int biome, byte light) {
            notNegative(index, "index");
            if (this.statesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.statesArray[this.statesOffset + this.statesStride * index] = state;
            }
            if (this.biomesArray != null) {
                this.biomesArray[this.biomesOffset + this.biomesStride * index] = biome;
            }
            if (this.lightArray != null) {
                this.lightArray[this.lightOffset + this.lightStride * index] = light;
            }
        }
    }

    /**
     * Container object representing a bulk data query.
     * <p>
     * A query consists of:
     * <ul>
     *     <li>a {@link QueryShape} containing the positions of the voxels to get the values at</li>
     *     <li>a {@link QueryOutput} defining which values to read, and to which the retrieved values will be written</li>
     * </ul>
     *
     * @author DaPorkchop_
     */
    interface Query {
        static Query of(@NonNull QueryShape shape, @NonNull QueryOutput output) {
            return new Query() {
                @Override
                public void validate() throws RuntimeException {
                    shape.validate();
                    output.validate();

                    int shapeCount = shape.count();
                    int outputCount = output.count();
                    checkState(shapeCount >= outputCount, "shape contains %d points, but output only has space for %d!", shapeCount, outputCount);
                }

                @Override
                public QueryShape shape() {
                    return shape;
                }

                @Override
                public QueryOutput output() {
                    return output;
                }
            };
        }

        /**
         * Ensures that this query's state is valid, throwing an exception if not.
         * <p>
         * If this method is not called and the query's state is invalid, the behavior of all methods is undefined.
         * <p>
         * It is recommended to call this once per method body before using a query instance, as it could allow the JVM to optimize the code more aggressively.
         *
         * @throws RuntimeException if the query's state is invalid
         */
        void validate() throws RuntimeException;

        /**
         * @return the query's {@link QueryShape shape}
         */
        QueryShape shape();

        /**
         * @return the query's {@link QueryOutput output}
         */
        QueryOutput output();
    }
}
