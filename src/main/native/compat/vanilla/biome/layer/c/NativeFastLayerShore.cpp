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

#include "NativeFastLayer.h"

inline int32_t eval(int64_t seed, int32_t x, int32_t z, int32_t center, Vec4i neighbors) {
    if (center == biome_ids.MUSHROOM_ISLAND) {
        return horizontal_and(neighbors != biome_ids.OCEAN)
                ? center
                : biome_ids.MUSHROOM_ISLAND_SHORE;
    }

    if (biomes.isJungle(center)) {
        if (horizontal_and(biomes.isJungleCompatible(neighbors))) {
            return horizontal_and(!biomes.isBiomeOceanic(neighbors))
                    ? center
                    : biome_ids.BEACH;
        } else {
            return biome_ids.JUNGLE_EDGE;
        }
    } else if (center == biome_ids.EXTREME_HILLS || center == biome_ids.EXTREME_HILLS_WITH_TREES || center == biome_ids.EXTREME_HILLS_EDGE) {
        if (biomes.isBiomeOceanic(center)) { //replaceIfNeighborOcean
            return center;
        } else {
            return horizontal_and(!biomes.isBiomeOceanic(neighbors))
                    ? center
                    : biome_ids.STONE_BEACH;
        }
    } else if (biomes.isSnowyBiome(center)) {
        if (biomes.isBiomeOceanic(center)) { //replaceIfNeighborOcean
            return center;
        } else {
            return horizontal_and(!biomes.isBiomeOceanic(neighbors))
                    ? center
                    : biome_ids.COLD_BEACH;
        }
    } else if (center == biome_ids.MESA || center == biome_ids.MESA_ROCK) {
        if (horizontal_and(!biomes.isBiomeOceanic(neighbors))) {
            return horizontal_and(biomes.isMesa(neighbors))
                    ? center
                    : biome_ids.DESERT;
        } else {
            return center;
        }
    } else if (center == biome_ids.OCEAN || center == biome_ids.DEEP_OCEAN || center == biome_ids.RIVER || center == biome_ids.SWAMPLAND) {
        return center;
    } else {
        return horizontal_and(!biomes.isBiomeOceanic(neighbors))
                ? center
                : biome_ids.BEACH;
    }
}

using layer = fp2::biome::fastlayer::padded_layer<>::impl<eval, fp2::biome::fastlayer::padded_layer_mode::sides>;

FP2_JNI(void, NativeFastLayerShore, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    layer{}.grid(env, seed, x, z, sizeX, sizeZ, _out, _in);
}

FP2_JNI(void, NativeFastLayerShore, multiGetGridsCombined0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_combined(env, seed, x, z, size, dist, depth, count, _out, _in);
}

FP2_JNI(void, NativeFastLayerShore, multiGetGridsIndividual0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_individual(env, seed, x, z, size, dist, depth, count, _out, _in);
}
