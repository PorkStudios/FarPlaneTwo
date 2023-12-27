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

inline Vec4f scale_weight(Vec4f v) {
    constexpr float DIV_1024 = 1.0f / 1024.0f;
    return (v * DIV_1024 - 0.5f) * 3.6f;
}

inline void eval(int64_t seed, int32_t x, int32_t z, Vec4i _v, int32_t** out) {
    int32_t v[4];
    _v.store(v);

    Vec4i rndX = x + Vec4i(0, 4, 0, 4);
    Vec4i rndZ = z + Vec4i(0, 0, 4, 4);

    fp2::biome::fastlayer::base_rng<Vec4q, Vec4i, compress, extend> rng(seed, rndX, rndZ);
    Vec4f wX = scale_weight(to_float(rng.nextInt<1024>())) + Vec4f(0.0f, 4.0f, 0.0f, 4.0f);
    Vec4f wZ = scale_weight(to_float(rng.nextInt<1024>())) + Vec4f(0.0f, 0.0f, 4.0f, 4.0f);

    for (int32_t dx = 0; dx < 4; dx++) {
        for (int32_t dz = 0; dz < 4; dz++) {
            Vec4f d = square((float) dx - wX) + square((float) dz - wZ);
            out[dx][dz] = v[horizontal_find_first(horizontal_min(d) == d)];
        }
    }
}

using layer = fp2::biome::fastlayer::zooming_layer<eval, 2>;

FP2_JNI(void, NativeFastLayerVoronoiZoom, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    layer{}.grid(env, seed, x, z, sizeX, sizeZ, _out, _in);
}

FP2_JNI(void, NativeFastLayerVoronoiZoom, multiGetGridsCombined0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_combined(env, seed, x, z, size, dist, depth, count, _out, _in);
}

FP2_JNI(void, NativeFastLayerVoronoiZoom, multiGetGridsIndividual0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_individual(env, seed, x, z, size, dist, depth, count, _out, _in);
}
