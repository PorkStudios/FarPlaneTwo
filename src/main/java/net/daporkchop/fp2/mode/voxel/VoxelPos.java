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

package net.daporkchop.fp2.mode.voxel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.util.math.MathUtil;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@ToString
public class VoxelPos implements IFarPos {
    protected final int level;
    protected final int x;
    protected final int y;
    protected final int z;

    public VoxelPos(@NonNull ByteBuf buf) {
        this.level = buf.readUnsignedByte();

        int interleavedHigh = buf.readInt();
        long interleavedLow = buf.readLong();
        this.x = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
        this.y = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
        this.z = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);
    }

    @Override
    public boolean isLevelValid() {
        return this.level >= 0 && this.level < V_MAX_LODS;
    }

    @Override
    public void writePos(@NonNull ByteBuf dst) {
        dst.writeByte(toByte(this.level))
                .writeInt(MathUtil.interleaveBitsHigh(this.x, this.y, this.z))
                .writeLong(MathUtil.interleaveBits(this.x, this.y, this.z));
    }

    @Override
    public byte[] toBytes() {
        byte[] arr = new byte[13];
        this.writePos(Unpooled.wrappedBuffer(arr).clear());
        return arr;
    }

    public int blockX() {
        return this.x * T_VOXELS << this.level;
    }

    public int blockY() {
        return this.y * T_VOXELS << this.level;
    }

    public int blockZ() {
        return this.z * T_VOXELS << this.level;
    }

    public int flooredChunkX() {
        return this.blockX() >> 4;
    }

    public int flooredChunkY() {
        return this.blockY() >> 4;
    }

    public int flooredChunkZ() {
        return this.blockZ() >> 4;
    }

    @Override
    public VoxelPos upTo(int targetLevel) {
        if (targetLevel == this.level) {
            return this;
        }
        checkArg(targetLevel > this.level, "targetLevel (%d) must be greater than current level (%d)", targetLevel, this.level);

        int shift = targetLevel - this.level;
        return new VoxelPos(targetLevel, this.x >> shift, this.y >> shift, this.z >> shift);
    }

    @Override
    public VoxelPos up() {
        return new VoxelPos(this.level + 1, this.x >> 1, this.y >> 1, this.z >> 1);
    }

    @Override
    public VoxelPos downTo(int targetLevel) {
        if (targetLevel == this.level) {
            return this;
        }
        checkArg(targetLevel < this.level, "targetLevel (%d) must be less than current level (%d)", targetLevel, this.level);

        int shift = this.level - targetLevel;
        return new VoxelPos(targetLevel, this.x << shift, this.y << shift, this.z << shift);
    }

    @Override
    public VoxelPos down() {
        return new VoxelPos(this.level - 1, this.x << 1, this.y << 1, this.z << 1);
    }

    @Override
    public boolean contains(@NonNull IFarPos posIn) {
        VoxelPos pos = (VoxelPos) posIn;
        int d = this.level - pos.level;
        return d > 0
               && (this.x << d) >= pos.x && ((this.x + 1) << d) <= pos.x
               && (this.y << d) >= pos.y && ((this.y + 1) << d) <= pos.y
               && (this.z << d) >= pos.z && ((this.z + 1) << d) <= pos.z;
    }

    @Override
    public boolean containedBy(@NonNull IntAxisAlignedBB coordLimits) {
        return coordLimits.contains(this.x, this.y, this.z);
    }

    @Override
    public boolean containedBy(@NonNull IntAxisAlignedBB[] coordLimits) {
        return coordLimits[this.level].contains(this.x, this.y, this.z);
    }

    @Override
    public Stream<VoxelPos> allPositionsInBB(int offsetMin, int offsetMax) {
        notNegative(offsetMin, "offsetMin");
        notNegative(offsetMax, "offsetMax");

        if (cb((long) offsetMin + offsetMax + 1L) < 256L) { //fast-track: fill an array and create a simple stream over that
            VoxelPos[] arr = new VoxelPos[cb(offsetMin + offsetMax + 1)];
            for (int i = 0, dx = -offsetMin; dx <= offsetMax; dx++) {
                for (int dy = -offsetMin; dy <= offsetMax; dy++) {
                    for (int dz = -offsetMin; dz <= offsetMax; dz++) {
                        arr[i++] = new VoxelPos(this.level, this.x + dx, this.y + dy, this.z + dz);
                    }
                }
            }
            return Stream.of(arr);
        } else { //slower fallback: dynamically computed stream
            return IntStream.rangeClosed(this.x - offsetMin, this.x + offsetMax)
                    .mapToObj(x -> IntStream.rangeClosed(this.y - offsetMin, this.y + offsetMax)
                            .mapToObj(y -> IntStream.rangeClosed(this.z - offsetMin, this.z + offsetMax)
                                    .mapToObj(z -> new VoxelPos(this.level, x, y, z)))
                            .flatMap(Function.identity()))
                    .flatMap(Function.identity());
        }
    }

    @Override
    public int manhattanDistance(@NonNull IFarPos posIn) {
        VoxelPos pos = (VoxelPos) posIn;
        if (this.level == pos.level) {
            return abs(this.x - pos.x) + abs(this.y - pos.y) + abs(this.z - pos.z);
        } else {
            int l0 = this.level;
            int l1 = pos.level;
            int s0 = max(l1 - l0, 0);
            int s1 = max(l0 - l1, 0);
            int s2 = max(s0, s1);
            return (abs((this.x >> s0) - (pos.x >> s1)) << s2)
                   + (abs((this.y >> s0) - (pos.y >> s1)) << s2)
                   + (abs((this.z >> s0) - (pos.z >> s1)) << s2);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof VoxelPos) {
            VoxelPos pos = (VoxelPos) obj;
            return this.x == pos.x && this.y == pos.y && this.z == pos.z && this.level == pos.level;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.x * 1317194159 + this.y * 1964379643 + this.z * 1656858407 + this.level;
    }

    @Override
    public long localHash() {
        return interleaveBits(this.x, this.y, this.z);
    }

    @Override
    public int compareTo(IFarPos posIn) {
        int d = Integer.compare(this.level, posIn.level());
        if (d == 0) {
            if (posIn instanceof VoxelPos) {
                VoxelPos pos = (VoxelPos) posIn;
                if ((d = Integer.compare(this.x, pos.x)) == 0 && (d = Integer.compare(this.z, pos.z)) == 0) {
                    d = Integer.compare(this.y, pos.y);
                }
            } else {
                return VoxelPos.class.getName().compareTo(posIn.getClass().getName());
            }
        }
        return d;
    }
}
