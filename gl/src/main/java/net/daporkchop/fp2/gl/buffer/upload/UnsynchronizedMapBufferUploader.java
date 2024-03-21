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

import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.GLBuffer;

import java.util.ArrayDeque;

/**
 * @author DaPorkchop_
 */
public final class UnsynchronizedMapBufferUploader extends AbstractAsynchronousBufferUploader {
    private final ArrayDeque<PendingCopy> pendingCopies = new ArrayDeque<>();

    private final GLBuffer stagingBuffer;
    private final BufferUploader fallback;

    public UnsynchronizedMapBufferUploader(OpenGL gl, long arenaSize, BufferUploader fallback) {
        super(gl);

        this.stagingBuffer = GLBuffer.create(gl, ); //TODO: this needs to be a buffer with immutable storage!!!
        this.fallback = fallback;
    }

    @Override
    public void uploadRange(GLBuffer buffer, long offset, long addr, long size) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        super.close();

        this.stagingBuffer.close();
        this.fallback.close();
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class PendingCopy {
        protected final long srcOffset;
        protected final GLBuffer dst;
        protected final long dstOffset;
        protected final long size;
    }
}
