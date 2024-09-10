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
import net.daporkchop.fp2.gl.OpenGLException;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;
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

        //As there isn't an equivalent to realloc() for OpenGL buffers, we're going to have to emulate it ourself
        //  by copying the data we want to retain to a temporary location, re-defining the buffer storage using
        //  glBufferData() and then copying the retained data back into this buffer now that its storage has been
        //  redefined. There are two ways we can approach this:
        //  1. We can allocate the temporary storage in GPU memory using a temporary OpenGL buffer object. This has
        //     the advantage of not stalling the graphics pipeline, as copying the data to and from the temporary
        //     buffer can be done asynchronously. However, doing this assumes that the GPU has enough memory to
        //     fit not only this buffer with the new capacity, but also all the data we're preserving in the separate
        //     temporary buffer. The graphics driver MAY be able to automatically overflow into client (CPU) memory
        //     if there isn't enough dedicated GPU memory available, but we can't rely on that, so we'll have to do
        //     a lot of manual error checking to ensure that we can fall back to the second approach if necessary.
        //  2. We can simply download the data we want to retain from the GPU into main memory using
        //     glGetBufferSubData(), re-define the buffer storage using glBufferData() and then restore the modified
        //     data using glBufferSubData(). This will work reliably as long as there is enough CPU memory available,
        //     but will quite certainly cause the graphics pipeline to stall as the CPU will have to wait for the GPU
        //     to catch up before the data download can begin.
        //
        //  We'll try to use the first approach, and fall back to the second approach if we encounter an out-of-memory
        //  condition.
        //
        //  In the unlikely event that there isn't enough GPU or CPU memory, I frankly don't know or care
        //  what happens - most likely we'll end up taking the second approach, at which point malloc() will return
        //  a null pointer and the graphics driver will probably segfault in glGetBufferSubData(). That said, I've
        //  yet to see anyone complaining about FP2 using up all their memory, so I don't think this is a particularly
        //  pressing issue.

        //check for OpenGL errors here, as we're going to be relying on them to detect if we run out of memory
        //  and we don't want any errors which occurred previously to affect the result
        this.gl.checkError();

        //If non-null, we're taking the second approach and this is a pointer to the CPU-side buffer containing the
        //  retained data.
        long cpuBuffer = 0L;
        try {
            TRY_GPU_COPY:
            try (GLMutableBuffer tempBuffer = new GLMutableBuffer(this.gl)) {
                //try to allocate storage for the temporary buffer
                try {
                    tempBuffer.capacity(retainedCapacity, BufferUsage.STREAM_COPY);
                    this.gl.checkError(); //check if a GL_OUT_OF_MEMORY error occurred
                } catch (OpenGLException e) {
                    if (e.errorCode != GL_OUT_OF_MEMORY) {
                        throw e;
                    }

                    //there's not enough GPU memory available to allocate a temporary buffer on the GPU!
                    //  fortunately, all of the original data is still sitting in this buffer, so we can simply delete
                    //  the temporary buffer again and forget this ever happened.
                    break TRY_GPU_COPY;
                }

                //copy the retained data from this buffer to the temporary buffer
                tempBuffer.copyRange(this, 0L, 0L, retainedCapacity);

                //try to re-allocate storage for this buffer to the new capacity
                try {
                    this.capacity(capacity, usage);
                    this.gl.checkError(); //check if a GL_OUT_OF_MEMORY error occurred
                } catch (OpenGLException e) {
                    if (e.errorCode != GL_OUT_OF_MEMORY) {
                        throw e;
                    }

                    //there's not enough GPU memory available to allocate the resized buffer on the GPU!
                    //  unfortunately, some drivers seem to forget the original buffer contents if glBufferData
                    //  fails due to out of memory. the data is not lost, however: we can still fall back to the
                    //  slow GPU->CPU->GPU copy approach, only we're going to download the data from tempBuffer
                    //  instead of this.

                    //allocate buffer on CPU side
                    cpuBuffer = PUnsafe.allocateMemory(retainedCapacity);

                    //download data to main memory (from tempBuffer)
                    tempBuffer.getBufferSubData(0L, cpuBuffer, retainedCapacity);

                    //we will now break out of the RESIZE_COPY block, so tempBuffer will be deleted by the
                    //  try-with-resources block and we'll then proceed to re-allocate this buffer's storage
                    //  and re-upload the data (without downloading the retained data again from this buffer)
                    break TRY_GPU_COPY;
                }

                //copy the retained data from the temporary buffer back to this buffer
                this.copyRange(tempBuffer, 0L, 0L, retainedCapacity);

                //the first approach (GPU-side copy) worked, delete the temporary buffer and return :)
                return;
            }

            if (cpuBuffer == 0L) {
                //allocate buffer on CPU side
                cpuBuffer = PUnsafe.allocateMemory(retainedCapacity);

                //download data to main memory (from tempBuffer)
                this.getBufferSubData(0L, cpuBuffer, retainedCapacity);
            } else {
                //if cpuBuffer is non-null, it's already been allocated and filled with the data we want to
                //  retain, so we can immediately proceed to re-allocate the storage and upload from it
            }

            //update capacity
            this.capacity(capacity, usage);

            //re-upload retained data
            this.bufferSubData(0L, cpuBuffer, retainedCapacity);
        } finally {
            //this will do nothing if cpuBuffer was never allocated (i.e. is still null)
            PUnsafe.freeMemory(cpuBuffer);
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
