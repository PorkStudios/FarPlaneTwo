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
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRandomValues;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRiverMix;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;

/**
 * @author DaPorkchop_
 */
public class NativeFastLayerRiverMix extends FastLayerRiverMix {
    public NativeFastLayerRiverMix(long seed) {
        super(seed);
    }

    @Override
    public void getGrid(@NonNull IntArrayAllocator alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        this.child.getGrid(alloc, x, z, sizeX, sizeZ, out);

        int[] river = alloc.get(sizeX * sizeZ);
        try {
            this.childRiver.getGrid(alloc, x, z, sizeX, sizeZ, river);

            this.mix0(sizeX * sizeZ, out, river);
        } finally {
            alloc.release(river);
        }
    }

    @Override
    public void multiGetGrids(@NonNull IntArrayAllocator alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        this.child.multiGetGrids(alloc, x, z, size, dist, depth, count, out);

        int[] river = alloc.get(count * count * size * size);
        try {
            this.childRiver.multiGetGrids(alloc, x, z, size, dist, depth, count, river);

            this.mix0(count * count * size * size, out, river);
        } finally {
            alloc.release(river);
        }
    }

    protected native void mix0(int count, @NonNull int[] biome, @NonNull int[] river);
}
