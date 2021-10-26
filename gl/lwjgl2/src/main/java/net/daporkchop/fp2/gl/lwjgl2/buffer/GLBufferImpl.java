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

package net.daporkchop.fp2.gl.lwjgl2.buffer;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.lwjgl2.LWJGL2;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class GLBufferImpl implements GLBuffer {
    protected static int bufferUsage(@NonNull BufferUsage usage) {
        switch (usage) {
            case STREAM_DRAW:
                return GL_STREAM_DRAW;
            case STREAM_READ:
                return GL_STREAM_READ;
            case STREAM_COPY:
                return GL_STREAM_COPY;
            case STATIC_DRAW:
                return GL_STATIC_DRAW;
            case STATIC_READ:
                return GL_STATIC_READ;
            case STATIC_COPY:
                return GL_STATIC_COPY;
            case DYNAMIC_READ:
                return GL_DYNAMIC_READ;
            case DYNAMIC_DRAW:
                return GL_DYNAMIC_DRAW;
            case DYNAMIC_COPY:
                return GL_DYNAMIC_COPY;
            default:
                throw new IllegalStateException();
        }
    }

    protected final LWJGL2 gl;

    protected long capacity = -1L;

    protected final int id;
    protected final int usage;

    public GLBufferImpl(@NonNull LWJGL2 gl, @NonNull BufferUsage usage) {
        this.gl = gl;
        this.usage = bufferUsage(usage);

        this.id = glGenBuffers();
        this.gl.resourceArena().register(this, this.id, GL15::glDeleteBuffers);
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    @Override
    public void capacity(long capacity) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            glBufferData(target.id(), this.capacity = notNegative(capacity, "capacity"), this.usage);
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

        if (size <= Integer.MAX_VALUE) { //data is small enough that we can upload it in one run
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                glBufferData(target.id(), DirectBufferHackery.wrapByte(addr, (int) size), this.usage);
                this.capacity = size;
            });
        } else { //data is too big to upload all at once, we'll need to resort to uploading it in smaller blocks
            //allocate storage
            this.capacity(size);

            //upload data
            this.uploadRange(0L, addr, size);
        }
    }

    @Override
    public void upload(@NonNull ByteBuffer data) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            glBufferData(target.id(), data, this.usage);
            this.capacity = data.remaining();
        });
    }

    @Override
    public void upload(@NonNull ByteBuf data) {
        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            int readableBytes = data.readableBytes();

            if (data.hasMemoryAddress()) { //fast-track: upload whole buffer contents at once
                glBufferData(target.id(), DirectBufferHackery.wrapByte(data.memoryAddress() + data.readerIndex(), readableBytes), this.usage);
            } else { //assume the buffer is a composite (we don't care about heap buffers lol)
                glBufferData(target.id(), readableBytes, this.usage);
                ByteBuffer[] nioBuffers = data.nioBuffers(data.readerIndex(), readableBytes);
                long off = 0L;
                for (ByteBuffer nioBuffer : nioBuffers) {
                    int size = nioBuffer.remaining();
                    glBufferSubData(target.id(), off, nioBuffer);
                    off += size;
                }
            }
            this.capacity = readableBytes;
        });
    }

    @Override
    public void uploadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            //LWJGL2 doesn't expose glBufferSubData with a direct memory pointer, so we'll upload the data in increments of Integer.MAX_VALUE
            for (long offset = 0L, blockSize; offset < size; offset += blockSize) {
                blockSize = min(size - offset, Integer.MAX_VALUE);
                glBufferSubData(target.id(), start + offset, DirectBufferHackery.wrapByte(addr + offset, toInt(blockSize)));
            }
        });
    }

    @Override
    public void uploadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            glBufferSubData(target.id(), start, data);
        });
    }

    @Override
    public void uploadRange(long start, @NonNull ByteBuf data) {
        int readableBytes = data.readableBytes();
        checkRangeLen(this.capacity, start, readableBytes);

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            if (data.hasMemoryAddress()) { //fast-track: upload whole buffer contents at once
                glBufferSubData(target.id(), start, data.internalNioBuffer(data.readerIndex(), readableBytes));
            } else { //assume the buffer is a composite (we don't care about heap buffers lol)
                long offset = start;
                for (ByteBuffer nioBuffer : data.nioBuffers(data.readerIndex(), readableBytes)) {
                    int size = nioBuffer.remaining();
                    glBufferSubData(target.id(), offset, nioBuffer);
                    offset += size;
                }
            }
        });
    }

    @Override
    public void downloadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            //LWJGL2 doesn't expose glGetBufferSubData with a direct memory pointer, so we'll download the data in increments of Integer.MAX_VALUE
            for (long offset = 0L, blockSize; offset < size; offset += blockSize) {
                blockSize = min(size - offset, Integer.MAX_VALUE);
                glGetBufferSubData(target.id(), start + offset, DirectBufferHackery.wrapByte(addr + offset, toInt(blockSize)));
            }
        });
    }

    @Override
    public void downloadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            glGetBufferSubData(target.id(), start, data);
        });
    }

    public void bind(@NonNull BufferTarget target, @NonNull Consumer<BufferTarget> callback) {
        int old = glGetInteger(target.binding());
        try {
            glBindBuffer(target.id(), this.id);

            callback.accept(target);
        } finally {
            glBindBuffer(target.id(), old);
        }
    }

    /**
     * Binds this buffer to a shader buffer binding slot.
     *
     * @param target the binding type
     * @param index  the binding index
     */
    public void bindBase(int target, int index) {
        glBindBufferBase(target, index, this.id);
    }

    @Override
    public void map(boolean read, boolean write, @NonNull LongConsumer callback) {
        assert read || write : "at least one of read or write must be enabled!";

        int access = read
                ? write ? GL_READ_WRITE : GL_READ_ONLY
                : write ? GL_WRITE_ONLY : -1;

        this.bind(BufferTarget.ARRAY_BUFFER, target -> {
            ByteBuffer buffer = glMapBuffer(target.id(), access, null);
            try {
                callback.accept(PUnsafe.pork_directBufferAddress(buffer));
            } finally {
                glUnmapBuffer(target.id());
            }
        });
    }
}
