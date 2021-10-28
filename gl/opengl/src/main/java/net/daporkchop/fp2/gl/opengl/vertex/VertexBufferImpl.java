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

package net.daporkchop.fp2.gl.opengl.vertex;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.vertex.VertexBuffer;
import net.daporkchop.fp2.gl.vertex.VertexWriter;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class VertexBufferImpl implements VertexBuffer {
    @Getter
    protected final VertexFormatImpl format;

    protected final OpenGL gl;
    protected final GLAPI api;

    @Getter
    protected int capacity;

    public VertexBufferImpl(@NonNull VertexFormatImpl format) {
        this.gl = format.gl;
        this.api = this.gl.api();
        this.format = format;
    }

    /**
     * @author DaPorkchop_
     */
    public static class Interleaved extends VertexBufferImpl {
        protected final long stride;
        protected final GLBuffer buffer;

        public Interleaved(@NonNull VertexFormatImpl format, @NonNull BufferUsage usage) {
            super(format);

            this.stride = format.size();
            this.buffer = this.gl.createBuffer(usage);
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
        public void set(int startIndex, @NonNull VertexWriter writerIn) {
            VertexWriterImpl.Interleaved writer = (VertexWriterImpl.Interleaved) writerIn;
            checkArg(writerIn.format() == this.format, "mismatched vertex formats!");
            checkRangeLen(this.capacity, startIndex, writerIn.size());

            this.buffer.uploadRange(startIndex * this.stride, writer.addr, writer.size() * this.stride);
        }
    }
}
