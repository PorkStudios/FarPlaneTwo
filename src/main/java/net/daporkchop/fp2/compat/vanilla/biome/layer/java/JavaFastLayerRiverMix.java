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
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayerWithRiverSource;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerRiverMix;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelperCached.*;

/**
 * @author DaPorkchop_
 * @see GenLayerRiverMix
 */
public class JavaFastLayerRiverMix extends AbstractFastLayerWithRiverSource {
    public JavaFastLayerRiverMix(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        return this.mix0(this.child.getSingle(alloc, x, z), this.childRiver.getSingle(alloc, x, z));
    }

    @Override
    public void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        this.child.getGrid(alloc, x, z, sizeX, sizeZ, out);

        int[] river = alloc.atLeast(sizeX * sizeZ);
        try {
            this.childRiver.getGrid(alloc, x, z, sizeX, sizeZ, river);

            this.mix0(sizeX * sizeZ, out, river);
        } finally {
            alloc.release(river);
        }
    }

    @Override
    public void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        this.child.multiGetGrids(alloc, x, z, size, dist, depth, count, out);

        int[] river = alloc.atLeast(count * count * size * size);
        try {
            this.childRiver.multiGetGrids(alloc, x, z, size, dist, depth, count, river);

            this.mix0(count * count * size * size, out, river);
        } finally {
            alloc.release(river);
        }
    }

    protected void mix0(int count, @NonNull int[] biome, @NonNull int[] river) {
        for (int i = 0; i < count; i++) {
            biome[i] = this.mix0(biome[i], river[i]);
        }
    }

    protected int mix0(int biome, int river) {
        if (biome != ID_OCEAN && biome != ID_DEEP_OCEAN && river == ID_RIVER) {
            if (biome == ID_ICE_PLAINS) {
                return ID_FROZEN_RIVER;
            } else if (biome != ID_MUSHROOM_ISLAND && biome != ID_MUSHROOM_ISLAND_SHORE) {
                return river & 0xFF;
            } else {
                return ID_MUSHROOM_ISLAND_SHORE;
            }
        }
        return biome;
    }
}
