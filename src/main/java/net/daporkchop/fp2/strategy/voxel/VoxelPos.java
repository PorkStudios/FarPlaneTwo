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

package net.daporkchop.fp2.strategy.voxel;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@ToString
public class VoxelPos implements IFarPos {
    protected final int x;
    protected final int y;
    protected final int z;
    protected final int level;

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
    public VoxelPos upTo(int targetLevel) {
        if (targetLevel == this.level) {
            return this;
        }
        checkArg(targetLevel > this.level, "targetLevel (%d) must be greater than current level (%d)", targetLevel, this.level);

        int shift = targetLevel - this.level;
        return new VoxelPos(this.x >> shift, this.y >> shift, this.z >> shift, targetLevel);
    }

    @Override
    public VoxelPos up() {
        return new VoxelPos(this.x >> 1, this.y >> 1, this.z >> 1, this.level + 1);
    }

    @Override
    public RenderMode mode() {
        return RenderMode.VOXEL;
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
}
