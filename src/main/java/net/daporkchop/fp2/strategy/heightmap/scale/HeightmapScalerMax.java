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

import static java.lang.Math.*;
import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;

/**
 * Scales heightmaps based on the highest sample.
 *
 * @author DaPorkchop_
 */
public class HeightmapScalerMax implements HeightmapScaler {
    @Override
    public void scale(@NonNull HeightmapPiece[] srcs, @NonNull HeightmapPiece dst) {
        for (int subX = 0; subX < 2; subX++) {
            for (int subZ = 0; subZ < 2; subZ++) {
                HeightmapPiece src = srcs[subX * 2 + subZ];

                int baseX = subX * (HEIGHTMAP_VOXELS >> 1);
                int baseZ = subZ * (HEIGHTMAP_VOXELS >> 1);

                for (int x = 0; x < HEIGHTMAP_VOXELS; x += 2) {
                    for (int z = 0; z < HEIGHTMAP_VOXELS; z += 2) {
                        this.scaleSample(src, x, z, dst, baseX + (x >> 1), baseZ + (z >> 1));
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

        if (d0 > d1 && d0 > d2 && d0 > d3) {
            dst.set(dstX, dstZ, height0, src.block(srcX, srcZ), src.light(srcX, srcZ));
        } else if (d1 > d0 && d1 > d2 && d1 > d3) {
            dst.set(dstX, dstZ, height1, src.block(srcX, srcZ + 1), src.light(srcX, srcZ + 1));
        } else if (d2 > d0 && d2 > d1 && d2 > d3) {
            dst.set(dstX, dstZ, height2, src.block(srcX + 1, srcZ), src.light(srcX + 1, srcZ));
        } else {
            dst.set(dstX, dstZ, height3, src.block(srcX + 1, srcZ + 1), src.light(srcX + 1, srcZ + 1));
        }
    }
}
