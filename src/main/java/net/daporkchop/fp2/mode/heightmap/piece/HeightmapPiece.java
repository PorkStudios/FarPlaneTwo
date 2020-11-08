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

package net.daporkchop.fp2.mode.heightmap.piece;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A "piece" containing the data used by the heightmap rendering mode.
 *
 * @author DaPorkchop_
 */
@Getter
public class HeightmapPiece implements IFarPiece {
    //layout (in ints):
    //0: height
    //1: (light << 24) | state
    //2: (waterBiome << 16) | (waterLight << 8) | biome
    //   ^ top 8 bits are free

    public static final int ENTRY_SIZE = 3;
    public static final int ENTRY_COUNT = T_VOXELS * T_VOXELS;

    public static final int TOTAL_SIZE = ENTRY_COUNT * ENTRY_SIZE;
    public static final int TOTAL_SIZE_BYTES = TOTAL_SIZE * 4;

    static int index(int x, int z) {
        checkArg(x >= 0 && x < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, z=%d)", x, z);
        return (x * T_VOXELS + z) * ENTRY_SIZE;
    }

    static void writeData(long base, HeightmapSample data)    {
        PUnsafe.putInt(base + 0L, data.height);
        PUnsafe.putInt(base + 4L, (data.light << 24) | data.state);
        PUnsafe.putInt(base + 8L, (data.waterBiome << 16) | (data.waterLight << 8) | data.biome);
    }

    static void readData(long base, HeightmapSample data)    {
        int i0 = PUnsafe.getInt(base + 0L);
        int i1 = PUnsafe.getInt(base + 4L);
        int i2 = PUnsafe.getInt(base + 8L);

        data.height = i0;
        data.state = i1 & 0x00FFFFFF;
        data.light = i1 >>> 24;
        data.biome = i2 & 0xFF;

        data.waterLight = (i2 >>> 8) & 0xFF;
        data.waterBiome = (i2 >>> 16) & 0xFF;
    }

    protected final long addr = PUnsafe.allocateMemory(this, TOTAL_SIZE_BYTES);

    public HeightmapPiece set(int x, int z, HeightmapSample data)  {
        writeData(this.addr + index(x, z) * 4L, data);
        return this;
    }

    public void get(int x, int z, HeightmapSample data) {
        readData(this.addr + index(x, z) * 4L, data);
    }

    public int height(int x, int z) {
        return PUnsafe.getInt(this.addr + index(x, z) * 4L);
    }

    @Override
    public RenderMode mode() {
        return RenderMode.HEIGHTMAP;
    }

    @Override
    public void reset() {
        PUnsafe.setMemory(this.addr, HeightmapPiece.TOTAL_SIZE_BYTES, (byte) 0); //just clear it
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
        return false; //the heightmap renderer has no concept of an "empty" piece
    }
}
