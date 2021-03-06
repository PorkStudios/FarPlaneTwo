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

package net.daporkchop.fp2.mode.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface IHeightmapRenderStrategy extends IFarRenderStrategy<HeightmapPos, HeightmapTile> {
    @Override
    default Stream<HeightmapPos> bakeOutputs(@NonNull HeightmapPos srcPos) {
        int x = srcPos.x();
        int z = srcPos.z();
        int level = srcPos.level();

        HeightmapPos[] arr = new HeightmapPos[4 + 9];
        int i = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                arr[i++] = new HeightmapPos(level, x + dx, z + dz);
            }
        }
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                arr[i++] = new HeightmapPos(level - 1, (x << 1) + dx, (z << 1) + dz);
            }
        }
        return Arrays.stream(arr, 0, i);
    }

    @Override
    default Stream<HeightmapPos> bakeInputs(@NonNull HeightmapPos dstPos) {
        int x = dstPos.x();
        int z = dstPos.z();
        int level = dstPos.level();

        return Stream.of(
                //same level
                dstPos,
                new HeightmapPos(level, x, z + 1),
                new HeightmapPos(level, x + 1, z),
                new HeightmapPos(level, x + 1, z + 1),
                //above level
                new HeightmapPos(level + 1, x >> 1, z >> 1),
                new HeightmapPos(level + 1, x >> 1, (z >> 1) + 1),
                new HeightmapPos(level + 1, (x >> 1) + 1, z >> 1),
                new HeightmapPos(level + 1, (x >> 1) + 1, (z >> 1) + 1));
    }
}
