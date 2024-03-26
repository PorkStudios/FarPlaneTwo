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

import java.nio.ByteBuffer;

/**
 * Enqueues data to be uploaded into parts of buffers.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class BufferUploader implements AutoCloseable {
    protected final OpenGL gl;

    /**
     * Uploads the given data into the given buffer at the given offset.
     * <p>
     * The uploaded data is not guaranteed to be visible until this uploader is {@link #flush() flushed}.
     *
     * @param buffer the {@link GLBuffer} to upload into
     * @param offset the offset in the given buffer for the data to be uploaded to
     * @param addr   a pointer to the data
     * @param size   the size of the data, in bytes
     */
    public abstract void uploadRange(GLBuffer buffer, long offset, long addr, long size);

    /**
     * Uploads the given data into the given buffer at the given offset.
     * <p>
     * The uploaded data is not guaranteed to be visible until this uploader is {@link #flush() flushed}.
     *
     * @param buffer the {@link GLBuffer} to upload into
     * @param offset the offset in the given buffer for the data to be uploaded to
     * @param data   a {@link ByteBuffer} containing the data to upload
     */
    public abstract void uploadRange(GLBuffer buffer, long offset, ByteBuffer data);

    /**
     * Flushes all pending buffer uploads.
     * <p>
     * After this method is called, all data uploaded by previous calls to {@link #uploadRange} will be visible in the corresponding target buffers.
     * <p>
     * If any buffers used as the destination of a previously queued {@link #uploadRange} call have been closed or resized before being flushed, the behavior is undefined.
     */
    public abstract void flush();

    /**
     * Should be called periodically (roughly once per frame) to update any internal state
     */
    public abstract void tick();

    @Override
    public void close() {
        //no-op
    }
}
