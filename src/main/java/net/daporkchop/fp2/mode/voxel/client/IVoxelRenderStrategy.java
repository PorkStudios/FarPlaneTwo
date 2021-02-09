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

package net.daporkchop.fp2.mode.voxel.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.stream.Stream;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public interface IVoxelRenderStrategy extends IFarRenderStrategy<VoxelPos, VoxelPiece> {
    @Override
    default Stream<VoxelPos> bakeOutputs(@NonNull VoxelPos srcPos) {
        int x = srcPos.x();
        int y = srcPos.y();
        int z = srcPos.z();
        int level = srcPos.level();

        VoxelPos[] arr = new VoxelPos[8 + 27];
        int i = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 0; dz++) {
                    arr[i++] = new VoxelPos(x + dx, y + dy, z + dz, level);
                }
            }
        }
        for (int dx = -2; dx <= 0; dx++) {
            for (int dy = -2; dy <= 0; dy++) {
                for (int dz = -2; dz <= 0; dz++) {
                    arr[i++] = new VoxelPos((x << 1) + dx, (y << 1) + dy, (z << 1) + dz, level - 1);
                }
            }
        }
        return Stream.of(arr);
    }

    @Override
    default Stream<VoxelPos> bakeInputs(@NonNull VoxelPos dstPos) {
        int x = dstPos.x();
        int y = dstPos.y();
        int z = dstPos.z();
        int level = dstPos.level();

        return Stream.of(
                //same level
                dstPos, new VoxelPos(x, y, z + 1, level),
                new VoxelPos(x, y + 1, z, level), new VoxelPos(x, y + 1, z + 1, level),
                new VoxelPos(x + 1, y, z, level), new VoxelPos(x + 1, y, z + 1, level),
                new VoxelPos(x + 1, y + 1, z, level), new VoxelPos(x + 1, y + 1, z + 1, level),
                //above level
                new VoxelPos(x >> 1, y >> 1, z >> 1, level + 1), new VoxelPos(x >> 1, y >> 1, (z >> 1) + 1, level + 1),
                new VoxelPos(x >> 1, (y >> 1) + 1, z >> 1, level + 1), new VoxelPos(x >> 1, (y >> 1) + 1, (z >> 1) + 1, level + 1),
                new VoxelPos((x >> 1) + 1, y >> 1, z >> 1, level + 1), new VoxelPos((x >> 1) + 1, y >> 1, (z >> 1) + 1, level + 1),
                new VoxelPos((x >> 1) + 1, (y >> 1) + 1, z >> 1, level + 1), new VoxelPos((x >> 1) + 1, (y >> 1) + 1, (z >> 1) + 1, level + 1));
    }

    @Override
    default long posSize() {
        return 4L * INT_SIZE;
    }

    @Override
    default void writePos(@NonNull VoxelPos pos, long addr) {
        PUnsafe.putInt(addr + 0 * INT_SIZE, pos.x());
        PUnsafe.putInt(addr + 1 * INT_SIZE, pos.y());
        PUnsafe.putInt(addr + 2 * INT_SIZE, pos.z());
        PUnsafe.putInt(addr + 3 * INT_SIZE, pos.level());
    }

    @Override
    default VoxelPos readPos(long addr) {
        int x = PUnsafe.getInt(addr + 0 * INT_SIZE);
        int y = PUnsafe.getInt(addr + 1 * INT_SIZE);
        int z = PUnsafe.getInt(addr + 2 * INT_SIZE);
        int level = PUnsafe.getInt(addr + 3 * INT_SIZE);

        return new VoxelPos(x, y, z, level);
    }
}
