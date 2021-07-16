/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.mode.heightmap;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@ToString
public class HeightmapPos implements IFarPos {
    protected final int level;
    protected final int x;
    protected final int z;

    public HeightmapPos(@NonNull ByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public int blockX() {
        return this.x * T_VOXELS << this.level;
    }

    public int blockZ() {
        return this.z * T_VOXELS << this.level;
    }

    public int flooredChunkX() {
        return this.blockX() >> 4;
    }

    public int flooredChunkZ() {
        return this.blockZ() >> 4;
    }

    public ChunkPos flooredChunkPos() {
        return new ChunkPos(this.flooredChunkX(), this.flooredChunkZ());
    }

    @Override
    public void writePosNoLevel(@NonNull ByteBuf dst) {
        dst.writeInt(this.x).writeInt(this.z);
    }

    @Override
    public int[] coordinates() {
        return new int[]{ this.x, this.z };
    }

    @Override
    public HeightmapPos upTo(int targetLevel) {
        if (targetLevel == this.level) {
            return this;
        }
        checkArg(targetLevel > this.level, "targetLevel (%d) must be greater than current level (%d)", targetLevel, this.level);

        int shift = targetLevel - this.level;
        return new HeightmapPos(targetLevel, this.x >> shift, this.z >> shift);
    }

    @Override
    public HeightmapPos up() {
        return new HeightmapPos(this.level + 1, this.x >> 1, this.z >> 1);
    }

    @Override
    public boolean contains(@NonNull IFarPos posIn) {
        HeightmapPos pos = (HeightmapPos) posIn;
        int shift = this.level - pos.level;
        return shift > 0
               && (this.x << shift) >= pos.x && ((this.x + 1) << shift) <= pos.x
               && (this.z << shift) >= pos.z && ((this.z + 1) << shift) <= pos.z;
    }

    @Override
    public AxisAlignedBB bounds() {
        int shift = this.level + T_SHIFT;
        return new AxisAlignedBB(
                this.x << shift, Integer.MIN_VALUE, this.z << shift,
                (this.x + 1) << shift, Integer.MAX_VALUE, (this.z + 1) << shift);
    }

    @Override
    public int manhattanDistance(@NonNull IFarPos posIn) {
        HeightmapPos pos = (HeightmapPos) posIn;
        if (this.level == pos.level) {
            return abs(this.x - pos.x) + abs(this.z - pos.z);
        } else {
            int l0 = this.level;
            int l1 = pos.level;
            int s0 = max(l1 - l0, 0);
            int s1 = max(l0 - l1, 0);
            int s2 = max(s0, s1);
            return (abs((this.x >> s0) - (pos.x >> s1)) << s2)
                   + (abs((this.z >> s0) - (pos.z >> s1)) << s2);
        }
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

    @Override
    public int hashCode() {
        return this.x * 1317194159 + this.z * 1656858407 + this.level;
    }

    @Override
    public int compareTo(IFarPos posIn) {
        int d = Integer.compare(this.level, posIn.level());
        if (d == 0) {
            if (posIn instanceof HeightmapPos) {
                HeightmapPos pos = (HeightmapPos) posIn;
                if ((d = Integer.compare(this.x, pos.x)) == 0) {
                    d = Integer.compare(this.z, pos.z);
                }
            } else {
                return HeightmapPos.class.getName().compareTo(posIn.getClass().getName());
            }
        }
        return d;
    }
}
