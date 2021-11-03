/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.global;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeWriter;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.buffer.GLBufferImpl;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class GlobalAttributeBufferImpl implements GlobalAttributeBuffer {
    protected final AttributeFormatImpl format;
    protected final GLBufferImpl buffer;

    protected final long stride;

    protected int capacity;

    public GlobalAttributeBufferImpl(@NonNull AttributeFormatImpl format, @NonNull BufferUsage usage) {
        this.format = format;
        this.buffer = format.gl().createBuffer(usage);

        this.stride = format.stridePacked();
    }

    @Override
    public void close() {
        this.buffer.close();
    }

    @Override
    public void resize(int capacity) {
        this.capacity = capacity;

        this.buffer.capacity(capacity * this.stride);
    }

    @Override
    public void set(int index, @NonNull GlobalAttributeWriter _writer) {
        GlobalAttributeWriterImpl writer = (GlobalAttributeWriterImpl) _writer;
        checkArg(writer.format() == this.format, "mismatched attribute formats!");
        checkIndex(this.capacity, index);

        this.buffer.uploadRange(index * this.stride, writer.addr, this.stride);
    }
}
