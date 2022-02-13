/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.buffer.GLBuffer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class InterleavedAttributeBufferImpl<F extends InterleavedAttributeFormatImpl<F, S>, S> extends AttributeBufferImpl<F, S> {
    protected final InterleavedStructFormat<S> structFormat;

    protected final GLBuffer buffer;
    protected final long stride;

    protected int capacity;

    public InterleavedAttributeBufferImpl(@NonNull F format, @NonNull BufferUsage usage) {
        super(format);
        this.structFormat = format.structFormat();

        this.buffer = this.gl.createBuffer(usage);
        this.stride = this.structFormat.stride();
    }

    @Override
    public void close() {
        this.buffer.close();
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public void capacity(int capacity) {
        this.capacity = notNegative(capacity, "capacity");
        this.buffer.capacity(capacity * this.stride);
    }

    @Override
    public void resize(int capacity) {
        this.capacity = notNegative(capacity, "capacity");
        this.buffer.resize(capacity * this.stride);
    }

    @Override
    public void invalidate(int startIndex, int count) {
        checkRangeLen(this.capacity, startIndex, count);
        //no-op
    }

    @Override
    public void setContents(@NonNull S struct) {
        this.structFormat.upload(struct, this.buffer);
        this.capacity = 1;
    }

    @Override
    public void setContents(@NonNull S... structs) {
        this.structFormat.upload(structs, this.buffer);
        this.capacity = structs.length;
    }

    @Override
    public void setContentsFrom(@NonNull AttributeBuffer<S> _buffer) {
        InterleavedAttributeBufferImpl<F, S> buffer = (InterleavedAttributeBufferImpl<F, S>) _buffer;
        checkArg(buffer.structFormat() == this.structFormat, "mismatched struct formats!");

        long size = buffer.buffer().capacity();
        this.buffer.capacity(size);
        this.buffer.copyRange(buffer.buffer, 0L, 0L, size);
    }

    @Override
    public void set(int startIndex, @NonNull AttributeWriter<S> _writer) {
        InterleavedAttributeWriterImpl<F, S> writer = (InterleavedAttributeWriterImpl<F, S>) _writer;
        checkArg(writer.structFormat() == this.structFormat, "mismatched struct formats!");
        checkRangeLen(this.capacity, startIndex, writer.size());

        this.buffer.uploadRange(startIndex * this.stride, writer.baseAddr, writer.size() * this.stride);
    }
}
