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

package net.daporkchop.fp2.gl.buffer;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link GLBuffer} which uses the standard OpenGL functions to operate on buffers.
 * <p>
 * Almost all operations will require the buffer to be bound and unbound.
 *
 * @author DaPorkchop_
 */
public class BasicGLBufferImpl extends GLBuffer {
    public BasicGLBufferImpl(OpenGL gl, BufferUsage usage) {
        super(gl, usage, gl.glGenBuffer());
    }

    @Override
    public void capacity(long capacity) {
        notNegative(capacity, "capacity");

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glBufferData(target.id(), capacity, 0L, this.usage);
            this.capacity = capacity;
        });
    }

    @Override
    public void upload(long addr, long size) {
        notNegative(size, "size");

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glBufferData(target.id(), size, addr, this.usage);
            this.capacity = size;
        });
    }

    @Override
    public void upload(@NonNull ByteBuffer data) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glBufferData(target.id(), data, this.usage);
            this.capacity = data.remaining();
        });
    }

    @Override
    protected void uploadComposite(@NonNull ByteBuf data) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            int readableBytes = data.readableBytes();

            //initialize storage
            this.gl.glBufferData(target.id(), readableBytes, 0L, this.usage);
            this.capacity = readableBytes;

            //upload each data block individually
            long offset = 0L;
            for (ByteBuffer nioBuffer : data.nioBuffers()) {
                this.gl.glBufferSubData(target.id(), offset, nioBuffer);
                offset += nioBuffer.remaining();
            }
        });
    }

    @Override
    public void uploadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glBufferSubData(target.id(), start, size, addr);
        });
    }

    @Override
    public void uploadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glBufferSubData(target.id(), start, data);
        });
    }

    @Override
    protected void uploadRangeComposite(long start, @NonNull ByteBuf data) {
        checkRangeLen(this.capacity, start, data.readableBytes());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            //upload each data block individually
            long offset = start;
            for (ByteBuffer nioBuffer : data.nioBuffers()) {
                this.gl.glBufferSubData(target.id(), offset, nioBuffer);
                offset += nioBuffer.remaining();
            }
        });
    }

    @Override
    public void downloadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glGetBufferSubData(target.id(), start, size, addr);
        });
    }

    @Override
    public void downloadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glGetBufferSubData(target.id(), start, data);
        });
    }

    @Override
    public void copyRange(@NonNull GLBuffer src, long srcOffset, long dstOffset, long size) {
        checkRangeLen(src.capacity(), srcOffset, size);
        checkRangeLen(this.capacity, dstOffset, size);

        src.bind(BufferTarget.COPY_READ_BUFFER, srcTarget -> {
            this.bind(BufferTarget.COPY_WRITE_BUFFER, dstTarget -> {
                this.gl.glCopyBufferSubData(srcTarget.id(), dstTarget.id(), srcOffset, dstOffset, size);
            });
        });
    }

    @Override
    protected void map(int access, LongConsumer callback) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            long addr = this.gl.glMapBuffer(target.id(), access);
            try {
                callback.accept(addr);
            } finally {
                this.gl.glUnmapBuffer(target.id());
            }
        });
    }

    @Override
    protected void mapRange(int access, long offset, long length, LongConsumer callback) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            long addr = this.gl.glMapBufferRange(target.id(), offset, length, access);
            try {
                callback.accept(addr);
            } finally {
                this.gl.glUnmapBuffer(target.id());
            }
        });
    }

    @Override
    protected Mapping map(int access) {
        return this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            return new Mapping(this.gl.glMapBuffer(target.id(), access, this.capacity, null));
        });
    }

    @Override
    protected Mapping mapRange(int access, long offset, long length) {
        return this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            return new Mapping(this.gl.glMapBufferRange(target.id(), offset, length, access, null));
        });
    }

    @Override
    protected void unmap() {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.gl.glUnmapBuffer(target.id());
        });
    }
}
