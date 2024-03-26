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

package net.daporkchop.fp2.gl.buffer.upload;

import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;

import java.nio.ByteBuffer;

/**
 * Uploads data into a temporary buffer, then {@link OpenGL#glCopyBufferSubData copies} the data from the temporary buffer to the destination buffer.
 * This is less likely to stall the graphics pipeline than {@link OpenGL#glBufferSubData}, although will most likely be outperformed by {@link UnsynchronizedMapBufferUploader}
 * on systems with a discrete GPU.
 * <p>
 * Uploaded data will be visible immediately, there is no need to {@link #flush() flush} or {@link #tick() tick} this {@link BufferUploader}.
 *
 * @author DaPorkchop_
 */
public final class ScratchCopyBufferUploader extends AbstractImmediateBufferUploader {
    private final GLMutableBuffer scratchBuffer;

    public ScratchCopyBufferUploader(OpenGL gl) {
        this.scratchBuffer = GLMutableBuffer.create(gl);
    }

    @Override
    public void uploadRange(GLBuffer buffer, long offset, long addr, long size) {
        this.scratchBuffer.upload(addr, size, BufferUsage.STREAM_COPY);
        this.scratchBuffer.copyRange(0L, buffer, offset, this.scratchBuffer.capacity());
    }

    @Override
    public void uploadRange(GLBuffer buffer, long offset, ByteBuffer data) {
        this.scratchBuffer.upload(data, BufferUsage.STREAM_COPY);
        this.scratchBuffer.copyRange(0L, buffer, offset, this.scratchBuffer.capacity());
    }

    @Override
    public void close() {
        super.close();
        this.scratchBuffer.close();
    }
}
