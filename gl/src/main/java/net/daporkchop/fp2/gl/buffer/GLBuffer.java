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
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static java.lang.Math.min;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.notNegative;

/**
 * @author DaPorkchop_
 */
public abstract class GLBuffer implements AutoCloseable {
    /**
     * Creates a new buffer.
     *
     * @param gl    the OpenGL context
     * @param usage the buffer's usage
     * @return the created buffer
     */
    public static GLBuffer create(OpenGL gl, BufferUsage usage) {
        if (GLExtension.GL_ARB_direct_state_access.supported(gl)) {
            return new DSAGLBufferImpl(gl, usage);
        } else {
            return new BasicGLBufferImpl(gl, usage);
        }
    }

    protected final OpenGL gl;

    protected long capacity = -1L;

    protected final int id;
    protected final int usage;

    protected GLBuffer(OpenGL gl, BufferUsage usage, int id) {
        this.gl = gl;
        this.id = id;
        this.usage = usage.usage();

        //TODO: figure out if i can safely get rid of this
        this.capacity(0L);
    }

    /**
     * @return the ID of the corresponding OpenGL Buffer Object
     */
    public final int id() {
        return this.id;
    }

    /**
     * @return this buffer's current capacity
     */
    public final long capacity() {
        return this.capacity;
    }

    @Override
    public void close() {
        this.gl.glDeleteBuffer(this.id); //TODO: warn if garbage-collected
    }

    /**
     * Sets the capacity of this buffer.
     * <p>
     * This will discard the buffer's previous contents, orphaning its previous storage and allocating a new one.
     *
     * @param capacity the new capacity
     */
    public abstract void capacity(long capacity);

    /**
     * Sets the capacity of this buffer.
     * <p>
     * Unlike {@link #capacity(long)}, this method will retain as much of the original data as possible.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with undefined contents.
     *
     * @param capacity the new capacity
     */
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

    /**
     * Sets the buffer contents.
     *
     * @param addr the base address of the data to upload
     * @param size the size of the data (in bytes)
     */
    public abstract void upload(long addr, long size);

    /**
     * Sets the buffer contents.
     *
     * @param data the {@link ByteBuffer} containing the data to upload
     */
    public abstract void upload(@NonNull ByteBuffer data);

    /**
     * Sets the buffer contents.
     *
     * @param data the {@link ByteBuf} containing the data to upload
     */
    public void upload(@NonNull ByteBuf data) {
        if (data.nioBufferCount() == 1) { //fast path: upload whole buffer contents at once
            this.upload(data.nioBuffer());
        } else { //slower fallback path for composite buffers
            this.uploadComposite(data);
        }
    }

    protected abstract void uploadComposite(@NonNull ByteBuf data);

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address of the data to upload
     * @param size  the size of the data (in bytes)
     */
    public abstract void uploadRange(long start, long addr, long size);

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuffer} containing the data to upload
     */
    public abstract void uploadRange(long start, @NonNull ByteBuffer data);

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuf} containing the data to upload
     */
    public void uploadRange(long start, @NonNull ByteBuf data) {
        if (data.nioBufferCount() == 1) { //fast path: upload whole buffer contents at once
            this.uploadRange(start, data.nioBuffer());
        } else { //slower fallback path for composite buffers
            this.uploadRangeComposite(start, data);
        }
    }

    protected abstract void uploadRangeComposite(long start, @NonNull ByteBuf data);

    /**
     * Downloads the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address where the data should be stored
     * @param size  the size of the data (in bytes)
     */
    public abstract void downloadRange(long start, long addr, long size);

    /**
     * Downloads the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuffer} where the data should be stored
     */
    public abstract void downloadRange(long start, @NonNull ByteBuffer data);

    /**
     * Copies a range of data from the given source buffer into this buffer.
     *
     * @param src       the buffer to copy the data from
     * @param srcOffset the offset in the source buffer to begin copying from
     * @param dstOffset the offset in this buffer to begin copying to
     * @param size      the number of bytes to copy
     */
    public abstract void copyRange(@NonNull GLBuffer src, long srcOffset, long dstOffset, long size);

    /**
     * Copies a range of data from this buffer into the given destination buffer.
     *
     * @param srcOffset the offset in this buffer to begin copying from
     * @param dst       the buffer to copy the data to
     * @param dstOffset the offset in the destination buffer to begin copying to
     * @param size      the number of bytes to copy
     */
    public void copyRange(long srcOffset, @NonNull GLBuffer dst, long dstOffset, long size) {
        dst.copyRange(this, srcOffset, dstOffset, size);
    }

    /**
     * Executes the given action with this buffer bound to the given {@link BufferTarget buffer binding target}.
     *
     * @param target   the {@link BufferTarget buffer binding target} to bind the buffer to
     * @param callback the action to run
     */
    public void bind(@NonNull BufferTarget target, @NonNull Consumer<BufferTarget> callback) {
        int old = this.gl.glGetInteger(target.binding());
        try {
            this.gl.glBindBuffer(target.id(), this.id);

            callback.accept(target);
        } finally {
            this.gl.glBindBuffer(target.id(), old);
        }
    }

    /**
     * Maps this buffer's contents into client address space and passes the mapping address to the given callback function before unmapping the buffer again.
     *
     * @param read     whether the mapping may be read from
     * @param write    whether the mapping may be written to
     * @param callback the callback function
     */
    public void map(boolean read, boolean write, @NonNull LongConsumer callback) {
        int access;
        if (read && write) {
            access = GL_READ_WRITE;
        } else if (read) {
            access = GL_READ_ONLY;
        } else if (write) {
            access = GL_WRITE_ONLY;
        } else {
            throw new IllegalArgumentException("at least one of read or write access must be enabled!");
        }

        this.map(access, callback);
    }

    protected abstract void map(int access, @NonNull LongConsumer callback);
}
