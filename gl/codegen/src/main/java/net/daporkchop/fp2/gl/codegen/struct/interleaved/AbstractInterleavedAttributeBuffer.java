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

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeArray;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;
import net.daporkchop.fp2.gl.attribute.vao.VertexArrayObject;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInterleavedAttributeBuffer<STRUCT extends AttributeStruct> extends NewAttributeBuffer<STRUCT> {
    public final GLBuffer buffer;

    public AbstractInterleavedAttributeBuffer(NewAttributeFormat<STRUCT> format, BufferUsage usage) {
        super(format);
        this.buffer = GLBuffer.create(format.gl(), usage);
    }

    @Override
    public void capacity(@NotNegative int capacity) {
        this.buffer.capacity(notNegative(capacity, "capacity") * this.format().size());
        this.capacity = capacity;
    }

    @Override
    public void resize(@NotNegative int capacity) {
        this.buffer.resize(notNegative(capacity, "capacity") * this.format().size());
        this.capacity = capacity;
    }

    @Override
    public void setContentsFrom(NewAttributeBuffer<STRUCT> buffer) {
        checkArg(this.getClass() == buffer.getClass(), "incompatible vertex formats: %s\n%s", this.format(), buffer.format());
        this.capacity(buffer.capacity());
        this.buffer.copyRange(((AbstractInterleavedAttributeBuffer<STRUCT>) buffer).buffer, 0L, 0L, this.capacity * this.format().size());
    }

    @Override
    public void set(NewAttributeWriter<STRUCT> writer) {
        checkArg(this.format().getClass() == writer.format().getClass(), "incompatible vertex formats: %s\n%s", this.format(), writer.format());

        int count = writer.size();
        this.buffer.upload(((AbstractInterleavedAttributeWriter<STRUCT>) writer).address, count * this.format().size());
        this.capacity = count;
    }

    @Override
    public void set(@NotNegative int startIndex, NewAttributeWriter<STRUCT> writer) {
        checkArg(this.format().getClass() == writer.format().getClass(), "incompatible vertex formats: %s\n%s", this.format(), writer.format());
        int count = writer.size();
        checkRangeLen(this.capacity, startIndex, count);

        long address = ((AbstractInterleavedAttributeWriter<STRUCT>) writer).address;
        long size = this.format().size();
        if (startIndex == 0 && count == this.capacity) { //we're overwriting the entire buffer contents, orphan the old storage for maximum performance
            this.buffer.upload(address, count * size);
        } else {
            this.buffer.uploadRange(startIndex * size, address, count * size);
        }
    }

    @Override
    public STRUCT setToSingle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributeArray<STRUCT> setToMany(@Positive int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int configure(@NotNegative int bindingIndex, @NonNull VertexArrayObject vao, @NotNegative int divisor) throws UnsupportedOperationException {
        int expected = bindingIndex + this.format().occupiedVertexAttributes();
        this.buffer.bind(BufferTarget.ARRAY_BUFFER, target -> {
            int result = this.configure0(bindingIndex, vao, divisor);
            checkState(result == expected, "expected %s, but got %s", expected, result);
        });
        return expected;
    }

    protected abstract int configure0(int bindingIndex, VertexArrayObject vao, int divisor);

    @Override
    public void close() {
        this.buffer.close();
    }
}
