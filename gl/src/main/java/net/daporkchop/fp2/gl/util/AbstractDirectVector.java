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

import lombok.NonNull;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractDirectVector implements AutoCloseable {
    protected static final int DEFAULT_INITIAL_CAPACITY = 16;

    private final DirectMemoryAllocator alloc;
    protected long address;
    protected @NotNegative int size;
    protected @Positive int capacity;
    protected final @Positive int elementSize;

    protected AbstractDirectVector(@NonNull DirectMemoryAllocator alloc, @Positive int initialCapacity, @Positive int elementSize) {
        this.alloc = alloc;

        this.size = 0;
        this.capacity = positive(initialCapacity, "initialCapacity");
        this.elementSize = positive(elementSize, "elementSize");
        this.address = alloc.alloc(Math.multiplyExact(initialCapacity, (long) elementSize));
    }

    /**
     * @return the number of elements written so far
     */
    public final int size() {
        return this.size;
    }

    /**
     * @return the number of elements which can be added to this writer before it needs to be resized
     */
    public final int capacity() {
        return this.capacity;
    }

    /**
     * Clears this instance, discarding all previously contained elements.
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
    protected void grow(@NotNegative int oldCapacity, @NotNegative int newCapacity) {
        this.address = this.alloc.realloc(this.address, newCapacity * (long) this.elementSize);
    }

    /**
     * Appends a new element to the writer by incrementing its position by one. The element's contents are initially undefined.
     * <p>
     * Any existing handles to other elements in this vector will be invalidated by this operation.
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
     * Any existing handles to other elements in this vector will be invalidated by this operation.
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
     * Appends a new element to the writer by incrementing its position by one. The element's contents are initially filled with zero bytes.
     * <p>
     * Any existing handles to other elements in this vector will be invalidated by this operation.
     */
    public final void appendZero() {
        this.appendUninitialized();
        this.setZero(this.size - 1);
    }

    /**
     * Appends new elements to the writer by incrementing its position by the given {@code count}. The element's contents are initially filled with zero bytes.
     * <p>
     * Any existing handles to other elements in this vector will be invalidated by this operation.
     *
     * @param count the number of elements to append
     */
    public final void appendZero(@Positive int count) {
        this.appendUninitialized(count);
        this.setZero(this.size - count, count);
    }

    /**
     * Fills the element at the given index with zero bytes.
     *
     * @param index the index of the element to fill with zero bytes
     */
    public void setZero(@NotNegative int index) {
        this.setZero(index, 1);
    }

    /**
     * Fills the elements starting at the given index with zero bytes.
     *
     * @param start the index of the first element to fill with zero bytes
     * @param count the number of elements to fill with zero bytes
     */
    public void setZero(@NotNegative int start, @NotNegative int count) {
        checkRangeLen(this.size, start, count);
        PUnsafe.setMemory(this.address + start * (long) this.elementSize, count * (long) this.elementSize, (byte) 0);
    }

    /**
     * Fills the elements starting at the given index with zero bytes.
     *
     * @param start the index of the first element to fill with zero bytes
     * @param count the number of elements to fill with zero bytes
     */
    protected final void setZero(@NotNegative int start, @NotNegative int count, @Positive long elementSize) {
        checkRangeLen(this.size, start, count);
        PUnsafe.setMemory(this.address + start * elementSize, count * elementSize, (byte) 0);
    }

    /**
     * @return a view of this vector's contents as a direct {@link ByteBuffer}
     * @throws RuntimeException if this vector's contents are too large to be stored in a single {@link ByteBuffer}
     */
    public ByteBuffer byteBufferView() {
        return DirectBufferHackery.wrapByte(this.address, Math.multiplyExact(this.size, this.elementSize));
    }

    @Override
    public final void close() {
        this.alloc.free(this.address);
    }

    /**
     * Gets the address of the element with the given index in this vector.
     *
     * @param index       the element index
     * @param elementSize the size (in bytes) of an element
     * @return the address of the element with the given index
     * @throws IndexOutOfBoundsException if the given element index is out of bounds
     */
    protected final long getElementPtr(@NotNegative int index, @Positive long elementSize) {
        checkIndex(this.size, index);
        return this.address + index * elementSize;
    }

    /**
     * Gets the base address of the element range with the given starting index and length in this vector.
     *
     * @param index       the index of the first element in the range
     * @param count       the range length
     * @param elementSize the size (in bytes) of an element
     * @return the base address of the element range
     * @throws IndexOutOfBoundsException if the given element range indices are out of bounds
     */
    protected final long getElementRangeBasePtr(@NotNegative int index, @NotNegative int count, @Positive long elementSize) {
        checkRangeLen(this.size, index, count);
        return this.address + index * elementSize;
    }
}
