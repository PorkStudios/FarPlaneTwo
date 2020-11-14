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
import net.daporkchop.fp2.mode.api.server.gen.IFarAssembler;
import net.daporkchop.fp2.mode.voxel.piece.VoxelDataSample;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.piece.VoxelData;
import net.daporkchop.fp2.mode.voxel.piece.VoxelSample;

import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class VoxelAssembler extends AbstractVoxelGenerator<VoxelData> implements IFarAssembler<VoxelData, VoxelPiece> {
    @Override
    public long assemble(@NonNull VoxelData data, @NonNull VoxelPiece piece) {
        double[][] dMap = DMAP_CACHE.get();
        VoxelDataSample sample = new VoxelDataSample();
        for (int x = DMAP_MIN; x < DMAP_MAX; x++) {
            for (int y = DMAP_MIN; y < DMAP_MAX; y++) {
                for (int z = DMAP_MIN; z < DMAP_MAX; z++) {
                    data.get(clamp(x, 0, T_VOXELS - 1), clamp(y, 0, T_VOXELS - 1), clamp(z, 0, T_VOXELS - 1), sample);
                    int di = densityIndex(x, y, z);
                    dMap[0][di] = sample.density0 - POS_ONE;
                    dMap[1][di] = sample.density1 - POS_ONE;
                }
            }
        }
        this.buildMesh(0, 0, 0, 0, piece, dMap, data);
        return 0L;
    }

    @Override
    protected int getFaceState(int blockX, int blockY, int blockZ, int level, double nx, double ny, double nz, int edge, int layer, VoxelData param) {
        VoxelDataSample sample = new VoxelDataSample();
        param.get(blockX, blockY, blockZ, sample);
        if (sample.state == 0) {
            for (int dx = -1; dx <= 1 && sample.state == 0; dx++) {
                for (int dy = -1; dy <= 1 && sample.state == 0; dy++) {
                    for (int dz = -1; dz <= 1 && sample.state == 0; dz++) {
                        param.get(clamp(blockX + dx, 0, T_VOXELS - 1), clamp(blockY + dy, 0, T_VOXELS - 1), clamp(blockZ + dz, 0, T_VOXELS - 1), sample);
                    }
                }
            }
        }
        return sample.state;
    }

    @Override
    protected void populateVoxelBlockData(int blockX, int blockY, int blockZ, int level, double nx, double ny, double nz, VoxelSample sample, VoxelData param) {
        VoxelDataSample sample0 = new VoxelDataSample();
        param.get((blockX >> level) & T_MASK, (blockY >> level) & T_MASK, (blockZ >> level) & T_MASK, sample0);
        sample.biome = sample0.biome;
        sample.light = sample0.light;
    }
}
