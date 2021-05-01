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
    return center == biomes.OCEAN && horizontal_and(neighbors == biomes.OCEAN)
            ? biomes.DEEP_OCEAN
            : center;
}

using layer = fp2::biome::fastlayer::padded_layer<>::impl<eval, fp2::biome::fastlayer::padded_layer_mode::sides>;

FP2_JNI(void, NativeFastLayerDeepOcean, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    layer{}.grid(env, seed, x, z, sizeX, sizeZ, _out, _in);
}

FP2_JNI(void, NativeFastLayerDeepOcean, multiGetGridsCombined0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_combined(env, seed, x, z, size, dist, depth, count, _out, _in);
}

FP2_JNI(void, NativeFastLayerDeepOcean, multiGetGridsIndividual0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_individual(env, seed, x, z, size, dist, depth, count, _out, _in);
}
