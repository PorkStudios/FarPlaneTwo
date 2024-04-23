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

package net.daporkchop.fp2.core.engine.client.bake.storage;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.BakeOutput;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.draw.index.NewIndexBuffer;
import net.daporkchop.fp2.gl.draw.index.NewIndexFormat;

import java.util.Map;

/**
 * Associates vertex and index data for a tile position and pass index with a location in a {@link net.daporkchop.fp2.gl.attribute.vao.VertexArrayObject}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class BakeStorage<VertexType extends AttributeStruct> implements AutoCloseable {
    /**
     * The OpenGL context.
     */
    public final OpenGL gl;

    /**
     * The {@link BufferUploader} which will be used for uploading bake data into buffers.
     * <p>
     * This is not owned by the bake storage. User code is responsible for ticking, flushing and closing the uploader, although the bake storage
     * may explicitly flush the uploader if necessary.
     */
    public final BufferUploader bufferUploader;

    /**
     * The format of vertex data in this storage.
     */
    public final NewAttributeFormat<VertexType> vertexFormat;

    /**
     * The format of index data in this storage.
     */
    public final NewIndexFormat indexFormat;

    @Override
    public abstract void close();

    /**
     * Updates the data in this storage.
     * <p>
     * Note that the updated data may be buffered in the {@link BufferUploader buffer uploader} instance, the user must explicitly {@link BufferUploader#flush() flush} the
     * uploader to ensure that all changes are made visible.
     *
     * @param changes a {@link Map} indicating the tiles whose data has been updated. A value of {@code null} indicates that the corresponding tile
     *                has been removed
     */
    public abstract void update(Map<TilePos, BakeOutput<VertexType>> changes);

    /**
     * Finds the locations of the render data for the tile at the given position.
     *
     * @param pos the tile position
     * @return an array containing the locations of the corresponding index and vertex data for each render pass, or {@code null} if the tile couldn't be found
     */
    public abstract Location[] find(TilePos pos);

    /**
     * @param level the detail level
     * @param pass  the render pass index
     * @return a reference to the vertex buffer containing the vertex data for the given pass
     */
    public abstract NewAttributeBuffer<VertexType> vertexBuffer(int level, int pass);

    /**
     * @param level the detail level
     * @param pass  the render pass index
     * @return a reference to the index buffer containing the index data for the given pass
     */
    public abstract NewIndexBuffer indexBuffer(int level, int pass);

    /**
     * @return debug statistics about this storage's current state
     */
    public abstract DebugStats.Renderer stats();

    /**
     * The location of the tile data for a given render pass within this storage.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static final class Location {
        /**
         * The base vertex index.
         */
        public final int baseVertex;
        /**
         * The number of vertex indices after {@link #baseVertex} which belong to this location.
         * <p>
         * This is probably only useful for {@code glDrawRangeElements}.
         */
        public final int vertexCount;
        /**
         * The base index of the tile's index data.
         */
        public final int firstIndex;
        /**
         * The number of indices to draw, starting at {@link #firstIndex}
         */
        public final int count;
    }
}
