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

package net.daporkchop.fp2.gl.codegen.draw.index;

import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class IndexBufferImpl extends IndexBuffer {
    //none of these methods have codegen because this class isn't really performance-critical

    private final GLMutableBuffer buffer;

    IndexBufferImpl(IndexFormat format, OpenGL gl) {
        super(format);
        this.buffer = GLMutableBuffer.create(gl);
    }

    @Override
    public void capacity(@NotNegative int capacity, BufferUsage usage) {
        this.buffer.capacity(notNegative(capacity, "capacity") * this.format().size(), usage);
        this.capacity = capacity;
    }

    @Override
    public void resize(@NotNegative int capacity, BufferUsage usage) {
        this.buffer.resize(notNegative(capacity, "capacity") * this.format().size(), usage);
        this.capacity = capacity;
    }

    @Override
    public void copyTo(int srcIndex, IndexBuffer dstBuffer, int dstIndex, int length) {
        checkArg(this.format() == dstBuffer.format(), "incompatible index formats: %s\n%s", this.format(), dstBuffer.format());
        checkRangeLen(this.capacity(), dstIndex, length);
        checkRangeLen(dstBuffer.capacity(), dstIndex, length);

        long size = this.format().size();
        this.buffer.copyRange(srcIndex * size, ((IndexBufferImpl) dstBuffer).buffer, dstIndex * size, length * size);
    }

    @Override
    public void set(IndexWriter writer, BufferUsage usage) {
        checkArg(this.format() == writer.format(), "incompatible index formats: %s\n%s", this.format(), writer.format());

        int count = writer.size();
        this.buffer.upload(((AbstractIndexWriter) writer).address, count * this.format().size(), usage);
        this.capacity = count;
    }

    @Override
    public void setRange(@NotNegative int startIndex, IndexWriter writer, BufferUploader uploader) {
        checkArg(this.format() == writer.format(), "incompatible index formats: %s\n%s", this.format(), writer.format());
        int count = writer.size();
        checkRangeLen(this.capacity(), startIndex, count);

        long address = ((AbstractIndexWriter) writer).address;
        long size = this.format().size();
        uploader.uploadRange(this.buffer, startIndex * size, address, count * size);
    }

    @Override
    public GLBuffer elementsBuffer() {
        return this.buffer;
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}
