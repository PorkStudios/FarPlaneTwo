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

package net.daporkchop.fp2.core.util.recycler;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.util.IReusablePersistent;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A super trivial, unsafe recycler for re-usable objects.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
//directly extend ArrayDeque to eliminate one indirection
public abstract class SimpleRecycler<V> extends ArrayDeque<V> implements Recycler<V> {
    /**
     * Creates a new {@link Recycler} which uses the given {@link Supplier} to allocate new values.
     *
     * @param factory the {@link Supplier} used to allocate new values
     * @return a new {@link Recycler}
     */
    public static <V> Recycler<V> withFactory(@NonNull Supplier<? extends V> factory) {
        return new SimpleRecycler<V>() {
            @Override
            protected V allocate0() {
                return factory.get();
            }

            @Override
            protected void reset0(@NonNull V value) {
                //no-op
            }
        };
    }

    @Override
    public V allocate() {
        return this.isEmpty()
                ? Objects.requireNonNull(this.allocate0(), "allocate0 returned null!")
                : this.pop();
    }

    /**
     * Allocates a new instance.
     *
     * @return the newly allocated instance
     */
    protected abstract V allocate0();

    @Override
    public void release(@NonNull V value) {
        this.reset0(value);
        this.push(value);
    }

    /**
     * Resets the instance to its initial state so that it can be re-used again in the future.
     *
     * @param value the instance to reset
     */
    protected abstract void reset0(@NonNull V value);

    /**
     * Implementation of {@link SimpleRecycler} which is able to handle {@link IReusablePersistent} values.
     * <p>
     * Values are created using a {@link Supplier} provided at construction time.
     *
     * @param <V> the value type
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    public static class OfReusablePersistent<V extends IReusablePersistent> extends SimpleRecycler<V> {
        @NonNull
        protected final Supplier<V> factory;

        @Override
        protected V allocate0() {
            return this.factory.get();
        }

        @Override
        protected void reset0(@NonNull V value) {
            value.reset();
        }
    }
}
