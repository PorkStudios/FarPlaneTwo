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

package net.daporkchop.fp2.core.mode.voxel;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarPosSerializer;
import net.daporkchop.fp2.core.util.math.MathUtil;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class VoxelPosSerializer implements IFarPosSerializer<VoxelPos> {
    public static final VoxelPosSerializer INSTANCE = new VoxelPosSerializer();

    private static final int LEVEL_OFFSET = 0;
    private static final int HIGH_OFFSET = LEVEL_OFFSET + BYTE_SIZE;
    private static final int LOW_OFFSET = HIGH_OFFSET + INT_SIZE;

    private static final int SIZE = LOW_OFFSET + LONG_SIZE;

    static {
        checkState(PlatformInfo.UNALIGNED, "Unaligned memory access not supported?!? (what kind of computer are you running this on lol)");
    }

    @Override
    public long posSize() {
        return SIZE;
    }

    //
    // STORE
    //

    @Override
    public void storePos(@NonNull VoxelPos pos, long addr) {
        PUnsafe.putByte(addr + LEVEL_OFFSET, toByte(pos.level(), "level"));

        int interleavedHigh = MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z());
        long interleavedLow = MathUtil.interleaveBits(pos.x(), pos.y(), pos.z());
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //write in big-endian
            interleavedHigh = Integer.reverseBytes(interleavedHigh);
            interleavedLow = Long.reverseBytes(interleavedLow);
        }
        PUnsafe.putInt(addr + HIGH_OFFSET, interleavedHigh);
        PUnsafe.putLong(addr + LOW_OFFSET, interleavedLow);
    }

    @Override
    public void storePos(@NonNull VoxelPos pos, byte[] arr, int index) {
        this.storePos(pos, arr, PUnsafe.arrayByteElementOffset(index));
    }

    @Override
    public void storePos(@NonNull VoxelPos pos, Object base, long offset) {
        PUnsafe.putByte(base, offset + LEVEL_OFFSET, toByte(pos.level(), "level"));

        int interleavedHigh = MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z());
        long interleavedLow = MathUtil.interleaveBits(pos.x(), pos.y(), pos.z());
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //write in big-endian
            interleavedHigh = Integer.reverseBytes(interleavedHigh);
            interleavedLow = Long.reverseBytes(interleavedLow);
        }
        PUnsafe.putInt(base, offset + HIGH_OFFSET, interleavedHigh);
        PUnsafe.putLong(base, offset + LOW_OFFSET, interleavedLow);
    }

    @Override
    public void storePos(@NonNull VoxelPos pos, @NonNull ByteBuf buf) {
        buf.ensureWritable(SIZE).writeByte(pos.level())
                //write in big-endian
                .writeInt(MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()))
                .writeLong(MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public void storePos(@NonNull VoxelPos pos, @NonNull ByteBuf buf, int index) {
        buf.setByte(index + LEVEL_OFFSET, pos.level())
                //write in big-endian
                .setInt(index + HIGH_OFFSET, MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()))
                .setLong(index + LOW_OFFSET, MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public void storePos(@NonNull VoxelPos pos, @NonNull ByteBuffer buf) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        buf.put(toByte(pos.level(), "level"))
                .putInt(MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()))
                .putLong(MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    @Override
    public void storePos(@NonNull VoxelPos pos, @NonNull ByteBuffer buf, int index) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        buf.put(index + LEVEL_OFFSET, toByte(pos.level(), "level"))
                .putInt(index + HIGH_OFFSET, MathUtil.interleaveBitsHigh(pos.x(), pos.y(), pos.z()))
                .putLong(index + LOW_OFFSET, MathUtil.interleaveBits(pos.x(), pos.y(), pos.z()));
    }

    //
    // LOAD
    //

    @Override
    public VoxelPos loadPos(long addr) {
        int level = PUnsafe.getByte(addr + LEVEL_OFFSET) & 0xFF;

        int interleavedHigh = PUnsafe.getInt(addr + HIGH_OFFSET);
        long interleavedLow = PUnsafe.getLong(addr + LOW_OFFSET);
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //read in big-endian
            interleavedHigh = Integer.reverseBytes(interleavedHigh);
            interleavedLow = Long.reverseBytes(interleavedLow);
        }
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new VoxelPos(level, x, y, z);
    }

    @Override
    public VoxelPos loadPos(byte[] arr, int index) {
        return this.loadPos(arr, PUnsafe.arrayByteElementOffset(index));
    }

    @Override
    public VoxelPos loadPos(Object base, long offset) {
        int level = PUnsafe.getByte(base, offset + LEVEL_OFFSET) & 0xFF;

        int interleavedHigh = PUnsafe.getInt(base, offset + HIGH_OFFSET);
        long interleavedLow = PUnsafe.getLong(base, offset + LOW_OFFSET);
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //read in big-endian
            interleavedHigh = Integer.reverseBytes(interleavedHigh);
            interleavedLow = Long.reverseBytes(interleavedLow);
        }
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new VoxelPos(level, x, y, z);
    }

    @Override
    public VoxelPos loadPos(@NonNull ByteBuf buf) {
        int level = buf.readUnsignedByte();

        //read in big-endian
        int interleavedHigh = buf.readInt();
        long interleavedLow = buf.readLong();
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new VoxelPos(level, x, y, z);
    }

    @Override
    public VoxelPos loadPos(@NonNull ByteBuf buf, int index) {
        int level = buf.getUnsignedByte(index + LEVEL_OFFSET);

        //read in big-endian
        int interleavedHigh = buf.getInt(index + HIGH_OFFSET);
        long interleavedLow = buf.getLong(index + LOW_OFFSET);
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new VoxelPos(level, x, y, z);
    }

    @Override
    public VoxelPos loadPos(@NonNull ByteBuffer buf) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        int level = buf.get() & 0xFF;

        int interleavedHigh = buf.getInt();
        long interleavedLow = buf.getLong();
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new VoxelPos(level, x, y, z);
    }

    @Override
    public VoxelPos loadPos(@NonNull ByteBuffer buf, int index) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        int level = buf.get(index + LEVEL_OFFSET) & 0xFF;

        int interleavedHigh = buf.getInt(index + HIGH_OFFSET);
        long interleavedLow = buf.getLong(index + LOW_OFFSET);
        int x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        int y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        int z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

        return new VoxelPos(level, x, y, z);
    }
}
