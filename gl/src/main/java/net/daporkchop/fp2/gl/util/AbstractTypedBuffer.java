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
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of a wrapper around one or more {@link net.daporkchop.fp2.gl.buffer.GLBuffer OpenGL buffers} for storing
 * sequences of typed data.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractTypedBuffer implements AutoCloseable {
    protected @NotNegative int capacity;

    /**
     * @return the number of data elements that this buffer can store
     */
    public final int capacity() {
        return this.capacity;
    }

    /**
     * Sets the capacity of this buffer.
     * <p>
     * After this method returns, the buffer's contents are undefined.
     *
     * @param capacity the new capacity
     * @param usage    the expected usage flag for the new buffer storage
     */
    public abstract void capacity(@NotNegative int capacity, BufferUsage usage);

    /**
     * Sets the capacity of this buffer.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with undefined contents.
     *
     * @param capacity the new capacity
     * @param usage    the expected usage flag for the new buffer storage
     */
    public abstract void resize(@NotNegative int capacity, BufferUsage usage);

    /**
     * Invalidates the element at the given index. After calling this method, the element's contents are undefined.
     * <p>
     * When invalidating multiple sequential elements, {@link #invalidate(int, int)} will likely be more performant.
     *
     * @param index the index of the element to invalidate
     */
    public void invalidate(@NotNegative int index) {
        this.invalidate(index, 1);
    }

    /**
     * Invalidates all elements in the given range. After calling this method, the contents of all elements in the range are undefined.
     *
     * @param startIndex the index of the first element to invalidate (inclusive)
     * @param count      the number of elements to invalidate
     */
    public void invalidate(int startIndex, int count) {
        checkRangeLen(this.capacity(), startIndex, count);
        //default implementation does nothing
    }

    @Override
    public abstract void close();
}
