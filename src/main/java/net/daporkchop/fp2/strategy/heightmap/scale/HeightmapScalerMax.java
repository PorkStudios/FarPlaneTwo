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

package net.daporkchop.fp2.strategy.heightmap.scale;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;

import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Scales heightmaps based on the highest sample.
 *
 * @author DaPorkchop_
 */
public class HeightmapScalerMax implements HeightmapScaler {
    @Override
    public Stream<HeightmapPos> outputs(@NonNull HeightmapPos srcPos) {
        return Stream.of(srcPos.up());
    }

    @Override
    public Stream<HeightmapPos> inputs(@NonNull HeightmapPos dstPos) {
        checkArg(dstPos.level() > 0, "cannot generate inputs for level 0!");
        return Stream.of(
                new HeightmapPos(dstPos.x() << 1, dstPos.z() << 1, dstPos.level() - 1),
                new HeightmapPos(dstPos.x() << 1, (dstPos.z() << 1) + 1, dstPos.level() - 1),
                new HeightmapPos((dstPos.x() << 1) + 1, dstPos.z() << 1, dstPos.level() - 1),
                new HeightmapPos((dstPos.x() << 1) + 1, (dstPos.z() << 1) + 1, dstPos.level() - 1)
        );
    }

    @Override
    public void scale(@NonNull HeightmapPiece[] srcs, @NonNull HeightmapPiece dst) {
        for (int subX = 0; subX < 2; subX++) {
            for (int subZ = 0; subZ < 2; subZ++) {
                HeightmapPiece src = srcs[subX * 2 + subZ];
                int baseX = subX * (HEIGHTMAP_VOXELS >> 1);
                int baseZ = subZ * (HEIGHTMAP_VOXELS >> 1);

                for (int x = 0; x < HEIGHTMAP_VOXELS; x += 2) {
                    for (int z = 0; z < HEIGHTMAP_VOXELS; z += 2) {
                        int dstX = baseX + (x >> 1);
                        int dstZ = baseZ + (z >> 1);

                        this.scaleSample(src, x, z, dst, dstX, dstZ);
                    }
                }
            }
        }
    }

    protected void scaleSample(HeightmapPiece src, int srcX, int srcZ, HeightmapPiece dst, int dstX, int dstZ) {
        int height0 = src.height(srcX, srcZ);
        int height1 = src.height(srcX, srcZ + 1);
        int height2 = src.height(srcX + 1, srcZ);
        int height3 = src.height(srcX + 1, srcZ + 1);
        int avgHeight = (height0 + height1 + height2 + height3) >> 2;

        int d0 = abs(height0 - avgHeight);
        int d1 = abs(height1 - avgHeight);
        int d2 = abs(height2 - avgHeight);
        int d3 = abs(height3 - avgHeight);

        int height;
        int block;
        int light;
        int biome;
        if (d0 > d1 && d0 > d2 && d0 > d3) {
            height = height0;
            block = src.block(srcX, srcZ);
            light = src.light(srcX, srcZ);
            biome = src.biome(srcX, srcZ);
        } else if (d1 > d0 && d1 > d2 && d1 > d3) {
            height = height1;
            block = src.block(srcX, srcZ + 1);
            light = src.light(srcX, srcZ + 1);
            biome = src.biome(srcX, srcZ + 1);
        } else if (d2 > d0 && d2 > d1 && d2 > d3) {
            height = height2;
            block = src.block(srcX + 1, srcZ);
            light = src.light(srcX + 1, srcZ);
            biome = src.biome(srcX + 1, srcZ);
        } else {
            height = height3;
            block = src.block(srcX + 1, srcZ + 1);
            light = src.light(srcX + 1, srcZ + 1);
            biome = src.biome(srcX + 1, srcZ + 1);
        }

        dst.set(dstX, dstZ, height, block, light);
        dst.setBiome(dstX, dstZ, biome);
        if (dstX == 0) {
            dst.setBiome(-1, dstZ, biome);
        } else if (dstZ == HEIGHTMAP_VOXELS - 1) {
            dst.setBiome(HEIGHTMAP_VOXELS, dstZ, biome);
        }
        if (dstZ == 0) {
            dst.setBiome(dstX, -1, biome);
        } else if (dstZ == HEIGHTMAP_VOXELS - 1) {
            dst.setBiome(dstX, HEIGHTMAP_VOXELS, biome);
        }
    }
}
