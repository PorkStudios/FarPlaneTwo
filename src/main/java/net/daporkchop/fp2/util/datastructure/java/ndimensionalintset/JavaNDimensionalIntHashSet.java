/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.util.datastructure.java.ndimensionalintset;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.Arrays;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Hash-based implementation of {@link NDimensionalIntSet} which works with arbitrary dimension counts.
 *
 * @author DaPorkchop_
 */
@Getter
public class JavaNDimensionalIntHashSet extends ObjectOpenCustomHashSet<int[]> implements NDimensionalIntSet {
    protected static final Strategy<int[]> STRATEGY = new Strategy<int[]>() {
        @Override
        public int hashCode(int[] o) {
            return Arrays.hashCode(o);
        }

        @Override
        public boolean equals(int[] a, int[] b) {
            return Arrays.equals(a, b);
        }
    };

    protected final int dimensions;
    protected int refCnt = 1;

    public JavaNDimensionalIntHashSet(int dimensions) {
        super(STRATEGY);

        this.dimensions = positive(dimensions, "dimensions");
    }

    @Override
    public synchronized NDimensionalIntSet retain() throws AlreadyReleasedException {
        if (this.refCnt == 0) {
            throw new AlreadyReleasedException();
        }
        this.refCnt++;
        return this;
    }

    @Override
    public long count() {
        return this.size();
    }

    @Override
    public synchronized boolean release() throws AlreadyReleasedException {
        if (this.refCnt == 0) {
            throw new AlreadyReleasedException();
        }
        return --this.refCnt == 0;
    }

    @Override
    public boolean add(@NonNull int[] point) {
        checkArg(point.length == this.dimensions, this.dimensions);
        return super.add(point);
    }

    @Override
    public boolean remove(@NonNull int... point) {
        checkArg(point.length == this.dimensions, this.dimensions);
        return super.remove(point);
    }

    @Override
    public boolean contains(@NonNull int... point) {
        checkArg(point.length == this.dimensions, this.dimensions);
        return super.contains(point);
    }
}
