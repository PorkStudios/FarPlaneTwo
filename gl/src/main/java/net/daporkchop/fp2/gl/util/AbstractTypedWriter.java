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

package net.daporkchop.fp2.gl.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of a CPU-side buffer for building sequences of typed data to be uploaded to
 * the GL.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractTypedWriter implements AutoCloseable {
    protected @NotNegative int size;
    protected @Positive int capacity;

    /**
     * @return the number of elements written so far
     */
    public final int size() {
        return this.size;
    }

    /**
     * @return the number of elements which can be stored in this writer before it needs to be resized
     */
    public final int capacity() {
        return this.capacity;
    }

    /**
     * Clears this writer instance, discarding all previously written elements.
     */
    public final void clear() {
        this.size = 0;
    }

    /**
     * Ensures that this writer has sufficient remaining capacity to append at least the given number of elements.
     *
     * @param count the number of additional elements to reserve capacity for
     */
    public final void reserve(@NotNegative int count) {
        int requiredCapacity = Math.addExact(this.size, notNegative(count, "count"));
        if (this.capacity < requiredCapacity) {
            this.grow(requiredCapacity);

            //this will theoretically help the JIT if we have a reserve followed by a fixed number of appends
            if (this.capacity < Math.addExact(this.size, count)) {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Resizes the internal buffer to fit at least the given number of elements.
     *
     * @param requiredCapacity the minimum required capacity
     */
    protected final void grow(@NotNegative int requiredCapacity) {
        int oldCapacity = this.capacity;
        if (oldCapacity < requiredCapacity) {
            //TODO: a more efficient method of computing the new capacity???
            do {
                this.capacity = Math.multiplyExact(this.capacity, 2);
            } while (requiredCapacity > this.capacity);

            this.grow(oldCapacity, this.capacity);
        }
    }

    /**
     * Grows the internal buffer to the given capacity.
     *
     * @param oldCapacity the previous capacity
     * @param newCapacity the new capacity
     */
    protected abstract void grow(@NotNegative int oldCapacity, @NotNegative int newCapacity);

    /**
     * Appends a new element to the writer by incrementing its position by one. The element's contents are initially undefined.
     * <p>
     * Any existing handles to other elements in this writer will be invalidated by this operation.
     */
    public final void appendUninitialized() {
        int newSize = Math.incrementExact(this.size);
        if (newSize > this.capacity) {
            this.grow(newSize);
        }
        this.size = newSize;
    }

    /**
     * Appends new elements to the writer by incrementing its position by the given {@code count}. The element's contents are initially undefined.
     * <p>
     * Any existing handles to other elements in this writer will be invalidated by this operation.
     *
     * @param count the number of elements to append
     */
    public final void appendUninitialized(@Positive int count) {
        int newSize = Math.addExact(this.size, positive(count, "count"));
        if (newSize > this.capacity) {
            this.grow(newSize);
        }
        this.size = newSize;
    }

    /**
     * Copies the element at the given source index to the given destination index.
     *
     * @param src the source index
     * @param dst the destination index
     */
    public void copy(@NotNegative int src, @NotNegative int dst) {
        this.copy(src, dst, 1);
    }

    /**
     * Copies the elements starting at the given source index to the given destination index.
     * <p>
     * The behavior of this method is undefined if the two ranges overlap.
     *
     * @param src    the source index
     * @param dst    the destination index
     * @param length the number of elements to copy
     */
    public abstract void copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length);

    @Override
    public abstract void close();
}
