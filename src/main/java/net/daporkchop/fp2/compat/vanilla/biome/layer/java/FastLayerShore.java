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
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayer;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.gen.layer.GenLayerShore;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerShore
 */
public class FastLayerShore extends AbstractFastLayer {
    //AAAAAAAAAAAAA dear god i hate vanilla why is none of this consistent
    //that said, i can't actually *change* anything for fear of causing some super obscure compatibility bug...

    public FastLayerShore(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        int center, v0, v1, v2, v3;

        int[] arr = alloc.get(3 * 3);
        try {
            this.child.getGrid(alloc, x - 1, z - 1, 3, 3, arr);

            v0 = arr[1];
            v2 = arr[3];
            center = arr[4];
            v1 = arr[5];
            v3 = arr[7];
        } finally {
            alloc.release(arr);
        }

        if (center == ID_MUSHROOM_ISLAND) {
            return v0 != ID_OCEAN && v1 != ID_OCEAN && v2 != ID_OCEAN && v3 != ID_OCEAN
                    ? center
                    : ID_MUSHROOM_ISLAND_SHORE;
        }

        Biome centerBiome = Biome.getBiome(center);
        if (centerBiome != null && centerBiome.getBiomeClass() == BiomeJungle.class) {
            if (isJungleCompatible(v0) && isJungleCompatible(v1) && isJungleCompatible(v2) && isJungleCompatible(v3)) {
                return !isBiomeOceanic(v0) && !isBiomeOceanic(v1) && !isBiomeOceanic(v2) && !isBiomeOceanic(v3)
                        ? center
                        : ID_BEACH;
            } else {
                return ID_JUNGLE_EDGE;
            }
        } else if (center == ID_EXTREME_HILLS || center == ID_EXTREME_HILLS_WITH_TREES || center == ID_EXTREME_HILLS_EDGE) {
            if (isBiomeOceanic(centerBiome)) { //replaceIfNeighborOcean
                return center;
            } else {
                return !isBiomeOceanic(v0) && !isBiomeOceanic(v1) && !isBiomeOceanic(v2) && !isBiomeOceanic(v3)
                        ? center
                        : ID_STONE_BEACH;
            }
        } else if (centerBiome != null && centerBiome.isSnowyBiome()) {
            if (isBiomeOceanic(centerBiome)) { //replaceIfNeighborOcean
                return center;
            } else {
                return !isBiomeOceanic(v0) && !isBiomeOceanic(v1) && !isBiomeOceanic(v2) && !isBiomeOceanic(v3)
                        ? center
                        : ID_COLD_BEACH;
            }
        } else if (center == ID_MESA || center == ID_MESA_ROCK) {
            if (!isBiomeOceanic(v0) && !isBiomeOceanic(v1) && !isBiomeOceanic(v2) && !isBiomeOceanic(v3)) {
                return isMesa(v0) && isMesa(v1) && isMesa(v2) && isMesa(v3)
                        ? center
                        : ID_DESERT;
            } else {
                return center;
            }
        } else if (center == ID_OCEAN || center == ID_DEEP_OCEAN || center == ID_RIVER || center == ID_SWAMPLAND) {
            return center;
        } else {
            return !isBiomeOceanic(v0) && !isBiomeOceanic(v1) && !isBiomeOceanic(v2) && !isBiomeOceanic(v3)
                    ? center
                    : ID_BEACH;
        }
    }
}
