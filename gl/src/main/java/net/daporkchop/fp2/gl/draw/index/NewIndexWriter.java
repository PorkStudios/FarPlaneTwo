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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.util.AbstractTypedWriter;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class NewIndexWriter extends AbstractTypedWriter {
    private final NewIndexFormat format;

    /**
     * @return the {@link NewIndexFormat} which this writer can write indices for
     */
    public final NewIndexFormat format() {
        return this.format;
    }

    /**
     * Appends a new index to this writer.
     *
     * @param value the index value to append
     * @return this writer
     */
    public abstract NewIndexWriter append(int value);

    /**
     * Appends 4 indices to this writer, forming a single quad.
     *
     * @param oppositeCorner the index of the vertex in the corner opposite the provoking vertex
     * @param c0             the index of one of the edge vertices
     * @param c1             the index of the other edge vertex
     * @param provoking      the index of the provoking vertex
     */
    public NewIndexWriter appendQuad(int oppositeCorner, int c0, int c1, int provoking) {
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
    public NewIndexWriter appendQuadAsTriangles(int oppositeCorner, int c0, int c1, int provoking) {
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
}
