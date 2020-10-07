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
import lombok.NonNull;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class VoxelPieceBuilder implements IFarPieceBuilder {
    protected static final int INDEX_SIZE = VoxelPiece.ENTRY_COUNT * 2;
    protected static final int DATA_SIZE = VoxelPiece.ENTRY_COUNT * VoxelPiece.ENTRY_DATA_SIZE_BYTES;
    protected static final int BUILDER_SIZE = INDEX_SIZE + DATA_SIZE;

    protected final long addr = PUnsafe.allocateMemory(this, BUILDER_SIZE);

    protected int nextSlot = 0; //next data slot to use

    public VoxelPieceBuilder set(int x, int y, int z, VoxelData data)   {
        long indexAddr = this.addr + VoxelPiece.index(x, y, z) * 2L;
        int index = PUnsafe.getShort(indexAddr);
        if (index < 0)  { //index is unset, allocate new one
            PUnsafe.putShort(indexAddr, (short) (index = this.nextSlot++));
        }

        VoxelPiece.writeData(this.addr + INDEX_SIZE + index * VoxelPiece.ENTRY_DATA_SIZE_BYTES, data);
        return this;
    }

    public boolean get(int x, int y, int z, VoxelData data)   {
        long indexAddr = this.addr + VoxelPiece.index(x, y, z) * 2L;
        int index = PUnsafe.getShort(indexAddr);
        if (index < 0)  { //index is unset, don't read data
            data.reset();
            return false;
        }

        VoxelPiece.readData(this.addr + INDEX_SIZE + index * VoxelPiece.ENTRY_DATA_SIZE_BYTES, data);
        return true;
    }

    @Override
    public void reset() {
        if (this.nextSlot != 0) {
            this.nextSlot = 0;
            PUnsafe.setMemory(this.addr, INDEX_SIZE, (byte) 0xFF); //fill index with -1
            //data doesn't need to be cleared, it's effectively wiped along with the index
        }
    }

    @Override
    public boolean write(@NonNull ByteBuf dst) {
        if (this.nextSlot == 0) { //builder is empty, nothing needs to be encoded
            return true;
        }

        int sizeIndex = dst.writerIndex();
        dst.writeIntLE(-1);

        int count = 0;
        for (int i = 0; i < VoxelPiece.ENTRY_COUNT; i++)  { //iterate through the index and search for set voxels
            int index = PUnsafe.getShort(this.addr + i * 2L);
            if (index >= 0) { //voxel is set
                dst.writeShortLE(index); //write index
                long base = this.addr + INDEX_SIZE + index * VoxelPiece.ENTRY_DATA_SIZE_BYTES;
                for (int j = 0; j < VoxelPiece.ENTRY_DATA_SIZE; j++) { //write voxel data
                    dst.writeIntLE(PUnsafe.getInt(base + j * 4L));
                }
                count++;
            }
        }

        dst.setIntLE(sizeIndex, count);
        return false;
    }
}
