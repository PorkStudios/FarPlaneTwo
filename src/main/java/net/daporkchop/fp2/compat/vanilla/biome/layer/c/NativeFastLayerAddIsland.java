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
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerAddIsland;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;

/**
 * @author DaPorkchop_
 */
public class NativeFastLayerAddIsland extends FastLayerAddIsland {
    public NativeFastLayerAddIsland(long seed) {
        super(seed);
    }

    @Override
    public void getGrid(@NonNull IntArrayAllocator alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        int[] in = alloc.get((sizeX + 2) * (sizeZ + 2));
        try {
            this.child.getGrid(alloc, x - 1, z - 1, sizeX + 2, sizeZ + 2, in);

            this.getGrid0(this.seed, x, z, sizeX, sizeZ, out, in);
        } finally {
            alloc.release(in);
        }
    }

    protected native void getGrid0(long seed, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in);
}
