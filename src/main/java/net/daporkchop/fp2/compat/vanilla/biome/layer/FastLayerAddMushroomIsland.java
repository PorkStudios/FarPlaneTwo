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
import net.minecraft.world.gen.layer.GenLayerAddMushroomIsland;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerAddMushroomIsland
 */
public class FastLayerAddMushroomIsland extends FastLayer {

    public FastLayerAddMushroomIsland(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        int center, v0, v1, v2, v3;

        int[] arr = alloc.get(3 * 3);
        try {
            this.parent.getGrid(alloc, x - 1, z - 1, 3, 3, arr);

            v0 = arr[0];
            v2 = arr[2];
            center = arr[4];
            v1 = arr[6];
            v3 = arr[8];
        } finally {
            alloc.release(arr);
        }

        if ((v0 | v1 | v2 | v3) == 0 && nextInt(start(this.seed, x, z), 100) == 0) {
            return ID_MUSHROOM_ISLAND;
        } else {
            return center;
        }
    }
}
