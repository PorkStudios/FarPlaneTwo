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

package net.daporkchop.fp2.gl.opengl.buffer;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.lib.common.pool.handle.Handle;

import java.nio.ByteBuffer;

/**
 * @author DaPorkchop_
 */
public class UploadCopyingGLBufferImpl extends SimpleGLBufferImpl {
    protected static final long UPLOADRANGE_COPY_THRESHOLD = 1L << 20L; //1MiB

    public UploadCopyingGLBufferImpl(@NonNull OpenGL gl, @NonNull BufferUsage usage) {
        super(gl, usage);
    }

    @Override
    public void uploadRange(long start, long addr, long size) {
        if (size >= UPLOADRANGE_COPY_THRESHOLD) {
            super.uploadRange(start, addr, size);
            return;
        }

        try (Handle<GLBuffer> handle = this.gl.tmpBufferPool().get()) {
            GLBuffer tmpBuffer = handle.get();

            tmpBuffer.upload(addr, size);
            this.copyRange(tmpBuffer, 0L, start, tmpBuffer.capacity());
        }
    }

    @Override
    public void uploadRange(long start, @NonNull ByteBuffer data) {
        if (data.remaining() >= UPLOADRANGE_COPY_THRESHOLD) {
            super.uploadRange(start, data);
            return;
        }

        try (Handle<GLBuffer> handle = this.gl.tmpBufferPool().get()) {
            GLBuffer tmpBuffer = handle.get();

            tmpBuffer.upload(data);
            this.copyRange(tmpBuffer, 0L, start, tmpBuffer.capacity());
        }
    }

    @Override
    protected void uploadRangeComposite(long start, @NonNull ByteBuf data) {
        if (data.readableBytes() >= UPLOADRANGE_COPY_THRESHOLD) {
            super.uploadRange(start, data);
            return;
        }

        try (Handle<GLBuffer> handle = this.gl.tmpBufferPool().get()) {
            GLBuffer tmpBuffer = handle.get();

            tmpBuffer.upload(data);
            this.copyRange(tmpBuffer, 0L, start, tmpBuffer.capacity());
        }
    }
}
