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

package net.daporkchop.fp2.gl.buffer;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.gl.GLResource;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

/**
 * An OpenGL buffer.
 *
 * @author DaPorkchop_
 */
public interface GLBuffer extends GLResource {
    /**
     * @return the capacity of this buffer
     */
    long capacity();

    /**
     * Sets the capacity of this buffer.
     * <p>
     * If the buffer already has the requested capacity, nothing is changed. Otherwise, the buffer is resized and its contents are now undefined.
     *
     * @param capacity the new capacity
     */
    void capacity(long capacity);

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
    void resize(long capacity);

    /**
     * Sets the buffer contents.
     *
     * @param addr the base address of the data to upload
     * @param size the size of the data (in bytes)
     */
    void upload(long addr, long size);

    /**
     * Sets the buffer contents.
     *
     * @param data the {@link ByteBuffer} containing the data to upload
     */
    void upload(@NonNull ByteBuffer data);

    /**
     * Sets the buffer contents.
     *
     * @param data the {@link ByteBuf} containing the data to upload
     */
    void upload(@NonNull ByteBuf data);

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address of the data to upload
     * @param size  the size of the data (in bytes)
     */
    void uploadRange(long start, long addr, long size);

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuffer} containing the data to upload
     */
    void uploadRange(long start, @NonNull ByteBuffer data);

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuf} containing the data to upload
     */
    void uploadRange(long start, @NonNull ByteBuf data);

    /**
     * Downloads the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address where the data should be stored
     * @param size  the size of the data (in bytes)
     */
    void downloadRange(long start, long addr, long size);

    /**
     * Downloads the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuffer} where the data should be stored
     */
    void downloadRange(long start, @NonNull ByteBuffer data);

    /**
     * Maps this buffer's contents into client address space and passes the mapping address to the given callback function before unmapping the buffer again.
     *
     * @param read     whether or not the mapping may be read from
     * @param write    whether or not the mapping may be written to
     * @param callback the callback function
     */
    void map(boolean read, boolean write, @NonNull LongConsumer callback);
}
