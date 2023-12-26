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
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.io.IOException;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Stores server-side data for the voxel strategy.
 *
 * @author DaPorkchop_
 */
@Getter
public class Tile implements IFarTile {
    static {
        //we copy values directly between int[] and off-heap memory
        PUnsafe.requireTightlyPackedPrimitiveArrays();

        //we assume unaligned memory accesses are supported
        PUnsafe.requireUnalignedAccess();
    }

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
    public static final int TILE_SIZE = INDEX_SIZE + ENTRY_FULL_SIZE_BYTES * ENTRY_COUNT;

    public static final IVariableSizeRecyclingCodec<Tile> CODEC = new IVariableSizeRecyclingCodec<Tile>() {
        @Override
        public long maxSize() {
            return TILE_SIZE;
        }

        @Override
        public void load(@NonNull Tile tile, @NonNull DataIn in) throws IOException {
            tile.reset();

            int count = tile.count = in.readIntLE();

            long addr = tile.addr + INDEX_SIZE;
            for (int i = 0; i < count; i++) { //copy data
                int pos = in.readUnsignedShortLE();

                PUnsafe.putShort(tile.addr + pos * 2L, (short) i); //put data slot into index

                PUnsafe.putChar(addr, (char) pos); //prefix data with pos
                addr += 2L;
                for (int j = 0; j < ENTRY_DATA_SIZE; j++, addr += 4L) {
                    PUnsafe.putUnalignedInt(addr, in.readIntLE());
                }
            }
        }

        @Override
        public void store(@NonNull Tile tile, @NonNull DataOut out) throws IOException {
            //first pass: determine number of set voxels for length prefix
            //TODO: modify tile data format so that we don't need a length prefix
            int count = 0;
            for (int i = 0; i < ENTRY_COUNT; i++) { //iterate through the index and search for set voxels
                int index = PUnsafe.getShort(tile.addr + i * 2L);
                if (index >= 0) { //voxel is set
                    count++;
                }
            }

            //second pass: actually write data
            out.writeIntLE(count);
            for (int i = 0; i < ENTRY_COUNT; i++) { //iterate through the index and search for set voxels
                int index = PUnsafe.getShort(tile.addr + i * 2L);
                if (index >= 0) { //voxel is set
                    //write position
                    out.writeShortLE(i);

                    long data = tile.addr + INDEX_SIZE + index * ENTRY_DATA_SIZE_BYTES;
                    for (int j = 0; j < ENTRY_DATA_SIZE; j++) { //write voxel data
                        out.writeIntLE(PUnsafe.getUnalignedInt(data + j * 4L));
                    }
                }
            }
        }
    };

    static int index(int x, int y, int z) {
        assert x >= 0 && x < T_VOXELS : "x=" + x;
        assert y >= 0 && y < T_VOXELS : "y=" + y;
        assert z >= 0 && z < T_VOXELS : "z=" + z;

        return (x * T_VOXELS + y) * T_VOXELS + z;
    }

    static void writeData(long base, TileData data) {
        PUnsafe.putInt(base + 0L, (data.x << 24) | (data.y << 16) | (data.z << 8) | data.edges);
        PUnsafe.putInt(base + 4L, (data.biome << 8) | (data.light & 0xFF));
        PUnsafe.copyMemory(data.states, PUnsafe.ARRAY_INT_BASE_OFFSET, null, base + 8L, 4L * EDGE_COUNT);
    }

    static void readData(long base, TileData data) {
        int i0 = PUnsafe.getInt(base + 0L);
        int i1 = PUnsafe.getInt(base + 4L);

        data.x = i0 >>> 24;
        data.y = (i0 >> 16) & 0xFF;
        data.z = (i0 >> 8) & 0xFF;
        data.edges = i0 & 0x3F;

        data.biome = (i1 >> 8) & 0xFF;
        data.light = (byte) i1;

        PUnsafe.copyMemory(null, base + 8L, data.states, PUnsafe.ARRAY_INT_BASE_OFFSET, 4L * EDGE_COUNT);
    }

    static void readOnlyPos(long base, TileData data) {
        int i0 = PUnsafe.getInt(base + 0L);

        data.x = i0 >>> 24;
        data.y = (i0 >> 16) & 0xFF;
        data.z = (i0 >> 8) & 0xFF;
    }

    static int readOnlyPosAndReturnEdges(long base, double[] dst, int dstOff) {
        int i0 = PUnsafe.getInt(base + 0L);

        dst[dstOff + 0] = (i0 >>> 24) / (double) POS_ONE;
        dst[dstOff + 1] = ((i0 >> 16) & 0xFF) / (double) POS_ONE;
        dst[dstOff + 2] = ((i0 >> 8) & 0xFF) / (double) POS_ONE;

        return i0 & 0x3F;
    }

    protected final long addr = PUnsafe.allocateMemory(TILE_SIZE);

    protected int count = -1; //the number of voxels in the tile that are set

    @Setter
    protected long extra = 0L;

    public Tile() {
        this.reset();

        PCleaner.cleaner(this, this.addr);
    }

    /**
     * Gets the voxel at the given index.
     *
     * @param index the index of the voxel to get
     * @param data  the {@link TileData} instance to store the data into
     * @return the relative offset of the voxel (combined XYZ coords)
     */
    public int get(int index, TileData data) {
        long base = this.addr + INDEX_SIZE + checkIndex(this.count, index) * ENTRY_FULL_SIZE_BYTES;
        readData(base + 2L, data);
        return PUnsafe.getChar(base);
    }

    public int getOnlyPos(int index, TileData data) {
        long base = this.addr + INDEX_SIZE + checkIndex(this.count, index) * ENTRY_FULL_SIZE_BYTES;
        readOnlyPos(base + 2L, data);
        return PUnsafe.getChar(base);
    }

    public boolean get(int x, int y, int z, TileData data) {
        int index = PUnsafe.getShort(this.addr + index(x, y, z) * 2L);
        if (index < 0) { //index is unset, don't read sample
            return false;
        }

        readData(this.addr + INDEX_SIZE + index * ENTRY_FULL_SIZE_BYTES + 2L, data);
        return true;
    }

    public boolean getOnlyPos(int x, int y, int z, TileData data) {
        int index = PUnsafe.getShort(this.addr + index(x, y, z) * 2L);
        if (index < 0) { //index is unset, don't read sample
            return false;
        }

        readOnlyPos(this.addr + INDEX_SIZE + index * ENTRY_FULL_SIZE_BYTES + 2L, data);
        return true;
    }

    public Tile set(int x, int y, int z, TileData data) {
        long indexAddr = this.addr + Tile.index(x, y, z) * 2L;
        int index = PUnsafe.getShort(indexAddr);
        if (index < 0) { //index is unset, allocate new one
            PUnsafe.putShort(indexAddr, (short) (index = this.count++));
        }

        Tile.writeData(this.addr + Tile.INDEX_SIZE + index * Tile.ENTRY_DATA_SIZE_BYTES, data);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return this.count == 0;
    }

    @Override
    public void reset() {
        this.extra = 0L;

        if (this.count != 0) {
            this.count = 0;
            PUnsafe.setMemory(this.addr, Tile.INDEX_SIZE, (byte) 0xFF); //fill index with -1
            //data doesn't need to be cleared, it's effectively wiped along with the index
        }
    }

    @Override
    public void read(@NonNull ByteBuf src) {
        this.reset();

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

    @Override
    public boolean write(@NonNull ByteBuf dst) {
        if (this.count == 0) { //tile is empty, nothing needs to be encoded
            return true;
        }

        int sizeIndex = dst.writerIndex();
        dst.writeIntLE(-1);

        int count = 0;
        for (int i = 0; i < ENTRY_COUNT; i++) { //iterate through the index and search for set voxels
            int index = PUnsafe.getShort(this.addr + i * 2L);
            if (index >= 0) { //voxel is set
                dst.writeShortLE(i); //write position
                long base = this.addr + INDEX_SIZE + index * ENTRY_DATA_SIZE_BYTES;
                for (int j = 0; j < ENTRY_DATA_SIZE; j++) { //write voxel data
                    dst.writeIntLE(PUnsafe.getInt(base + j * 4L));
                }
                count++;
            }
        }

        dst.setIntLE(sizeIndex, count);
        return false;
    }

    public int getOnlyPosAndReturnEdges(int x, int y, int z, double[] dst, int dstOff) {
        int index = PUnsafe.getShort(this.addr + index(x, y, z) * 2L);
        if (index < 0) { //index is unset, don't read data
            return -1;
        }

        return readOnlyPosAndReturnEdges(this.addr + INDEX_SIZE + index * ENTRY_FULL_SIZE_BYTES + 2L, dst, dstOff);
    }
}
