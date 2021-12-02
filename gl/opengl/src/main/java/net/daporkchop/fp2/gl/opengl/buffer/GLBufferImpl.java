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

package net.daporkchop.fp2.gl.opengl.buffer;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class GLBufferImpl implements GLBuffer {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected long capacity = -1L;

    protected final int id;
    protected final int usage;

    public GLBufferImpl(@NonNull OpenGL gl, @NonNull BufferUsage usage) {
        this.gl = gl;
        this.api = gl.api();
        this.usage = GLEnumUtil.from(usage);

        this.id = this.api.glGenBuffer();
        this.gl.resourceArena().register(this, this.id, this.api::glDeleteBuffer);

        this.capacity(0L);
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    @Override
    public void capacity(long capacity) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.api.glBufferData(target.target(), this.capacity = notNegative(capacity, "capacity"), 0L, this.usage);
        });
    }

    @Override
    public void resize(long capacity) {
        long retainedCapacity = min(this.capacity, notNegative(capacity, "capacity"));

        if (retainedCapacity <= 0L) { //previous capacity was unset, so no data needs to be retained
            this.capacity(capacity);
            return;
        } else if (this.capacity == capacity) { //capacity remains unchanged, nothing to do!
            return;
        }

        long buffer = PUnsafe.allocateMemory(retainedCapacity);
        try {
            //download data to main memory
            this.downloadRange(0L, buffer, retainedCapacity);

            //update capacity
            this.capacity(capacity);

            //re-upload retained data
            this.uploadRange(0L, buffer, retainedCapacity);
        } finally {
            PUnsafe.freeMemory(buffer);
        }
    }

    @Override
    public void upload(long addr, long size) {
        notNegative(size, "size");

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.api.glBufferData(target.target(), size, addr, this.usage);
            this.capacity = size;
        });
    }

    @Override
    public void upload(@NonNull ByteBuffer data) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.api.glBufferData(target.target(), data, this.usage);
            this.capacity = data.remaining();
        });
    }

    @Override
    public void upload(@NonNull ByteBuf data) {
        if (data.nioBufferCount() == 1) { //fast path: upload whole buffer contents at once
            this.upload(data.nioBuffer());
        } else { //slower fallback path for composite buffers
            this.uploadComposite(data);
        }
    }

    protected void uploadComposite(@NonNull ByteBuf data) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            int readableBytes = data.readableBytes();

            //initialize storage
            this.api.glBufferData(target.target(), readableBytes, 0L, this.usage);
            this.capacity = readableBytes;

            //upload each data block individually
            long offset = 0L;
            for (ByteBuffer nioBuffer : data.nioBuffers()) {
                this.api.glBufferSubData(target.target(), offset, nioBuffer);
                offset += nioBuffer.remaining();
            }
        });
    }

    @Override
    public void uploadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.api.glBufferSubData(target.target(), start, size, addr);
        });
    }

    @Override
    public void uploadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.api.glBufferSubData(target.target(), start, data);
        });
    }

    @Override
    public void uploadRange(long start, @NonNull ByteBuf data) {
        if (data.nioBufferCount() == 1) { //fast path: upload whole buffer contents at once
            this.uploadRange(start, data.nioBuffer());
        } else { //slower fallback path for composite buffers
            this.uploadRangeComposite(start, data);
        }
    }

    protected void uploadRangeComposite(long start, @NonNull ByteBuf data) {
        checkRangeLen(this.capacity, start, data.readableBytes());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            //upload each data block individually
            long offset = start;
            for (ByteBuffer nioBuffer : data.nioBuffers()) {
                this.api.glBufferSubData(target.target(), offset, nioBuffer);
                offset += nioBuffer.remaining();
            }
        });
    }

    @Override
    public void downloadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.api.glGetBufferSubData(target.target(), start, size, addr);
        });
    }

    @Override
    public void downloadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            this.api.glGetBufferSubData(target.target(), start, data);
        });
    }

    public void bind(@NonNull BufferTarget target, @NonNull Consumer<BufferTarget> callback) {
        int old = this.api.glGetInteger(target.binding());
        try {
            this.api.glBindBuffer(target.target(), this.id);

            callback.accept(target);
        } finally {
            this.api.glBindBuffer(target.target(), old);
        }
    }

    @Override
    public void map(boolean read, boolean write, @NonNull LongConsumer callback) {
        assert read || write : "at least one of read or write must be enabled!";

        int access = read
                ? write ? GL_READ_WRITE : GL_READ_ONLY
                : write ? GL_WRITE_ONLY : -1;

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            long addr = this.api.glMapBuffer(target.target(), access);
            try {
                callback.accept(addr);
            } finally {
                this.api.glUnmapBuffer(target.target());
            }
        });
    }
}
