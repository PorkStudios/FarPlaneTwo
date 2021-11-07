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

package net.daporkchop.fp2.gl.opengl.attribute.local;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.opengl.buffer.GLBufferImpl;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeWriter;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class LocalAttributeBufferImpl extends BaseAttributeBufferImpl implements LocalAttributeBuffer {
    protected final GLBufferImpl buffer;
    protected final long stride;

    protected int capacity;

    public LocalAttributeBufferImpl(@NonNull AttributeFormatImpl format, @NonNull BufferUsage usage) {
        super(format);

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

        this.buffer.resize(capacity * this.stride);
    }

    @Override
    public void set(int startIndex, @NonNull LocalAttributeWriter _writer) {
        LocalAttributeWriterImpl writer = (LocalAttributeWriterImpl) _writer;
        checkArg(writer.format() == this.format, "mismatched attribute formats!");
        checkRangeLen(this.capacity, startIndex, writer.size());

        this.buffer.uploadRange(startIndex * this.stride, writer.addr, writer.size() * this.stride);
    }

    public void bindVertexAttribute(@NonNull GLAPI api, int bindingIndex, @NonNull AttributeImpl attrib) {
        this.buffer.bind(BufferTarget.ARRAY_BUFFER, target -> attrib.configureVertexAttribute(api, bindingIndex, this.format.offsetsPacked()[attrib.index()], toInt(this.stride, "stride")));
    }
}
