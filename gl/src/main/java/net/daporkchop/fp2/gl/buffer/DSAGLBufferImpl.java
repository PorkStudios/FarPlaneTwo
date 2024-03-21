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

import static net.daporkchop.lib.common.util.PValidation.checkRangeLen;
import static net.daporkchop.lib.common.util.PValidation.notNegative;

/**
 * Implementation of {@link GLBuffer} which uses {@link net.daporkchop.fp2.gl.GLExtension#GL_ARB_direct_state_access ARB_direct_state_access} to operate on buffers without having to
 * bind and unbind them.
 *
 * @author DaPorkchop_
 */
public class DSAGLBufferImpl extends GLBuffer {
    public DSAGLBufferImpl(OpenGL gl, BufferUsage usage) {
        super(gl, usage, gl.glCreateBuffer());
    }

    @Override
    public void capacity(long capacity) {
        notNegative(capacity, "capacity");

        this.gl.glNamedBufferData(this.id, capacity, 0L, this.usage);
        this.capacity = capacity;
    }

    @Override
    public void upload(long addr, long size) {
        notNegative(size, "size");

        this.gl.glNamedBufferData(this.id, size, addr, this.usage);
        this.capacity = size;
    }

    @Override
    public void upload(@NonNull ByteBuffer data) {
        this.gl.glNamedBufferData(this.id, data, this.usage);
        this.capacity = data.remaining();
    }

    @Override
    protected void uploadComposite(@NonNull ByteBuf data) {
        int readableBytes = data.readableBytes();

        //initialize storage
        this.gl.glNamedBufferData(this.id, readableBytes, 0L, this.usage);
        this.capacity = readableBytes;

        //upload each data block individually
        long offset = 0L;
        for (ByteBuffer nioBuffer : data.nioBuffers()) {
            this.gl.glNamedBufferSubData(this.id, offset, nioBuffer);
            offset += nioBuffer.remaining();
        }
    }

    @Override
    public void uploadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.gl.glNamedBufferSubData(this.id, start, size, addr);
    }

    @Override
    public void uploadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.gl.glNamedBufferSubData(this.id, start, data);
    }

    @Override
    protected void uploadRangeComposite(long start, @NonNull ByteBuf data) {
        checkRangeLen(this.capacity, start, data.readableBytes());

        //upload each data block individually
        long offset = start;
        for (ByteBuffer nioBuffer : data.nioBuffers()) {
            this.gl.glBufferSubData(this.id, offset, nioBuffer);
            offset += nioBuffer.remaining();
        }
    }

    @Override
    public void downloadRange(long start, long addr, long size) {
        checkRangeLen(this.capacity, start, size);

        this.gl.glGetNamedBufferSubData(this.id, start, size, addr);
    }

    @Override
    public void downloadRange(long start, @NonNull ByteBuffer data) {
        checkRangeLen(this.capacity, start, data.remaining());

        this.gl.glGetNamedBufferSubData(this.id, start, data);
    }

    @Override
    public void copyRange(@NonNull GLBuffer src, long srcOffset, long dstOffset, long size) {
        checkRangeLen(src.capacity(), srcOffset, size);
        checkRangeLen(this.capacity, dstOffset, size);

        this.gl.glCopyNamedBufferSubData(src.id, this.id, srcOffset, dstOffset, size);
    }

    @Override
    protected void map(int access, LongConsumer callback) {
        long addr = this.gl.glMapNamedBuffer(this.id, access);
        try {
            callback.accept(addr);
        } finally {
            this.gl.glUnmapNamedBuffer(this.id);
        }
    }

    @Override
    protected void mapRange(int access, long offset, long length, LongConsumer callback) {
        long addr = this.gl.glMapNamedBufferRange(this.id, offset, length, access);
        try {
            callback.accept(addr);
        } finally {
            this.gl.glUnmapNamedBuffer(this.id);
        }
    }

    @Override
    protected Mapping map(int access) {
        return new Mapping(this.gl.glMapNamedBuffer(this.id, access, this.capacity, null));
    }

    @Override
    protected Mapping mapRange(int access, long offset, long length) {
        return new Mapping(this.gl.glMapNamedBufferRange(this.id, offset, length, access, null));
    }

    @Override
    protected void unmap() {
        this.gl.glUnmapNamedBuffer(this.id);
    }
}
