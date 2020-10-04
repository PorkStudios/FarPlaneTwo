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

package net.daporkchop.fp2.mode.voxel.server.scale;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.fp2.mode.api.server.scale.IFarScaler;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.VoxelPos;

import java.util.stream.Stream;

import static net.daporkchop.fp2.mode.voxel.server.gen.VoxelGeneratorConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
//TODO: figure out whether or not this is actually correct
public class VoxelScalerAvg implements IFarScaler<VoxelPos, VoxelPiece> {
    @Override
    public Stream<VoxelPos> outputs(@NonNull VoxelPos srcPos) {
        return Stream.of(srcPos.up());
    }

    @Override
    public Stream<VoxelPos> inputs(@NonNull VoxelPos dstPos) {
        checkArg(dstPos.level() > 0, "cannot generate inputs for level 0!");

        int x = dstPos.x() << 1;
        int y = dstPos.y() << 1;
        int z = dstPos.z() << 1;
        int level = dstPos.level();

        return Stream.of(
                new VoxelPos(x, y, z, level - 1),
                new VoxelPos(x, y, z + 1, level - 1),
                new VoxelPos(x, y + 1, z, level - 1),
                new VoxelPos(x, y + 1, z + 1, level - 1),
                new VoxelPos(x + 1, y, z, level - 1),
                new VoxelPos(x + 1, y, z + 1, level - 1),
                new VoxelPos(x + 1, y + 1, z, level - 1),
                new VoxelPos(x + 1, y + 1, z + 1, level - 1));
    }

    @Override
    public void scale(@NonNull VoxelPiece[] srcs, @NonNull IFarPieceBuilder dst) {
        VoxelData[] datas = new VoxelData[9];
        for (int i = 0; i < 9; i++) {
            datas[i] = new VoxelData();
        }

        for (int subX = 0; subX < 2; subX++) {
            for (int subY = 0; subY < 2; subY++) {
                for (int subZ = 0; subZ < 2; subZ++) {
                    VoxelPiece src = srcs[(subX * 2 + subY) * 2 + subZ];
                    int baseX = subX * (T_VOXELS >> 1);
                    int baseY = subY * (T_VOXELS >> 1);
                    int baseZ = subZ * (T_VOXELS >> 1);

                    for (int x = 0; x < T_VOXELS; x += 2) {
                        for (int y = 0; y < T_VOXELS; y += 2) {
                            for (int z = 0; z < T_VOXELS; z += 2) {
                                int dstX = baseX + (x >> 1);
                                int dstY = baseY + (y >> 1);
                                int dstZ = baseZ + (z >> 1);

                                this.scaleSample(src, x, y, z, dst, dstX, dstY, dstZ, datas);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void scaleSample(VoxelPiece src, int srcX, int srcY, int srcZ, VoxelPiece dst, int dstX, int dstY, int dstZ, VoxelData[] datas) {
        int validFlags = 0;
        int validCount = 0;
        for (int i = 0; i < 8; i++) { //fetch data from all 8 contributing voxels
            if (src.get(srcX + ((i >> 2) & 1), srcY + ((i >> 1) & 1), srcZ + (i & 1), datas[i])) {
                validFlags |= 1 << i;
                validCount++;
            }
        }

        if (validCount == 0) { //none of the voxels contain any data at all
            return;
        }

        VoxelData data = datas[8].reset();

        double x = 0.0d;
        double y = 0.0d;
        double z = 0.0d;
        for (int i = 0; i < 8; i++) { //compute average voxel position
            if ((validFlags & (1 << i)) != 0) {
                x += datas[i].x + ((i >> 2) & 1);
                y += datas[i].y + ((i >> 1) & 1);
                z += datas[i].z + (i & 1);
            }
        }
        x /= validCount;
        y /= validCount;
        z /= validCount;
        data.x = x * 0.5d;
        data.y = y * 0.5d;
        data.z = z * 0.5d;

        int edges = 0;
        for (int edge = 0; edge < 3; edge++) { //compute connection edges
            for (int base = CONNECTION_SUB_NEIGHBOR_COUNT * edge, i = 0; i < CONNECTION_SUB_NEIGHBOR_COUNT; i++) {
                int j = CONNECTION_SUB_NEIGHBORS[base + i];
                if ((validFlags & (1 << j)) != 0) {
                    edges |= datas[j].edges & (1 << edge);
                }
            }
        }
        data.edges = ((edges & 4) << 9) | ((edges & 2) << 6) | ((edges & 1) << 3);

        //compute appearance data
        //TODO: i need a better algorithm for selecting which voxel to use
        int j = (floorI(x) << 2) | (floorI(y) << 1) | floorI(z);
        if ((validFlags & (1 << j)) != 0)   {
            data.state = datas[j].state;
            data.biome = datas[j].biome;
            data.light = datas[j].light;
        } else {
            //fall back to using anything
            for (int i = 0; i < 8; i++) {
                if ((validFlags & (1 << i)) != 0) {
                    data.state = datas[i].state;
                    data.biome = datas[i].biome;
                    data.light = datas[i].light;
                    break;
                }
            }
        }

        dst.set(dstX, dstY, dstZ, data);
    }
}
