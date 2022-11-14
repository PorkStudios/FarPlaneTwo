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

import net.daporkchop.lib.common.annotation.param.NotNegative;

/**
 * A fixed-size, random-access container for attribute data of a specific attribute type.
 * <p>
 * An {@link AttributeArray} is a linear, finite sequence of elements of attribute data of a specific attribute type. Aside from its content, the essential property of
 * an {@link AttributeArray} is its length:
 *
 * <ul>
 *     <li>an {@link AttributeArray}'s {@code length} is the number of elements it contains. The length is never negative and never changes.</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
public interface AttributeArray<S> extends BaseAttributeAccess {
    /**
     * @return the {@link AttributeFormat} used by this writer
     */
    @Override
    AttributeFormat<S> format();

    /**
     * @return the number of vertices written so far
     */
    int length();

    /**
     * Gets an instance of {@link S the attribute struct type} which serves as a handle to access the attribute data at the given index.
     * <p>
     * The handle must be {@link AttributeStruct#close() closed} once the user has finished accessing the data, at which point the handle will become invalid and must not
     * be used again. The handle will also be invalidated when the array is {@link #close() closed}, so all live handles must be closed before then.
     *
     * @param index the index of the element to access
     * @return a {@link S handle} for accessing the attribute data at the given index
     */
    S element(@NotNegative int index);

    /**
     * Copies the attribute values at the given source index to the given destination index.
     *
     * @param src the source index
     * @param dst the destination index
     */
    AttributeArray<S> copy(@NotNegative int src, @NotNegative int dst);

    /**
     * Copies the attribute values at the given source index to the given destination index.
     * <p>
     * The behavior of this method is undefined if the two ranges overlap.
     *
     * @param src    the source index
     * @param dst    the destination index
     * @param length the number of elements to copy
     */
    AttributeArray<S> copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length);
}
