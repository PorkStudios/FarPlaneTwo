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

package net.daporkchop.fp2.core.engine.client.index.postable;

import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;

/**
 * An indexed table of tile positions stored on the GPU.
 *
 * @author DaPorkchop_
 */
public abstract class RenderPosTable implements AutoCloseable {
    @Override
    public abstract void close();

    /**
     * Adds the given tile position to this table.
     * <p>
     * If the given tile position is already in this table, this method does nothing.
     *
     * @param pos the tile position
     * @return the index of the tile position in the vertex buffer at the corresponding detail level
     */
    public abstract int add(TilePos pos);

    /**
     * Removes the given tile position from this table.
     *
     * @param pos the tile position
     * @return the index of the tile position in the vertex buffer at the corresponding detail level, or a negative value if the tile position was not previously present
     */
    public abstract int remove(TilePos pos);

    /**
     * Makes all changes visible to the GL.
     */
    public abstract void flush();

    /**
     * @param level the detail level
     * @return a reference to the vertex buffer containing the list of tile positions for the given detail level
     */
    public abstract NewAttributeBuffer<VoxelGlobalAttributes> vertexBuffer(int level);

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface _DefragCallback {
        /**
         * Process a notification that a single tile position has been relocated during defragmentation.
         *
         * @param pos      the tile position which was relocated
         * @param oldIndex the tile position's old index
         * @param newIndex the tile position's new index
         */
        void relocated(TilePos pos, int oldIndex, int newIndex);
    }
}
