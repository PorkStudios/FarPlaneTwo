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

package net.daporkchop.fp2.mode.heightmap.server.scale;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.server.scale.IFarPieceScaler;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapSample;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;

import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Scales heightmap pieces by copying the sample with the greatest height deviation in each 2x2 square of high-detail samples.
 * <p>
 * This probably isn't the absolute best solution, but it's pretty fast and does a good job of preserving detail in bumpy regions.
 *
 * @author DaPorkchop_
 */
public class HeightmapPieceScalerMax implements IFarPieceScaler<HeightmapPos, HeightmapPiece> {
    @Override
    public Stream<HeightmapPos> outputs(@NonNull HeightmapPos srcPos) {
        return Stream.of(srcPos.up());
    }

    @Override
    public Stream<HeightmapPos> inputs(@NonNull HeightmapPos dstPos) {
        checkArg(dstPos.level() > 0, "cannot generate inputs for level 0!");

        int x = dstPos.x() << 1;
        int z = dstPos.z() << 1;
        int level = dstPos.level();

        return Stream.of(
                new HeightmapPos(x, z, level - 1),
                new HeightmapPos(x, z + 1, level - 1),
                new HeightmapPos(x + 1, z, level - 1),
                new HeightmapPos(x + 1, z + 1, level - 1));
    }

    @Override
    public long scale(@NonNull HeightmapPiece[] srcs, @NonNull HeightmapPiece dst) {
        HeightmapSample data = new HeightmapSample();

        for (int subX = 0; subX < 2; subX++) {
            for (int subZ = 0; subZ < 2; subZ++) {
                HeightmapPiece src = srcs[subX * 2 + subZ];
                int baseX = subX * (T_VOXELS >> 1);
                int baseZ = subZ * (T_VOXELS >> 1);

                for (int x = 0; x < T_VOXELS; x += 2) {
                    for (int z = 0; z < T_VOXELS; z += 2) {
                        int dstX = baseX + (x >> 1);
                        int dstZ = baseZ + (z >> 1);

                        this.scaleSample(src, x, z, data);
                        dst.set(dstX, dstZ, data);
                    }
                }
            }
        }
        return 0L;
    }

    protected void scaleSample(HeightmapPiece src, int srcX, int srcZ, HeightmapSample data) {
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
            src.get(srcX, srcZ, data);
        } else if (d1 > d0 && d1 > d2 && d1 > d3) {
            src.get(srcX, srcZ + 1, data);
        } else if (d2 > d0 && d2 > d1 && d2 > d3) {
            src.get(srcX + 1, srcZ, data);
        } else {
            src.get(srcX + 1, srcZ + 1, data);
        }
    }
}
