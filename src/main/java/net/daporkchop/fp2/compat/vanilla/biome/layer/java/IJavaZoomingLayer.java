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

package net.daporkchop.fp2.compat.vanilla.biome.layer.java;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IZoomingLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * Extension of {@link IZoomingLayer} for Java implementations.
 *
 * @author DaPorkchop_
 */
public interface IJavaZoomingLayer extends IZoomingLayer {
    static int[] offsetsStandard(int inSizeX, int inSizeZ) {
        return new int[]{
                0 * inSizeZ + 0,
                1 * inSizeZ + 0,
                0 * inSizeZ + 1,
                1 * inSizeZ + 1
        };
    }

    static int[] offsetsInverted(int inSizeX, int inSizeZ) {
        return new int[]{
                0 * inSizeZ + 0,
                0 * inSizeZ + 1,
                1 * inSizeZ + 0,
                1 * inSizeZ + 1
        };
    }

    /**
     * @return the neighbor value offsets
     */
    default int[] offsets(int inSizeX, int inSizeZ) {
        return offsetsStandard(inSizeX, inSizeZ);
    }

    void zoomTile0(int x, int z, @NonNull int[] v, @NonNull int[] out, int off, int size);

    @Override
    default void getGrid0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in) {
        if (IZoomingLayer.isAligned(this.shift(), x, z, sizeX, sizeZ)) {
            this.getGrid0_aligned(alloc, x, z, sizeX, sizeZ, out, in);
        } else {
            this.getGrid0_unaligned(alloc, x, z, sizeX, sizeZ, out, in);
        }
    }

    default void getGrid0_aligned(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in) {
        final int shift = this.shift();

        final int inX = x >> shift;
        final int inZ = z >> shift;
        final int inSizeX = (sizeX >> shift) + 1;
        final int inSizeZ = (sizeZ >> shift) + 1;

        int[] offsets = this.offsets(inSizeX, inSizeZ);
        int[] v = new int[4];

        for (int tileX = 0; tileX < inSizeX - 1; tileX++) {
            for (int tileZ = 0; tileZ < inSizeZ - 1; tileZ++) {
                final int inIdx = tileX * inSizeZ + tileZ;
                for (int i = 0; i < 4; i++) {
                    v[i] = in[offsets[i] + inIdx];
                }

                this.zoomTile0((inX + tileX) << shift, (inZ + tileZ) << shift, v, out, (tileX << shift) * sizeZ + (tileZ << shift), sizeZ);
            }
        }
    }

    default void getGrid0_unaligned(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in) {
        final int shift = this.shift();
        final int tileSize = 1 << shift;
        final int mask = tileSize - 1;

        final int inX = x >> shift;
        final int inZ = z >> shift;
        final int inSizeX = (sizeX >> shift) + 2;
        final int inSizeZ = (sizeZ >> shift) + 2;
        final int tempSizeX = (inSizeX - 1) << shift;
        final int tempSizeZ = (inSizeZ - 1) << shift;

        int[] offsets = this.offsets(inSizeX, inSizeZ);
        int[] v = new int[4];

        int[] temp = alloc.atLeast(tempSizeX * tempSizeZ);
        try {
            //zoom individual tiles
            for (int tileX = 0; tileX < inSizeX - 1; tileX++) {
                for (int tileZ = 0; tileZ < inSizeZ - 1; tileZ++) {
                    final int inIdx = tileX * inSizeZ + tileZ;
                    for (int i = 0; i < 4; i++) {
                        v[i] = in[offsets[i] + inIdx];
                    }

                    this.zoomTile0((inX + tileX) << shift, (inZ + tileZ) << shift, v, temp, (tileX << shift) * tempSizeZ + (tileZ << shift), tempSizeZ);
                }
            }

            //remove padding from output tiles
            for (int dx = 0; dx < sizeX; dx++) {
                System.arraycopy(temp, (dx + (x & mask)) * tempSizeZ + (z & mask), out, dx * sizeZ, sizeZ);
            }
        } finally {
            alloc.release(temp);
        }
    }

    @Override
    default void multiGetGridsCombined0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int shift = this.shift();
        final int tileSize = 1 << shift;
        final int mask = tileSize - 1;

        final int inSize = ((((dist >> depth) + 1) * count) >> shift) + 2;
        final int inTileSize = (size >> shift) + 2;
        final int tempTileSize = (inTileSize - 1) << shift;

        int[] offsets = this.offsets(inSize, inSize);
        int[] v = new int[4];

        int[] temp = alloc.atLeast(count * count * tempTileSize * tempTileSize);
        try {
            //zoom individual tiles
            for (int tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int gridZ = 0; gridZ < count; gridZ++, tempIdx += tempTileSize * tempTileSize) {
                    final int inX = mulAddShift(gridX, dist, x, depth) >> shift;
                    final int inZ = mulAddShift(gridZ, dist, z, depth) >> shift;
                    final int offsetX = mulAddShift(gridX, dist, gridX & mask, depth) >> shift;
                    final int offsetZ = mulAddShift(gridZ, dist, gridZ & mask, depth) >> shift;

                    for (int tileX = 0; tileX < inTileSize - 1; tileX++) {
                        for (int tileZ = 0; tileZ < inTileSize - 1; tileZ++) {
                            final int inIdx = (offsetX + tileX) * inSize + (offsetZ + tileZ);
                            for (int i = 0; i < 4; i++) {
                                v[i] = in[offsets[i] + inIdx];
                            }

                            this.zoomTile0((inX + tileX) << shift, (inZ + tileZ) << shift, v, temp, tempIdx + (tileX << shift) * tempTileSize + (tileZ << shift), tempTileSize);
                        }
                    }
                }
            }

            //remove padding from output tiles
            for (int outIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int gridZ = 0; gridZ < count; gridZ++, outIdx += size * size, tempIdx += tempTileSize * tempTileSize) {
                    final int realX = mulAddShift(gridX, dist, x, depth);
                    final int realZ = mulAddShift(gridZ, dist, z, depth);

                    for (int dx = 0; dx < size; dx++) {
                        System.arraycopy(temp, tempIdx + (dx + (realX & mask)) * tempTileSize + (realZ & mask), out, outIdx + dx * size, size);
                    }
                }
            }
        } finally {
            alloc.release(temp);
        }
    }

    @Override
    default void multiGetGridsIndividual0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int shift = this.shift();
        final int tileSize = 1 << shift;
        final int mask = tileSize - 1;

        final int inSize = (size >> shift) + 2;
        final int tempSize = (inSize - 1) << shift;

        int[] offsets = this.offsets(inSize, inSize);
        int[] v = new int[4];

        int[] temp = alloc.atLeast(count * count * tempSize * tempSize);
        try {
            //zoom individual tiles
            for (int inIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int gridZ = 0; gridZ < count; gridZ++, inIdx += inSize * inSize, tempIdx += tempSize * tempSize) {
                    final int baseX = mulAddShift(gridX, dist, x, depth);
                    final int baseZ = mulAddShift(gridZ, dist, z, depth);
                    final int inX = baseX >> shift;
                    final int inZ = baseZ >> shift;

                    for (int tileX = 0; tileX < inSize - 1; tileX++) {
                        for (int tileZ = 0; tileZ < inSize - 1; tileZ++) {
                            final int tileInIdx = inIdx + tileX * inSize + tileZ;
                            for (int i = 0; i < 4; i++) {
                                v[i] = in[offsets[i] + tileInIdx];
                            }

                            this.zoomTile0((inX + tileX) << shift, (inZ + tileZ) << shift, v, temp, tempIdx + (tileX << shift) * tempSize + (tileZ << shift), tempSize);
                        }
                    }
                }
            }

            //remove padding from output tiles
            for (int outIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int gridZ = 0; gridZ < count; gridZ++, outIdx += size * size, tempIdx += tempSize * tempSize) {
                    final int realX = mulAddShift(gridX, dist, x, depth);
                    final int realZ = mulAddShift(gridZ, dist, z, depth);

                    for (int dx = 0; dx < size; dx++) {
                        System.arraycopy(temp, tempIdx + (dx + (realX & mask)) * tempSize + (realZ & mask), out, outIdx + dx * size, size);
                    }
                }
            }
        } finally {
            alloc.release(temp);
        }
    }
}
