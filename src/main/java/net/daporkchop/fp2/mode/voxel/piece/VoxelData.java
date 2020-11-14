/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.mode.voxel.piece;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import net.daporkchop.fp2.mode.api.piece.IFarData;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class VoxelData implements IFarData {
    //layout (in ints):
    //0: (density1 << 20) | (density0 << 16) | (biome << 8) | light
    //  ^ 8 bits are free
    //1: state

    public static final int SIZE = T_VOXELS;
    public static final int ENTRY_COUNT = SIZE * SIZE * SIZE;

    public static final int ENTRY_DATA_SIZE = 2;
    public static final int ENTRY_DATA_SIZE_BYTES = ENTRY_DATA_SIZE * 4;

    public static final int TOTAL_SIZE = ENTRY_DATA_SIZE * ENTRY_COUNT;
    public static final int TOTAL_SIZE_BYTES = ENTRY_DATA_SIZE_BYTES * ENTRY_COUNT;

    static int index(int x, int y, int z) {
        checkArg(x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE, "coordinates out of bounds (x=%d, y=%d, z=%d)", x, y, z);
        return (x * SIZE + y) * SIZE + z;
    }

    static void writeSample(long base, VoxelDataSample sample) {
        PUnsafe.putInt(base + 0L, (sample.density1 << 20) | (sample.density0 << 16) | (sample.biome << 8) | sample.light);
        PUnsafe.putInt(base + 4L, sample.state);
    }

    static void readSample(long base, VoxelDataSample sample) {
        int i0 = PUnsafe.getInt(base + 0L);
        int i1 = PUnsafe.getInt(base + 4L);

        sample.density0 = (i0 >> 20) & 0xF;
        sample.density1 = (i0 >> 16) & 0xF;
        sample.biome = (i0 >> 8) & 0xFF;
        sample.light = i0 & 0xFF;

        sample.state = i1;
    }

    protected final long addr = PUnsafe.allocateMemory(this, TOTAL_SIZE_BYTES);

    public VoxelData() {
        this.reset();
    }

    public void get(int x, int y, int z, VoxelDataSample sample) {
        readSample(this.addr + index(x, y, z) * ENTRY_DATA_SIZE_BYTES, sample);
    }

    public void set(int x, int y, int z, VoxelDataSample sample) {
        writeSample(this.addr + index(x, y, z) * ENTRY_DATA_SIZE_BYTES, sample);
    }

    @Override
    public void reset() {
        PUnsafe.setMemory(this.addr, TOTAL_SIZE, (byte) 0);
    }

    @Override
    public void read(@NonNull ByteBuf src) {
        if (PlatformInfo.IS_LITTLE_ENDIAN) {
            //copy everything in one go
            src.readBytes(Unpooled.wrappedBuffer(this.addr, TOTAL_SIZE_BYTES, false).writerIndex(0));
        } else {
            //read individual ints (reversing the byte order each time)
            for (int i = 0; i < TOTAL_SIZE; i++) {
                PUnsafe.putInt(this.addr + i * 4L, src.readIntLE());
            }
        }
    }

    @Override
    public boolean write(@NonNull ByteBuf dst) {
        if (PlatformInfo.IS_LITTLE_ENDIAN) {
            //copy everything in one go
            dst.writeBytes(Unpooled.wrappedBuffer(this.addr, TOTAL_SIZE_BYTES, false));
        } else {
            //write individual ints (reversing the byte order each time)
            dst.ensureWritable(TOTAL_SIZE_BYTES);
            for (int i = 0; i < TOTAL_SIZE; i++) {
                dst.writeIntLE(PUnsafe.getInt(this.addr + i * 4L));
            }
        }
        return false;
    }
}
