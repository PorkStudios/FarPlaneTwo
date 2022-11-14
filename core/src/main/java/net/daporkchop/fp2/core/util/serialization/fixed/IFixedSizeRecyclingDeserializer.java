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

import java.nio.ByteBuffer;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Decodes {@link T} instances from a binary representation. All serialized instances occupy the same number of bytes. When deserializing a value, an existing instance of
 * {@link T} is re-used and its state is overwritten.
 *
 * @author DaPorkchop_
 */
public interface IFixedSizeRecyclingDeserializer<T> {
    /**
     * @return the size of a serialized {@link T} instance, in bytes
     */
    long size();

    /**
     * Loads the position at the given off-heap memory address into a {@link T} instance.
     *
     * @param value the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param addr  the memory address
     */
    default void load(@NonNull T value, long addr) {
        this.load(value, null, addr);
    }

    /**
     * Loads the position at the given index in the given {@code byte[]} into a {@link T} instance.
     *
     * @param value the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param arr   the {@code byte[]} to read from
     * @param index the index to start reading at
     */
    default void load(@NonNull T value, @NonNull byte[] arr, int index) {
        this.load(value, arr, PUnsafe.arrayByteElementOffset(index));
    }

    /**
     * Loads the position at the given offset relative to the given Java object into a {@link T} instance.
     *
     * @param value  the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param base   the Java object to use as a base. If {@code null}, {@code offset} is assumed to be an off-heap memory address.
     * @param offset the base offset (in bytes) relative to the given Java object to read from
     */
    void load(@NonNull T value, Object base, long offset);

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link T} instance.
     * <p>
     * The buffer's {@link ByteBuf#readerIndex() reader index} will be increased by the number of bytes read.
     *
     * @param value the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param buf   the {@link ByteBuf} to read from
     */
    default void load(@NonNull T value, @NonNull ByteBuf buf) {
        int size = toIntExact(this.size());

        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.readableBytes() >= size);

        if (buf.hasMemoryAddress()) {
            this.load(value, buf.memoryAddress() + buf.readerIndex());
        } else if (buf.hasArray()) {
            this.load(value, buf.array(), buf.arrayOffset() + buf.readerIndex());
        } else { //buffer is probably a composite: we don't really care about this, do we?
            throw new IllegalArgumentException(buf.toString());
        }

        //advance readerIndex
        buf.skipBytes(size);
    }

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link T} instance.
     * <p>
     * The buffer's {@link ByteBuf#readerIndex() reader index} will not be modified.
     *
     * @param value the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param buf   the {@link ByteBuf} to read from
     * @param index the index in the buffer to read from
     */
    default void load(@NonNull T value, @NonNull ByteBuf buf, int index) {
        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.capacity() - notNegative(index, "index") < this.size());

        if (buf.hasMemoryAddress()) {
            this.load(value, buf.memoryAddress() + index);
        } else if (buf.hasArray()) {
            this.load(value, buf.array(), buf.arrayOffset() + index);
        } else { //buffer is probably a composite: we don't really care about this, do we?
            throw new IllegalArgumentException(buf.toString());
        }
    }

    /**
     * Loads the position from the given {@link ByteBuffer} into a {@link T} instance.
     * <p>
     * The buffer's {@link ByteBuffer#position() position} will be increased by the number of bytes read.
     *
     * @param value the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param buf   the {@link ByteBuffer} to read from
     */
    default void load(@NonNull T value, @NonNull ByteBuffer buf) {
        int size = toIntExact(this.size());

        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.remaining() >= size);

        if (buf.isDirect()) {
            this.load(value, PUnsafe.pork_directBufferAddress(buf) + buf.position());
        } else {
            this.load(value, buf.array(), buf.arrayOffset() + buf.position());
        }

        //advance position
        buf.position(buf.position() + size);
    }

    /**
     * Loads the position from the given {@link ByteBuffer} into a {@link T} instance.
     *
     * @param value the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param buf   the {@link ByteBuffer} to read from
     * @param index the index in the buffer to read from
     */
    default void load(@NonNull T value, @NonNull ByteBuffer buf, int index) {
        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.capacity() - notNegative(index, "index") < this.size());

        if (buf.isDirect()) {
            this.load(value, PUnsafe.pork_directBufferAddress(buf) + index);
        } else {
            this.load(value, buf.array(), buf.arrayOffset() + index);
        }
    }
}
