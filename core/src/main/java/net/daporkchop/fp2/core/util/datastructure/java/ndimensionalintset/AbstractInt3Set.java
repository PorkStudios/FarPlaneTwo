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
import net.daporkchop.lib.primitive.lambda.IntIntIntConsumer;

import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInt3Set extends AbstractJavaNDimensionalIntSet {
    @Override
    public final int dimensions() {
        return 3;
    }

    @Override
    public final boolean add(@NonNull int... point) {
        checkArg(point.length == 3);
        return this.add(point[0], point[1], point[2]);
    }

    @Override
    public final boolean remove(@NonNull int... point) {
        checkArg(point.length == 3);
        return this.remove(point[0], point[1], point[2]);
    }

    @Override
    public final boolean contains(@NonNull int... point) {
        checkArg(point.length == 3);
        return this.contains(point[0], point[1], point[2]);
    }

    @Override
    public final void forEach(@NonNull Consumer<int[]> callback) {
        this.forEach3D((x, y, z) -> callback.accept(new int[]{ x, y, z }));
    }

    @Override
    public final int countInRange(@NonNull int[] begin, @NonNull int[] end) {
        checkArg(begin.length == 3 && end.length == 3);
        return this.countInRange(begin[0], begin[1], begin[2], end[0], end[1], end[2]);
    }

    @Override
    public final int addAllInRange(@NonNull int[] begin, @NonNull int[] end) {
        checkArg(begin.length == 3 && end.length == 3);
        return this.addAllInRange(begin[0], begin[1], begin[2], end[0], end[1], end[2]);
    }

    @Override
    public abstract boolean add(int x, int y, int z);

    @Override
    public abstract boolean remove(int x, int y, int z);

    @Override
    public abstract boolean contains(int x, int y, int z);

    @Override
    public abstract void forEach3D(@NonNull IntIntIntConsumer callback);

    @Override
    public abstract int countInRange(int beginX, int beginY, int beginZ, int endX, int endY, int endZ);

    @Override
    public abstract int addAllInRange(int beginX, int beginY, int beginZ, int endX, int endY, int endZ);

    @Override
    public int hashCode() {
        final class State implements IntIntIntConsumer {
            int hash;

            @Override
            public void accept(int x, int y, int z) {
                this.hash += ((31 + x) * 31 + y) * 31 + z; //equivalent to Arrays.hashCode(new int[]{ x, y, z })
            }
        }

        State state = new State();
        this.forEach3D(state);
        return state.hash;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append('[');
        this.forEach3D((x, y, z) -> {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append('[').append(x).append(", ").append(y).append(", ").append(z).append(']');
        });
        return builder.append(']').toString();
    }
}
