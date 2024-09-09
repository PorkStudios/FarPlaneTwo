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
import net.daporkchop.lib.primitive.lambda.IntIntConsumer;

import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInt2Set extends AbstractJavaNDimensionalIntSet {
    @Override
    public final int dimensions() {
        return 2;
    }

    @Override
    public final boolean add(@NonNull int... point) {
        checkArg(point.length == 2);
        return this.add(point[0], point[1]);
    }

    @Override
    public final boolean remove(@NonNull int... point) {
        checkArg(point.length == 2);
        return this.remove(point[0], point[1]);
    }

    @Override
    public final boolean contains(@NonNull int... point) {
        checkArg(point.length == 2);
        return this.contains(point[0], point[1]);
    }

    @Override
    public final void forEach(@NonNull Consumer<int[]> callback) {
        this.forEach2D((x, y) -> callback.accept(new int[]{ x, y }));
    }

    @Override
    public final int countInRange(@NonNull int[] begin, @NonNull int[] end) {
        checkArg(begin.length == 2 && end.length == 2);
        return this.countInRange(begin[0], begin[1], end[0], end[1]);
    }

    @Override
    public final int addAllInRange(@NonNull int[] begin, @NonNull int[] end) {
        checkArg(begin.length == 2 && end.length == 2);
        return this.addAllInRange(begin[0], begin[1], end[0], end[1]);
    }

    @Override
    public abstract boolean add(int x, int y);

    @Override
    public abstract boolean remove(int x, int y);

    @Override
    public abstract boolean contains(int x, int y);

    @Override
    public abstract void forEach2D(@NonNull IntIntConsumer callback);

    @Override
    public abstract int countInRange(int beginX, int beginY, int endX, int endY);

    @Override
    public abstract int addAllInRange(int beginX, int beginY, int endX, int endY);

    @Override
    public int hashCode() {
        final class State implements IntIntConsumer {
            int hash;

            @Override
            public void accept(int x, int y) {
                this.hash += (31 + x) * 31 + y; //equivalent to Arrays.hashCode(new int[]{ x, y })
            }
        }

        State state = new State();
        this.forEach2D(state);
        return state.hash;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append('[');
        this.forEach2D((x, y) -> {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append('[').append(x).append(", ").append(y).append(']');
        });
        return builder.append(']').toString();
    }
}
