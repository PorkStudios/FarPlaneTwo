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

class weighted_random {
public:
    const fp2::fastmod_s64 _fm;

    weighted_random(const size_t count):
        _fm(count) {}

    int32_t select(fp2::biome::fastlayer::rng& rng) {
        return ((int32_t*) &((char*) this)[sizeof(weighted_random)])[rng.nextInt(_fm)];
    }
};

FP2_JNI(jlong, NativeFastLayerBiome, createState0) (JNIEnv* env, jclass cla,
        jobjectArray types_in) {
    size_t types_count = env->GetArrayLength(types_in);
    char** state = new char*[types_count + 1];
    state[types_count] = nullptr;

    for (size_t i = 0; i < types_count; i++) {
        jintArray type_in = (jintArray) env->GetObjectArrayElement(types_in, i);
        size_t type_in_count = env->GetArrayLength(type_in);

        char* r_out = new char[sizeof(weighted_random) + type_in_count * sizeof(int32_t)];

        weighted_random r_tmp(type_in_count);
        memcpy(&r_out[0], &r_tmp, sizeof(weighted_random));
        env->GetIntArrayRegion(type_in, 0, type_in_count, (jint*) &r_out[sizeof(weighted_random)]);

        state[i] = (char*) r_out;
    }

    return (jlong) state;
}

FP2_JNI(void, NativeFastLayerBiome, deleteState0) (JNIEnv* env, jclass cla,
        jlong _state) {
    char** state = (char**) _state;
    for (size_t i = 0; state[i]; i++) {
        delete state[i];
    }
    delete state;
}

inline int32_t eval(int64_t seed, int32_t x, int32_t z, int32_t rawValue, weighted_random** state) {
    int32_t value = rawValue & 0xFF;
    int32_t extra = (rawValue & 0xF00) >> 8;

    if (biomes.isBiomeOceanic(value) || value == biome_ids.MUSHROOM_ISLAND) {
        return value;
    } else if (extra != 0) {
        switch (value) {
            case 1:
                return fp2::biome::fastlayer::rng(seed, x, z).nextInt<3>() == 0 ? biome_ids.MESA_CLEAR_ROCK : biome_ids.MESA_ROCK;
            case 2:
                return biome_ids.JUNGLE;
            case 3:
                return biome_ids.REDWOOD_TAIGA;
        }
    }

    if (value >= 1 && value <= 4) {
        fp2::biome::fastlayer::rng rng(seed, x, z);
        return state[value - 1]->select(rng);
    }

    return biome_ids.MUSHROOM_ISLAND;
}

using layer = fp2::biome::fastlayer::translation_layer<weighted_random**>::impl<eval>;

FP2_JNI(void, NativeFastLayerBiome, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _inout, jlong state) {
    layer{}.grid(env, seed, x, z, sizeX, sizeZ, _inout, (weighted_random**) state);
}

FP2_JNI(void, NativeFastLayerBiome, multiGetGrids0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _inout, jlong state) {
    layer{}.grid_multi(env, seed, x, z, size, dist, depth, count, _inout, (weighted_random**) state);
}
