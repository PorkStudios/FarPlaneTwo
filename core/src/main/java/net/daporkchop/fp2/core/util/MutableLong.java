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

package net.daporkchop.fp2.core.util;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.fp2.core.util.recycler.Recycler;
import net.daporkchop.fp2.core.util.recycler.SimpleRecycler;
import net.daporkchop.lib.common.reference.cache.Cached;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple type which exposes a {@code long} value which can be modified.
 * <p>
 * Provides a means to emulate "pass-by-reference" with a {@code long} value without the added overhead of a {@link AtomicLong}.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
public final class MutableLong {
    private static final Cached<Recycler<MutableLong>> RECYCLER = Cached.threadLocal(() -> new SimpleRecycler<MutableLong>() {
        @Override
        protected MutableLong allocate0() {
            return new MutableLong(0L);
        }

        @Override
        protected void reset0(@NonNull MutableLong value) {
            value.set(0L);
        }
    });

    /**
     * Gets a {@link Recycler} for recycling {@link MutableLong} instances.
     * <p>
     * The returned {@link Recycler} instance is only valid in the current thread.
     *
     * @return a {@link Recycler}
     */
    public static Recycler<MutableLong> recycler() {
        return RECYCLER.get();
    }

    private long value;

    /**
     * @return the current value
     */
    public long get() {
        return this.value;
    }

    /**
     * Sets the current value to the given value.
     *
     * @param value the new value
     * @return the old value
     */
    public long set(long value) {
        long old = this.value;
        this.value = value;
        return old;
    }
}
