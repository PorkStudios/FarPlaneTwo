/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.heightmap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.util.math.MathUtil;
import net.daporkchop.lib.math.vector.Vec2i;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;
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

    @Deprecated
    public HeightmapPos(@NonNull ByteBuf buf) {
        this.level = buf.readUnsignedByte();

        long interleaved = buf.readLong();
        this.x = MathUtil.uninterleave2_0(interleaved);
        this.z = MathUtil.uninterleave2_1(interleaved);
    }

    @Override
    public boolean isLevelValid() {
        return this.level >= 0 && this.level < HMAX_LODS;
    }

    @Override
    public void writePos(@NonNull ByteBuf dst) {
        dst.writeByte(toByte(this.level)).writeLong(MathUtil.interleaveBits(this.x, this.z));
    }

    @Override
    public byte[] toBytes() {
        byte[] arr = new byte[9];
        this.writePos(Unpooled.wrappedBuffer(arr).clear());
        return arr;
    }

    public int blockX() {
        return this.x * HT_VOXELS << this.level;
    }

    public int blockZ() {
        return this.z * HT_VOXELS << this.level;
    }

    public int sideLength() {
        return HT_VOXELS << this.level;
    }

    public int flooredChunkX() {
        return this.blockX() >> 4;
    }

    public int flooredChunkZ() {
        return this.blockZ() >> 4;
    }

    public Vec2i flooredChunkPos() {
        return Vec2i.of(this.flooredChunkX(), this.flooredChunkZ());
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
    public HeightmapPos downTo(int targetLevel) {
        if (targetLevel == this.level) {
            return this;
        }
        checkArg(targetLevel < this.level, "targetLevel (%d) must be less than current level (%d)", targetLevel, this.level);

        int shift = this.level - targetLevel;
        return new HeightmapPos(targetLevel, this.x << shift, this.z << shift);
    }

    @Override
    public HeightmapPos down() {
        return new HeightmapPos(this.level - 1, this.x << 1, this.z << 1);
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
    public Stream<HeightmapPos> allPositionsInBB(int offsetMin, int offsetMax) {
        notNegative(offsetMin, "offsetMin");
        notNegative(offsetMax, "offsetMax");

        if (sq((long) offsetMin + offsetMax + 1L) < 256L) { //fast-track: fill an array and create a simple stream over that
            HeightmapPos[] arr = new HeightmapPos[sq(offsetMin + offsetMax + 1)];
            for (int i = 0, dx = -offsetMin; dx <= offsetMax; dx++) {
                for (int dz = -offsetMin; dz <= offsetMax; dz++) {
                    arr[i++] = new HeightmapPos(this.level, this.x + dx, this.z + dz);
                }
            }
            return Stream.of(arr);
        } else { //slower fallback: dynamically computed stream
            return IntStream.rangeClosed(this.x - offsetMin, this.x + offsetMax)
                    .mapToObj(x -> IntStream.rangeClosed(this.z - offsetMin, this.z + offsetMax)
                            .mapToObj(z -> new HeightmapPos(this.level, x, z)))
                    .flatMap(Function.identity());
        }
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
    public long localHash() {
        return interleaveBits(this.x, this.z);
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
