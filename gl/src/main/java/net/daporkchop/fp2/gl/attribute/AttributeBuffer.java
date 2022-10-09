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

package net.daporkchop.fp2.gl.attribute;

import lombok.NonNull;
import net.daporkchop.lib.common.annotation.param.Positive;

/**
 * A resizeable array of attribute data in server memory.
 *
 * @author DaPorkchop_
 */
public interface AttributeBuffer<S> extends BaseAttributeBuffer {
    /**
     * @return the {@link AttributeFormat} used by this buffer
     */
    @Override
    AttributeFormat<S> format();

    /**
     * @return the number of attribute data elements that this buffer can store
     */
    int capacity();

    /**
     * Sets the capacity of this buffer.
     * <p>
     * After this method returns, the buffer's contents are undefined.
     *
     * @param capacity the new capacity
     */
    void capacity(int capacity);

    /**
     * Sets the capacity of this buffer.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with undefined contents.
     *
     * @param capacity the new capacity
     */
    void resize(int capacity);

    /**
     * Invalidates the element at the given index. After calling this method, the element's contents are undefined.
     * <p>
     * When invalidating multiple sequential elements, {@link #invalidate(int, int)} will likely be more performant.
     *
     * @param index the index of the element to invalidate
     */
    default void invalidate(int index) {
        this.invalidate(index, 1);
    }

    /**
     * Invalidates all elements in the given range. After calling this method, the contents of all elements in the range are undefined.
     *
     * @param startIndex the index of the first element to invalidate (inclusive)
     * @param count      the number of elements to invalidate
     */
    void invalidate(int startIndex, int count);

    /**
     * Sets this buffer's contents to a copy of the data from the given buffer, discarding any existing data and modifying its capacity.
     *
     * @param buffer the buffer containing the element data. Must use the same {@link AttributeFormat}
     */
    void setContentsFrom(@NonNull AttributeBuffer<S> buffer);

    /**
     * Copies the attribute data from the given {@link AttributeWriter} into this buffer, discarding any existing data and modifying its capacity.
     *
     * @param writer a {@link AttributeWriter} containing the sequence of attribute data elements to copy
     */
    default void set(@NonNull AttributeWriter<S> writer) {
        int size = writer.size();
        if (this.capacity() != size) {
            this.capacity(size);
        }
        this.set(0, writer);
    }

    /**
     * Copies the attribute data from the given {@link AttributeWriter} into this buffer.
     *
     * @param startIndex the destination index for the first attribute data element
     * @param writer     a {@link AttributeWriter} containing the sequence of attribute data elements to copy
     */
    void set(int startIndex, @NonNull AttributeWriter<S> writer);

    /**
     * Gets an instance of {@link S the attribute struct type} which serves as a handle to write a single element of attribute data. When the instance is
     * {@link AttributeStruct#close() closed}, this buffer's {@link #capacity() capacity} will be set to {@code 1} and its contents set to the data values written to
     * the handle.
     * <p>
     * The handle must be {@link AttributeStruct#close() closed} once the user has finished writing the data. Any changes made to this buffer's contents or capacity after
     * this method is invoked will be silently overwritten when the handle is {@link AttributeStruct#close() closed}.
     * <p>
     * The handle's contents are initially undefined.
     *
     * @return a {@link S handle} for writing attribute values to and setting this buffer's contents
     */
    S setToSingle();

    /**
     * Gets an instance of {@link AttributeArray} for {@link S the attribute struct type} which serves as a handle to write multiple elements of attribute data. When the
     * instance is {@link AttributeArray#close() closed}, this buffer's {@link #capacity() capacity} will be set to {@code length} and its contents set to the data
     * values written to the array.
     * <p>
     * The array must be {@link AttributeArray#close() closed} once the user has finished writing the data. Any changes made to this buffer's contents or capacity after
     * this method is invoked will be silently overwritten when the array is {@link AttributeArray#close() closed}.
     * <p>
     * The array's contents are initially undefined.
     *
     * @return an {@link AttributeArray} for writing attribute values to and setting this buffer's contents
     */
    AttributeArray<S> setToMany(@Positive int length);
}
