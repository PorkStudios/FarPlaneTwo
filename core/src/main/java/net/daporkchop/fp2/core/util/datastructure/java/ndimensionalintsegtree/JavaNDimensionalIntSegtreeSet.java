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

package net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintsegtree;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class JavaNDimensionalIntSegtreeSet implements NDimensionalIntSegtreeSet {
    //this class is confusing because most ranges are inclusive. sorry about that...

    protected static final int LEVELS = Integer.SIZE;

    protected static int toMinAtLevel(int coord, int level) {
        if (level == LEVELS) {
            assert coord == 0 : "max-level coordinate must be zero: " + coord;

            return Integer.MIN_VALUE;
        } else {
            return coord << level;
        }
    }

    protected static int toMaxAtLevel(int coord, int level) {
        if (level == LEVELS) {
            assert coord == 0 : "max-level coordinate must be zero: " + coord;

            return Integer.MAX_VALUE;
        } else {
            return ((coord + 1) << level) - 1;
        }
    }

    protected static Stream<int[]> allLSBPermutations(int[] srcCoords) {
        if (srcCoords.length >= Integer.SIZE - 1) {
            //more bits would be required than could fit in an int, we have to take the (very) slow path.
            //  i don't care that this will literally never be used, it allows my implementation to work with any number of dimensions!
            return allLSBPermutations_slowPath(srcCoords);
        }

        return IntStream.range(0, 1 << srcCoords.length).mapToObj(bits -> {
            //duplicate source array
            int[] coords = srcCoords.clone();
            for (int i = 0; i < coords.length; i++) {
                //for each coordinate, extract the i-th bit from our counter and replace the coordinate's LSB with it
                coords[i] = (coords[i] & ~1) | ((bits >>> i) & 1);
            }
            return coords;
        });
    }

    protected static Stream<int[]> allLSBPermutations_slowPath(int[] srcCoords) {
        Stream<int[]> stream = Stream.of(new int[0]);
        for (int coord : srcCoords) {
            stream = stream.flatMap(coords -> {
                int[] c0 = Arrays.copyOf(coords, coords.length + 1);
                int[] c1 = Arrays.copyOf(coords, coords.length + 1);
                c0[coords.length] = coord & ~1;
                c1[coords.length] = coord | 1;
                return Stream.of(c0, c1);
            });
        }
        return stream;
    }

    protected final NDimensionalIntSet[] delegates;

    @Getter
    protected final int dimensions;

    public JavaNDimensionalIntSegtreeSet(int dimensions, @NonNull Datastructures datastructures) {
        this.dimensions = positive(dimensions, "dimensions");

        NDimensionalIntSet.Builder delegateBuilder = datastructures.nDimensionalIntSet().dimensions(dimensions);
        this.delegates = IntStream.rangeClosed(0, LEVELS)
                .mapToObj(i -> delegateBuilder.build())
                .toArray(NDimensionalIntSet[]::new);
    }

    @Override
    public int size() {
        return this.delegates[0].size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegates[0].isEmpty();
    }

    @Override
    public void clear() {
        for (NDimensionalIntSet delegate : this.delegates) {
            delegate.clear();
        }
    }

    @Override
    public boolean containsAny(@NonNull int[] a, @NonNull int[] b) {
        checkArg(a.length == this.dimensions && b.length == this.dimensions, this.dimensions);

        //make a the minimum and b the maximum point, component-wise
        int[] queryMin = new int[this.dimensions]; //clone to avoid mutating input arrays (i don't care that this is slow)
        int[] queryMax = new int[this.dimensions];
        for (int i = 0; i < this.dimensions; i++) {
            int ai = a[i];
            int bi = b[i];
            queryMin[i] = min(ai, bi);
            queryMax[i] = max(ai, bi);
        }

        return this.containsAnyAABB0(LEVELS, new int[this.dimensions], queryMin, queryMax);
    }

    protected boolean containsAnyAABB0(int level, int[] point, int[] queryMin, int[] queryMax) {
        checkArg(queryMin.length == this.dimensions && queryMax.length == this.dimensions && point.length == this.dimensions, this.dimensions);

        //make sure we're not outside the query AABB
        if (IntStream.range(0, this.dimensions).anyMatch(i -> queryMin[i] > toMaxAtLevel(point[i], level) || queryMax[i] < toMinAtLevel(point[i], level))) {
            return false;
        }

        //get value at current position
        boolean value = this.delegates[level].contains(point);

        //if we're entirely within the query AABB, return the value
        if (IntStream.range(0, this.dimensions).allMatch(i -> queryMin[i] <= toMinAtLevel(point[i], level) && queryMax[i] >= toMaxAtLevel(point[i], level))) {
            return value;
        }

        assert level != 0 : "tried to recurse lower than level 0!";

        //while a higher level's value might be a false positive, it can never be a false negative. therefore, if the value is already false, we can be sure
        //  that recursing any further would never return true, and therefore we can break out now.
        if (!value) {
            return false;
        }

        //shift the point coordinates left by one
        int[] shiftedPoint = point.clone();
        for (int i = 0; i < this.dimensions; i++) {
            shiftedPoint[i] <<= 1;
        }

        //recurse into every child point at the level below
        return allLSBPermutations(shiftedPoint).anyMatch(nextPoint -> this.containsAnyAABB0(level - 1, nextPoint, queryMin, queryMax));
    }

    @Override
    public boolean add(@NonNull int... point) {
        checkArg(point.length == this.dimensions, this.dimensions);

        if (this.delegates[0].add(point)) {
            point = point.clone();
            int lvl = 1;
            do {
                for (int i = 0; i < this.dimensions; i++) {
                    point[i] >>>= 1;
                }
            } while (lvl <= LEVELS && this.delegates[lvl++].add(point));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(@NonNull int... point) {
        checkArg(point.length == this.dimensions, this.dimensions);

        if (this.delegates[0].remove(point)) {
            point = point.clone();
            int lvl = 1;
            while (lvl <= LEVELS && allLSBPermutations(point).noneMatch(this.delegates[lvl - 1]::contains)) {
                for (int i = 0; i < this.dimensions; i++) {
                    point[i] >>>= 1;
                }
                checkState(this.delegates[lvl++].remove(point));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(@NonNull int... point) {
        return this.delegates[0].contains(point);
    }

    @Override
    public void forEach(@NonNull Consumer<int[]> callback) {
        this.delegates[0].forEach(callback);
    }
}
