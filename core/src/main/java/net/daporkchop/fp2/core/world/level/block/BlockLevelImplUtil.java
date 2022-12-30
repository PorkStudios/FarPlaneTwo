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

package net.daporkchop.fp2.core.world.level.block;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.FBlockLevelDataAvailability;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.fp2.core.server.world.FBlockLevelHolder;

import java.util.List;

/**
 * Static utility methods used by implementations of {@link FBlockLevel} and {@link FBlockLevelHolder}.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class BlockLevelImplUtil {
    //
    // TypeTransitionFilter utilities
    //

    public static int checkTransitionFilters(int lastType, int nextType, List<@NonNull TypeTransitionFilter> filters, int[] filterHitCounts) {
        assert lastType != nextType;

        //check if the transition matches any of the provided filters. we iterate over every filter even if we find a match, because we need to increment
        // the hit count for EVERY filter that reported a match, even though we'll never write more than one value into the output.
        int out = 0;
        for (int filterIndex = 0; filterIndex < filters.size(); filterIndex++) {
            TypeTransitionFilter filter = filters.get(filterIndex);
            if (!filter.shouldDisable(filterHitCounts[filterIndex]) //the filter isn't disabled
                && filter.transitionMatches(lastType, nextType)) { //the type transition matches the filter!
                out |= 1; //transitionMatches = true;
                filterHitCounts[filterIndex]++;

                if (filter.shouldAbort(filterHitCounts[filterIndex])) { //the filter wants us to abort this query after we finish writing the current value
                    out |= 2; //abort = true;
                }
            }
        }
        return out;
    }

    public static boolean checkTransitionFiltersResult_transitionMatches(int result) {
        assert (result & 3) == result : "invalid result value???";
        return (result & 1) != 0;
    }

    public static boolean checkTransitionFiltersResult_abort(int result) {
        assert (result & 3) == result : "invalid result value???";
        return (result & 2) != 0;
    }

    //
    // Generic coordinate utilities, used for computing prefetching regions
    //

    public boolean isAnyPointValid(@NonNull FBlockLevelDataAvailability level, @NonNull PointsQueryShape.OriginSizeStride shape) {
        IntAxisAlignedBB dataLimits = level.dataLimits();
        return isAnyPointValid(shape.originX(), shape.sizeX(), shape.strideX(), dataLimits.minX(), dataLimits.maxX())
               && isAnyPointValid(shape.originY(), shape.sizeY(), shape.strideY(), dataLimits.minY(), dataLimits.maxY())
               && isAnyPointValid(shape.originZ(), shape.sizeZ(), shape.strideZ(), dataLimits.minZ(), dataLimits.maxZ());
    }

    public boolean isAnyPointValid(int origin, int size, int stride, int min, int max) {
        //this could probably be implemented way faster, but i really don't care because this will never be called with a size larger than like 20

        for (int i = 0, pos = origin; i < size; i++, pos += stride) {
            if (pos >= min && pos < max) { //the point is valid
                return true;
            }
        }

        return false; //no points were valid
    }
}
