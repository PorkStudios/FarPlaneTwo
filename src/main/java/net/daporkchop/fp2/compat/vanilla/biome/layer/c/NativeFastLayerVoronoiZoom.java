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
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerVoronoiZoom;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;

/**
 * @author DaPorkchop_
 */
public class NativeFastLayerVoronoiZoom extends FastLayerVoronoiZoom {
    public NativeFastLayerVoronoiZoom(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        x -= 2;
        z -= 2;

        int[] in = alloc.get(2 * 2);
        try {
            this.child.getGrid(alloc, x >> 2, z >> 2, 2, 2, in);

            return this.getSingle0(this.seed, x, z, in);
        } finally {
            alloc.release(in);
        }
    }

    protected native int getSingle0(long seed, int x, int z, @NonNull int[] in);

    @Override
    public void getGrid(@NonNull IntArrayAllocator alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        x -= 2;
        z -= 2;

        int padding = isAligned(x, z, sizeX, sizeZ) ? 1 : 2;
        int lowSizeX = (sizeX >> 2) + padding;
        int lowSizeZ = (sizeZ >> 2) + padding;

        int[] in = alloc.get(lowSizeX * lowSizeZ);
        try {
            this.child.getGrid(alloc, x >> 2, z >> 2, lowSizeX, lowSizeZ, in);

            this.getGrid0(this.seed, x, z, sizeX, sizeZ, out, in);
        } finally {
            alloc.release(in);
        }
    }

    protected native void getGrid0(long seed, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in);
}
