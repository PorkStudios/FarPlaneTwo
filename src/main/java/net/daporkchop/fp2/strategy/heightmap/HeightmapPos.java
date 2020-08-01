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

package net.daporkchop.fp2.strategy.heightmap;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.misc.string.PStrings;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class HeightmapPos implements IFarPos {
    protected final int x;
    protected final int z;
    protected final int level;

    public HeightmapPos(@NonNull ByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public HeightmapPos(@NonNull HeightmapPiece piece) {
        this(piece.x, piece.z, piece.level);
    }

    @Override
    public void writePosNoLevel(@NonNull ByteBuf dst) {
        dst.writeInt(this.x).writeInt(this.z);
    }

    @Override
    public int hashCode() {
        return PMath.mix32(BinMath.packXY(this.x, this.z) ^ this.level);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof HeightmapPos) {
            HeightmapPos pos = (HeightmapPos) obj;
            return this.x == pos.x && this.z == pos.z && this.level == pos.level;
        } else {
            return false;
        }
    }

    public int blockX() {
        return this.x * T_VOXELS << this.level;
    }

    public int blockZ() {
        return this.z * T_VOXELS << this.level;
    }

    @Override
    public HeightmapPos up() {
        return new HeightmapPos(this.x >> 1, this.z >> 1, this.level + 1);
    }

    @Override
    public RenderMode mode() {
        return RenderMode.HEIGHTMAP;
    }

    @Override
    public String toString() {
        return PStrings.fastFormat("HeightmapPos(x=%d, z=%d, level=%d)", this.x, this.z, this.level);
    }
}
