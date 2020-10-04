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
    public static final int ENTRY_SIZE = 3;
    public static final int ENTRY_COUNT = T_VOXELS * T_VOXELS;

    public static final int TOTAL_SIZE = ENTRY_COUNT * ENTRY_SIZE;

    static int index(int x, int z) {
        checkArg(x >= 0 && x < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, z=%d)", x, z);
        return (x * T_VOXELS + z) * ENTRY_SIZE;
    }

    protected final long addr = PUnsafe.allocateMemory(this, TOTAL_SIZE);

    public void get(int x, int z, HeightmapData data) {
        long base = this.addr + index(x, z);

        int i = PUnsafe.getInt(base + 0L);
        data.height = i;

        i = PUnsafe.getInt(base + 4L);
        data.state = i & 0x00FFFFFF;
        data.light = i >>> 24;

        i = PUnsafe.getInt(base + 8L);
        data.biome = i & 0xFF;
        data.waterLight = (i >>> 8) & 0xFF;
        data.waterBiome = (i >>> 16) & 0xFF;
    }

    public int height(int x, int z) {
        return PUnsafe.getInt(this.addr + index(x, z));
    }

    @Override
    public RenderMode mode() {
        return RenderMode.HEIGHTMAP;
    }

    @Override
    public void read(@NonNull ByteBuf src) {
        src.readBytes(Unpooled.wrappedBuffer(this.addr, TOTAL_SIZE, false).writerIndex(0)); //simply copy data
    }
}
