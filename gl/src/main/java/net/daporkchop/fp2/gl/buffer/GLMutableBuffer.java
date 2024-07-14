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

import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An OpenGL buffer with mutable storage.
 *
 * @author DaPorkchop_
 */
public final class GLMutableBuffer extends GLBuffer {
    /**
     * Creates a new buffer.
     *
     * @param gl the OpenGL context
     * @return the created buffer
     */
    public static GLMutableBuffer create(OpenGL gl) {
        return new GLMutableBuffer(gl);
    }

    private GLMutableBuffer(OpenGL gl) {
        super(gl);

        //TODO: figure out if i can safely get rid of this
        this.capacity(0L, BufferUsage.STATIC_DRAW);
    }

    /**
     * Sets the capacity of this buffer.
     * <p>
     * This will discard the buffer's previous contents, orphaning its previous storage and allocating a new one.
     *
     * @param capacity the new capacity
     * @param usage    the buffer's usage
     */
    public void capacity(long capacity, BufferUsage usage) {
        this.checkOpen();
        notNegative(capacity, "capacity");

        if (this.invalidateSubdata && capacity == this.capacity) {
            // if GL_ARB_invalidate_subdata is supported and the buffer's capacity isn't changing, prefer glInvalidateBufferData() over allocating a new buffer storage
            this.gl.glInvalidateBufferData(this.id);
        } else if (this.dsa) {
            this.gl.glNamedBufferData(this.id, capacity, 0L, usage.usage());
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glBufferData(target.id(), capacity, 0L, usage.usage());
            });
        }
        this.capacity = capacity;
    }

    /**
     * Sets the capacity of this buffer.
     * <p>
     * Unlike {@link #capacity(long, BufferUsage)}, this method will retain as much of the original data as possible.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with undefined contents.
     *
     * @param capacity the new capacity
     * @param usage    the buffer's usage
     */
    public void resize(long capacity, @NonNull BufferUsage usage) {
        this.checkOpen();
        long retainedCapacity = min(this.capacity, notNegative(capacity, "capacity"));

        if (retainedCapacity <= 0L) { //previous capacity was unset, so no data needs to be retained
            this.capacity(capacity, usage);
            return;
        } else if (this.capacity == capacity) { //capacity remains unchanged, nothing to do!
            return;
        }

        long buffer = PUnsafe.allocateMemory(retainedCapacity);
        try {
            //download data to main memory
            this.getBufferSubData(0L, buffer, retainedCapacity);

            //update capacity
            this.capacity(capacity, usage);

            //re-upload retained data
            this.bufferSubData(0L, buffer, retainedCapacity);
        } finally {
            PUnsafe.freeMemory(buffer);
        }
    }

    /**
     * Sets the buffer contents.
     *
     * @param addr  the base address of the data to upload
     * @param size  the size of the data (in bytes)
     * @param usage the buffer's usage
     */
    public void upload(long addr, long size, @NonNull BufferUsage usage) {
        this.checkOpen();
        notNegative(size, "size");

        if (this.dsa) {
            this.gl.glNamedBufferData(this.id, size, addr, usage.usage());
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glBufferData(target.id(), size, addr, usage.usage());
            });
        }
        this.capacity = size;
    }

    /**
     * Sets the buffer contents.
     *
     * @param data  the {@link ByteBuffer} containing the data to upload
     * @param usage the buffer's usage
     */
    public void upload(@NonNull ByteBuffer data, @NonNull BufferUsage usage) {
        this.checkOpen();
        if (this.dsa) {
            this.gl.glNamedBufferData(this.id, data, usage.usage());
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glBufferData(target.id(), data, usage.usage());
            });
        }
        this.capacity = data.remaining();
    }
}
