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

package net.daporkchop.fp2.gl.attribute;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

import static net.daporkchop.lib.common.util.PValidation.checkRange;
import static net.daporkchop.lib.common.util.PValidation.checkRangeLen;

/**
 * Base implementation of an {@link AttributeFormat}.
 *
 * @param <STRUCT> the struct type
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class NewAttributeBuffer<STRUCT extends AttributeStruct> implements AutoCloseable {
    /**
     * The {@link NewAttributeFormat} which this writer can write vertex attributes for.
     */
    private final NewAttributeFormat<STRUCT> format;

    protected int capacity;

    /**
     * @return the number of attribute data elements that this buffer can store
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
     */
    public abstract void capacity(@NotNegative int capacity);

    /**
     * Sets the capacity of this buffer.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with undefined contents.
     *
     * @param capacity the new capacity
     */
    public abstract void resize(@NotNegative int capacity);

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

    /**
     * Sets this buffer's contents to a copy of the data from the given buffer, discarding any existing data and modifying its capacity.
     *
     * @param buffer the buffer containing the element data. Must use the same {@link AttributeFormat}
     */
    public abstract void setContentsFrom(@NonNull NewAttributeBuffer<STRUCT> buffer);

    /**
     * Copies the attribute data from the given {@link AttributeWriter} into this buffer, discarding any existing data and modifying its capacity.
     *
     * @param writer a {@link AttributeWriter} containing the sequence of attribute data elements to copy
     */
    public void set(@NonNull NewAttributeWriter<STRUCT> writer) {
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
    public abstract void set(@NotNegative int startIndex, @NonNull NewAttributeWriter<STRUCT> writer);

    /**
     * Gets an instance of {@link STRUCT the attribute struct type} which serves as a handle to write a single element of attribute data. When the instance is
     * {@link AttributeStruct#close() closed}, this buffer's {@link #capacity() capacity} will be set to {@code 1} and its contents set to the data values written to
     * the handle.
     * <p>
     * The handle must be {@link AttributeStruct#close() closed} once the user has finished writing the data. Any changes made to this buffer's contents or capacity after
     * this method is invoked will be silently overwritten when the handle is {@link AttributeStruct#close() closed}.
     * <p>
     * The handle's contents are initially undefined.
     *
     * @return a {@link STRUCT handle} for writing attribute values to and setting this buffer's contents
     */
    public abstract STRUCT setToSingle();

    /**
     * Gets an instance of {@link AttributeArray} for {@link STRUCT the attribute struct type} which serves as a handle to write multiple elements of attribute data. When the
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
    public abstract AttributeArray<STRUCT> setToMany(@Positive int length);

    @Override
    public abstract void close();
}
