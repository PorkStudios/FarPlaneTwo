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
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionBatchOutput;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.ThreadSafe;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

import static java.lang.Math.*;
import static java.util.Objects.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A read-only level consisting of voxels at integer coordinates.
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
 *     <li><strong>Ungenerated</strong>: the data at the given position has not yet been generated, and does not exist. Depending on whether the level
 *     {@link #generationAllowed() allows generation}, attempting to access ungenerated data will either cause the data to be generated (and therefore transition to
 *     <i>Exists</i> for future queries), or will cause {@link GenerationNotAllowedException} to be thrown.</li>
 *     <li><strong>Out-of-Bounds</strong>: the given position is outside of the level's {@link #dataLimits() coordinate limits} and cannot exist in any case. Attempts
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
 *     consists of a value which most accurately represents all of the voxels in the {@code stride}<sup>2</sup> area (for the 2-dimensional case) or
 *     {@code stride}<sup>3</sup> volume (for the 3-dimensional case).</li>
 * </ul>
 * <p>
 * <h2>Data Sampling</h2>
 * <p>
 * All data retrieval operations accept parameters which indicate how the operation should sample voxel data from the level: the <strong>sample resolution</strong> and
 * <strong>sampling mode</strong>.
 * <p>
 * <h3>Sample Resolution</h3>
 * The sample resolution is a {@link NotNegative not-negative} {@code int} which indicates the estimated distance (in units of voxels) between voxels that will be
 * sampled. It serves as a hint to the implementation that the user is reading voxel data over a large area, but with a distance of approximately {@code sampleResolution}
 * voxels between each sampled position (along each axis), and wishes to permit the implementation to return any value which it considers to "<strong>represent</strong>"
 * the the blocks contained in the <strong>sampling volume</strong> corresponding to the voxel position being accessed.
 * <p>
 * A sample resolution of exactly {@code 0} is considered the "standard", in that the query is requesting the exact block data at the requested position, and the sampling
 * mode is ignored. Implementations <strong>must</strong> respect this, and <strong>must not</strong> attempt to apply any optimizations which could result in returning
 * data which differs from the block stored at exactly the requested voxel position when the sample resolution is {@code 0}.
 * <p>
 * <h3>Sampling Mode</h3>
 * The {@link QuerySamplingMode sampling mode} is an {@code enum} which provides a hint to the implementation as to how the data will be used, and may affect how the data
 * value to represent a sampling volume is chosen. It is ignored if the sample resolution is exactly {@code 0}. See the individual enum properties for further information
 * as to their individual meanings.
 * <p>
 * <h3>Sampling Volume</h3>
 * When choosing the data to return when sampling the voxel at a given position, the implementation may take all of the voxels in the position's sampling volume into
 * account. The sampling volume for a given voxel position is an implementation-defined axis-aligned cuboid which contains the voxel position and has a side length of
 * no more than {@code 2 * roundUpToNextPowerOfTwo(sampleResolution)}, and may be arbitrarily aligned.
 * <p>
 * <i>Most implementations will use cubic volumes with a side length of either {@code roundUpToNextPowerOfTwo(sampleResolution)} or {@code sampleResolution}, with the
 * volume being aligned to integer multiples of the side length.</i>
 * <p>
 * <h2>Notes to Implementors</h2>
 * <p>
 * Implementations <strong>must</strong> be thread-safe. <sub>Implementors which achieve thread-safety by slapping {@code synchronized} on everything will be sentenced
 * to a long, painful death by being buried alive in the mountain of CPU cores which were left underutilized as a direct result of their lazy programming.</sub>
 * <p>
 * Ideally, all implementors would override every method declared in this interface, and would have several implementations of each method internally, each optimized for
 * specific cases. However, even though this is (obviously) not realistically possible, there are still some methods and/or edge cases for which implementors
 * <strong>should</strong> add specific optimized implementations. Specifically, in addition to methods without a default implementation, implementations are strongly
 * advised to override and provide optimized implementations for <strong>at least</strong> the following methods:<br>
 * <ul>
 *     <li>
 *         {@link #guaranteedDataAvailableVolume(int, int, int, int, int, int)}<br>
 *         <i>For implementations returning "real" data from persistent storage, consider growing the supplied bounding box up to the on-disk chunk/fragment/shard/...
 *         granularity of the underlying storage format.</i><br>
 *         <i>For implementations returning "fake" data generated on-the-fly, consider growing the supplied bounding box up to the granularity of any internal caches.</i><br>
 *     </li>
 *     <li>
 *         {@link #query(BatchDataQuery)}<br>
 *         <i>For implementations returning "real" data from persistent storage, consider prefetching all of the data into memory before beginning to copy data into the
 *         output buffer.</i><br>
 *         <i>Also consider optimizing for specific {@link PointsQueryShape} types, most importantly {@link PointsQueryShape.OriginSizeStride}.</i><br>
 *     </li>
 *     <li>
 *         {@link #getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, QuerySamplingMode)}<br>
 *         <i>For implementations returning "real" data from persistent storage, consider using a spatial datastructure to efficiently scan for type transitions in
 *         logarithmic time.</i><br>
 *         <i>For implementations returning "fake" data generated on-the-fly, consider using techniques such as binary search to efficiently scan for type
 *         transitions in logarithmic time.</i><br>
 *         <i>Consider optimizing for specific {@link Direction direction}s, most importantly {@link Direction#NEGATIVE_Y} and {@link Direction#POSITIVE_Y}</i><br>
 *     </li>
 * </ul>
 *
 * @author DaPorkchop_
 */
@ThreadSafe(except = "close")
public interface FBlockLevel extends FBlockLevelDataAvailability, AutoCloseable {
    /**
     * Closes this level, immediately releasing any internally allocated resources.
     * <p>
     * Once closed, all of this instance's methods will produce undefined behavior when called.
     * <p>
     * If not manually closed, a level will be implicitly closed when the instance is garbage-collected.
     */
    @Override
    void close();

    //
    // STATIC WORLD INFORMATION
    //

    /**
     * @return the {@link FGameRegistry} instance used by this level
     */
    FGameRegistry registry();

    /**
     * @return whether this level allows generating data which is not known
     */
    boolean generationAllowed();

    //
    // INDIVIDUAL DATA QUERIES
    //

    /**
     * Gets the state at the given voxel position.
     *
     * @param x                the X coordinate
     * @param y                the Y coordinate
     * @param z                the Z coordinate
     * @param sampleResolution the sampling resolution to use
     * @param samplingMode     the {@link QuerySamplingMode sampling mode} to use
     * @return the state
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this level doesn't allow generation
     */
    int getState(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException;

    /**
     * Gets the biome at the given voxel position.
     *
     * @param x                the X coordinate
     * @param y                the Y coordinate
     * @param z                the Z coordinate
     * @param sampleResolution the sampling resolution to use
     * @param samplingMode     the {@link QuerySamplingMode sampling mode} to use
     * @return the biome
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this level doesn't allow generation
     */
    int getBiome(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException;

    /**
     * Gets the packed block/sky light at the given voxel position.
     *
     * @param x                the X coordinate
     * @param y                the Y coordinate
     * @param z                the Z coordinate
     * @param sampleResolution the sampling resolution to use
     * @param samplingMode     the {@link QuerySamplingMode sampling mode} to use
     * @return the packed light levels
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this level doesn't allow generation
     */
    byte getLight(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException;

    //
    // BULK DATA QUERIES
    //

    /**
     * Issues a batch data retrieval query.
     *
     * @param query the {@link BatchDataQuery query} which describes the position(s) to query the data from, which data bands should be accessed, and where the retrieved
     *              data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this level doesn't allow generation
     */
    default void query(@NonNull BatchDataQuery query) throws GenerationNotAllowedException {
        this.query(new BatchDataQuery[]{ query }); //delegate to bulk implementation
    }

    /**
     * Issues multiple batch data retrieval queries.
     *
     * @param queries an array of {@link BatchDataQuery queries} which describe the position(s) to query the data from, which data bands should be accessed, and where the
     *                retrieved data should be stored
     * @throws GenerationNotAllowedException if the data at any of the queried voxel positions is not generated and this level doesn't allow generation
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
            int sampleResolution = query.sampleResolution();
            QuerySamplingMode samplingMode = query.samplingMode();

            int enabledBands = output.enabledBands();
            shape.forEach((index, x, y, z) -> {
                //initialize variables to throwaway values (they won't be used if the data band is disabled)
                int state = 0;
                int biome = 0;
                byte light = 0;

                //read values if corresponding bands are enabled
                if (BlockLevelConstants.isDataBandEnabled(enabledBands, BlockLevelConstants.DATA_BAND_ORDINAL_STATES)) {
                    state = this.getState(x, y, z, sampleResolution, samplingMode);
                }
                if (BlockLevelConstants.isDataBandEnabled(enabledBands, BlockLevelConstants.DATA_BAND_ORDINAL_BIOMES)) {
                    biome = this.getBiome(x, y, z, sampleResolution, samplingMode);
                }
                if (BlockLevelConstants.isDataBandEnabled(enabledBands, BlockLevelConstants.DATA_BAND_ORDINAL_LIGHT)) {
                    light = this.getLight(x, y, z, sampleResolution, samplingMode);
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
     *     <li>the current position is more than {@code maxDistance} voxels away from the origin point</li>
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
     * @param direction        the direction to iterate in
     * @param x                the X coordinate to begin iteration from
     * @param y                the Y coordinate to begin iteration from
     * @param z                the Z coordinate to begin iteration from
     * @param maxDistance      the maximum number of voxels to iterate through. The voxel being transitioned to is not included in this total: if {@code maxDistance} is
     *                         {@code 1L}, the transition from {@code (x,y,z)} to {@code (x+dx,y+dy,z+dz)} will still be checked
     * @param filters          a {@link List} of the {@link TypeTransitionFilter filters} to use for selecting which type transitions to include in the
     *                         {@link TypeTransitionSingleOutput output}. Type transitions which at least one of the filters will be written to the output. If the given
     *                         {@link List} is {@link List#isEmpty() empty}, no filtering will be applied.
     * @param output           a {@link TypeTransitionSingleOutput output} to store the encountered type transitions in
     * @param sampleResolution the sampling resolution to use
     * @param samplingMode     the {@link QuerySamplingMode sampling mode} to use
     * @return the number of elements written to the {@link TypeTransitionSingleOutput output}
     */
    default int getNextTypeTransitions(@NonNull Direction direction, int x, int y, int z, long maxDistance,
                                       @NonNull List<@NonNull TypeTransitionFilter> filters,
                                       @NonNull TypeTransitionSingleOutput output,
                                       @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) {
        //we can't respect the original values, so just overwrite them
        sampleResolution = 0;
        samplingMode = QuerySamplingMode.DONT_CARE;

        output.validate(); //this could potentially help JIT?

        if (notNegative(maxDistance, "maxDistance") == 0L) { //already reached the maximum search distance
            return 0;
        }

        final int outputCount = output.count();
        if (outputCount <= 0) { //we've already run out of output space lol
            return 0;
        }

        //ensure that the filters list supports constant-time random access
        if (!(filters instanceof RandomAccess)) {
            filters = new ArrayList<>(filters);
        }

        //before going any further, let's check if any filter requests that we abort the query immediately
        for (TypeTransitionFilter filter : filters) {
            if (filter.shouldAbort(0)) {
                return 0;
            }
        }

        final IntAxisAlignedBB dataLimits = this.dataLimits();
        final FExtendedStateRegistryData extendedStateRegistryData = this.registry().extendedStateRegistryData();

        final int dx = direction.x();
        final int dy = direction.y();
        final int dz = direction.z();

        if (!BlockLevelConstants.willVectorIntersectAABB(dataLimits, x, y, z, direction, maxDistance)) {
            //the data limits will never be intersected, so there's nothing left to do
            return 0;
        } else if (!dataLimits.contains(x, y, z)) {
            //the starting position is outside the data limits, but the search will eventually reach the data limits. jump directly to the position one voxel before
            maxDistance -= BlockLevelConstants.jumpToExclusiveDistance(dataLimits, x, y, z, direction, maxDistance);
            int nextX = BlockLevelConstants.jumpXCoordinateToExclusiveAABB(dataLimits, x, y, z, direction);
            int nextY = BlockLevelConstants.jumpYCoordinateToExclusiveAABB(dataLimits, x, y, z, direction);
            int nextZ = BlockLevelConstants.jumpZCoordinateToExclusiveAABB(dataLimits, x, y, z, direction);

            assert !dataLimits.contains(nextX, nextY, nextZ)
                    : "jump: position should be outside the level's bounds";
            assert dataLimits.contains(addExact(nextX, direction.x()), addExact(nextY, direction.y()), addExact(nextZ, direction.z()))
                    : "jump: position+1 should be inside the level's bounds";

            x = nextX;
            y = nextY;
            z = nextZ;
        }

        int[] filterHitCounts = new int[filters.size()];

        long distance = 0L;
        int writtenCount = 0;
        int lastType;
        boolean skippedNoData = false;

        //determine the initial block type
        try {
            lastType = extendedStateRegistryData.type(this.getState(x, y, z, sampleResolution, samplingMode));
        } catch (GenerationNotAllowedException e) {
            //we already encountered a NoData value!
            lastType = -1;
            skippedNoData = true;
        }

        //increment coordinates by one step, and abort if they exceed the data limits
        for (boolean lastInBounds = true, nextInBounds;
             distance++ < maxDistance && ((nextInBounds = dataLimits.contains(x = addExact(x, dx), y = addExact(y, dy), z = addExact(z, dz))) || lastInBounds);
             lastInBounds = nextInBounds) {
            int nextType;
            try {
                nextType = extendedStateRegistryData.type(this.getState(x, y, z, sampleResolution, samplingMode));
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
                for (int filterIndex = 0; filterIndex < filters.size(); filterIndex++) {
                    TypeTransitionFilter filter = filters.get(filterIndex);
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
            long maxDistance = query.maxDistance();
            List<@NonNull TypeTransitionFilter> filters = query.filters();
            PointsQueryShape shape = query.shape();
            TypeTransitionBatchOutput output = query.output();
            int sampleResolution = query.sampleResolution();
            QuerySamplingMode samplingMode = query.samplingMode();

            //dispatch a separate query for each point and store it in the corresponding slot index
            shape.forEach((index, x, y, z) -> {
                int length = this.getNextTypeTransitions(direction, x, y, z, maxDistance, filters, output.slot(index), sampleResolution, samplingMode);
                output.setLength(index, length);
            });
        }
    }
}
