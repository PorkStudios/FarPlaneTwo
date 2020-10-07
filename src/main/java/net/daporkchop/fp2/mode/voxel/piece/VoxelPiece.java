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
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Stores server-side data for the voxel strategy.
 *
 * @author DaPorkchop_
 */
@Getter
public class VoxelPiece implements IFarPiece {
    //layout (in ints):
    //0: (dx << 24) | (dy << 16) | (dz << 8) | edges
    //1: (biome << 8) | light
    //2: state
    //   ^ top 8 bits are free

    public static final int ENTRY_DATA_SIZE = 3;
    public static final int ENTRY_DATA_SIZE_BYTES = ENTRY_DATA_SIZE * 4;
    public static final int ENTRY_COUNT = T_VOXELS * T_VOXELS * T_VOXELS;

    public static final int ENTRY_FULL_SIZE_BYTES = ENTRY_DATA_SIZE * 4 + 2;
    public static final int PIECE_SIZE = ENTRY_FULL_SIZE_BYTES * ENTRY_COUNT;

    static int index(int x, int y, int z) {
        checkArg(x >= 0 && x < T_VOXELS && y >= 0 && y < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, y=%d, z=%d)", x, y, z);
        return (x * T_VOXELS + y) * T_VOXELS + z;
    }

    static void writeData(long base, VoxelData data) {
        int dx = floorI(clamp(data.x, 0., 1.) * 255.0d);
        int dy = floorI(clamp(data.y, 0., 1.) * 255.0d);
        int dz = floorI(clamp(data.z, 0., 1.) * 255.0d);
        int edges = data.edges; //TODO: do edge mask conversion in generators rather than here
        edges = ((edges & 0x800) >> 9) | ((edges & 0x80) >> 6) | ((edges & 0x8) >> 3);

        PUnsafe.putInt(base + 0L, (dx << 24) | (dy << 16) | (dz << 8) | edges);
        PUnsafe.putInt(base + 4L, (data.biome << 8) | data.light);
        PUnsafe.putInt(base + 8L, data.state);
    }

    static void readData(long base, VoxelData data) {
        int i0 = PUnsafe.getInt(base + 0L);
        int i1 = PUnsafe.getInt(base + 4L);
        int i2 = PUnsafe.getInt(base + 8L);

        data.x = (i0 >>> 24) / 255.0d;
        data.y = ((i0 >> 16) & 0xFF) / 255.0d;
        data.z = ((i0 >> 8) & 0xFF) / 255.0d;
        data.edges = i0 & 0xFF;

        data.state = i2;
        data.biome = i1 >>> 8;
        data.light = i1 & 0xFF;
    }

    protected final long addr = PUnsafe.allocateMemory(this, PIECE_SIZE);

    protected int count; //the number of voxels in the piece that are set

    @Override
    public RenderMode mode() {
        return RenderMode.VOXEL;
    }

    @Override
    public void read(@NonNull ByteBuf src) {
        int count = this.count = src.readIntLE();

        long addr = this.addr;
        for (int i = 0; i < count; i++) { //copy data
            PUnsafe.putShort(addr, src.readShortLE());
            addr += 2L;
            for (int j = 0; j < ENTRY_DATA_SIZE; j++, addr += 4L) {
                PUnsafe.putInt(addr, src.readIntLE());
            }
        }
    }

    /**
     * @return the number of set voxels in this piece
     */
    public int size() {
        return this.count;
    }

    /**
     * Gets the voxel at the given index.
     *
     * @param index the index of the voxel to get
     * @param data  the {@link VoxelData} instance to store the data into
     * @return the relative offset of the voxel (combined XYZ coords)
     */
    public int get(int index, VoxelData data) {
        long base = this.addr + checkIndex(this.count, index) * ENTRY_FULL_SIZE_BYTES;
        readData(base + 2L, data);
        return PUnsafe.getChar(base);
    }
}
