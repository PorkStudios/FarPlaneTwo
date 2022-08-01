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

package net.daporkchop.fp2.core.util.serialization.variable;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.core.util.MutableLong;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Decodes {@link T} instances from a binary representation. Serialized values have a variable size, which can be auto-detected during deserialization. When deserializing
 * a value, a new instance of {@link T} is allocated.
 *
 * @author DaPorkchop_
 */
public interface IVariableSizeConstructingDeserializer<T> {
    /**
     * @return the maximum size of a serialized position, in bytes
     */
    long maxSize();

    /**
     * Loads the position at the given off-heap memory address into a {@link T} instance.
     *
     * @param addr the memory address
     * @param read a {@link MutableLong} which will be updated with the number of bytes that were read
     * @return the {@link T} instance
     */
    default T load(long addr, @NonNull MutableLong read) {
        return this.load(null, addr, read);
    }

    /**
     * Loads the position at the given index in the given {@code byte[]} into a {@link T} instance.
     *
     * @param arr   the {@code byte[]} to read from
     * @param read  a {@link MutableLong} which will be updated with the number of bytes that were read
     * @param index the index to start reading at
     */
    default T load(@NonNull byte[] arr, int index, @NonNull MutableLong read) {
        return this.load(arr, PUnsafe.arrayByteElementOffset(index), read);
    }

    /**
     * Loads the position at the given offset relative to the given Java object into a {@link T} instance.
     *
     * @param base   the Java object to use as a base. If {@code null}, {@code offset} is assumed to be an off-heap memory address.
     * @param offset the base offset (in bytes) relative to the given Java object to load the position from
     * @param read   a {@link MutableLong} which will be updated with the number of bytes that were read
     * @return the {@link T} instance
     */
    T load(Object base, long offset, @NonNull MutableLong read);

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link T} instance.
     * <p>
     * The buffer's {@link ByteBuf#readerIndex() reader index} will be increased by the number of bytes read.
     *
     * @param buf  the {@link ByteBuf} to read from
     * @param read a {@link MutableLong} which will be updated with the number of bytes that were read
     * @return the {@link T} instance
     */
    default T load(@NonNull ByteBuf buf, @NonNull MutableLong read) {
        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.readableBytes() >= this.maxSize());

        T result;
        if (buf.hasMemoryAddress()) {
            result = this.load(buf.memoryAddress() + buf.readerIndex(), read);
        } else if (buf.hasArray()) {
            result = this.load(buf.array(), buf.arrayOffset() + buf.readerIndex(), read);
        } else { //buffer is probably a composite: we don't really care about this, do we?
            throw new IllegalArgumentException(buf.toString());
        }

        //advance readerIndex
        buf.skipBytes(toIntExact(read.get()));

        return result;
    }

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link T} instance.
     * <p>
     * The buffer's {@link ByteBuf#readerIndex() reader index} will not be modified.
     *
     * @param buf   the {@link ByteBuf} to read from
     * @param index the index in the buffer to read the position from
     * @param read  a {@link MutableLong} which will be updated with the number of bytes that were read
     * @return the {@link T} instance
     */
    default T load(@NonNull ByteBuf buf, int index, MutableLong read) {
        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.capacity() - notNegative(index, "index") < this.maxSize());

        if (buf.hasMemoryAddress()) {
            return this.load(buf.memoryAddress() + index, read);
        } else if (buf.hasArray()) {
            return this.load(buf.array(), buf.arrayOffset() + index, read);
        } else { //buffer is probably a composite: we don't really care about this, do we?
            throw new IllegalArgumentException(buf.toString());
        }
    }

    /**
     * Loads the position from the given {@link ByteBuffer} into a {@link T} instance.
     * <p>
     * The buffer's {@link ByteBuffer#position() position} will be increased by the number of bytes read.
     *
     * @param buf  the {@link ByteBuffer} to read from
     * @param read a {@link MutableLong} which will be updated with the number of bytes that were read
     * @return the {@link T} instance
     */
    default T load(@NonNull ByteBuffer buf, @NonNull MutableLong read) {
        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.remaining() >= this.maxSize());

        T result = buf.isDirect()
                ? this.load(PUnsafe.pork_directBufferAddress(buf) + buf.position(), read)
                : this.load(buf.array(), buf.arrayOffset() + buf.position(), read);

        //advance position
        buf.position(buf.position() + toIntExact(read.get()));

        return result;
    }

    /**
     * Loads the position from the given {@link ByteBuffer} into a {@link T} instance.
     *
     * @param buf   the {@link ByteBuffer} to read from
     * @param index the index in the buffer to read the position from
     * @param read  a {@link MutableLong} which will be updated with the number of bytes that were read
     * @return the {@link T} instance
     */
    default T load(@NonNull ByteBuffer buf, int index, @NonNull MutableLong read) {
        //we want to be absolutely certain that buffer has enough data available, as failure to do so could result in a load from an invalid memory address!
        checkIndex(buf.capacity() - notNegative(index, "index") < this.maxSize());

        return buf.isDirect()
                ? this.load(PUnsafe.pork_directBufferAddress(buf) + index, read)
                : this.load(buf.array(), buf.arrayOffset() + index, read);
    }
}
