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

package net.daporkchop.fp2.gl.attribute;

import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;

import java.util.function.Function;

/**
 * @author DaPorkchop_
 */
public interface AttributeFormat<S> extends BaseAttributeFormat {
    /**
     * @return the number of bytes used by each element in a {@link AttributeBuffer} using this format
     */
    @Override
    long size();

    AttributeWriter<S> createWriter();

    AttributeBuffer<S> createBuffer(@NonNull BufferUsage usage);

    /**
     * @author DaPorkchop_
     */
    interface Vertex<S> extends AttributeFormat<S> {
        /**
         * @return the number of vertex attribute binding locations taken up by this vertex attribute format
         */
        int occupiedVertexAttributes();

        /**
         * Configures the vertex attribute binding locations of all the vertex attributes referenced by this vertex attribute format.
         *
         * @param gl               the OpenGL context
         * @param program          the name of the OpenGL shader program to be configured
         * @param nameFormatter    a function for adjusting field names (e.g. adding a prefix or suffix)
         * @param baseBindingIndex the base vertex attribute binding index
         */
        void bindVertexAttributeLocations(@NonNull OpenGL gl, int program, @NonNull Function<String, String> nameFormatter, int baseBindingIndex);
    }
}
