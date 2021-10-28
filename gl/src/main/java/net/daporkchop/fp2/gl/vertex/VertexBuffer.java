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

package net.daporkchop.fp2.gl.vertex;

import lombok.NonNull;

/**
 * A buffer in client memory which is used for building sequences of vertex data which will later be saved in a {@link VertexMultiStorage}.
 * <p>
 * Vertex buffers are only able to operate on one vertex at a time, however attributes may be assigned in an arbitrary order.
 * <p>
 * Global attributes may be written at any time, and will not affect nor be affected by local attributes. However, local attributes may only be written when a vertex is
 * currently being written to (more precisely, between calls to {@link #beginEmptyVertex()}/{@link #beginCopiedVertex(int)} and {@link #endVertex()}). Attempting to do otherwise will
 * result in undefined behavior.
 *
 * @author DaPorkchop_
 */
public interface VertexBuffer {
    /**
     * @return the {@link VertexFormat} used by this buffer
     */
    VertexFormat format();

    /**
     * Appends a new vertex to the buffer and begins writing to it.
     * <p>
     * All attribute values in the new vertex are undefined.
     */
    VertexBuffer beginEmptyVertex();

    /**
     * Appends a new vertex to the buffer and begins writing to it.
     * <p>
     * All attribute values in the new vertex are copied from the vertex with the given index.
     *
     * @param srcVertexIndex the index of the vertex from which attribute values are to be copied
     */
    VertexBuffer beginCopiedVertex(int srcVertexIndex);

    /**
     * Finishes writing to the current vertex.
     *
     * @return the index of the completed vertex
     */
    int endVertex();

    /**
     * Sets the given {@link VertexAttribute.Int1} of the current vertex to the given value.
     *
     * @param attrib the {@link VertexAttribute.Int1}
     * @param v0     the value of the 0th component
     */
    VertexBuffer set(@NonNull VertexAttribute.Int1 attrib, int v0);

    /**
     * Sets the given {@link VertexAttribute.Int2} of the current vertex to the given value.
     *
     * @param attrib the {@link VertexAttribute.Int2}
     * @param v0     the value of the 0th component
     * @param v1     the value of the 1st component
     */
    VertexBuffer set(@NonNull VertexAttribute.Int2 attrib, int v0, int v1);

    /**
     * Sets the given {@link VertexAttribute.Int3} of the current vertex to the given value.
     *
     * @param attrib the {@link VertexAttribute.Int3}
     * @param v0     the value of the 0th component
     * @param v1     the value of the 1st component
     * @param v2     the value of the 2nd component
     */
    VertexBuffer set(@NonNull VertexAttribute.Int3 attrib, int v0, int v1, int v2);

    /**
     * Sets the given {@link VertexAttribute.Int3} of the current vertex to the given value.
     *
     * @param attrib the {@link VertexAttribute.Int3}
     * @param argb   the ARGB8888 value. the 4 color channels correspond to the 3 components as follows:<br>
     *               <ul>
     *                   <li>A {@code ->} <i>discarded</i></li>
     *                   <li>R {@code ->} 0</li>
     *                   <li>G {@code ->} 1</li>
     *                   <li>B {@code ->} 2</li>
     *               </ul>
     */
    VertexBuffer setARGB(@NonNull VertexAttribute.Int3 attrib, int argb);

    /**
     * Sets the given {@link VertexAttribute.Int4} of the current vertex to the given value.
     *
     * @param attrib the {@link VertexAttribute.Int4}
     * @param v0     the value of the 0th component
     * @param v1     the value of the 1st component
     * @param v2     the value of the 2nd component
     * @param v3     the value of the 3rd component
     */
    VertexBuffer set(@NonNull VertexAttribute.Int4 attrib, int v0, int v1, int v2, int v3);

    /**
     * Sets the given {@link VertexAttribute.Int4} of the current vertex to the given value.
     *
     * @param attrib the {@link VertexAttribute.Int4}
     * @param argb   the ARGB8888 value. the 4 color channels correspond to the 4 components as follows:<br>
     *               <ul>
     *                   <li>A {@code ->} 0</li>
     *                   <li>R {@code ->} 1</li>
     *                   <li>G {@code ->} 2</li>
     *                   <li>B {@code ->} 3</li>
     *               </ul>
     */
    VertexBuffer setARGB(@NonNull VertexAttribute.Int4 attrib, int argb);
}
