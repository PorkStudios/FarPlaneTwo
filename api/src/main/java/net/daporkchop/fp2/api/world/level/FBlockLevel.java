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

package net.daporkchop.fp2.api.world.level;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.ThreadSafe;

import java.util.Collection;

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
 *     <tr><td>{@link BlockLevelConstants#DATA_BAND_ORDINAL_STATES State}</td><td>{@code int}</td><td>A state ID, as returned by the corresponding methods in {@link FGameRegistry}</td></tr>
 *     <tr><td>{@link BlockLevelConstants#DATA_BAND_ORDINAL_BIOMES Biome}</td><td>{@code int}</td><td>A biome ID, as returned by the corresponding methods in {@link FGameRegistry}</td></tr>
 *     <tr><td>{@link BlockLevelConstants#DATA_BAND_ORDINAL_LIGHT Light}</td><td>{@code byte}</td><td>Block+sky light levels, as returned by {@link BlockLevelConstants#packLight(int, int)}</td></tr>
 * </table><br>
 * More bands may be added in the future as necessary.
 * <p>
 * <h2>Data Availability</h2>
 * <p>
 * Data at a given voxel position is in one of three possible availability states:<br>
 * <ul>
 *     <li><strong>Exists</strong>: the data at the given position exists and is accessible.</li>
 *     <li><strong>Ungenerated</strong>: the data at the given position has not yet been generated, and does not exist. Depending on whether the world
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
@ThreadSafe(except = "close")
public interface FBlockLevel extends AutoCloseable {
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
     * @return whether this world allows generating data which is not known
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
     * Checks whether <strong>any</strong> data in the given AABB exists.
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
     * @return whether any block data in the given AABB is known
     */
    boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    /**
     * Checks whether <strong>any</strong> data in the given AABB is known.
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
     * Issues a bulk data retrieval query.
     *
     * @param query the {@link DataQuery query} which describes the position(s) to query the data from, which data bands should be accessed, and where the retrieved data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this world doesn't allow generation
     */
    default void query(@NonNull DataQuery query) throws GenerationNotAllowedException {
        this.query(new DataQuery[]{ query }); //delegate to bulk implementation
    }

    /**
     * Issues multiple bulk data retrieval queries.
     *
     * @param queries an array of {@link DataQuery queries} which describe the position(s) to query the data from, which data bands should be accessed, and where the retrieved data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this world doesn't allow generation
     */
    default void query(@NonNull DataQuery... queries) throws GenerationNotAllowedException {
        //ensure all queries are valid
        for (DataQuery query : queries) {
            requireNonNull(query, "query").validate();
        }

        //dispatch queries
        for (DataQuery query : queries) {
            query.validate(); //validate again to potentially help JIT

            DataQueryShape shape = query.shape();
            DataQueryOutput output = query.output();

            int enabledBands = output.enabledBands();
            shape.forEach((index, x, y, z) -> {
                //initialize variables to throwaway values (they won't be used if the data band is disabled)
                int state = 0;
                int biome = 0;
                byte light = 0;

                //read values if corresponding bands are enabled
                if (BlockLevelConstants.isDataBandEnabled(enabledBands, BlockLevelConstants.DATA_BAND_ORDINAL_STATES)) {
                    state = this.getState(x, y, z);
                }
                if (BlockLevelConstants.isDataBandEnabled(enabledBands, BlockLevelConstants.DATA_BAND_ORDINAL_BIOMES)) {
                    biome = this.getBiome(x, y, z);
                }
                if (BlockLevelConstants.isDataBandEnabled(enabledBands, BlockLevelConstants.DATA_BAND_ORDINAL_LIGHT)) {
                    light = this.getLight(x, y, z);
                }

                //store values in query output
                output.setStateBiomesLight(index, state, biome, light);
            });
        }
    }

    //
    // INDIVIDUAL TYPE TRANSITION SEARCH QUERIES
    //

    /**
     * Searches for the next transition(s) from one block type to another, starting at the given position (exclusive) and iterating in the given direction. The query will
     * continue searching until one of the following events occurs:<br>
     * <ul>
     *     <li>one of the given {@link TypeTransitionQueryInterestFilter filters}' {@link TypeTransitionQueryInterestFilter#abortAfterHitCount abortAfterHitCount} limit
     *     is exceeded</li>
     *     <li>the {@link TypeTransitionOutput}'s capacity is exceeded, and no more output can be stored in it</li>
     *     <li>the current position goes beyond this level's {@link #dataLimits() data limits}</li>
     * </ul>
     * <p>
     * When a transition between block types is encountered, it is checked against all the given {@link TypeTransitionQueryInterestFilter filters}. If it matches at
     * least one filter, the transition's type transition descriptor and the XYZ coordinates of the block being transitioned to are stored in the next available index of
     * the given {@link TypeTransitionOutput output}.
     * <p>
     * The type transitions between data which exists and data which doesn't exist, as well as between non-existent voxels are not defined. If a query requires the
     * implementation to access voxel data which doesn't exist, this could result in, among other things, seemingly "impossible" sequences of transitions (e.g. two
     * transitions from {@link BlockLevelConstants#BLOCK_TYPE_INVISIBLE invisible} to {@link BlockLevelConstants#BLOCK_TYPE_OPAQUE} in a row with no other transitions
     * between them). To avoid this problem, the {@link TypeTransitionOutput output} has a special output band: the
     * {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA "skipped NoData" type transition query output band}, which is an array of
     * {@code boolean}s. A value of {@code true} for an individual transition indicates the implementation may have skipped one or more non-existent voxels prior to
     * encountering the current transition.
     *
     * @param direction the direction to iterate in
     * @param x         the X coordinate to begin iteration from
     * @param y         the Y coordinate to begin iteration from
     * @param z         the Z coordinate to begin iteration from
     * @param filters   a {@link Collection} of the {@link TypeTransitionQueryInterestFilter filters} to use for selecting which type transitions to include in the
     *                  {@link TypeTransitionOutput output}. Type transitions which at least one of the filters will be written to the output. If the given
     *                  {@link Collection} is {@link Collection#isEmpty() empty}, no filtering will be applied.
     * @param output    a {@link TypeTransitionOutput output} to store the encountered type transitions in
     * @return the number of elements written to the {@link TypeTransitionOutput output}
     */
    int getNextTypeTransitions(@NonNull Direction direction, int x, int y, int z,
                               @NonNull Collection<@NonNull TypeTransitionQueryInterestFilter> filters,
                               @NonNull TypeTransitionOutput output);

    //
    // BULK TYPE TRANSITION SEARCH QUERIES
    //

    //
    // DATA QUERY CLASSES
    //

    /**
     * Describes a query's shape.
     * <p>
     * A query shape consists of a sequence of voxel positions, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). It describes the positions of the voxels which
     * the query will access, as well as the order in which the query's results will be written to the {@link DataQueryOutput output}.
     *
     * @author DaPorkchop_
     */
    interface DataQueryShape {
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
     * A simple {@link DataQueryShape} which consists of a single position.
     *
     * @author DaPorkchop_
     */
    @Data
    final class SinglePointDataQueryShape implements DataQueryShape {
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
     * A simple {@link DataQueryShape} which consists of multiple positions. Positions are defined by three user-provided arrays, one for each axis.
     * <p>
     * Positions are returned in the order they are present in the arrays.
     *
     * @author DaPorkchop_
     */
    @Data
    final class MultiPointsDataQueryShape implements DataQueryShape {
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
     * A simple {@link DataQueryShape} which consists of an origin position, and per-axis sample counts and strides.
     * <p>
     * Positions are returned in XYZ order.
     *
     * @author DaPorkchop_
     */
    @Data
    final class OriginSizeStrideDataQueryShape implements DataQueryShape {
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
    interface DataQueryOutput {
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
         * @see BlockLevelConstants#isDataBandEnabled(int, int)
         */
        int enabledBands();

        /**
         * @return the number of output slots which will be read by this query
         */
        int count();

        /**
         * Sets the state value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#DATA_BAND_ORDINAL_STATES "states" data band} enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param state the new state value
         */
        void setState(int index, int state);

        /**
         * Sets the biome value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#DATA_BAND_ORDINAL_BIOMES "biome" data band} enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param biome the new biome value
         */
        void setBiome(int index, int biome);

        /**
         * Sets the light value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#DATA_BAND_ORDINAL_LIGHT "light" data band} enabled, the value will be silently discarded.
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
     * A simple {@link DataQueryOutput} consisting of a separate array for each data band.
     * <p>
     * Each data band array is described by the following:
     * <ul>
     *     <li>the array to which output is to be written, or {@code null} if the data band is to be disabled</li>
     *     <li>the offset at which to begin writing values to the array. Should be {@code 0} to begin writing at the beginning of the array</li>
     *     <li>the stride between values written to the array. Should be {@code 1} for tightly-packed output</li>
     * </ul>
     *
     * @author DaPorkchop_
     */
    @Data
    final class BandArraysDataQueryOutput implements DataQueryOutput {
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
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.DATA_BAND_ORDINAL_STATES);
            }
            if (this.biomesArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.DATA_BAND_ORDINAL_BIOMES);
            }
            if (this.lightArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.DATA_BAND_ORDINAL_LIGHT);
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
            checkIndex(this.count, index);
            if (this.biomesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.biomesArray[this.biomesOffset + this.biomesStride * index] = biome;
            }
        }

        @Override
        public void setLight(int index, byte light) {
            checkIndex(this.count, index);
            if (this.lightArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.lightArray[this.lightOffset + this.lightStride * index] = light;
            }
        }

        @Override
        public void setStateBiomesLight(int index, int state, int biome, byte light) {
            checkIndex(this.count, index);
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
     *     <li>a {@link DataQueryShape} containing the positions of the voxels to get the values at</li>
     *     <li>a {@link DataQueryOutput} defining which values to read, and to which the retrieved values will be written</li>
     * </ul>
     *
     * @author DaPorkchop_
     */
    interface DataQuery {
        static DataQuery of(@NonNull FBlockLevel.DataQueryShape shape, @NonNull FBlockLevel.DataQueryOutput output) {
            return new DataQuery() {
                @Override
                public void validate() throws RuntimeException {
                    shape.validate();
                    output.validate();

                    int shapeCount = shape.count();
                    int outputCount = output.count();
                    checkState(shapeCount >= outputCount, "shape contains %d points, but output only has space for %d!", shapeCount, outputCount);
                }

                @Override
                public DataQueryShape shape() {
                    return shape;
                }

                @Override
                public DataQueryOutput output() {
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
         * @return the query's {@link DataQueryShape shape}
         */
        DataQueryShape shape();

        /**
         * @return the query's {@link DataQueryOutput output}
         */
        DataQueryOutput output();
    }

    //
    // TYPE TRANSITION SEARCH QUERY CLASSES
    //

    /**
     * Describes which transitions are of interest to a type transition search query.
     *
     * @author DaPorkchop_
     */
    @Data
    @Builder
    final class TypeTransitionQueryInterestFilter {
        /**
         * A bitfield indicating which block types are allowed as the block type being transitioned from. Transitions from block types not included in this bitfield will
         * be ignored.
         * <p>
         * The bitfield must be non-empty (i.e. at least one bit must be set). It may be {@link BlockLevelConstants#allBlockTypes() full}, in which case transitions from
         * any block type will be included.
         */
        private final int fromTypes;

        /**
         * A bitfield indicating which block types are allowed as the block type being transitioned to. Transitions to block types not included in this bitfield will
         * be ignored.
         * <p>
         * The bitfield must be non-empty (i.e. at least one bit must be set). It may be {@link BlockLevelConstants#allBlockTypes() full}, in which case transitions from
         * any block type will be included.
         */
        private final int toTypes;

        /**
         * Indicates the maximum number of hits matching this filter to include in the output. Once this number of transitions matching this filter have been output, any
         * subsequent hits matching this filter will not be included in the output.
         * <p>
         * Special values:<br>
         * <ul>
         *     <li>if {@code < 0}, the filter will be allowed to write an unlimited number of output values without being disabled.</li>
         *     <li>if {@code == 0}, transitions matching this filter will never be included in the output <i>(unless they also match another filter)</i>. This option is
         *     intended to be used with {@link #abortAfterHitCount}, in order to permit filters which serve only to abort the query once hit some number of times without
         *     affecting the output.</li>
         * </ul>
         */
        private final int disableAfterHitCount;

        /**
         * Indicates the maximum number of hits matching this filter to allow. Once this filter has been hit this number of times, the query will be aborted and no
         * further values will be written to the output.
         * <p>
         * Note that transitions which match this filter but are not written to the output due to the value of {@link #disableAfterHitCount} being exceeded still count
         * towards the total number of hits.
         * <p>
         * Special values:<br>
         * <ul>
         *     <li>if {@code < 0}, the filter will be allowed to be hit an unlimited number of times without aborting the query.</li>
         *     <li>if {@code == 0}, the query will be aborted immediately.</li>
         * </ul>
         */
        private final int abortAfterHitCount;

        private TypeTransitionQueryInterestFilter(int fromTypes, int toTypes, int disableAfterHitCount, int abortAfterHitCount) {
            checkArg(BlockLevelConstants.isValidBlockTypeSet(fromTypes), "invalid bitfield in fromTypes: %d", fromTypes);
            checkArg(BlockLevelConstants.isValidBlockTypeSet(toTypes), "invalid bitfield in toTypes: %d", toTypes);

            this.fromTypes = fromTypes;
            this.toTypes = toTypes;
            this.disableAfterHitCount = disableAfterHitCount;
            this.abortAfterHitCount = abortAfterHitCount;
        }

        /**
         * Checks whether a transition from the given type to the given type matches this filter.
         *
         * @param fromType the type being transitioned from
         * @param toType   the type being transitioned to
         * @return whether a transition from the given type to the given type matches this filter
         */
        public boolean transitionMatches(int fromType, int toType) {
            return BlockLevelConstants.isBlockTypeEnabled(this.fromTypes, fromType)
                   && BlockLevelConstants.isBlockTypeEnabled(this.toTypes, toType);
        }
    }

    /**
     * Describes target for a type transition query's output to be written to.
     * <p>
     * A type transition query output consists of a sequence of data values, indexed from {@code 0} (inclusive) to {@link #count()} (exclusive). Each value is broken up
     * into multiple "bands", each of which contain a separate category of information. Bands may be enabled or disabled individually, allowing users to query only the
     * data they're interested in without wasting processing time reading unneeded values or allocating throwaway buffers.
     *
     * @author DaPorkchop_
     */
    interface TypeTransitionOutput {
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
         * @see BlockLevelConstants#isTypeTransitionQueryOutputBandEnabled(int, int)
         */
        int enabledBands();

        /**
         * @return the number of output slots which will be read by this query
         */
        int count();

        /**
         * Sets the type transition value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS "type transitions" type transition query output band}
         * enabled, the value will be silently discarded.
         *
         * @param index          the output index
         * @param typeTransition the new type transition value
         */
        void setTypeTransition(int index, byte typeTransition);

        /**
         * Sets the X coordinate value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X "X coordinates" type transition query output band}
         * enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param x     the new x coordinate value
         */
        void setX(int index, int x);

        /**
         * Sets the Y coordinate value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y "Y coordinates" type transition query output band}
         * enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param y     the new y coordinate value
         */
        void setY(int index, int y);

        /**
         * Sets the Z coordinate value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z "Z coordinates" type transition query output band}
         * enabled, the value will be silently discarded.
         *
         * @param index the output index
         * @param z     the new z coordinate value
         */
        void setZ(int index, int z);

        /**
         * Sets the Z coordinate value at the given output index.
         * <p>
         * If this output does not have the {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA "skipped NoData" type transition query output band}
         * enabled, the value will be silently discarded.
         *
         * @param index         the output index
         * @param skippedNoData the new "skipped NoData" value
         */
        void setSkippedNoData(int index, boolean skippedNoData);

        /**
         * Sets the values in multiple bands at the given output index.
         * <p>
         * Conceptually implemented by
         * <blockquote><pre>{@code
         * this.setTypeTransition(index, typeTransition);
         * this.setX(index, x);
         * this.setY(index, y);
         * this.setZ(index, z);
         * this.setSkippedNoData(index, skippedNoData);
         * }</pre></blockquote>
         * except the implementation has the opportunity to optimize this beyond what the user could write.
         *
         * @param index          the output index
         * @param typeTransition the new type transition descriptor value
         * @param x              the new x coordinate value
         * @param y              the new y coordinate value
         * @param z              the new z coordinate value
         * @param skippedNoData  the new "skipped NoData" value
         */
        default void setAll(int index, byte typeTransition, int x, int y, int z, boolean skippedNoData) {
            this.setTypeTransition(index, typeTransition);
            this.setX(index, x);
            this.setY(index, y);
            this.setZ(index, z);
            this.setSkippedNoData(index, skippedNoData);
        }
    }

    /**
     * A simple {@link TypeTransitionOutput} consisting of a separate array for each type transition query output band.
     * <p>
     * Each type transition query output band array is described by the following:
     * <ul>
     *     <li>the array to which output is to be written, or {@code null} if the type transition query output band is to be disabled</li>
     *     <li>the offset at which to begin writing values to the array. Should be {@code 0} to begin writing at the beginning of the array</li>
     *     <li>the stride between values written to the array. Should be {@code 1} for tightly-packed output</li>
     * </ul>
     *
     * @author DaPorkchop_
     */
    @Data
    final class BandArraysTypeTransitionOutput implements TypeTransitionOutput {
        private final byte[] typeTransitionsArray;
        private final int typeTransitionsOffset;
        private final int typeTransitionsStride;

        private final int[] xCoordinatesArray;
        private final int xCoordinatesOffset;
        private final int xCoordinatesStride;

        private final int[] yCoordinatesArray;
        private final int yCoordinatesOffset;
        private final int yCoordinatesStride;

        private final int[] zCoordinatesArray;
        private final int zCoordinatesOffset;
        private final int zCoordinatesStride;

        private final boolean[] skippedNoDataArray;
        private final int skippedNoDataOffset;
        private final int skippedNoDataStride;

        private final int count;

        @Override
        public void validate() throws RuntimeException {
            //make sure count is valid
            notNegative(this.count, "count");

            //make sure all the indices fit within the given arrays for the provided offset and stride
            if (this.count != 0) {
                if (this.typeTransitionsArray != null) {
                    checkRangeLen(this.typeTransitionsArray.length, this.typeTransitionsOffset,
                            multiplyExact(positive(this.typeTransitionsStride, "statesStride"), this.count) - this.typeTransitionsOffset);
                }
                if (this.xCoordinatesArray != null) {
                    checkRangeLen(this.xCoordinatesArray.length, this.xCoordinatesOffset,
                            multiplyExact(positive(this.xCoordinatesStride, "xCoordinatesStride"), this.count) - this.xCoordinatesOffset);
                }
                if (this.yCoordinatesArray != null) {
                    checkRangeLen(this.yCoordinatesArray.length, this.yCoordinatesOffset,
                            multiplyExact(positive(this.yCoordinatesStride, "yCoordinatesStride"), this.count) - this.yCoordinatesOffset);
                }
                if (this.zCoordinatesArray != null) {
                    checkRangeLen(this.zCoordinatesArray.length, this.zCoordinatesOffset,
                            multiplyExact(positive(this.zCoordinatesStride, "zCoordinatesStride"), this.count) - this.zCoordinatesOffset);
                }
                if (this.skippedNoDataArray != null) {
                    checkRangeLen(this.skippedNoDataArray.length, this.zCoordinatesOffset,
                            multiplyExact(positive(this.zCoordinatesStride, "skippedNoDataStride"), this.count) - this.zCoordinatesOffset);
                }
            }
        }

        @Override
        public int enabledBands() {
            int bands = 0;
            if (this.typeTransitionsArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS);
            }
            if (this.xCoordinatesArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X);
            }
            if (this.yCoordinatesArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y);
            }
            if (this.zCoordinatesArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z);
            }
            if (this.skippedNoDataArray != null) {
                bands |= BlockLevelConstants.dataBandFlag(BlockLevelConstants.TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA);
            }
            return bands;
        }

        @Override
        public void setTypeTransition(int index, byte typeTransition) {
            checkIndex(this.count, index);
            if (this.typeTransitionsArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.typeTransitionsArray[this.typeTransitionsOffset + this.typeTransitionsStride * index] = typeTransition;
            }
        }

        @Override
        public void setX(int index, int x) {
            checkIndex(this.count, index);
            if (this.xCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.xCoordinatesArray[this.xCoordinatesOffset + this.xCoordinatesStride * index] = x;
            }
        }

        @Override
        public void setY(int index, int y) {
            checkIndex(this.count, index);
            if (this.yCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.yCoordinatesArray[this.yCoordinatesOffset + this.yCoordinatesStride * index] = y;
            }
        }

        @Override
        public void setZ(int index, int z) {
            checkIndex(this.count, index);
            if (this.zCoordinatesArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.zCoordinatesArray[this.zCoordinatesOffset + this.zCoordinatesStride * index] = z;
            }
        }

        @Override
        public void setSkippedNoData(int index, boolean skippedNoData) {
            checkIndex(this.count, index);
            if (this.skippedNoDataArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.skippedNoDataArray[this.skippedNoDataOffset + this.skippedNoDataStride * index] = skippedNoData;
            }
        }

        @Override
        public void setAll(int index, byte typeTransition, int x, int y, int z, boolean skippedNoData) {
            checkIndex(this.count, index);
            if (this.typeTransitionsArray != null) {
                //we assume our state is valid, and since we know that the index is valid we can be sure there will be no overflows here
                this.typeTransitionsArray[this.typeTransitionsOffset + this.typeTransitionsStride * index] = typeTransition;
            }
            if (this.xCoordinatesArray != null) {
                this.xCoordinatesArray[this.xCoordinatesOffset + this.xCoordinatesStride * index] = x;
            }
            if (this.yCoordinatesArray != null) {
                this.yCoordinatesArray[this.yCoordinatesOffset + this.yCoordinatesStride * index] = y;
            }
            if (this.zCoordinatesArray != null) {
                this.zCoordinatesArray[this.zCoordinatesOffset + this.zCoordinatesStride * index] = z;
            }
            if (this.skippedNoDataArray != null) {
                this.skippedNoDataArray[this.skippedNoDataOffset + this.skippedNoDataStride * index] = skippedNoData;
            }
        }
    }
}
