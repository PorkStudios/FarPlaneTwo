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
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.buffer.upload.ImmediateBufferUploader;
import net.daporkchop.fp2.gl.util.AbstractTypedBuffer;
import net.daporkchop.lib.common.annotation.param.NotNegative;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class IndexBuffer extends AbstractTypedBuffer {
    private final IndexFormat format;

    /**
     * @return the {@link IndexFormat} which this buffer can store indices for
     */
    public final IndexFormat format() {
        return this.format;
    }

    /**
     * Copies index data from the given index buffer to this index buffer.
     *
     * @param srcIndex  the destination index in this buffer for the data to be copied to
     * @param dstBuffer the source buffer. Must use the same {@link #format() format}
     * @param dstIndex  the offset into the source buffer to begin copying from
     * @param length    the number of indexs to copy
     */
    public abstract void copyTo(int srcIndex, IndexBuffer dstBuffer, int dstIndex, int length);

    /**
     * Copies the index data from the given {@link IndexWriter} into this buffer, discarding any existing data and modifying its capacity.
     *
     * @param writer an {@link IndexWriter} containing the sequence of index data elements to copy
     */
    public void set(IndexWriter writer, BufferUsage usage) {
        int size = writer.size();
        if (this.capacity() != size) {
            this.capacity(size, usage);
        }
        this.setRange(0, writer);
    }

    /**
     * Copies the index data from the given {@link IndexWriter} into a subregion of this buffer.
     *
     * @param startIndex the destination index for the first index data element
     * @param writer     an {@link IndexWriter} containing the sequence of index data elements to copy
     */
    public void setRange(@NotNegative int startIndex, IndexWriter writer) {
        this.setRange(startIndex, writer, ImmediateBufferUploader.instance());
    }

    /**
     * Copies the index data from the given {@link IndexWriter} into a subregion of this buffer.
     * <p>
     * Unless the {@link BufferUploader} implementation specifies otherwise, the uploaded data may not be visible until {@link BufferUploader#flush() flushed}.
     *
     * @param startIndex the destination index for the first index data element
     * @param writer     an {@link IndexWriter} containing the sequence of index data elements to copy
     * @param uploader   a {@link BufferUploader} to be used for uploading the actual index data
     */
    public abstract void setRange(@NotNegative int startIndex, IndexWriter writer, BufferUploader uploader);

    /**
     * @return the underlying {@link GLBuffer}
     */
    public abstract GLBuffer elementsBuffer();
}
