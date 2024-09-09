/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintset;

import lombok.NonNull;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInt1Set extends AbstractJavaNDimensionalIntSet {
    @Override
    public final int dimensions() {
        return 1;
    }

    @Override
    public final boolean add(@NonNull int... point) {
        checkArg(point.length == 1);
        return this.add(point[0]);
    }

    @Override
    public final boolean remove(@NonNull int... point) {
        checkArg(point.length == 1);
        return this.remove(point[0]);
    }

    @Override
    public final boolean contains(@NonNull int... point) {
        checkArg(point.length == 1);
        return this.contains(point[0]);
    }

    @Override
    public final void forEach(@NonNull Consumer<int[]> callback) {
        this.forEach1D(x -> callback.accept(new int[]{ x }));
    }

    @Override
    public final int countInRange(@NonNull int[] begin, @NonNull int[] end) {
        checkArg(begin.length == 1 && end.length == 1);
        return this.countInRange(begin[0], end[0]);
    }

    @Override
    public final int addAllInRange(@NonNull int[] begin, @NonNull int[] end) {
        checkArg(begin.length == 1 && end.length == 1);
        return this.addAllInRange(begin[0], end[0]);
    }

    @Override
    public abstract boolean add(int x);

    @Override
    public abstract boolean remove(int x);

    @Override
    public abstract boolean contains(int x);

    @Override
    public abstract void forEach1D(@NonNull IntConsumer callback);

    @Override
    public abstract int countInRange(int beginX, int endX);

    @Override
    public abstract int addAllInRange(int beginX, int endX);

    @Override
    public int hashCode() {
        final class State implements IntConsumer {
            int hash;

            @Override
            public void accept(int x) {
                this.hash += 31 + x; //equivalent to Arrays.hashCode(new int[]{ x })
            }
        }

        State state = new State();
        this.forEach1D(state);
        return state.hash;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append('[');
        this.forEach1D(x -> {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append('[').append(x).append(']');
        });
        return builder.append(']').toString();
    }
}
