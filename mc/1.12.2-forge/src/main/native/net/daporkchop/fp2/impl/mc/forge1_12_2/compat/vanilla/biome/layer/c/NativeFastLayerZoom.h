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

inline int32_t select_mode_or_random(fp2::biome::fastlayer::rng& rng, int32_t (v)[4]) {
    //here be branch predictor hell
    if (v[1] == v[2] && v[2] == v[3]) {
        return v[1];
    } else if (v[0] == v[1] && v[0] == v[2]) {
        return v[0];
    } else if (v[0] == v[1] && v[0] == v[3]) {
        return v[0];
    } else if (v[0] == v[2] && v[0] == v[3]) {
        return v[0];
    } else if (v[0] == v[1] && v[2] != v[3]) {
        return v[0];
    } else if (v[0] == v[2] && v[1] != v[3]) {
        return v[0];
    } else if (v[0] == v[3] && v[1] != v[2]) {
        return v[0];
    } else if (v[1] == v[2] && v[0] != v[3]) {
        return v[1];
    } else if (v[1] == v[3] && v[0] != v[2]) {
        return v[1];
    } else if (v[2] == v[3] && v[0] != v[1]) {
        return v[2];
    } else {
        return v[rng.nextInt<4>()];
    }
}

inline void eval(int64_t seed, int32_t x, int32_t z, Vec4i _v, int32_t** out) {
    int32_t v[4];
    _v.store(v);

    fp2::biome::fastlayer::rng rng(seed, x, z);
    out[0][0] = v[0];
    out[0][1] = v[rng.nextInt<2>() << 1];
    out[1][0] = v[rng.nextInt<2>()];
    out[1][1] = select_mode_or_random(rng, v);
}

using layer = fp2::biome::fastlayer::zooming_layer<eval, 1>;

FP2_JNI(void, NativeFastLayerZoom, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    layer{}.grid(env, seed, x, z, sizeX, sizeZ, _out, _in);
}

FP2_JNI(void, NativeFastLayerZoom, multiGetGridsCombined0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_combined(env, seed, x, z, size, dist, depth, count, _out, _in);
}

FP2_JNI(void, NativeFastLayerZoom, multiGetGridsIndividual0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint depth, jint count, jintArray _out, jintArray _in) {
    layer{}.grid_multi_individual(env, seed, x, z, size, dist, depth, count, _out, _in);
}
