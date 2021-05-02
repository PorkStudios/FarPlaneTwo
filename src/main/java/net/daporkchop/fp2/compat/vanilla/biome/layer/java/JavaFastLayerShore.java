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
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.gen.layer.GenLayerShore;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerShore
 */
public class JavaFastLayerShore extends AbstractFastLayer implements IJavaPaddedLayer {
    //AAAAAAAAAAAAA dear god i hate vanilla why is none of this consistent
    //that said, i can't actually *change* anything for fear of causing some super obscure compatibility bug...

    public JavaFastLayerShore(long seed) {
        super(seed);
    }

    @Override
    public int[] offsets(int inSizeX, int inSizeZ) {
        return IJavaPaddedLayer.offsetsSides(inSizeX, inSizeZ);
    }

    @Override
    public int eval0(int x, int z, int center, @NonNull int[] v) {
        if (center == ID_MUSHROOM_ISLAND) {
            return v[0] != ID_OCEAN && v[1] != ID_OCEAN && v[2] != ID_OCEAN && v[3] != ID_OCEAN
                    ? center
                    : ID_MUSHROOM_ISLAND_SHORE;
        }

        Biome centerBiome = Biome.getBiome(center);
        if (centerBiome != null && centerBiome.getBiomeClass() == BiomeJungle.class) {
            if (isJungleCompatible(v[0]) && isJungleCompatible(v[1]) && isJungleCompatible(v[2]) && isJungleCompatible(v[3])) {
                return !isBiomeOceanic(v[0]) && !isBiomeOceanic(v[1]) && !isBiomeOceanic(v[2]) && !isBiomeOceanic(v[3])
                        ? center
                        : ID_BEACH;
            } else {
                return ID_JUNGLE_EDGE;
            }
        } else if (center == ID_EXTREME_HILLS || center == ID_EXTREME_HILLS_WITH_TREES || center == ID_EXTREME_HILLS_EDGE) {
            if (isBiomeOceanic(centerBiome)) { //replaceIfNeighborOcean
                return center;
            } else {
                return !isBiomeOceanic(v[0]) && !isBiomeOceanic(v[1]) && !isBiomeOceanic(v[2]) && !isBiomeOceanic(v[3])
                        ? center
                        : ID_STONE_BEACH;
            }
        } else if (centerBiome != null && centerBiome.isSnowyBiome()) {
            if (isBiomeOceanic(centerBiome)) { //replaceIfNeighborOcean
                return center;
            } else {
                return !isBiomeOceanic(v[0]) && !isBiomeOceanic(v[1]) && !isBiomeOceanic(v[2]) && !isBiomeOceanic(v[3])
                        ? center
                        : ID_COLD_BEACH;
            }
        } else if (center == ID_MESA || center == ID_MESA_ROCK) {
            if (!isBiomeOceanic(v[0]) && !isBiomeOceanic(v[1]) && !isBiomeOceanic(v[2]) && !isBiomeOceanic(v[3])) {
                return isMesa(v[0]) && isMesa(v[1]) && isMesa(v[2]) && isMesa(v[3])
                        ? center
                        : ID_DESERT;
            } else {
                return center;
            }
        } else if (center == ID_OCEAN || center == ID_DEEP_OCEAN || center == ID_RIVER || center == ID_SWAMPLAND) {
            return center;
        } else {
            return !isBiomeOceanic(v[0]) && !isBiomeOceanic(v[1]) && !isBiomeOceanic(v[2]) && !isBiomeOceanic(v[3])
                    ? center
                    : ID_BEACH;
        }
    }
}
