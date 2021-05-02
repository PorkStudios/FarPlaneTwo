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
import net.minecraft.world.gen.layer.GenLayerBiomeEdge;

import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelper.*;
import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelperCached.biomesEqualOrMesaPlateau;
import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelperCached.canBiomesBeNeighbors;

/**
 * @author DaPorkchop_
 * @see GenLayerBiomeEdge
 */
public class JavaFastLayerBiomeEdge extends AbstractFastLayer implements IJavaPaddedLayer {
    public static int replaceBiomeEdgeIfNecessary(int center, int v0, int v1, int v2, int v3, int replace, int with) {
        if (!biomesEqualOrMesaPlateau(center, replace)) {
            return -1;
        }

        return canBiomesBeNeighbors(v0, replace) && canBiomesBeNeighbors(v1, replace) && canBiomesBeNeighbors(v2, replace) && canBiomesBeNeighbors(v3, replace)
                ? center
                : with;
    }

    public static int replaceBiomeEdge(int center, int v0, int v1, int v2, int v3, int replace, int with) {
        if (center != replace) {
            return -1;
        }

        return biomesEqualOrMesaPlateau(v0, replace) && biomesEqualOrMesaPlateau(v1, replace) && biomesEqualOrMesaPlateau(v2, replace) && biomesEqualOrMesaPlateau(v3, replace)
                ? center
                : with;
    }

    public static boolean areNoneEqual(int v0, int v1, int v2, int v3, int check) {
        return v0 != check && v1 != check && v2 != check && v3 != check;
    }

    public JavaFastLayerBiomeEdge(long seed) {
        super(seed);
    }

    @Override
    public int[] offsets(int inSizeX, int inSizeZ) {
        return IJavaPaddedLayer.offsetsSides(inSizeX, inSizeZ);
    }

    @Override
    public int eval0(int x, int z, int center, @NonNull int[] v) {
        int out;
        if ((out = replaceBiomeEdgeIfNecessary(center, v[0], v[1], v[2], v[3], ID_EXTREME_HILLS, ID_EXTREME_HILLS_EDGE)) >= 0
            || (out = replaceBiomeEdge(center, v[0], v[1], v[2], v[3], ID_MESA_ROCK, ID_MESA)) >= 0
            || (out = replaceBiomeEdge(center, v[0], v[1], v[2], v[3], ID_MESA_CLEAR_ROCK, ID_MESA)) >= 0
            || (out = replaceBiomeEdge(center, v[0], v[1], v[2], v[3], ID_REDWOOD_TAIGA, ID_TAIGA)) >= 0) {
            return out;
        } else if (center == ID_DESERT) {
            return areNoneEqual(v[0], v[1], v[2], v[3], ID_ICE_PLAINS)
                    ? center
                    : ID_EXTREME_HILLS_WITH_TREES;
        } else if (center == ID_SWAMPLAND) {
            if (areNoneEqual(v[0], v[1], v[2], v[3], ID_DESERT) && areNoneEqual(v[0], v[1], v[2], v[3], ID_COLD_TAIGA) && areNoneEqual(v[0], v[1], v[2], v[3], ID_ICE_PLAINS)) {
                return areNoneEqual(v[0], v[1], v[2], v[3], ID_JUNGLE)
                        ? center
                        : ID_JUNGLE_EDGE;
            } else {
                return ID_PLAINS;
            }
        } else {
            return center;
        }
    }
}
