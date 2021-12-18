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

inline int32_t eval(int64_t seed, int32_t x, int32_t z, int32_t center, Vec4i neighbors, fp2::pinned_int_array& _river, size_t& riverIdx) {
    int32_t river = _river[riverIdx++];
    int32_t riverSubMod = (river - 2) % 29;

    if (center != 0 && river >= 2 && riverSubMod == 1 && !biomes.isMutation(center)) {
        int32_t mutation = biomes.getMutationForBiome(center);
        return mutation < 0 ? center : mutation;
    }

    fp2::biome::fastlayer::rng rng(seed, x, z);
    if (riverSubMod != 0 && rng.nextInt<3>() != 0) { //check riverSubMod==0 first to avoid having to yet another expensive modulo
        // computation (rng is updated afterward either way)
        return center;
    }

    int32_t mutation = center;
    if (center == biome_ids.DESERT) {
        mutation = biome_ids.DESERT_HILLS;
    } else if (center == biome_ids.FOREST) {
        mutation = biome_ids.FOREST_HILLS;
    } else if (center == biome_ids.BIRCH_FOREST) {
        mutation = biome_ids.BIRCH_FOREST_HILLS;
    } else if (center == biome_ids.ROOFED_FOREST) {
        mutation = biome_ids.PLAINS;
    } else if (center == biome_ids.TAIGA) {
        mutation = biome_ids.TAIGA_HILLS;
    } else if (center == biome_ids.REDWOOD_TAIGA) {
        mutation = biome_ids.REDWOOD_TAIGA_HILLS;
    } else if (center == biome_ids.COLD_TAIGA) {
        mutation = biome_ids.COLD_TAIGA_HILLS;
    } else if (center == biome_ids.PLAINS) {
        if (rng.nextInt<3>() == 0) {
            mutation = biome_ids.FOREST_HILLS;
        } else {
            mutation = biome_ids.FOREST;
        }
    } else if (center == biome_ids.ICE_PLAINS) {
        mutation = biome_ids.ICE_MOUNTAINS;
    } else if (center == biome_ids.JUNGLE) {
        mutation = biome_ids.JUNGLE_HILLS;
    } else if (center == biome_ids.OCEAN) {
        mutation = biome_ids.DEEP_OCEAN;
    } else if (center == biome_ids.EXTREME_HILLS) {
        mutation = biome_ids.EXTREME_HILLS_WITH_TREES;
    } else if (center == biome_ids.SAVANNA) {
        mutation = biome_ids.SAVANNA_PLATEAU;
    } else if (biomes.biomesEqualOrMesaPlateau(center, biome_ids.MESA_ROCK)) {
        mutation = biome_ids.MESA;
    } else if (center == biome_ids.DEEP_OCEAN && rng.nextInt<3>() == 0) {
        if (rng.nextInt<2>() == 0) {
            mutation = biome_ids.PLAINS;
        } else {
            mutation = biome_ids.FOREST;
        }
    }

    if (!biomes.isValid(mutation)) {
        mutation = -1;
    }

    if (riverSubMod == 0 && mutation != center && (mutation = biomes.getMutationForBiome(mutation)) < 0) {
        mutation = center;
    }

    if (mutation == center) {
        return center;
    } else {
        return horizontal_count(biomes.biomesEqualOrMesaPlateau(neighbors, center)) >= 3 ? mutation : center;
    }
}

using layer = fp2::biome::fastlayer::padded_layer<fp2::pinned_int_array&, size_t&>::impl<eval, fp2::biome::fastlayer::padded_layer_mode::sides>;

FP2_JNI(void, NativeFastLayerHills, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    fp2::pinned_int_array river(env, _out);
    size_t riverIdx = 0;
    layer{}.grid(env, seed, x, z, sizeX, sizeZ, _out, _in, river, riverIdx);
}

FP2_JNI(void, NativeFastLayerHills, multiGetGridsCombined0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    fp2::pinned_int_array river(env, _out);
    size_t riverIdx = 0;
    layer{}.grid_multi_combined(env, seed, x, z, size, dist, depth, count, _out, _in, river, riverIdx);
}

FP2_JNI(void, NativeFastLayerHills, multiGetGridsIndividual0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    fp2::pinned_int_array river(env, _out);
    size_t riverIdx = 0;
    layer{}.grid_multi_individual(env, seed, x, z, size, dist, depth, count, _out, _in, river, riverIdx);
}
