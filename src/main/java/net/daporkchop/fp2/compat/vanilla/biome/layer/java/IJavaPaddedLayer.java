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
import net.daporkchop.fp2.compat.vanilla.biome.layer.IPaddedLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * Extension of {@link IPaddedLayer} for Java implementations.
 *
 * @author DaPorkchop_
 */
public interface IJavaPaddedLayer extends IPaddedLayer {
    static int[] offsetsCorners(int inSizeX, int inSizeZ) {
        return new int[]{
                -1 * inSizeZ + -1,
                1 * inSizeZ + -1,
                -1 * inSizeZ + 1,
                1 * inSizeZ + 1
        };
    }

    static int[] offsetsSides(int inSizeX, int inSizeZ) {
        return new int[]{
                -1 * inSizeZ + 0,
                0 * inSizeZ + -1,
                0 * inSizeZ + 1,
                1 * inSizeZ + 0
        };
    }

    static int[] offsetsSidesFinalTwoReversed(int inSizeX, int inSizeZ) {
        return new int[]{
                -1 * inSizeZ + 0,
                0 * inSizeZ + -1,
                1 * inSizeZ + 0,
                0 * inSizeZ + 1
        };
    }

    /**
     * @return the neighbor value offsets
     */
    int[] offsets(int inSizeX, int inSizeZ);

    int eval0(int x, int z, int center, @NonNull int[] v);

    @Override
    default int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        final int inSizeX = 3;
        final int inSizeZ = 3;

        int[] in = alloc.atLeast(inSizeX * inSizeZ);
        int[] v = alloc.atLeast(4);
        try {
            this.child().getGrid(alloc, x - 1, z - 1, inSizeX, inSizeZ, in);

            int[] offsets = this.offsets(inSizeX, inSizeZ);
            final int inIdx = 1 * inSizeZ + 1;
            for (int i = 0; i < 4; i++) {
                v[i] = in[offsets[i] + inIdx];
            }

            return this.eval0(x, z, in[inIdx], v);
        } finally {
            alloc.release(v);
            alloc.release(in);
        }
    }

    @Override
    default void getGrid0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in) {
        final int inSizeX = sizeX + 2;
        final int inSizeZ = sizeZ + 2;

        int[] offsets = this.offsets(inSizeX, inSizeZ);
        int[] v = new int[4];

        for (int outIdx = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, outIdx++) {
                final int inIdx = (dx + 1) * inSizeZ + (dz + 1);
                for (int i = 0; i < 4; i++) {
                    v[i] = in[offsets[i] + inIdx];
                }

                out[outIdx] = this.eval0(x + dx, z + dz, in[inIdx], v);
            }
        }
    }

    @Override
    default void multiGetGridsCombined0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int inSize = (((dist >> depth) + 1) * count) + 2;
        final int mask = (depth != 0) ? 1 : 0;

        int[] offsets = this.offsets(inSize, inSize);
        int[] v = new int[4];

        for (int outIdx = 0, gridX = 0; gridX < count; gridX++) {
            for (int gridZ = 0; gridZ < count; gridZ++) {
                final int baseX = mulAddShift(gridX, dist, x, depth);
                final int baseZ = mulAddShift(gridZ, dist, z, depth);
                final int offsetX = mulAddShift(gridX, dist, gridX & mask, depth);
                final int offsetZ = mulAddShift(gridZ, dist, gridZ & mask, depth);

                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++, outIdx++) {
                        final int inIdx = (offsetX + dx + 1) * inSize + (offsetZ + dz + 1);
                        for (int i = 0; i < 4; i++) {
                            v[i] = in[offsets[i] + inIdx];
                        }

                        out[outIdx] = this.eval0(baseX + dx, baseZ + dz, in[inIdx], v);
                    }
                }
            }
        }
    }

    @Override
    default void multiGetGridsIndividual0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int inSize = size + 2;

        int[] offsets = this.offsets(inSize, inSize);
        int[] v = new int[4];

        for (int outIdx = 0, inBase = 0, gridX = 0; gridX < count; gridX++) {
            for (int gridZ = 0; gridZ < count; gridZ++, inBase += inSize * inSize) {
                final int baseX = mulAddShift(gridX, dist, x, depth);
                final int baseZ = mulAddShift(gridZ, dist, z, depth);

                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++, outIdx++) {
                        final int inIdx = inBase + (dx + 1) * inSize + (dz + 1);
                        for (int i = 0; i < 4; i++) {
                            v[i] = in[offsets[i] + inIdx];
                        }

                        out[outIdx] = this.eval0(baseX + dx, baseZ + dz, in[inIdx], v);
                    }
                }
            }
        }
    }
}
