/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.engine;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.util.math.MathUtil;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.io.IOException;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class TilePosCodec {
    private static final int LEVEL_OFFSET = 0;
    private static final int HIGH_OFFSET = LEVEL_OFFSET + BYTE_SIZE;
    private static final int LOW_OFFSET = HIGH_OFFSET + INT_SIZE;

    private static final int SIZE = LOW_OFFSET + LONG_SIZE;

    /**
     * @return the size of a serialized {@link TilePos} instance, in bytes
     */
    public static long size() {
        return SIZE;
    }

    //
    // STORE
    //

    /**
     * Stores a {@link TilePos} instance at the given off-heap memory address.
     *
     * @param pos  the {@link TilePos} instance
     * @param addr the memory address
     */
    public static void store(TilePos pos, long addr) {
        PUnsafe.putByte(addr + LEVEL_OFFSET, toByte(pos.level(), "level"));
        PUnsafe.putUnalignedIntBE(addr + HIGH_OFFSET, MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()));
        PUnsafe.putUnalignedLongBE(addr + LOW_OFFSET, MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    /**
     * Stores a {@link TilePos} instance to the given {@code byte[]} starting at the given index.
     *
     * @param pos   the {@link TilePos} instance
     * @param arr   the {@code byte[]} to write to
     * @param index the index to start writing at
     */
    public static void store(TilePos pos, byte[] arr, int index) {
        store(pos, arr, PUnsafe.arrayByteElementOffset(index));
    }

    /**
     * Stores a {@link TilePos} instance at the given offset relative to the given Java object.
     *
     * @param pos    the {@link TilePos} instance
     * @param base   the Java object to use as a base. If {@code null}, {@code offset} is assumed to be an off-heap memory address.
     * @param offset the base offset (in bytes) relative to the given Java object to write to
     */
    public static void store(TilePos pos, Object base, long offset) {
        PUnsafe.putByte(base, offset + LEVEL_OFFSET, toByte(pos.level(), "level"));
        PUnsafe.putUnalignedIntBE(base, offset + HIGH_OFFSET, MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()));
        PUnsafe.putUnalignedLongBE(base, offset + LOW_OFFSET, MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    /**
     * Writes a {@link TilePos} instance to the given {@link ByteBuf}.
     * <p>
     * The buffer's {@link ByteBuf#writerIndex() writer index} will be increased by the number of bytes written.
     *
     * @param pos the {@link TilePos} instance
     * @param buf the {@link ByteBuf} to write to
     */
    public static void store(TilePos pos, @NonNull ByteBuf buf) {
        buf.ensureWritable(SIZE).writeByte(pos.level())
                //write in big-endian
                .writeInt(MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()))
                .writeLong(MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    /**
     * Writes a {@link TilePos} instance to the given {@link ByteBuf}.
     * <p>
     * The buffer's {@link ByteBuf#writerIndex() writer index} will not be modified.
     *
     * @param pos   the {@link TilePos} instance
     * @param buf   the {@link ByteBuf} to write to
     * @param index the index in the buffer to write to
     */
    public static void store(TilePos pos, @NonNull ByteBuf buf, int index) {
        buf.setByte(index + LEVEL_OFFSET, pos.level())
                //write in big-endian
                .setInt(index + HIGH_OFFSET, MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()))
                .setLong(index + LOW_OFFSET, MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    /**
     * Writes a {@link TilePos} instance to the given {@link DataOut}.
     *
     * @param pos the {@link TilePos} instance
     * @param out the {@link DataOut} to write to
     */
    public static void writePos(TilePos pos, @NonNull DataOut out) throws IOException {
        out.writeByte(pos.level());
        out.writeInt(MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()));
        out.writeLong(MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    //
    // LOAD
    //

    /**
     * Loads the position at the given off-heap memory address into a {@link TilePos} instance.
     *
     * @param addr the memory address
     * @return the {@link TilePos} instance
     */
    public static TilePos load(long addr) {
        int level = PUnsafe.getByte(addr + LEVEL_OFFSET) & 0xFF;

        int interleavedHigh = PUnsafe.getUnalignedIntBE(addr + HIGH_OFFSET);
        long interleavedLow = PUnsafe.getUnalignedLongBE(addr + LOW_OFFSET);
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new TilePos(level, x, y, z);
    }

    /**
     * Loads the position at the given index in the given {@code byte[]} into a {@link TilePos} instance.
     *
     * @param arr   the {@code byte[]} to read from
     * @param index the index to start reading at
     */
    public static TilePos load(byte[] arr, int index) {
        return load(arr, PUnsafe.arrayByteElementOffset(index));
    }

    /**
     * Loads the position at the given offset relative to the given Java object into a {@link TilePos} instance.
     *
     * @param base   the Java object to use as a base. If {@code null}, {@code offset} is assumed to be an off-heap memory address.
     * @param offset the base offset (in bytes) relative to the given Java object to read from
     * @return the {@link TilePos} instance
     */
    public static TilePos load(Object base, long offset) {
        int level = PUnsafe.getByte(base, offset + LEVEL_OFFSET) & 0xFF;

        int interleavedHigh = PUnsafe.getUnalignedIntBE(base, offset + HIGH_OFFSET);
        long interleavedLow = PUnsafe.getUnalignedLongBE(base, offset + LOW_OFFSET);
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new TilePos(level, x, y, z);
    }

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link TilePos} instance.
     * <p>
     * The buffer's {@link ByteBuf#readerIndex() reader index} will be increased by the number of bytes read.
     *
     * @param buf the {@link ByteBuf} to read from
     * @return the {@link TilePos} instance
     */
    public static TilePos load(@NonNull ByteBuf buf) {
        int level = buf.readUnsignedByte();

        //read in big-endian
        int interleavedHigh = buf.readInt();
        long interleavedLow = buf.readLong();
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new TilePos(level, x, y, z);
    }

    /**
     * Loads the position from the given {@link ByteBuf} into a {@link TilePos} instance.
     * <p>
     * The buffer's {@link ByteBuf#readerIndex() reader index} will not be modified.
     *
     * @param buf   the {@link ByteBuf} to read from
     * @param index the index in the buffer to read from
     * @return the {@link TilePos} instance
     */
    public static TilePos load(@NonNull ByteBuf buf, int index) {
        int level = buf.getUnsignedByte(index + LEVEL_OFFSET);

        //read in big-endian
        int interleavedHigh = buf.getInt(index + HIGH_OFFSET);
        long interleavedLow = buf.getLong(index + LOW_OFFSET);
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new TilePos(level, x, y, z);
    }

    /**
     * Loads the position from the given {@link DataIn} into a {@link TilePos} instance.
     *
     * @param in the {@link DataIn} to read from
     * @return the {@link TilePos} instance
     */
    public static TilePos readPos(@NonNull DataIn in) throws IOException {
        int level = in.readUnsignedByte();

        int interleavedHigh = in.readInt();
        long interleavedLow = in.readLong();
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);
        return new TilePos(level, x, y, z);
    }
}
