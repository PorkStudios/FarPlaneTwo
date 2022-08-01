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

package net.daporkchop.fp2.core.util.serialization.fixed;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Encodes {@link T} instances to a binary representation. All serialized instances occupy the same number of bytes.
 *
 * @author DaPorkchop_
 */
public interface IFixedSizeSerializer<T> {
    /**
     * @return the size of a serialized {@link T} instance, in bytes
     */
    long size();

    /**
     * Stores a {@link T} instance at the given off-heap memory address.
     *
     * @param value the {@link T} instance
     * @param addr  the memory address
     */
    default void store(T value, long addr) {
        this.store(value, null, addr);
    }

    /**
     * Stores a {@link T} instance to the given {@code byte[]} starting at the given index.
     *
     * @param value the {@link T} instance
     * @param arr   the {@code byte[]} to write to
     * @param index the index to start writing at
     */
    default void store(T value, @NonNull byte[] arr, int index) {
        this.store(value, arr, PUnsafe.arrayByteElementOffset(index));
    }

    /**
     * Stores a {@link T} instance at the given offset relative to the given Java object.
     *
     * @param value  the {@link T} instance
     * @param base   the Java object to use as a base. If {@code null}, {@code offset} is assumed to be an off-heap memory address.
     * @param offset the base offset (in bytes) relative to the given Java object to store the position to
     */
    void store(T value, Object base, long offset);

    /**
     * Writes a {@link T} instance to the given {@link ByteBuf}.
     * <p>
     * The buffer's {@link ByteBuf#writerIndex() writer index} will be increased by the number of bytes written.
     *
     * @param value the {@link T} instance
     * @param buf   the {@link ByteBuf} to write to
     */
    default void store(T value, @NonNull ByteBuf buf) {
        int size = toIntExact(this.size());

        //we want to be absolutely certain that the value can fit within the buffer's limit, as failure to do so could result in a write to an invalid memory address!
        buf.ensureWritable(size);

        if (buf.hasMemoryAddress()) {
            this.store(value, buf.memoryAddress() + buf.writerIndex());
        } else if (buf.hasArray()) {
            this.store(value, buf.array(), buf.arrayOffset() + buf.writerIndex());
        } else { //buffer is probably a composite: we don't really care about this, do we?
            throw new IllegalArgumentException(buf.toString());
        }

        //advance writerIndex
        buf.writerIndex(buf.writerIndex() + size);
    }

    /**
     * Writes a {@link T} instance to the given {@link ByteBuf}.
     * <p>
     * The buffer's {@link ByteBuf#writerIndex() writer index} will not be modified.
     *
     * @param value the {@link T} instance
     * @param buf   the {@link ByteBuf} to write to
     * @param index the index in the buffer to write the position at
     */
    default void store(T value, @NonNull ByteBuf buf, int index) {
        //we want to be absolutely certain that the value can fit within the buffer's limit, as failure to do so could result in a write to an invalid memory address!
        if (buf.capacity() - notNegative(index, "index") < this.size()) {
            int oldWriterIndex = buf.writerIndex();
            try {
                buf.writerIndex(index);
                this.store(value, buf); //delegate to appending store(), which will call ensureWritable() to increase the buffer's capacity
            } finally {
                buf.writerIndex(oldWriterIndex);
            }
        }

        if (buf.hasMemoryAddress()) {
            this.store(value, buf.memoryAddress() + index);
        } else if (buf.hasArray()) {
            this.store(value, buf.array(), buf.arrayOffset() + index);
        } else { //buffer is probably a composite: we don't really care about this, do we?
            throw new IllegalArgumentException(buf.toString());
        }
    }

    /**
     * Writes a {@link T} instance to the given {@link ByteBuffer}.
     * <p>
     * The buffer's {@link ByteBuffer#position() position} will be increased by the number of bytes written.
     *
     * @param value the {@link T} instance
     * @param buf   the {@link ByteBuffer} to write to
     */
    default void store(T value, @NonNull ByteBuffer buf) {
        int size = toIntExact(this.size());

        //we want to be absolutely certain that the value can fit within the buffer's limit, as failure to do so could result in a write to an invalid memory address!
        if (buf.remaining() < size) {
            throw new BufferOverflowException();
        }

        if (buf.isDirect()) {
            this.store(value, PUnsafe.pork_directBufferAddress(buf) + buf.position());
        } else {
            this.store(value, buf.array(), buf.arrayOffset() + buf.position());
        }

        //advance position
        buf.position(buf.position() + size);
    }

    /**
     * Writes a {@link T} instance to the given {@link ByteBuffer}.
     * <p>
     * The buffer's {@link ByteBuffer#position() position} will not be modified.
     *
     * @param value the {@link T} instance
     * @param buf   the {@link ByteBuffer} to write to
     * @param index the index in the buffer to write the position at
     */
    default void store(T value, @NonNull ByteBuffer buf, int index) {
        //we want to be absolutely certain that the value can fit within the buffer's limit, as failure to do so could result in a write to an invalid memory address!
        if (buf.limit() - notNegative(index, "index") < this.size()) { //the buffer could potentially overflow
            throw new BufferOverflowException();
        }

        if (buf.isDirect()) {
            this.store(value, PUnsafe.pork_directBufferAddress(buf) + index);
        } else {
            this.store(value, buf.array(), buf.arrayOffset() + index);
        }
    }
}
