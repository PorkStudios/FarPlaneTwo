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

package net.daporkchop.fp2.client.gl.vertex.buffer;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * A container for storing groups of vertex attributes.
 *
 * @author DaPorkchop_
 */
public interface IVertexBuffer extends RefCounted {
    /**
     * Configures the given {@link VertexArrayObject} with the vertex attributes in this buffer.
     *
     * @param vao the {@link VertexArrayObject} to configure
     */
    void configureVAO(@NonNull VertexArrayObject vao);

    /**
     * @return the vertex format used by the vertices in this buffer
     */
    VertexFormat format();

    /**
     * @return a new {@link IVertexBuilder} compatible with this vertex buffer
     */
    IVertexBuilder builder();

    /**
     * @return the number of vertices that this buffer can store
     */
    int capacity();

    /**
     * Sets the capacity of this vertex buffer.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with undefined contents.
     *
     * @param capacity the new capacity
     */
    void resize(int capacity);

    /**
     * Sets the vertex data for a range of vertices.
     * <p>
     * The number of vertices is computed based on the number of readable bytes in the buffer and the vertex format.
     *
     * @param startIndex the index in this vertex buffer to begin copying vertices into
     * @param builder    a {@link IVertexBuilder} containing the vertex data. The behavior is undefined if the returned builder wasn't created by this buffer's {@link #builder()}
     *                   method
     */
    void set(int startIndex, @NonNull IVertexBuilder builder);

    @Override
    int refCnt();

    @Override
    IVertexBuffer retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
