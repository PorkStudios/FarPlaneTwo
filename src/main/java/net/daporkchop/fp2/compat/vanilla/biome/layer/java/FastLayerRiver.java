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
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayer;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerRiver;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerRiver
 */
public class FastLayerRiver extends FastLayer {
    private static int riverFilter(int i) {
        return i >= 2 ? 2 + (i & 1) : i;
    }

    public FastLayerRiver(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        int[] arr = alloc.get(3 * 3);
        try {
            this.parent.getGrid(alloc, x - 1, z - 1, 3, 3, arr);

            int center = riverFilter(arr[4]);
            if (center == riverFilter(arr[1]) && center == riverFilter(arr[3]) && center == riverFilter(arr[5]) && center == riverFilter(arr[7])) {
                return -1;
            } else {
                return ID_RIVER;
            }
        } finally {
            alloc.release(arr);
        }
    }
}
