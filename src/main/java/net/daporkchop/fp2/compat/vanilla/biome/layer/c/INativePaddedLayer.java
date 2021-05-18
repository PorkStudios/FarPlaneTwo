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

package net.daporkchop.fp2.compat.vanilla.biome.layer.c;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IPaddedLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.IJavaPaddedLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

/**
 * Extension of {@link IPaddedLayer} for native implementations.
 *
 * @author DaPorkchop_
 */
public interface INativePaddedLayer extends IJavaPaddedLayer {
    /**
     * @return the seed used by this layer for random number generation
     */
    long seed();

    @Override
    default void getGrid0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in) {
        this.getGrid0(this.seed(), x, z, sizeX, sizeZ, out, in);
    }

    void getGrid0(long seed, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in);

    @Override
    default void multiGetGridsCombined0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        this.multiGetGridsCombined0(this.seed(), x, z, size, dist, depth, count, out, in);
    }

    void multiGetGridsCombined0(long seed, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);

    @Override
    default void multiGetGridsIndividual0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        this.multiGetGridsIndividual0(this.seed(), x, z, size, dist, depth, count, out, in);
    }

    void multiGetGridsIndividual0(long seed, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);
}
