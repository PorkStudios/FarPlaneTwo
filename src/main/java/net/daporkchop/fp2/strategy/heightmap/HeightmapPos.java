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
import lombok.ToString;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.math.PMath;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@ToString
public class HeightmapPos implements IFarPos {
    protected final int x;
    protected final int z;
    protected final int level;

    public HeightmapPos(@NonNull ByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt());
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
    public boolean contains(@NonNull IFarPos posIn) {
        HeightmapPos pos = (HeightmapPos) posIn;
        /*if (this.level > pos.level) {
            int ownMinBlockX = this.x * T_VOXELS << this.level;
            int ownMinBlockZ = this.z * T_VOXELS << this.level;
            int ownMaxBlockX = ownMinBlockX + (1 << this.level);
            int ownMaxBlockZ = ownMinBlockZ + (1 << this.level);
            int posMinBlockX = pos.x * T_VOXELS << pos.level;
            int posMinBlockZ = pos.z * T_VOXELS << pos.level;
            int posMaxBlockX = posMinBlockX + (1 << pos.level);
            int posMaxBlockZ = posMinBlockZ + (1 << pos.level);
            return posMinBlockX >= ownMinBlockX && posMinBlockZ >= ownMinBlockZ
                    && posMaxBlockX <= ownMaxBlockX && posMaxBlockZ <= ownMaxBlockZ;
        }*/
        int d = this.level - pos.level;
        return d > 0
               && (this.x << d) >= pos.x && ((this.x + 1) << d) <= pos.x
               && (this.z << d) >= pos.z && ((this.z + 1) << d) <= pos.z;
    }
}
