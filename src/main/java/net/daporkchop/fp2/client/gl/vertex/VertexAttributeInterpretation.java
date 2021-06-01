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

package net.daporkchop.fp2.client.gl.vertex;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;

/**
 * The different ways a vertex attribute's values may be interpreted.
 *
 * @author DaPorkchop_
 */
public enum VertexAttributeInterpretation {
    INTEGER {
        @Override
        public void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer, int components, @NonNull VertexAttributeType type, int stride, int offset, int divisor) {
            vao.attrI(buffer, components, type.glType(), stride, offset, divisor);
        }
    },
    FLOAT {
        @Override
        public void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer, int components, @NonNull VertexAttributeType type, int stride, int offset, int divisor) {
            vao.attrF(buffer, components, type.glType(), false, stride, offset, divisor);
        }
    },
    NORMALIZED_FLOAT {
        @Override
        public void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer, int components, @NonNull VertexAttributeType type, int stride, int offset, int divisor) {
            vao.attrF(buffer, components, type.glType(), true, stride, offset, divisor);
        }
    };

    /**
     * Appends a vertex attribute to the given {@link VertexArrayObject} configuration using this interpretation.
     *
     * @param vao        the {@link VertexArrayObject}
     * @param buffer     the {@link IGLBuffer} that will contain the vertex data
     * @param components the number of values in the vertex attribute
     * @param type       the vertex attribute's data type
     * @param stride     the distance between occurrences of the vertex attribute in the vertex buffer (in bytes)
     * @param offset     the offset of the vertex attribute from the beginning of the vertex (in bytes). If {@code 0}, the values are tightly packed.
     * @param divisor    the vertex attribute divisor. If {@code 0}, no divisor will be used.
     */
    public abstract void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer, int components, @NonNull VertexAttributeType type, int stride, int offset, int divisor);
}
