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

/**
 * A {@link IFastLayer} which zooms in.
 * <p>
 * Implementors should always override {@link #multiGetGridsIndividual(ArrayAllocator, int, int, int, int, int, int, int[])}, and override {@link #multiGetGridsCombined(ArrayAllocator, int, int, int, int, int, int, int[])}
 * whenever possible.
 *
 * @author DaPorkchop_
 */
public interface IZoomingLayer extends IFastLayer {
    /**
     * @return whether or not the given grid request is properly grid-aligned, and therefore doesn't need to be padded as much
     */
    static boolean isAligned(int shift, int x, int z, int sizeX, int sizeZ) {
        return ((x | z | sizeX | sizeZ) & ((1 << shift) - 1)) == 0;
    }

    /**
     * @return the number of bits to shift zoomed coordinates by
     */
    int shift();

    /**
     * @return the next layer in the generation chain
     */
    IFastLayer child();

    @Override
    default void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        int shift = this.shift();
        int padding = IZoomingLayer.isAligned(shift, x, z, sizeX, sizeZ) ? 1 : 2;
        int lowSizeX = (sizeX >> shift) + padding;
        int lowSizeZ = (sizeZ >> shift) + padding;

        int[] in = alloc.atLeast(lowSizeX * lowSizeZ);
        try {
            this.child().getGrid(alloc, x >> shift, z >> shift, lowSizeX, lowSizeZ, in);

            this.getGrid0(alloc, x, z, sizeX, sizeZ, out, in);
        } finally {
            alloc.release(in);
        }
    }

    void getGrid0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in);

    @Override
    default void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        int shift = this.shift();
        if (((size >> shift) + 2) < (dist >> (depth + shift))) { //if each small grid is smaller than the space between each grid, we should continue getting
            // multiple grids rather than merging the requests
            this.multiGetGridsIndividual(alloc, x, z, size, dist, depth, count, out);
        } else { //the requests can be combined into a single one
            this.multiGetGridsCombined(alloc, x, z, size, dist, depth, count, out);
        }
    }

    default void multiGetGridsCombined(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        int shift = this.shift();
        int lowSize = ((((dist >> depth) + 1) * count) >> shift) + 2;
        int[] in = alloc.atLeast(lowSize * lowSize);
        try {
            this.child().getGrid(alloc, x >> (depth + shift), z >> (depth + shift), lowSize, lowSize, in);

            this.multiGetGridsCombined0(alloc, x, z, size, dist, depth, count, out, in);
        } finally {
            alloc.release(in);
        }
    }

    void multiGetGridsCombined0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);

    default void multiGetGridsIndividual(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        int shift = this.shift();
        int lowSize = (size >> shift) + 2;

        int[] in = alloc.atLeast(count * count * lowSize * lowSize);
        try {
            this.child().multiGetGrids(alloc, x, z, lowSize, dist, depth + shift, count, in);

            this.multiGetGridsIndividual0(alloc, x, z, size, dist, depth, count, out, in);
        } finally {
            alloc.release(in);
        }
    }

    void multiGetGridsIndividual0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);
}
