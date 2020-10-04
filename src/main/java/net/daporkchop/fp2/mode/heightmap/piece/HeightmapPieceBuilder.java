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
import lombok.NonNull;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * @author DaPorkchop_
 */
public class HeightmapPieceBuilder implements IFarPieceBuilder {
    protected final long addr = PUnsafe.allocateMemory(this, HeightmapPiece.TOTAL_SIZE);

    public HeightmapPieceBuilder set(int x, int z, HeightmapData data)  {
        long base = this.addr + HeightmapPiece.index(x, z);
        PUnsafe.putInt(base + 0L, data.height);
        PUnsafe.putInt(base + 4L, (data.light << 24) | data.state);
        PUnsafe.putInt(base + 8L, (data.waterBiome << 16) | (data.waterLight << 8) | data.biome);
        return this;
    }

    @Override
    public void reset() {
        PUnsafe.setMemory(this.addr, HeightmapPiece.TOTAL_SIZE, (byte) 0); //just clear it
    }

    @Override
    public boolean write(@NonNull ByteBuf dst) {
        dst.writeBytes(Unpooled.wrappedBuffer(this.addr, HeightmapPiece.TOTAL_SIZE, false)); //just copy it
        return false;
    }
}
