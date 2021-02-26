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

package net.daporkchop.fp2.client.gl.commandbuffer;

/**
 * Buffer for OpenGL draw commands.
 * <p>
 * Note that draw commands are not guaranteed to be executed in any particular order, although it is likely that they will be executed in
 * the same order in which they were submitted.
 *
 * @author DaPorkchop_
 */
public interface IDrawCommandBuffer extends AutoCloseable {
    /**
     * Prepares to enter a new drawing phase.
     * <p>
     * This will clear any previously buffered commands.
     */
    IDrawCommandBuffer begin();

    /**
     * Issues an indexed draw command.
     *
     * @param x          the X coordinate (in tiles)
     * @param y          the Y coordinate (in tiles)
     * @param z          the Z coordinate (in tiles)
     * @param level      the tile's zoom level
     * @param baseVertex the offset of the first vertex from the beginning of the bound vertex buffer, in vertex size units
     * @param firstIndex the offset of the first index from the beginning of the bound index buffer, in index size units
     * @param count      the number of indices to render
     * @see <a href="https://www.khronos.org/opengl/wiki/GLAPI/glDrawElementsInstancedBaseVertexBaseInstance">OpenGL docs for a more detailed explaination of the {@code baseVertex} and {@code firstIndex} parameters</a>
     */
    void drawElements(int x, int y, int z, int level, int baseVertex, int firstIndex, int count);

    /**
     * Finishes the currently active drawing phase (started by {@link #begin()}).
     */
    @Override
    void close();

    /**
     * Issues the draw commands.
     * <p>
     * This method may be called multiple times.
     */
    void draw();

    /**
     * Deletes the buffer immediately (rather than relying on GC to delete it eventually).
     */
    void delete();
}
