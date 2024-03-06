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

import java.util.EnumSet;
import java.util.function.Function;

/**
 * @param <STRUCT> the struct type
 * @author DaPorkchop_
 */
public interface NewAttributeFormat<STRUCT extends AttributeStruct> {
    /**
     * The number of bytes occupied by a single element using this format.
     */
    long size();

    /**
     * The {@link AttributeTarget} which this attribute format is suitable for.
     */
    EnumSet<AttributeTarget> validTargets();

    /**
     * Checks if this attribute format is suitable for the given {@link AttributeTarget}.
     *
     * @param target the {@link AttributeTarget}
     * @return {@code true} if this attribute format is suitable for the given {@link AttributeTarget}
     */
    default boolean supports(AttributeTarget target) {
        return this.validTargets().contains(target);
    }

    /**
     * Creates a new {@link NewAttributeWriter} for writing attributes using this attribute format.
     *
     * @return the created {@link NewAttributeWriter}
     */
    NewAttributeWriter<STRUCT> createWriter();

    /**
     * Creates a new {@link NewAttributeBuffer} for storing attributes using this attribute format.
     *
     * @return the created {@link NewAttributeBuffer}
     */
    NewAttributeBuffer<STRUCT> createBuffer();

    /**
     * @param <STRUCT> the struct type
     * @author DaPorkchop_
     */
    interface Vertex<STRUCT extends AttributeStruct> extends NewAttributeFormat<STRUCT> {
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

    /**
     * @param <STRUCT> the struct type
     * @author DaPorkchop_
     */
    interface Uniform<STRUCT extends AttributeStruct> extends NewAttributeFormat<STRUCT> {
        /**
         * Creates a new {@link NewUniformBuffer} for storing individual shader uniforms using this attribute format.
         *
         * @return the created {@link NewUniformBuffer}
         */
        NewUniformBuffer<STRUCT> createUniformBuffer();
    }
}
