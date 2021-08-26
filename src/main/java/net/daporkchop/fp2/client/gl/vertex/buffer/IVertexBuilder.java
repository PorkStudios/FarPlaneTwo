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
import net.daporkchop.fp2.client.gl.vertex.attribute.IVertexAttribute;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * Temporary buffer which assembles vertex attribute data for later upload to an {@link IVertexBuffer}.
 *
 * @author DaPorkchop_
 */
public interface IVertexBuilder extends RefCounted {
    /**
     * Gets the memory address at which the given vertex attribute's value for the given vertex index should be written.
     *
     * @param attribute an {@link IVertexAttribute}
     * @param vertIndex the index of the vertex to write
     * @return the memory address at which the given vertex attribute's value for the given vertex index should be written
     */
    long addressFor(@NonNull IVertexAttribute attribute, int vertIndex);

    /**
     * Appends a new vertex to this builder.
     * <p>
     * The values of all vertex attributes are undefined.
     *
     * @return the new vertex' index
     */
    int appendVertex();

    /**
     * Appends a new vertex to this builder.
     * <p>
     * The values of all vertex attributes will be copied from the vertex at the given index.
     *
     * @param srcIndex the index of the vertex to copy attribute values from
     * @return the new vertex' index
     */
    int appendDuplicateVertex(int srcIndex);

    /**
     * @return the number of vertices in this builder
     */
    int size();

    /**
     * Clears all previously added vertices.
     */
    void clear();

    @Override
    int refCnt();

    @Override
    IVertexBuilder retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
