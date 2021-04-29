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
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;

/**
 * A {@link IFastLayer} which zooms in.
 * <p>
 * Implementors should always override {@link #multiGetGridsIndividual(IntArrayAllocator, int, int, int, int, int, int[])}, and override {@link #multiGetGridsCombined(IntArrayAllocator, int, int, int, int, int, int[])}
 * whenever possible.
 *
 * @author DaPorkchop_
 */
public interface IZoomingLayer extends IFastLayer {
    /**
     * @return the number of bits to shift zoomed coordinates by
     */
    int shift();

    @Override
    default void multiGetGrids(@NonNull IntArrayAllocator alloc, int x, int z, int size, int dist, int count, @NonNull int[] out) {
        int shift = this.shift();
        if (((size >> shift) + 2) < (dist >> shift)) { //if each small grid is smaller than the space between each grid, we should continue getting
            // multiple grids rather than merging the requests
            this.multiGetGridsIndividual(alloc, x, z, size, dist, count, out);
        } else { //the requests can be combined into a single one
            this.multiGetGridsCombined(alloc, x, z, size, dist, count, out);
        }
    }

    default void multiGetGridsCombined(@NonNull IntArrayAllocator alloc, int x, int z, int size, int dist, int count, @NonNull int[] out) {
        IFastLayer.super.multiGetGrids(alloc, x, z, size, dist, count, out); //fall back to slow implementation
        //this.multiGetGridsIndividual(alloc, x, z, size, dist, count, out); //this method is less critical to achieving good performance
    }

    default void multiGetGridsIndividual(@NonNull IntArrayAllocator alloc, int x, int z, int size, int dist, int count, @NonNull int[] out) {
        IFastLayer.super.multiGetGrids(alloc, x, z, size, dist, count, out); //fall back to slow implementation
    }
}
