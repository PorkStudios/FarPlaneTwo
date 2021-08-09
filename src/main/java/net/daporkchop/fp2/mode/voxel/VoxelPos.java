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
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
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
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
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
    public void writePosNoLevel(@NonNull ByteBuf dst) {
        dst.writeInt(this.x).writeInt(this.y).writeInt(this.z);
    }

    @Override
    public int[] coordinates() {
        return new int[]{ this.x, this.y, this.z };
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
    public AxisAlignedBB bounds() {
        int shift = this.level + T_SHIFT;
        return new AxisAlignedBB(
                this.x << shift, this.y << shift, this.z << shift,
                (this.x + 1) << shift, (this.y + 1) << shift, (this.z + 1) << shift);
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
    public int localHash() {
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
