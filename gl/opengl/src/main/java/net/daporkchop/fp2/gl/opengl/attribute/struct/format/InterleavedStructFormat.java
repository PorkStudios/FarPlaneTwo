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

package net.daporkchop.fp2.gl.opengl.attribute.struct.format;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeWriterImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class InterleavedStructFormat<S> extends StructFormat<S, InterleavedStructLayout> {
    protected final long stride;

    public InterleavedStructFormat(@NonNull OpenGL gl, @NonNull InterleavedStructLayout layout) {
        super(gl, layout);

        this.stride = layout.stride();
    }

    @Override
    public long totalSize() {
        return this.stride;
    }

    /**
     * Copies fields in the layout format from the given source to the given destination.
     *
     * @param srcBase   the source base instance
     * @param srcOffset the source base offset
     * @param dstBase   the destination base instance
     * @param dstOffset the destination base offset
     */
    public abstract void copy(Object srcBase, long srcOffset, Object dstBase, long dstOffset);

    /**
     * Configures the current VAO with this format's attributes at the given attribute indices.
     * <p>
     * This assumes the VAO is currently bound, and that the buffer which will contain the vertex data is bound to {@link OpenGLConstants#GL_ARRAY_BUFFER}.
     *
     * @param api              the {@link GLAPI} instance
     * @param attributeIndices an array for converting struct member indices to vertex attribute indices
     */
    public abstract void configureVAO(@NonNull GLAPI api, @NonNull int[] attributeIndices);

    @Override
    public abstract InterleavedAttributeWriterImpl<?, S> writer(@NonNull AttributeFormatImpl<?, S, ?> attributeFormat);

    @Override
    public abstract InterleavedAttributeBufferImpl<?, S> buffer(@NonNull AttributeFormatImpl<?, S, ?> attributeFormat, @NonNull BufferUsage usage);
}
