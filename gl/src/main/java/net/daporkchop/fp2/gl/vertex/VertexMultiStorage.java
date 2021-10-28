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
 * A container which is able to store multiple {@link VertexBuffer}s.
 * <p>
 * This is conceptually implemented as an array of "slots" and an array of vertices, where a vertex consists of all local attribute values, and a slot consists of all global attribute
 * values as well as a reference to a range of the vertices array.
 *
 * @author DaPorkchop_
 */
public interface VertexMultiStorage {
    /**
     * @return the {@link VertexFormat} used by this storage
     */
    VertexFormat format();

    /**
     * @return the current number of slots
     */
    int capacity();

    /**
     * Stores the data in the given {@link VertexBuffer} in this storage.
     *
     * @param buffer the {@link VertexBuffer} whose data should be stored
     * @return a handle which can be used to reference the inserted data in this storage
     */
    int put(@NonNull VertexBuffer buffer);

    /**
     * Removes the vertex data inserted into this storage with the given handle.
     * <p>
     * The handle will be invalidated, and the corresponding {@link #instanceId(int)} and {@link #baseVertex(int)} will become available for new data to use.
     * <p>
     * If this method is called
     *
     * @param handle the handle
     */
    void remove(int handle);

    /**
     * Gets the instance ID for the vertex data inserted into this storage with the given handle.
     *
     * @param handle the handle
     * @return the instance ID for the corresponding vertex data
     */
    int instanceId(int handle);

    /**
     * Gets the base vertex index for the vertex data inserted into this storage with the given handle.
     *
     * @param handle the handle
     * @return the base vertex index for the corresponding vertex data
     */
    int baseVertex(int handle);
}
