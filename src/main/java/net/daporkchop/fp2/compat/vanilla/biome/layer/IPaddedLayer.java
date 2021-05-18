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

package net.daporkchop.fp2.compat.vanilla.biome.layer;

import lombok.NonNull;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * A {@link IFastLayer} whose child requests are larger than the initial input request.
 * <p>
 * Implementors should always override {@link #multiGetGridsIndividual(ArrayAllocator, int, int, int, int, int, int, int[])}, and override
 * {@link #multiGetGridsCombined(ArrayAllocator, int, int, int, int, int, int, int[])} whenever possible.
 *
 * @author DaPorkchop_
 */
public interface IPaddedLayer extends IFastLayer {
    /**
     * @return the next layer in the generation chain
     */
    IFastLayer child();

    @Override
    default void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        int[] in = alloc.atLeast((sizeX + 2) * (sizeZ + 2));
        try {
            this.child().getGrid(alloc, x - 1, z - 1, sizeX + 2, sizeZ + 2, in);

            this.getGrid0(alloc, x, z, sizeX, sizeZ, out, in);
        } finally {
            alloc.release(in);
        }
    }

    void getGrid0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in);

    @Override
    default void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        if (size + 2 < asrRound(dist, depth)) { //if the padded request bounds don't intersect, we should continue issuing multiget requests rather than combining
            this.multiGetGridsIndividual(alloc, x, z, size, dist, depth, count, out);
        } else { //the requests can be combined into a single one
            this.multiGetGridsCombined(alloc, x, z, size, dist, depth, count, out);
        }
    }

    default void multiGetGridsCombined(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        int lowSize = (((dist >> depth) + 1) * count) + 2;
        int[] in = alloc.atLeast(lowSize * lowSize);
        try {
            this.child().getGrid(alloc, (x >> depth) - 1, (z >> depth) - 1, lowSize, lowSize, in);

            this.multiGetGridsCombined0(alloc, x, z, size, dist, depth, count, out, in);
        } finally {
            alloc.release(in);
        }
    }

    void multiGetGridsCombined0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);

    default void multiGetGridsIndividual(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        int lowSize = size + 2;
        int[] in = alloc.atLeast(count * count * lowSize * lowSize);
        try {
            this.child().multiGetGrids(alloc, x - (1 << depth), z - (1 << depth), lowSize, dist, depth, count, in);

            this.multiGetGridsIndividual0(alloc, x, z, size, dist, depth, count, out, in);
        } finally {
            alloc.release(in);
        }
    }

    void multiGetGridsIndividual0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);
}
