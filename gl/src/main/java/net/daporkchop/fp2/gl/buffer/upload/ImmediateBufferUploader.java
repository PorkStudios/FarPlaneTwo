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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.fp2.gl.buffer.GLBuffer;

import java.nio.ByteBuffer;

/**
 * A simple {@link BufferUploader} which simply calls {@link net.daporkchop.fp2.gl.OpenGL#glBufferSubData} to set ranges of buffer data.
 * <p>
 * Uploaded data will be visible immediately, there is no need to {@link #flush() flush} or {@link #tick() tick} this {@link BufferUploader}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImmediateBufferUploader extends AbstractImmediateBufferUploader {
    private static final BufferUploader instance = new ImmediateBufferUploader();

    /**
     * @return an instance of {@link ImmediateBufferUploader}
     */
    public static BufferUploader instance() {
        return instance;
    }

    @Override
    public void uploadRange(GLBuffer buffer, long offset, long addr, long size) {
        buffer.bufferSubData(offset, addr, size);
    }

    @Override
    public void uploadRange(GLBuffer buffer, long offset, ByteBuffer data) {
        buffer.bufferSubData(offset, data);
    }
}
