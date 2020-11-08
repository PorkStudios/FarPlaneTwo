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

import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
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
    //                                       ^ 2 bits are free
    //1: (biome << 8) | light
    //  ^ 16 bits are free
    //2: state0
    //3: state1
    //4: state2

    public static final int ENTRY_COUNT = T_VOXELS * T_VOXELS * T_VOXELS;
    protected static final int INDEX_SIZE = ENTRY_COUNT * 2;

    public static final int ENTRY_DATA_SIZE = 2 + EDGE_COUNT;
    public static final int ENTRY_DATA_SIZE_BYTES = ENTRY_DATA_SIZE * 4;

    public static final int ENTRY_FULL_SIZE_BYTES = ENTRY_DATA_SIZE * 4 + 2;
    public static final int PIECE_SIZE = INDEX_SIZE + ENTRY_FULL_SIZE_BYTES * ENTRY_COUNT;

    static int index(int x, int y, int z) {
        checkArg(x >= 0 && x < T_VOXELS && y >= 0 && y < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, y=%d, z=%d)", x, y, z);
        return (x * T_VOXELS + y) * T_VOXELS + z;
    }

    static void writeData(long base, VoxelData data) {
        PUnsafe.putInt(base + 0L, (data.x << 24) | (data.y << 16) | (data.z << 8) | data.edges);
        PUnsafe.putInt(base + 4L, (data.biome << 8) | data.light);
        PUnsafe.copyMemory(data.states, PUnsafe.ARRAY_INT_BASE_OFFSET, null, base + 8L, 4L * EDGE_COUNT);
    }

    static void readData(long base, VoxelData data) {
        int i0 = PUnsafe.getInt(base + 0L);
        int i1 = PUnsafe.getInt(base + 4L);

        data.x = i0 >>> 24;
        data.y = (i0 >> 16) & 0xFF;
        data.z = (i0 >> 8) & 0xFF;
        data.edges = i0 & 0x3F;

        data.biome = (i1 >> 8) & 0xFF;
        data.light = i1 & 0xFF;

        PUnsafe.copyMemory(null, base + 8L, data.states, PUnsafe.ARRAY_INT_BASE_OFFSET, 4L * EDGE_COUNT);
    }

    static void readOnlyPos(long base, VoxelData data) {
        int i0 = PUnsafe.getInt(base + 0L);

        data.x = i0 >>> 24;
        data.y = (i0 >> 16) & 0xFF;
        data.z = (i0 >> 8) & 0xFF;
    }

    protected final long addr = PUnsafe.allocateMemory(this, PIECE_SIZE);

    protected int count = -1; //the number of voxels in the piece that are set

    @Override
    public RenderMode mode() {
        return RenderMode.VOXEL;
    }

    @Override
    public void read(@NonNull ByteBuf src) {
        if (this.count != 0) { //index needs to be cleared
            PUnsafe.setMemory(this.addr, INDEX_SIZE, (byte) 0xFF); //fill index with -1
        }

        int count = this.count = src.readIntLE();

        long addr = this.addr + INDEX_SIZE;
        for (int i = 0; i < count; i++) { //copy data
            int pos = src.readShortLE();
            PUnsafe.putShort(this.addr + pos * 2L, (short) i); //put data slot into index

            PUnsafe.putChar(addr, (char) pos); //prefix data with pos
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
        long base = this.addr + INDEX_SIZE + checkIndex(this.count, index) * ENTRY_FULL_SIZE_BYTES;
        readData(base + 2L, data);
        return PUnsafe.getChar(base);
    }

    public boolean get(int x, int y, int z, VoxelData data)   {
        int index = PUnsafe.getShort(this.addr + index(x, y, z) * 2L);
        if (index < 0)  { //index is unset, don't read data
            return false;
        }

        readData(this.addr + INDEX_SIZE + index * ENTRY_FULL_SIZE_BYTES + 2L, data);
        return true;
    }

    public boolean getOnlyPos(int x, int y, int z, VoxelData data)   {
        int index = PUnsafe.getShort(this.addr + index(x, y, z) * 2L);
        if (index < 0)  { //index is unset, don't read data
            return false;
        }

        readOnlyPos(this.addr + INDEX_SIZE + index * ENTRY_FULL_SIZE_BYTES + 2L, data);
        return true;
    }
}
