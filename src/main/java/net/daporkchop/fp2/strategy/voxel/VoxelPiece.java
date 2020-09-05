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
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.base.AbstractFarPiece;
import net.daporkchop.fp2.util.Constants;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class VoxelPiece extends AbstractFarPiece<VoxelPos> {
    public static final int ENTRY_SIZE = 1;

    public static final int ENTRY_COUNT = T_VOXELS * T_VOXELS * T_VOXELS;

    public static final int TOTAL_SIZE = ENTRY_COUNT * ENTRY_SIZE;

    private static int index(int x, int y, int z) {
        checkArg(x >= 0 && x < T_VOXELS && y >= 0 && y < T_VOXELS && z >= 0 && z < T_VOXELS, "coordinates out of bounds (x=%d, y=%d, z=%d)", x, y, z);
        return ((x * T_VOXELS + y) * T_VOXELS + z) * ENTRY_SIZE;
    }

    protected final IntBuffer data = Constants.createIntBuffer(TOTAL_SIZE);

    public VoxelPiece(@NonNull VoxelPos pos) {
        super(pos, RenderMode.VOXEL);
    }

    public VoxelPiece(@NonNull ByteBuf src) {
        super(src, RenderMode.VOXEL);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            this.data.put(i, src.readInt());
        }
    }

    @Override
    protected void writeBody(@NonNull ByteBuf dst) {
        for (int i = 0; i < TOTAL_SIZE; i++) {
            dst.writeInt(this.data.get(i));
        }
    }

    public double dx(int x, int y, int z) {
        return (this.data.get(index(x, y, z)) >>> 24) / 255.0d;
    }

    public double dy(int x, int y, int z) {
        return ((this.data.get(index(x, y, z)) >>> 16) & 0xFF) / 255.0d;
    }

    public double dz(int x, int y, int z) {
        return ((this.data.get(index(x, y, z)) >>> 8) & 0xFF) / 255.0d;
    }

    public int edgeMask(int x, int y, int z) {
        return this.data.get(index(x, y, z)) & 0xFF;
    }

    public VoxelPiece set(int x, int y, int z, double dx, double dy, double dz, int edgeMask) {
        int base = index(x, y, z);

        edgeMask = ((edgeMask & 0x800) >> 9) | ((edgeMask & 0x80) >> 6) | ((edgeMask & 0x8) >> 3);

        dx = clamp(dx, 0., 1.) * 255.0d;
        dy = clamp(dy, 0., 1.) * 255.0d;
        dz = clamp(dz, 0., 1.) * 255.0d;

        this.data.put(base, (floorI(dx) << 24) | (floorI(dy) << 16) | (floorI(dz) << 8) | edgeMask);
        this.markDirty();
        return this;
    }
}
