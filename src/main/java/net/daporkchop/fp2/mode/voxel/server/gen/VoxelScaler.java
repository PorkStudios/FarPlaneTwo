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

package net.daporkchop.fp2.mode.voxel.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelData;
import net.daporkchop.fp2.mode.voxel.piece.VoxelDataSample;

import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class VoxelScaler implements IFarScaler<VoxelPos, VoxelData> {
    @Override
    public Stream<VoxelPos> outputs(@NonNull VoxelPos srcPos) {
        return Stream.of(srcPos.up());
    }

    @Override
    public Stream<VoxelPos> inputs(@NonNull VoxelPos dstPos) {
        int x = dstPos.x() << 1;
        int y = dstPos.y() << 1;
        int z = dstPos.z() << 1;
        int level = dstPos.level() - 1;
        VoxelPos[] arr = new VoxelPos[8];
        for (int i = 0, dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    arr[i++] = new VoxelPos(x + dx, y + dy, z + dz, level);
                }
            }
        }
        return Stream.of(arr);
    }

    @Override
    public void scale(@NonNull VoxelData[] srcs, @NonNull VoxelData dst) {
        VoxelDataSample in = new VoxelDataSample();
        VoxelDataSample out = new VoxelDataSample();
        for (int srcX = 0; srcX < 2; srcX++) {
            for (int srcY = 0; srcY < 2; srcY++) {
                for (int srcZ = 0; srcZ < 2; srcZ++) {
                    VoxelData src = srcs[(srcX * 2 + srcY) * 2 + srcZ];
                    for (int x = 0; x < T_VOXELS; x += 2) {
                        for (int y = 0; y < T_VOXELS; y += 2) {
                            for (int z = 0; z < T_VOXELS; z+= 2) {
                                out.reset();
                                for (int subX = 0; subX < 2; subX++) {
                                    for (int subY = 0; subY < 2; subY++) {
                                        for (int subZ = 0; subZ < 2; subZ++) {
                                            src.get(x + subX, y + subY, z + subZ, in);
                                            out.density0 += in.density0;
                                            out.density1 += in.density1;
                                        }
                                    }
                                }
                                out.density0 >>= 3;
                                out.density1 >>= 3;
                                out.state = in.state;
                                out.biome = in.biome;
                                out.light = in.light;
                                dst.set((srcX << (T_SHIFT - 1)) + (x >> 1), (srcY << (T_SHIFT - 1)) + (y >> 1), (srcZ << (T_SHIFT - 1)) + (z >> 1), out);
                            }
                        }
                    }
                }
            }
        }
    }
}
