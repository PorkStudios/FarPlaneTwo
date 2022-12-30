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

package net.daporkchop.fp2.core.minecraft.world;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.core.server.world.FBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.world.level.block.AbstractFBlockLevelHolder;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.util.GlobalAllocators.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactFBlockLevelHolder<PREFETCHED extends AbstractPrefetchedExactFBlockLevel> extends AbstractFBlockLevelHolder implements FBlockLevelHolder.Exact {
    public AbstractExactFBlockLevelHolder(@NonNull IFarLevelServer level) {
        super(level);
    }

    //
    // Shared implementations of FBlockLevel's type transition search methods
    //

    /**
     * @param prefetchedLevel the existing {@link PREFETCHED prefetched level} which the query is being executed in, or {@code null} if it is not being executed in a
     *                        prefetched level
     * @see FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, QuerySamplingMode)
     */
    public final int getNextTypeTransitions(@NonNull Direction direction, int x, int y, int z, long maxDistance,
                                            @NonNull List<@NonNull TypeTransitionFilter> filters,
                                            @NonNull TypeTransitionSingleOutput output,
                                            @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode,
                                            PREFETCHED prefetchedLevel) {
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

        //allocate a temporary array for storing the per-filter hit counts
        ArrayAllocator<int[]> alloc = ALLOC_INT.get();
        int[] filterHitCounts = alloc.atLeast(filters.size());

        try {
            //now that we've done all the complex argument validation, delegate to the real implementation
            return this.getNextTypeTransitions(
                    x, y, z, dx, dy, dz, maxDistance,
                    dataLimits, filters, filterHitCounts, output, extendedStateRegistryData, sampleResolution, samplingMode, prefetchedLevel);
        } finally {
            alloc.release(filterHitCounts);
        }
    }

    /**
     * The real implementation of {@link #getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, QuerySamplingMode, AbstractPrefetchedExactFBlockLevel)}, which
     * is called after all the arguments have been validated.
     *
     * @param x                         the X coordinate to begin iteration from
     * @param y                         the Y coordinate to begin iteration from
     * @param z                         the Z coordinate to begin iteration from
     * @param dx                        the iteration direction's step along the X axis. The value may be one of {@code 0}, {@code 1} or {@code -1}. Exactly one of
     *                                  {@code dx}, {@code dy} and {@code dz} will be non-zero; the other two will be {@code 0}.
     * @param dy                        the iteration direction's step along the Y axis. The value may be one of {@code 0}, {@code 1} or {@code -1}. Exactly one of
     *                                  {@code dx}, {@code dy} and {@code dz} will be non-zero; the other two will be {@code 0}.
     * @param dz                        the iteration direction's step along the Z axis. The value may be one of {@code 0}, {@code 1} or {@code -1}. Exactly one of
     *                                  {@code dx}, {@code dy} and {@code dz} will be non-zero; the other two will be {@code 0}.
     * @param maxDistance               the maximum number of voxels to iterate
     * @param dataLimits                this level's {@link FBlockLevel#dataLimits() data limits}
     * @param filters                   the {@link TypeTransitionFilter type transition filters} to use
     * @param filterHitCounts           an {@code int[]} to use for tracking the number of times each filter has been hit. The array's length is guaranteed to be greater
     *                                  than or equal to {@code filters.size()}.
     * @param output                    the {@link TypeTransitionSingleOutput output} to store the encountered type transitions in
     * @param extendedStateRegistryData this level's {@link FExtendedStateRegistryData}
     * @param sampleResolution          the sample resolution, as described in {@link FBlockLevel}
     * @param samplingMode              the {@link QuerySamplingMode sampling mode}, as described in {@link FBlockLevel}
     * @param prefetchedLevel           the existing {@link PREFETCHED prefetched level} which the query is being executed in, or {@code null} if it is not being executed
     *                                  in a prefetched level
     * @return the number of elements written to the {@link TypeTransitionSingleOutput output}
     * @see FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, net.daporkchop.fp2.api.world.level.query.QuerySamplingMode)
     */
    protected abstract int getNextTypeTransitions(int x, int y, int z, int dx, int dy, int dz, long maxDistance,
                                                  @NonNull IntAxisAlignedBB dataLimits,
                                                  @NonNull List<@NonNull TypeTransitionFilter> filters, @NonNull int[] filterHitCounts,
                                                  @NonNull TypeTransitionSingleOutput output,
                                                  @NonNull FExtendedStateRegistryData extendedStateRegistryData,
                                                  @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode,
                                                  PREFETCHED prefetchedLevel);

    //
    // Generic coordinate utilities, used for computing prefetching regions
    //

    protected Consumer<IntConsumer> chunkCoordSupplier(int origin, int size, int stride, int min, int max, int chunkShift, int chunkSize) {
        if (stride >= chunkSize) {
            return callback -> {
                for (int i = 0, block = origin; i < size && block < max; i++, block += stride) {
                    if (block >= min) {
                        callback.accept(block >> chunkShift);
                    }
                }
            };
        } else {
            return callback -> {
                for (int chunk = max(origin, min) >> chunkShift, limit = min(origin + (size - 1) * stride, max) >> chunkShift; chunk <= limit; chunk++) {
                    callback.accept(chunk);
                }
            };
        }
    }
}
