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

package net.daporkchop.fp2.core.mode.heightmap;

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
public final class HeightmapPosSerializer implements IFarPosSerializer<HeightmapPos> {
    public static final HeightmapPosSerializer INSTANCE = new HeightmapPosSerializer();

    private static final int LEVEL_OFFSET = 0;
    private static final int COORDS_OFFSET = LEVEL_OFFSET + BYTE_SIZE;

    private static final int SIZE = COORDS_OFFSET + LONG_SIZE;

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
    public void storePos(@NonNull HeightmapPos pos, long addr) {
        PUnsafe.putByte(addr + LEVEL_OFFSET, toByte(pos.level(), "level"));

        long interleaved = MathUtil.interleaveBits(pos.x(), pos.z());
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //write in big-endian
            interleaved = Long.reverseBytes(interleaved);
        }
        PUnsafe.putLong(addr + COORDS_OFFSET, interleaved);
    }

    @Override
    public void storePos(@NonNull HeightmapPos pos, byte[] arr, int index) {
        this.storePos(pos, arr, PUnsafe.arrayByteElementOffset(index));
    }

    @Override
    public void storePos(@NonNull HeightmapPos pos, Object base, long offset) {
        PUnsafe.putByte(base, offset + LEVEL_OFFSET, toByte(pos.level(), "level"));

        long interleaved = MathUtil.interleaveBits(pos.x(), pos.z());
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //write in big-endian
            interleaved = Long.reverseBytes(interleaved);
        }
        PUnsafe.putLong(base, offset + COORDS_OFFSET, interleaved);
    }

    @Override
    public void storePos(@NonNull HeightmapPos pos, @NonNull ByteBuf buf) {
        buf.ensureWritable(SIZE).writeByte(pos.level())
                //write in big-endian
                .writeLong(MathUtil.interleaveBits(pos.x(), pos.z()));
    }

    @Override
    public void storePos(@NonNull HeightmapPos pos, @NonNull ByteBuf buf, int index) {
        buf.setByte(index + LEVEL_OFFSET, pos.level())
                //write in big-endian
                .setLong(index + COORDS_OFFSET, MathUtil.interleaveBits(pos.x(), pos.z()));
    }

    @Override
    public void storePos(@NonNull HeightmapPos pos, @NonNull ByteBuffer buf) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        buf.put(toByte(pos.level(), "level"))
                .putLong(MathUtil.interleaveBits(pos.x(), pos.z()));
    }

    @Override
    public void storePos(@NonNull HeightmapPos pos, @NonNull ByteBuffer buf, int index) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        buf.put(index + LEVEL_OFFSET, toByte(pos.level(), "level"))
                .putLong(index + COORDS_OFFSET, MathUtil.interleaveBits(pos.x(), pos.z()));
    }

    //
    // LOAD
    //

    @Override
    public HeightmapPos loadPos(long addr) {
        int level = PUnsafe.getByte(addr + LEVEL_OFFSET) & 0xFF;

        long interleaved = PUnsafe.getLong(addr + COORDS_OFFSET);
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //read in big-endian
            interleaved = Long.reverseBytes(interleaved);
        }
        int x = MathUtil.uninterleave2_0(interleaved);
        int z = MathUtil.uninterleave2_1(interleaved);

        return new HeightmapPos(level, x, z);
    }

    @Override
    public HeightmapPos loadPos(byte[] arr, int index) {
        return this.loadPos(arr, PUnsafe.arrayByteElementOffset(index));
    }

    @Override
    public HeightmapPos loadPos(Object base, long offset) {
        int level = PUnsafe.getByte(base, offset + LEVEL_OFFSET) & 0xFF;

        long interleaved = PUnsafe.getLong(base, offset + COORDS_OFFSET);
        if (PlatformInfo.IS_LITTLE_ENDIAN) { //read in big-endian
            interleaved = Long.reverseBytes(interleaved);
        }
        int x = MathUtil.uninterleave2_0(interleaved);
        int z = MathUtil.uninterleave2_1(interleaved);

        return new HeightmapPos(level, x, z);
    }

    @Override
    public HeightmapPos loadPos(@NonNull ByteBuf buf) {
        int level = buf.readUnsignedByte();

        //read in big-endian
        long interleaved = buf.readLong();
        int x = MathUtil.uninterleave2_0(interleaved);
        int z = MathUtil.uninterleave2_1(interleaved);

        return new HeightmapPos(level, x, z);
    }

    @Override
    public HeightmapPos loadPos(@NonNull ByteBuf buf, int index) {
        int level = buf.getUnsignedByte(index + LEVEL_OFFSET);

        //read in big-endian
        long interleaved = buf.getLong(index + COORDS_OFFSET);
        int x = MathUtil.uninterleave2_0(interleaved);
        int z = MathUtil.uninterleave2_1(interleaved);

        return new HeightmapPos(level, x, z);
    }

    @Override
    public HeightmapPos loadPos(@NonNull ByteBuffer buf) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        int level = buf.get() & 0xFF;

        long interleaved = buf.getLong();
        int x = MathUtil.uninterleave2_0(interleaved);
        int z = MathUtil.uninterleave2_1(interleaved);

        return new HeightmapPos(level, x, z);
    }

    @Override
    public HeightmapPos loadPos(@NonNull ByteBuffer buf, int index) {
        checkArg(buf.order() == ByteOrder.BIG_ENDIAN, "buffer must be big-endian");

        int level = buf.get(index + LEVEL_OFFSET) & 0xFF;

        long interleaved = buf.getLong(index + COORDS_OFFSET);
        int x = MathUtil.uninterleave2_0(interleaved);
        int z = MathUtil.uninterleave2_1(interleaved);

        return new HeightmapPos(level, x, z);
    }
}
