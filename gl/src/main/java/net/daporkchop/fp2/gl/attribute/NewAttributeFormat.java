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
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.OpenGL;

import java.util.EnumSet;
import java.util.function.Function;

/**
 * @param <STRUCT> the struct type
 * @author DaPorkchop_
 */
public abstract class NewAttributeFormat<STRUCT extends AttributeStruct> {
    private final OpenGL gl;
    private final EnumSet<AttributeTarget> validTargets;
    private final long size;

    public NewAttributeFormat(OpenGL gl, EnumSet<AttributeTarget> validTargets, long size) {
        this.gl = gl;
        this.validTargets = validTargets.clone();
        this.size = size;
    }

    /**
     * @return the OpenGL context which this attribute format may be used for
     */
    public final OpenGL gl() {
        return this.gl;
    }

    /**
     * The number of bytes occupied by a single element using this format.
     */
    public final long size() {
        return this.size;
    }

    /**
     * Checks if this attribute format is suitable for the given {@link AttributeTarget}.
     *
     * @param target the {@link AttributeTarget}
     * @return {@code true} if this attribute format is suitable for the given {@link AttributeTarget}
     */
    public final boolean supports(AttributeTarget target) {
        return this.validTargets.contains(target);
    }

    public final boolean supportsVertexAttribute() {
        return this.supports(AttributeTarget.VERTEX_ATTRIBUTE);
    }

    public final boolean supportsShaderStorageBuffer() {
        return this.supports(AttributeTarget.SSBO);
    }

    public final boolean supportsUniformBuffer() {
        return this.supports(AttributeTarget.UBO);
    }

    /**
     * Creates a new {@link NewAttributeBuffer} for storing attributes using this attribute format.
     *
     * @return the created {@link NewAttributeBuffer}
     */
    public abstract NewAttributeBuffer<STRUCT> createBuffer();

    /**
     * Creates a new {@link NewAttributeWriter} for writing attributes using this attribute format.
     *
     * @param alloc a {@link DirectMemoryAllocator} to use for allocating memory
     * @return the created {@link NewAttributeWriter}
     */
    public abstract NewAttributeWriter<STRUCT> createWriter(@NonNull DirectMemoryAllocator alloc);

    /**
     * @return the number of vertex attribute binding locations taken up by this vertex attribute format
     * @throws UnsupportedOperationException if this attribute format doesn't {@link #supportsVertexAttribute() support vertex attributes}
     */
    public abstract int occupiedVertexAttributes() throws UnsupportedOperationException;

    /**
     * Configures the vertex attribute binding locations of all the vertex attributes referenced by this vertex attribute format.
     *
     * @param program          the name of the OpenGL shader program to be configured
     * @param nameFormatter    a function for adjusting field names (e.g. adding a prefix or suffix)
     * @param baseBindingIndex the base vertex attribute binding index
     * @throws UnsupportedOperationException if this attribute format doesn't {@link #supportsVertexAttribute() support vertex attributes}
     */
    public abstract void bindVertexAttributeLocations(int program, @NonNull Function<String, String> nameFormatter, int baseBindingIndex) throws UnsupportedOperationException;

    /**
     * Creates a new {@link NewUniformBuffer} for storing individual shader uniforms using this attribute format.
     *
     * @return the created {@link NewUniformBuffer}
     * @throws UnsupportedOperationException if this attribute format doesn't {@link #supportsUniformBuffer() support uniform buffers}
     */
    public abstract NewUniformBuffer<STRUCT> createUniformBuffer() throws UnsupportedOperationException;
}
