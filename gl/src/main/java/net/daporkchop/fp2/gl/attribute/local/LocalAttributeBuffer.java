/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.gl.attribute.local;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BaseAttributeBuffer;

/**
 * A resizeable array of local attribute data in server memory.
 *
 * @author DaPorkchop_
 */
public interface LocalAttributeBuffer<S> extends BaseAttributeBuffer<S, LocalAttributeFormat<S>> {
    /**
     * @return the number of attribute data elements that this buffer can store
     */
    int capacity();

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
     * Copies the attribute data from the given {@link LocalAttributeWriter} into this buffer.
     *
     * @param startIndex the destination index for the first attribute data element
     * @param writer     a {@link LocalAttributeWriter} containing the sequence of attribute data elements to copy
     */
    void set(int startIndex, @NonNull LocalAttributeWriter<S> writer);
}
