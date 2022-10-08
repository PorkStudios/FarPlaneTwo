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
 */

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.buffer.GLBuffer;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class InterleavedAttributeBufferImpl<F extends InterleavedAttributeFormatImpl<F, S>, S> extends AttributeBufferImpl<F, S> {
    protected final GLBuffer buffer;

    protected int capacity;

    public InterleavedAttributeBufferImpl(@NonNull F format, @NonNull BufferUsage usage) {
        super(format);

        this.buffer = this.gl.createBuffer(usage);
    }

    @Override
    public void close() {
        this.buffer.close();
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    /**
     * Generated code overrides this and delegates to {@link #capacity_withStride(int, long)} with the stride as an additional parameter.
     */
    @Override
    public abstract void capacity(int capacity);

    protected void capacity_withStride(int capacity, @Positive long stride) {
        this.capacity = notNegative(capacity, "capacity");
        this.buffer.capacity(capacity * stride);
    }

    /**
     * Generated code overrides this and delegates to {@link #resize_withStride(int, long)} with the stride as an additional parameter.
     */
    @Override
    public abstract void resize(int capacity);

    protected void resize_withStride(int capacity, @Positive long stride) {
        this.capacity = notNegative(capacity, "capacity");
        this.buffer.resize(capacity * stride);
    }

    @Override
    public void invalidate(int startIndex, int count) {
        checkRangeLen(this.capacity, startIndex, count);
        //no-op
    }

    @Override
    public void setContentsFrom(@NonNull AttributeBuffer<S> _buffer) {
        InterleavedAttributeBufferImpl<F, S> buffer = (InterleavedAttributeBufferImpl<F, S>) _buffer;
        checkArg(buffer.getClass() == this.getClass(), "mismatched formats!");

        long size = buffer.buffer().capacity();
        this.buffer.capacity(size);
        this.buffer.copyRange(buffer.buffer, 0L, 0L, size);
    }

    /**
     * Generated code overrides this and delegates to {@link #set_withStride(AttributeWriter, long)} with the stride as an additional parameter.
     */
    @Override
    public abstract void set(@NonNull AttributeWriter<S> _writer);

    protected void set_withStride(@NonNull AttributeWriter<S> _writer, @Positive long stride) {
        InterleavedAttributeWriterImpl<F, S> writer = (InterleavedAttributeWriterImpl<F, S>) _writer;
        checkArg(writer.format() == this.format(), "mismatched formats!");

        int size = writer.size();
        this.capacity = size;
        this.buffer.upload(writer.baseAddr, size * stride);
    }

    /**
     * Generated code overrides this and delegates to {@link #set_withStride(int, AttributeWriter, long)} with the stride as an additional parameter.
     */
    @Override
    public abstract void set(int startIndex, @NonNull AttributeWriter<S> _writer);

    protected void set_withStride(int startIndex, @NonNull AttributeWriter<S> _writer, @Positive long stride) {
        InterleavedAttributeWriterImpl<F, S> writer = (InterleavedAttributeWriterImpl<F, S>) _writer;
        checkArg(writer.format() == this.format(), "mismatched formats!");
        checkRangeLen(this.capacity, startIndex, writer.size());

        this.buffer.uploadRange(startIndex * stride, writer.baseAddr, writer.size() * stride);
    }

    /**
     * Generated code overrides this and delegates to {@link #setToSingle_withStride(long, long)} with the stride as an additional parameter.
     */
    @Override
    public abstract S setToSingle();

    /**
     * Generated code overrides this and delegates to {@link #setToSingle_withStride(long, long)} with the stride as an additional parameter.
     */
    public abstract void setToSingle(long addr);

    protected void setToSingle_withStride(long addr, @Positive long stride) {
        this.capacity = 1;
        this.buffer.upload(addr, stride);
    }
}
