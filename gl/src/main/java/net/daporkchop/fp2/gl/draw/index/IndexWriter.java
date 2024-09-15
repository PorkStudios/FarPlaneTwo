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

package net.daporkchop.fp2.gl.draw.index;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.util.AbstractTypedWriter;
import net.daporkchop.lib.common.annotation.param.NotNegative;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class IndexWriter extends AbstractTypedWriter {
    private final IndexFormat format;

    /**
     * @return the {@link IndexFormat} which this writer can write indices for
     */
    public final IndexFormat format() {
        return this.format;
    }

    /**
     * Appends a new index to this writer.
     *
     * @param value the index value to append
     * @return this writer
     */
    public abstract IndexWriter append(int value);

    /**
     * Appends 4 indices to this writer, forming a single quad.
     *
     * @param oppositeCorner the index of the vertex in the corner opposite the provoking vertex
     * @param c0             the index of one of the edge vertices
     * @param c1             the index of the other edge vertex
     * @param provoking      the index of the provoking vertex
     */
    public IndexWriter appendQuad(int oppositeCorner, int c0, int c1, int provoking) {
        this.reserve(4);

        this.append(c1);
        this.append(oppositeCorner);
        this.append(c0);
        this.append(provoking);
        return this;
    }

    /**
     * Appends 6 indices to this writer, forming a single quad consisting of two triangles.
     *
     * @param oppositeCorner the index of the vertex in the corner opposite the provoking vertex
     * @param c0             the index of one of the edge vertices
     * @param c1             the index of the other edge vertex
     * @param provoking      the index of the provoking vertex
     */
    public IndexWriter appendQuadAsTriangles(int oppositeCorner, int c0, int c1, int provoking) {
        this.reserve(6);

        //first triangle
        this.append(oppositeCorner);
        this.append(c0);
        this.append(provoking);

        //second triangle
        this.append(c1);
        this.append(oppositeCorner);
        this.append(provoking);

        return this;
    }

    /**
     * Gets the index value at the given position in this writer.
     *
     * @param index the index of the index value to get
     * @return the index value
     */
    public abstract int get(int index);

    @Override
    @Deprecated
    public final void copyTo(@NotNegative int srcIndex, @NonNull AbstractTypedWriter dstWriter, @NotNegative int dstIndex) {
        this.copyTo(srcIndex, (IndexWriter) dstWriter, dstIndex);
    }

    @Override
    @Deprecated
    public final void copyTo(@NotNegative int srcIndex, @NonNull AbstractTypedWriter dstWriter, @NotNegative int dstIndex, @NotNegative int length) {
        this.copyTo(srcIndex, (IndexWriter) dstWriter, dstIndex, length);
    }

    /**
     * Copies the element at the given source index to the given destination index in the given destination writer.
     *
     * @param srcIndex  the source index
     * @param dstWriter the destination writer
     * @param dstIndex  the destination index
     */
    public void copyTo(@NotNegative int srcIndex, @NonNull IndexWriter dstWriter, @NotNegative int dstIndex) {
        this.copyTo(srcIndex, dstWriter, dstIndex, 1);
    }

    /**
     * Copies the elements starting at the given source index to the given destination index in the given destination writer
     * <p>
     * The behavior of this method is undefined if the two ranges overlap.
     *
     * @param srcIndex  the source index
     * @param dstWriter the destination writer
     * @param dstIndex  the destination index
     * @param length    the number of elements to copy
     */
    public abstract void copyTo(@NotNegative int srcIndex, @NonNull IndexWriter dstWriter, @NotNegative int dstIndex, @NotNegative int length);

    /**
     * Adds the given value to each of the index values in this writer.
     *
     * @param delta the value to add
     */
    public abstract void offsetIndices(int delta);
}
