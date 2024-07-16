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

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.vao.VertexArrayVertexBuffer;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.buffer.upload.ImmediateBufferUploader;
import net.daporkchop.fp2.gl.util.AbstractTypedBuffer;
import net.daporkchop.lib.common.annotation.param.NotNegative;

/**
 * @param <STRUCT> the struct type
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class NewAttributeBuffer<STRUCT extends AttributeStruct> extends AbstractTypedBuffer {
    private final NewAttributeFormat<STRUCT> format;

    /**
     * @return the {@link NewAttributeFormat} which this buffer can store vertex attributes for
     */
    public final NewAttributeFormat<STRUCT> format() {
        return this.format;
    }

    /**
     * Copies attribute data from the given attribute buffer to this attribute buffer.
     *
     * @param srcIndex  the destination index in this buffer for the data to be copied to
     * @param dstBuffer the source buffer. Must use the same {@link #format() format}
     * @param dstIndex  the offset into the source buffer to begin copying from
     * @param length    the number of attributes to copy
     */
    public abstract void copyTo(int srcIndex, NewAttributeBuffer<STRUCT> dstBuffer, int dstIndex, int length);

    /**
     * Copies the attribute data from the given {@link NewAttributeWriter} into this buffer, discarding any existing data and modifying its capacity.
     *
     * @param writer a {@link NewAttributeWriter} containing the sequence of attribute data elements to copy
     */
    public void set(NewAttributeWriter<STRUCT> writer, BufferUsage usage) {
        int size = writer.size();
        if (this.capacity() != size) {
            this.capacity(size, usage);
        }
        this.setRange(0, writer);
    }

    /**
     * Copies the attribute data from the given {@link NewAttributeWriter} into a subregion of this buffer.
     *
     * @param startIndex the destination index for the first attribute data element
     * @param writer     a {@link NewAttributeWriter} containing the sequence of attribute data elements to copy
     */
    public void setRange(@NotNegative int startIndex, @NonNull NewAttributeWriter<STRUCT> writer) {
        this.setRange(startIndex, writer, ImmediateBufferUploader.instance());
    }

    /**
     * Copies the attribute data from the given {@link NewAttributeWriter} into a subregion of this buffer.
     * <p>
     * Unless the {@link BufferUploader} implementation specifies otherwise, the uploaded data may not be visible until {@link BufferUploader#flush() flushed}.
     *
     * @param startIndex the destination index for the first attribute data element
     * @param writer     a {@link NewAttributeWriter} containing the sequence of attribute data elements to copy
     * @param uploader   a {@link BufferUploader} to be used for uploading the actual attribute data
     */
    public abstract void setRange(@NotNegative int startIndex, @NonNull NewAttributeWriter<STRUCT> writer, @NonNull BufferUploader uploader);

    /**
     * @param divisor the vertex attribute divisor
     * @return the vertex buffers corresponding to each vertex attribute in this buffer's vertex attribute format
     * @throws UnsupportedOperationException if this attribute buffer uses an {@link NewAttributeFormat attribute format} which doesn't support vertex attributes, or if the {@code divisor} is greater than {@code 0} and {@link net.daporkchop.fp2.gl.GLExtension#GL_ARB_instanced_arrays} isn't supported
     */
    public abstract VertexArrayVertexBuffer[] buffers(@NotNegative int divisor) throws UnsupportedOperationException;

    /**
     * @return the {@link GLBuffer} which should be bound to a {@link net.daporkchop.fp2.gl.OpenGLConstants#GL_UNIFORM_BUFFER} binding point to use this buffer's contents as a UBO array
     * @apiNote the returned {@link GLBuffer} should not be modified in any way!
     * @throws UnsupportedOperationException if this attribute buffer uses an {@link NewAttributeFormat attribute format} which doesn't support UBOs
     */
    public abstract GLBuffer bufferUBO() throws UnsupportedOperationException;

    /**
     * @return the {@link GLBuffer} which should be bound to a {@link net.daporkchop.fp2.gl.OpenGLConstants#GL_SHADER_STORAGE_BUFFER} binding point to use this buffer's contents as an SSBO array
     * @apiNote the returned {@link GLBuffer} should not be modified in any way!
     * @throws UnsupportedOperationException if this attribute buffer uses an {@link NewAttributeFormat attribute format} which doesn't support SSBOs
     */
    public abstract GLBuffer bufferSSBO() throws UnsupportedOperationException;
}
