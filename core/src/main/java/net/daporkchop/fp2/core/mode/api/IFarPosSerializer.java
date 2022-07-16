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

package net.daporkchop.fp2.core.mode.api;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.util.GlobalAllocators.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Encodes and decodes {@link IFarPos} instances to/from a binary representation.
 *
 * @author DaPorkchop_
 */
public interface IFarPosSerializer<POS extends IFarPos> {
    /**
     * @return the size of a position, in bytes
     */
    long posSize();

    /**
     * Stores a {@link POS} instance at the given off-heap memory address.
     *
     * @param pos  the {@link POS} instance
     * @param addr the memory address
     */
    default void storePos(@NonNull POS pos, long addr) {
        this.storePos(pos, null, addr);
    }

    /**
     * Stores a {@link POS} instance to the given {@code byte[]} starting at the given index.
     *
     * @param pos   the {@link POS} instance
     * @param arr   the {@code byte[]} to write to
     * @param index the index to start writing at
     */
    default void storePos(@NonNull POS pos, @NonNull byte[] arr, int index) {
        this.storePos(pos, arr, PUnsafe.arrayByteElementOffset(index));
    }

    /**
     * Stores a {@link POS} instance at the given offset relative to the given Java object.
     *
     * @param pos    the {@link POS} instance
     * @param base   the Java object to use as a base. If {@code null}, {@code offset} is assumed to be an off-heap memory address.
     * @param offset the base offset (in bytes) relative to the given Java object to store the position to
     */
    void storePos(@NonNull POS pos, Object base, long offset);

    /**
     * Writes a {@link POS} instance to the given {@link ByteBuf}.
     *
     * @param pos the {@link POS} instance
     * @param buf the {@link ByteBuf} to write to
     */
    default void storePos(@NonNull POS pos, @NonNull ByteBuf buf) {
        int posSize = toIntExact(this.posSize());

        ArrayAllocator<byte[]> alloc = ALLOC_BYTE.get();
        byte[] arr = alloc.atLeast(posSize);
        try {
            //write position to temporary array
            this.storePos(pos, arr, 0);

            //copy serialized position to buffer
            buf.writeBytes(arr, 0, posSize);
        } finally {
            alloc.release(arr);
        }
    }

    /**
     * Writes a {@link POS} instance to the given {@link ByteBuf}.
     *
     * @param pos   the {@link POS} instance
     * @param buf   the {@link ByteBuf} to write to
     * @param index the index in the buffer to write the position at
     */
    default void storePos(@NonNull POS pos, @NonNull ByteBuf buf, int index) {
        int posSize = toIntExact(this.posSize());

        ArrayAllocator<byte[]> alloc = ALLOC_BYTE.get();
        byte[] arr = alloc.atLeast(posSize);
        try {
            //write position to temporary array
            this.storePos(pos, arr, 0);

            //copy serialized position to buffer
            buf.setBytes(index, arr, 0, posSize);
        } finally {
            alloc.release(arr);
        }
    }

    /**
     * Writes a {@link POS} instance to the given {@link ByteBuffer}.
     *
     * @param pos the {@link POS} instance
     * @param buf the {@link ByteBuffer} to write to
     */
    default void storePos(@NonNull POS pos, @NonNull ByteBuffer buf) {
        checkIndex(buf.remaining() >= this.posSize(), "buffer overflow");

        if (buf.isDirect()) {
            this.storePos(pos, PUnsafe.pork_directBufferAddress(buf) + buf.position());
        } else {
            this.storePos(pos, buf.array(), buf.arrayOffset() + buf.position());
        }
    }

    /**
     * Writes a {@link POS} instance to the given {@link ByteBuffer}.
     *
     * @param pos   the {@link POS} instance
     * @param buf   the {@link ByteBuffer} to write to
     * @param index the index in the buffer to write the position at
     */
    default void storePos(@NonNull POS pos, @NonNull ByteBuffer buf, int index) {
        checkRangeLen(buf.limit(), index, (int) this.posSize());

        if (buf.isDirect()) {
            this.storePos(pos, PUnsafe.pork_directBufferAddress(buf) + index);
        } else {
            this.storePos(pos, buf.array(), buf.arrayOffset() + index);
        }
    }

    /**
     * Loads the position at the given off-heap memory address into a {@link POS} instance.
     *
     * @param addr the memory address
     * @return the {@link POS} instance
     */
    default POS loadPos(long addr) {
        return this.loadPos(null, addr);
    }

    /**
     * Loads the position at the given index in the given {@code byte[]} into a {@link POS} instance.
     *
     * @param arr   the {@code byte[]} to read from
     * @param index the index to start reading at
     */
    default POS loadPos(@NonNull byte[] arr, int index) {
        return this.loadPos(arr, PUnsafe.arrayByteElementOffset(index));
    }

    /**
     * Loads the position at the given offset relative to the given Java object into a {@link POS} instance.
     *
     * @param base   the Java object to use as a base. If {@code null}, {@code offset} is assumed to be an off-heap memory address.
     * @param offset the base offset (in bytes) relative to the given Java object to load the position from
     * @return the {@link POS} instance
     */
    POS loadPos(Object base, long offset);

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link POS} instance.
     *
     * @param buf the {@link ByteBuf} to read from
     * @return the {@link POS} instance
     */
    default POS loadPos(@NonNull ByteBuf buf) {
        int posSize = toIntExact(this.posSize());

        ArrayAllocator<byte[]> alloc = ALLOC_BYTE.get();
        byte[] arr = alloc.atLeast(posSize);
        try {
            //copy serialized position from buffer
            buf.readBytes(arr, 0, posSize);

            //load position from temporary array
            return this.loadPos(arr, 0);
        } finally {
            alloc.release(arr);
        }
    }

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link POS} instance.
     *
     * @param buf   the {@link ByteBuf} to read from
     * @param index the index in the buffer to read the position from
     * @return the {@link POS} instance
     */
    default POS loadPos(@NonNull ByteBuf buf, int index) {
        int posSize = toIntExact(this.posSize());

        ArrayAllocator<byte[]> alloc = ALLOC_BYTE.get();
        byte[] arr = alloc.atLeast(posSize);
        try {
            //copy serialized position from buffer
            buf.getBytes(index, arr, 0, posSize);

            //load position from temporary array
            return this.loadPos(arr, 0);
        } finally {
            alloc.release(arr);
        }
    }

    /**
     * Loads the position from the given {@link ByteBuffer} into a {@link POS} instance.
     *
     * @param buf the {@link ByteBuffer} to read from
     * @return the {@link POS} instance
     */
    default POS loadPos(@NonNull ByteBuffer buf) {
        checkIndex(buf.remaining() >= this.posSize(), "buffer overflow");

        return buf.isDirect()
                ? this.loadPos(PUnsafe.pork_directBufferAddress(buf) + buf.position())
                : this.loadPos(buf.array(), buf.arrayOffset() + buf.position());
    }

    /**
     * Loads the position from the given {@link ByteBuffer} into a {@link POS} instance.
     *
     * @param buf   the {@link ByteBuffer} to read from
     * @param index the index in the buffer to read the position from
     * @return the {@link POS} instance
     */
    default POS loadPos(@NonNull ByteBuffer buf, int index) {
        checkRangeLen(buf.limit(), index, (int) this.posSize());

        return buf.isDirect()
                ? this.loadPos(PUnsafe.pork_directBufferAddress(buf) + index)
                : this.loadPos(buf.array(), buf.arrayOffset() + index);
    }
}
