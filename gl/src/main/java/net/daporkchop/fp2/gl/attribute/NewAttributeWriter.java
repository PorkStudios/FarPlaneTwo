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
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

/**
 * An append-only buffer for attribute data of a specific attribute type.
 *
 * @param <STRUCT> the struct type
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class NewAttributeWriter<STRUCT extends AttributeStruct> implements AutoCloseable {
    /**
     * The {@link NewAttributeFormat} which this writer can write vertex attributes for.
     */
    private final NewAttributeFormat<STRUCT> format;

    protected int size = 0;

    /**
     * @return the number of vertices written so far
     */
    public final int size() {
        return this.size;
    }

    /**
     * Gets an instance of {@link STRUCT the attribute struct type} which serves as a handle to access the element at the front of this writer.
     * <p>
     * The handle must be {@link AttributeStruct#close() closed} once the user has finished accessing the data, at which point the handle will become invalid and must not
     * be used again. The handle will also be invalidated when {@link #append() a new element is appended}, so all live handles must be closed before then.
     * <p>
     * Equivalent to {@code this.at(0)}.
     *
     * @return a {@link STRUCT handle} for accessing the element at the front of this writer
     */
    public final STRUCT front() {
        return this.at(0);
    }

    /**
     * Gets an instance of {@link STRUCT the attribute struct type} which serves as a handle to access the element at the back of this writer.
     * <p>
     * The handle must be {@link AttributeStruct#close() closed} once the user has finished accessing the data, at which point the handle will become invalid and must not
     * be used again. The handle will also be invalidated when {@link #append() a new element is appended}, so all live handles must be closed before then.
     * <p>
     * Equivalent to {@code this.at(this.size() - 1)}.
     *
     * @return a {@link STRUCT handle} for accessing the element at the back of this writer
     */
    public final STRUCT back() {
        return this.at(this.size() - 1);
    }

    /**
     * Gets an instance of {@link STRUCT the attribute struct type} which serves as a handle to access the attribute data at the given index.
     * <p>
     * The handle must be {@link AttributeStruct#close() closed} once the user has finished accessing the data, at which point the handle will become invalid and must not
     * be used again. The handle will also be invalidated when {@link #append() a new element is appended}, so all live handles must be closed before then.
     *
     * @param index the index of the element to access
     * @return a {@link STRUCT handle} for accessing the attribute data at the given index
     */
    public abstract STRUCT at(@NotNegative int index);

    /**
     * Appends a new element to the writer by incrementing its position by one, and then gets an instance of {@link STRUCT the attribute struct type} which serves as a handle
     * to access the attribute data at the new position. The element's contents are initially undefined.
     * <p>
     * The handle must be {@link AttributeStruct#close() closed} once the user has finished accessing the data, at which point the handle will become invalid and must not
     * be used again. The handle will also be invalidated when {@link #append() a new element is appended}, so all live handles must be closed before then.
     * <p>
     * Any existing handles to other elements in this writer will be invalidated by this operation.
     *
     * @return a {@link STRUCT} instance for struct data to be written to
     */
    public STRUCT append() {
        this.appendUninitialized();
        return this.back();
    }

    /**
     * Appends a new element to the writer by incrementing its position by one. The element's contents are initially undefined.
     * <p>
     * Any existing handles to other elements in this writer will be invalidated by this operation.
     */
    public abstract void appendUninitialized();

    /**
     * Appends new elements to the writer by incrementing its position by the given {@code count}. The element's contents are initially undefined.
     * <p>
     * Any existing handles to other elements in this writer will be invalidated by this operation.
     *
     * @param count the number of elements to append
     */
    public abstract void appendUninitialized(@Positive int count);

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
