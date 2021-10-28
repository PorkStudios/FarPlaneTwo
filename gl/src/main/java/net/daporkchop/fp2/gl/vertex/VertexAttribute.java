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

/**
 * A single vertex attribute.
 *
 * @author DaPorkchop_
 */
public interface VertexAttribute {
    /**
     * @return this attribute's name
     */
    String name();

    /**
     * @return whether or not this vertex attribute is global
     */
    boolean global();

    /**
     * @return the number of components in this vertex attribute
     */
    int components();

    /**
     * @return this vertex attribute's type
     */
    VertexAttributeType type();

    /**
     * @return this vertex attribute's interpretation
     */
    VertexAttributeInterpretation interpretation();

    /**
     * A vertex attribute consisting of a single integer component.
     *
     * @author DaPorkchop_
     */
    interface Int1 extends VertexAttribute {
        @Override
        default int components() {
            return 1;
        }
    }

    /**
     * A vertex attribute consisting of two integer components.
     *
     * @author DaPorkchop_
     */
    interface Int2 extends VertexAttribute {
        @Override
        default int components() {
            return 2;
        }
    }

    /**
     * A vertex attribute consisting of three integer components.
     *
     * @author DaPorkchop_
     */
    interface Int3 extends VertexAttribute {
        @Override
        default int components() {
            return 3;
        }
    }

    /**
     * A vertex attribute consisting of four integer components.
     *
     * @author DaPorkchop_
     */
    interface Int4 extends VertexAttribute {
        @Override
        default int components() {
            return 4;
        }
    }
}
