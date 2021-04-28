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

#include <lib/vectorclass-2.01.03/vectorclass.h>

#include <cstring>
#include <vector>

template<typename S, typename T> constexpr T conv_raw(const S v) {
    return *(T*) &v;
}

template<typename T> inline T scale_weight(T v) {
    constexpr float DIV_1024 = 1.0f / 1024.0f;

    return (v * DIV_1024 - 0.5f) * 3.6f;
}

FP2_JNI(jint, NativeFastLayerVoronoiZoom, getSingle0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jintArray _in) {
    fp2::pinned_int_array in(env, _in);

    Vec4i rndX = (x & ~3) + Vec4i(0, 0, 4, 4);
    Vec4i rndZ = (z & ~3) + Vec4i(0, 4, 0, 4);

    fp2::biome::fastlayer::base_rng<Vec4q, Vec4i, compress, extend> rng(seed, rndX, rndZ);
    Vec4f wX = scale_weight(to_float(rng.nextInt<1024>())) + Vec4f(0.0f, 0.0f, 4.0f, 4.0f);
    Vec4f wZ = scale_weight(to_float(rng.nextInt<1024>())) + Vec4f(0.0f, 4.0f, 0.0f, 4.0f);

    Vec4f d = square((float) (x & 3) - wX) + square((float) (z & 3) - wZ);
    return in[horizontal_find_first(horizontal_min(d) == d)];
}

//convert RNG output to floats (vectorized impl)
inline void rng_to_float(std::vector<float>& arr, const size_t cnt) {
    constexpr float DIV_1024 = 1.0f / 1024.0f;

    size_t i = 0;
    for (Vec4i vi; i < (cnt & ~3); i += 4) {
        vi.load(&arr[i]);
        scale_weight(to_float(vi)).store(&arr[i]);
    }
    for (; i < cnt; i++) { //scalar processing for final few elements
        arr[i] = scale_weight((float) conv_raw<float, int32_t>(arr[i]));
    }
}

inline void zoom_aligned(JNIEnv* env,
        int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
    const int32_t lowX = x >> 2;
    const int32_t lowZ = z >> 2;
    const int32_t lowSizeX = (sizeX >> 2) + 1;
    const int32_t lowSizeZ = (sizeZ >> 2) + 1;

    //compute weights
    std::vector<float> weights(lowSizeX * lowSizeZ << 1);
    for (int32_t i = 0, dx = 0; dx < lowSizeX; dx++) {
        for (int32_t dz = 0; dz < lowSizeZ; dz++) {
            fp2::biome::fastlayer::rng rng(seed, (lowX + dx) << 2, (lowZ + dz) << 2);
            weights[i++] = conv_raw<int32_t, float>(rng.nextInt<1024>());
            weights[i++] = conv_raw<int32_t, float>(rng.nextInt<1024>());
        }
    }
    rng_to_float(weights, lowSizeX * lowSizeZ << 1);

    const Vec4i in_offsets(0 * lowSizeZ + 0, 0 * lowSizeZ + 1, 1 * lowSizeZ + 0, 1 * lowSizeZ + 1);
    fp2::pinned_int_array out(env, _out);
    fp2::pinned_int_array in(env, _in);

    for (int32_t tileX = 0; tileX < lowSizeX - 1; tileX++) {
        for (int32_t tileZ = 0; tileZ < lowSizeZ - 1; tileZ++) {
            int32_t v[4]; //load values with single instruction using SSE
            lookup<(1 << 30)>((tileX * lowSizeZ + tileZ) + in_offsets, &in[0]).store(v);

            Vec4f w0, w1;
            w0.load(&weights[(tileX * lowSizeZ + tileZ) << 1]);
            w1.load(&weights[((tileX + 1) * lowSizeZ + tileZ) << 1]);

            Vec4f wX = blend4<0, 2, 4, 6>(w0, w1) + Vec4f(0.0f, 0.0f, 4.0f, 4.0f);
            Vec4f wZ = blend4<1, 3, 5, 7>(w0, w1) + Vec4f(0.0f, 4.0f, 0.0f, 4.0f);

            for (int32_t dx = 0; dx < 4; dx++) {
                for (int32_t dz = 0; dz < 4; dz++) {
                    Vec4f d = square((float) dx - wX) + square((float) dz - wZ);
                    out[((tileX << 2) + dx) * sizeZ + ((tileZ << 2) + dz)] = v[horizontal_find_first(horizontal_min(d) == d)];
                }
            }
        }
    }
}

inline void zoom_unaligned(JNIEnv* env,
        int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
    const int32_t lowX = x >> 2;
    const int32_t lowZ = z >> 2;
    const int32_t lowSizeX = (sizeX >> 2) + 2;
    const int32_t lowSizeZ = (sizeZ >> 2) + 2;
    const int32_t tempSizeX = (lowSizeX - 1) << 2;
    const int32_t tempSizeZ = (lowSizeZ - 1) << 2;

    //compute weights
    std::vector<float> weights(lowSizeX * lowSizeZ << 1);
    for (int32_t i = 0, dx = 0; dx < lowSizeX; dx++) {
        for (int32_t dz = 0; dz < lowSizeZ; dz++) {
            fp2::biome::fastlayer::rng rng(seed, (lowX + dx) << 2, (lowZ + dz) << 2);
            weights[i++] = conv_raw<int32_t, float>(rng.nextInt<1024>());
            weights[i++] = conv_raw<int32_t, float>(rng.nextInt<1024>());
        }
    }
    rng_to_float(weights, lowSizeX * lowSizeZ << 1);

    std::vector<int32_t> temp(tempSizeX * tempSizeZ);
    {
        const Vec4i in_offsets(0 * lowSizeZ + 0, 0 * lowSizeZ + 1, 1 * lowSizeZ + 0, 1 * lowSizeZ + 1);
        fp2::pinned_int_array in(env, _in);

        for (int32_t tileX = 0; tileX < lowSizeX - 1; tileX++) {
            for (int32_t tileZ = 0; tileZ < lowSizeZ - 1; tileZ++) {
                int32_t v[4]; //load values with single instruction using SSE
                lookup<(1 << 30)>((tileX * lowSizeZ + tileZ) + in_offsets, &in[0]).store(v);

                Vec4f w0, w1;
                w0.load(&weights[(tileX * lowSizeZ + tileZ) << 1]);
                w1.load(&weights[((tileX + 1) * lowSizeZ + tileZ) << 1]);

                Vec4f wX = blend4<0, 2, 4, 6>(w0, w1) + Vec4f(0.0f, 0.0f, 4.0f, 4.0f);
                Vec4f wZ = blend4<1, 3, 5, 7>(w0, w1) + Vec4f(0.0f, 4.0f, 0.0f, 4.0f);

                for (int32_t dx = 0; dx < 4; dx++) {
                    for (int32_t dz = 0; dz < 4; dz++) {
                        Vec4f d = square((float) dx - wX) + square((float) dz - wZ);
                        temp[((tileX << 2) + dx) * tempSizeZ + ((tileZ << 2) + dz)] = v[horizontal_find_first(horizontal_min(d) == d)];
                    }
                }
            }
        }
    }

    {
        fp2::pinned_int_array out(env, _out);

        for (int32_t dx = 0; dx < sizeX; dx++) {
            memcpy(&out[dx * sizeZ], &temp[(dx + (x & 3)) * tempSizeZ + (z & 3)], sizeZ * sizeof(int32_t));
        }
    }
}

FP2_JNI(void, NativeFastLayerVoronoiZoom, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    if (!((x | z | sizeX | sizeZ) & 3)) {
        zoom_aligned(env, seed, x, z, sizeX, sizeZ, _out, _in);
    } else {
        zoom_unaligned(env, seed, x, z, sizeX, sizeZ, _out, _in);
    }
}
