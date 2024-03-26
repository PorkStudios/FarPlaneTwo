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

package net.daporkchop.fp2.gl.codegen.struct.interleaved;

import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;
import net.daporkchop.fp2.gl.attribute.vao.VertexArrayVertexBuffer;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.Arrays;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInterleavedAttributeBuffer<STRUCT extends AttributeStruct> extends NewAttributeBuffer<STRUCT> {
    public final GLMutableBuffer buffer;
    public final BufferUsage usage;

    public AbstractInterleavedAttributeBuffer(NewAttributeFormat<STRUCT> format, BufferUsage usage) {
        super(format);
        this.usage = usage;
        this.buffer = GLMutableBuffer.create(format.gl());
    }

    @Override
    public void capacity(@NotNegative int capacity) {
        this.buffer.capacity(notNegative(capacity, "capacity") * this.format().size(), this.usage);
        this.capacity = capacity;
    }

    @Override
    public void resize(@NotNegative int capacity) {
        this.buffer.resize(notNegative(capacity, "capacity") * this.format().size(), this.usage);
        this.capacity = capacity;
    }

    @Override
    public void copyTo(int srcIndex, NewAttributeBuffer<STRUCT> dstBuffer, int dstIndex, int length) {
        checkArg(this.getClass() == dstBuffer.getClass(), "incompatible vertex formats: %s\n%s", this.format(), dstBuffer.format());
        checkRangeLen(this.capacity(), dstIndex, length);
        checkRangeLen(dstBuffer.capacity(), dstIndex, length);

        long size = this.format().size();
        this.buffer.copyRange(srcIndex * size, ((AbstractInterleavedAttributeBuffer<STRUCT>) dstBuffer).buffer, dstIndex * size, length * size);
    }

    @Override
    public void set(NewAttributeWriter<STRUCT> writer) {
        checkArg(this.format().getClass() == writer.format().getClass(), "incompatible vertex formats: %s\n%s", this.format(), writer.format());

        int count = writer.size();
        this.buffer.upload(((AbstractInterleavedAttributeWriter<STRUCT>) writer).address, count * this.format().size(), this.usage);
        this.capacity = count;
    }

    @Override
    public void setRange(@NotNegative int startIndex, NewAttributeWriter<STRUCT> writer, BufferUploader uploader) {
        checkArg(this.format().getClass() == writer.format().getClass(), "incompatible vertex formats: %s\n%s", this.format(), writer.format());
        int count = writer.size();
        checkRangeLen(this.capacity, startIndex, count);

        long address = ((AbstractInterleavedAttributeWriter<STRUCT>) writer).address;
        long size = this.format().size();
        uploader.uploadRange(this.buffer, startIndex * size, address, count * size);
    }

    @Override
    public final VertexArrayVertexBuffer[] buffers(int divisor) throws UnsupportedOperationException {
        VertexArrayVertexBuffer[] result = new VertexArrayVertexBuffer[this.format().occupiedVertexAttributes()];
        Arrays.fill(result, VertexArrayVertexBuffer.create(this.buffer.id(), 0L, toIntExact(this.format().size()), divisor));
        return result;
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}
