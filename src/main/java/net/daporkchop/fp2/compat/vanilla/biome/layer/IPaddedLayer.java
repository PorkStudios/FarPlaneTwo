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
 * A {@link IFastLayer} whose child requests are larger than the initial input request.
 * <p>
 * Implementors are advised to override {@link #multiGetGridsCombined(IntArrayAllocator, int, int, int, int, int, int[])} and
 * {@link #multiGetGridsIndividual(IntArrayAllocator, int, int, int, int, int, int[])}.
 *
 * @author DaPorkchop_
 */
public interface IPaddedLayer extends IFastLayer {
    /**
     * @return the amount of padding to apply toward negative coordinates
     */
    int paddingRadiusNegative();

    /**
     * @return the amount of padding to apply toward positive coordinates
     */
    int paddingRadiusPositive();

    @Override
    default void multiGetGrids(@NonNull IntArrayAllocator alloc, int x, int z, int size, int dist, int count, @NonNull int[] out) {
        if (size + this.paddingRadiusNegative() + this.paddingRadiusPositive() < dist) { //if the padded request bounds don't intersect, we should continue issuing multiget requests rather than combining
            this.multiGetGridsIndividual(alloc, x, z, size, dist, count, out);
        } else { //the requests can be combined into a single one
            this.multiGetGridsCombined(alloc, x, z, size, dist, count, out);
        }
    }

    default void multiGetGridsCombined(@NonNull IntArrayAllocator alloc, int x, int z, int size, int dist, int count, @NonNull int[] out) {
        IFastLayer.super.multiGetGrids(alloc, x, z, size, dist, count, out); //fall back to slow implementation
    }

    default void multiGetGridsIndividual(@NonNull IntArrayAllocator alloc, int x, int z, int size, int dist, int count, @NonNull int[] out) {
        IFastLayer.super.multiGetGrids(alloc, x, z, size, dist, count, out); //fall back to slow implementation
    }
}
