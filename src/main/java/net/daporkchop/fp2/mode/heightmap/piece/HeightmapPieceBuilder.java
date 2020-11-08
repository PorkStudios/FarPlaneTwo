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
import lombok.Setter;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * @author DaPorkchop_
 */
public class HeightmapPieceBuilder implements IFarPieceBuilder {
    protected final long addr = PUnsafe.allocateMemory(this, HeightmapPiece.TOTAL_SIZE_BYTES);

    @Getter
    @Setter
    protected long extra;

    public HeightmapPieceBuilder set(int x, int z, HeightmapSample data)  {
        HeightmapPiece.writeSample(this.addr + HeightmapPiece.index(x, z) * 4L, data);
        return this;
    }

    public HeightmapSample get(int x, int z, HeightmapSample data)  {
        HeightmapPiece.readSample(this.addr + HeightmapPiece.index(x, z) * 4L, data);
        return data;
    }

    @Override
    public void reset() {
        PUnsafe.setMemory(this.addr, HeightmapPiece.TOTAL_SIZE_BYTES, (byte) 0); //just clear it
        this.extra = 0L;
    }

    @Override
    public boolean write(@NonNull ByteBuf dst) {
        dst.writeBytes(Unpooled.wrappedBuffer(this.addr, HeightmapPiece.TOTAL_SIZE_BYTES, false)); //just copy it
        //screw endianess, we ensure that only little-endian machines can boot this during startup
        // now, i could make an implementation for big-endian machines that flips the order, but i won't because that would be a pain
        // nobody with a non-x86 machine will likely ever run this mod
        // ...except people running the new macs with apple silicon
        // ...but screw them, lol
        // "think different" my ass
        return false;
    }
}
