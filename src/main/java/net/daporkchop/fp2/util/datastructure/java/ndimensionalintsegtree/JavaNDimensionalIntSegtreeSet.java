/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.util.datastructure.java.ndimensionalintsegtree;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.util.datastructure.Datastructures;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class JavaNDimensionalIntSegtreeSet extends AbstractRefCounted implements NDimensionalIntSegtreeSet {
    protected static Stream<int[]> allLSBPermutations(@NonNull int[] srcCoords) {
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
        this.delegates = IntStream.range(0, Integer.SIZE - 1)
                .mapToObj(i -> delegateBuilder.build())
                .toArray(NDimensionalIntSet[]::new);
    }

    @Override
    public NDimensionalIntSegtreeSet retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        for (NDimensionalIntSet delegate : this.delegates) {
            delegate.release();
        }
    }

    @Override
    public long count() {
        return this.delegates[0].count();
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
        throw new UnsupportedOperationException(); //TODO: implement this
    }

    @Override
    public boolean containsAny(int shift, @NonNull int... point) {
        checkArg(point.length == this.dimensions, this.dimensions);

        if (shift < this.delegates.length) {
            return this.delegates[shift].contains(point);
        } else { //high levels will be 0 in any case, so we don't bother storing it
            return !this.isEmpty();
        }
    }

    @Override
    public boolean add(@NonNull int... point) {
        checkArg(point.length == this.dimensions, this.dimensions);

        if (this.delegates[0].add(point)) {
            point = point.clone();
            int lvl = 1;
            do {
                for (int i = 0; i < this.dimensions; i++) {
                    point[i] >>= 1;
                }
            } while (lvl < this.delegates.length && this.delegates[lvl++].add(point));
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
            while (lvl < this.delegates.length && allLSBPermutations(point).noneMatch(this.delegates[lvl]::contains)) {
                for (int i = 0; i < this.dimensions; i++) {
                    point[i] >>= 1;
                }
                this.delegates[lvl++].remove(point);
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
