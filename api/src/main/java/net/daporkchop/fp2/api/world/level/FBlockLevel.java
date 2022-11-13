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

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.query.BatchDataQuery;
import net.daporkchop.fp2.api.world.level.query.BatchTypeTransitionQuery;
import net.daporkchop.fp2.api.world.level.query.DataQueryBatchOutput;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionBatchOutput;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.ThreadSafe;

import java.util.Collection;

import static java.lang.Integer.*;
import static java.lang.Math.*;
import static java.util.Objects.*;

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
     * however all queries outside the data limits will return some predetermined value.
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
     * Issues a batch data retrieval query.
     *
     * @param query the {@link BatchDataQuery query} which describes the position(s) to query the data from, which data bands should be accessed, and where the retrieved
     *              data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this world doesn't allow generation
     */
    default void query(@NonNull BatchDataQuery query) throws GenerationNotAllowedException {
        this.query(new BatchDataQuery[]{ query }); //delegate to bulk implementation
    }

    /**
     * Issues multiple batch data retrieval queries.
     *
     * @param queries an array of {@link BatchDataQuery queries} which describe the position(s) to query the data from, which data bands should be accessed, and where the
     *                retrieved data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this world doesn't allow generation
     */
    default void query(@NonNull BatchDataQuery... queries) throws GenerationNotAllowedException {
        //ensure all queries are valid
        for (BatchDataQuery query : queries) {
            requireNonNull(query, "query").validate();
        }

        //dispatch queries
        for (BatchDataQuery query : queries) {
            query.validate(); //validate again to potentially help JIT

            PointsQueryShape shape = query.shape();
            DataQueryBatchOutput output = query.output();

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
     *     <li>one of the given {@link TypeTransitionFilter filters}' {@link TypeTransitionFilter#abortAfterHitCount() abortAfterHitCount} limit
     *     is exceeded</li>
     *     <li>the {@link TypeTransitionSingleOutput}'s capacity is exceeded, and no more output can be stored in it</li>
     *     <li>the current position goes beyond this level's {@link #dataLimits() data limits}</li>
     * </ul>
     * <p>
     * When a transition between block types is encountered, it is checked against all the given {@link TypeTransitionFilter filters}. If it matches at
     * least one filter, the transition's type transition descriptor and the XYZ coordinates of the block being transitioned to are stored in the next available index of
     * the given {@link TypeTransitionSingleOutput output}.
     * <p>
     * The type transitions between data which exists and data which doesn't exist, as well as between non-existent voxels are not defined. If a query requires the
     * implementation to access voxel data which doesn't exist, this could result in, among other things, seemingly "impossible" sequences of transitions (e.g. two
     * transitions from {@link BlockLevelConstants#BLOCK_TYPE_INVISIBLE invisible} to {@link BlockLevelConstants#BLOCK_TYPE_OPAQUE} in a row with no other transitions
     * between them). To avoid this problem, the {@link TypeTransitionSingleOutput output} has a special output band: the
     * {@link BlockLevelConstants#TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA "skipped NoData" type transition query output band}, which is an array of
     * {@code boolean}s. A value of {@code true} for an individual transition indicates the implementation may have skipped one or more non-existent voxels prior to
     * encountering the current transition.
     *
     * @param direction the direction to iterate in
     * @param x         the X coordinate to begin iteration from
     * @param y         the Y coordinate to begin iteration from
     * @param z         the Z coordinate to begin iteration from
     * @param filters   a {@link Collection} of the {@link TypeTransitionFilter filters} to use for selecting which type transitions to include in the
     *                  {@link TypeTransitionSingleOutput output}. Type transitions which at least one of the filters will be written to the output. If the given
     *                  {@link Collection} is {@link Collection#isEmpty() empty}, no filtering will be applied.
     * @param output    a {@link TypeTransitionSingleOutput output} to store the encountered type transitions in
     * @return the number of elements written to the {@link TypeTransitionSingleOutput output}
     */
    default int getNextTypeTransitions(@NonNull Direction direction, int x, int y, int z,
                                       @NonNull Collection<@NonNull TypeTransitionFilter> filters,
                                       @NonNull TypeTransitionSingleOutput output) {
        output.validate(); //this could potentially help JIT?

        final IntAxisAlignedBB dataLimits = this.dataLimits();

        final FExtendedStateRegistryData extendedStateRegistryData = this.registry().extendedStateRegistryData();

        final int outputCount = output.count();
        int writtenCount = 0;

        if (outputCount <= 0) { //we've already run out of output space lol
            return 0;
        }

        final int dx = direction.x();
        final int dy = direction.y();
        final int dz = direction.z();

        if (!dataLimits.contains(x, y, z)) { //the given position is already outside the world's data limits
            //check if the point will ever intersect the data limits if stepping in the given direction
            if ((dx == 0
                    ? dataLimits.containsX(x) //the X coordinate will never change, it must be within the data limits in order to ever intersect it
                    : (dx < 0) == (dataLimits.minX() < x)) //make sure the X coordinate is increasing towards the data limits - if it's moving away it'll never intersect
                  //ditto for Y and Z axes
                && (dy == 0 ? dataLimits.containsY(y) : (dy < 0) == (dataLimits.minY() < y))
                && (dz == 0 ? dataLimits.containsZ(z) : (dz < 0) == (dataLimits.minZ() < z))) {
                //the data limits will eventually be intersected, let's jump directly to the position one voxel before entering
                if (dx != 0) {
                    x = dx < 0 ? dataLimits.maxX() : decrementExact(dataLimits.minX());
                }
                if (dy != 0) {
                    y = dy < 0 ? dataLimits.maxY() : decrementExact(dataLimits.minY());
                }
                if (dz != 0) {
                    z = dz < 0 ? dataLimits.maxZ() : decrementExact(dataLimits.minZ());
                }
            } else {
                //the data limits will never be intersected, so there's nothing left to do
                return 0;
            }
        }

        TypeTransitionFilter[] filtersArray = filters.toArray(new TypeTransitionFilter[0]);
        int[] filterHitCounts = new int[filtersArray.length];

        //before reading any block data, let's check if any filter requests that we abort the query immediately
        for (TypeTransitionFilter filter : filtersArray) {
            if (filter.shouldAbort(0)) {
                return 0;
            }
        }

        int lastType;
        boolean skippedNoData = false;

        //determine the initial block type
        try {
            lastType = extendedStateRegistryData.type(this.getState(x, y, z));
        } catch (GenerationNotAllowedException e) {
            //we already encountered a NoData value!
            lastType = -1;
            skippedNoData = true;
        }

        //increment coordinates by one step, and abort if they exceed the data limits
        while (dataLimits.contains(x = addExact(x, dx), y = addExact(y, dy), z = addExact(z, dz))) {
            int nextType;
            try {
                nextType = extendedStateRegistryData.type(this.getState(x, y, z));
            } catch (GenerationNotAllowedException e) {
                //found a NoData value, remember that we encountered it and resume search
                lastType = -1;
                skippedNoData = true;

                //TODO: we could probably do a reverse binary search using containsAnyData() to efficiently skip to the end of the current data void, under the assumption
                // that containsAnyData() is implemented efficiently (using a segment tree).
                continue;
            }

            if (lastType >= 0 //the last voxel wasn't a NoData value
                && lastType != nextType) { //the types are different -> this is a type transition!

                //check if the transition matches any of the provided filters. we iterate over every filter even if we find a match, because we need to increment
                // the hit count for EVERY filter that reported a match, even though we'll never write more than one value into the output.
                boolean transitionMatches = false;
                boolean abort = false;
                for (int filterIndex = 0; filterIndex < filtersArray.length; filterIndex++) {
                    TypeTransitionFilter filter = filtersArray[filterIndex];
                    if (!filter.shouldDisable(filterHitCounts[filterIndex]) //the filter isn't disabled
                        && filter.transitionMatches(lastType, nextType)) { //the type transition matches the filter!
                        transitionMatches = true;
                        filterHitCounts[filterIndex]++;

                        if (filter.shouldAbort(filterHitCounts[filterIndex])) { //the filter wants us to abort this query after we finish writing the current value
                            abort = true;
                        }
                    }
                }

                if (transitionMatches) {
                    output.setAll(writtenCount, BlockLevelConstants.getTypeTransition(lastType, nextType), x, y, z, skippedNoData);

                    //reset the skippedNoData flag, since any previously skipped ranges of NoData values have already been reported
                    skippedNoData = false;

                    if (++writtenCount == outputCount) { //the output is now full!
                        break;
                    }
                }

                if (abort) {
                    break;
                }
            }

            //save the current type as the last type so that we can continue to the next voxel
            lastType = nextType;
        }

        return writtenCount;
    }

    //
    // BULK TYPE TRANSITION SEARCH QUERIES
    //

    /**
     * Issues a batch type transition search query.
     *
     * @param query the {@link BatchTypeTransitionQuery query} which describes the position(s) to query the data from, which data bands should be accessed, and where the retrieved
     *              data should be stored
     */
    default void query(@NonNull BatchTypeTransitionQuery query) {
        this.query(new BatchTypeTransitionQuery[]{ query }); //delegate to bulk implementation
    }

    /**
     * Issues multiple batch type transition search queries.
     *
     * @param queries an array of {@link BatchTypeTransitionQuery queries} which describe the position(s) to query the data from, which data bands should be accessed, and where the
     *                retrieved data should be stored
     */
    default void query(@NonNull BatchTypeTransitionQuery... queries) {//ensure all queries are valid
        for (BatchTypeTransitionQuery query : queries) {
            requireNonNull(query, "query").validate();
        }

        //dispatch queries
        for (BatchTypeTransitionQuery query : queries) {
            query.validate(); //validate again to potentially help JIT

            Direction direction = query.direction();
            Collection<@NonNull TypeTransitionFilter> filters = query.filters();
            PointsQueryShape shape = query.shape();
            TypeTransitionBatchOutput output = query.output();

            //dispatch a separate query for each point and store it in the corresponding slot index
            shape.forEach((index, x, y, z) -> {
                int length = this.getNextTypeTransitions(direction, x, y, z, filters, output.slot(index));
                output.setLength(index, length);
            });
        }
    }
}
