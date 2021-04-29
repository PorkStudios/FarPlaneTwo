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

#ifndef NATIVELAYER_NATIVEFASTLAYERZOOM_H
#define NATIVELAYER_NATIVEFASTLAYERZOOM_H

#include <fp2.h>
#include "NativeFastLayer.h"

#include <cstring>
#include <vector>

namespace fp2::biome::fastlayer {
    using int4 = int32_t[4];

    template<int32_t(SELECT_CORNER)(rng&, int4&)> inline void zoom_aligned(JNIEnv* env,
        int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
        const int32_t lowX = x >> 1;
        const int32_t lowZ = z >> 1;
        const int32_t lowSizeX = (sizeX >> 1) + 1;
        const int32_t lowSizeZ = (sizeZ >> 1) + 1;

        const Vec4i in_offsets(0 * lowSizeZ + 0, 1 * lowSizeZ + 0, 0 * lowSizeZ + 1, 1 * lowSizeZ + 1);
        fp2::pinned_int_array out(env, _out);
        fp2::pinned_int_array in(env, _in);

        for (int32_t tileX = 0; tileX < lowSizeX - 1; tileX++) {
            for (int32_t tileZ = 0; tileZ < lowSizeZ - 1; tileZ++) {
                int4 v; //load values with single instruction using SSE
                lookup<(1 << 30)>((tileX * lowSizeZ + tileZ) + in_offsets, &in[0]).store(v);

                fp2::biome::fastlayer::rng rng(seed, (lowX + tileX) << 1, (lowZ + tileZ) << 1);
                out[((tileX << 1) + 0) * sizeZ + ((tileZ << 1) + 0)] = v[0];
                out[((tileX << 1) + 0) * sizeZ + ((tileZ << 1) + 1)] = v[rng.nextInt<2>() << 1];
                out[((tileX << 1) + 1) * sizeZ + ((tileZ << 1) + 0)] = v[rng.nextInt<2>()];
                out[((tileX << 1) + 1) * sizeZ + ((tileZ << 1) + 1)] = SELECT_CORNER(rng, v);
            }
        }
    }

    template<int32_t(SELECT_CORNER)(rng&, int4&)> inline void zoom_unaligned(JNIEnv* env,
        int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
        const int32_t lowX = x >> 1;
        const int32_t lowZ = z >> 1;
        const int32_t lowSizeX = (sizeX >> 1) + 2;
        const int32_t lowSizeZ = (sizeZ >> 1) + 2;
        const int32_t tempSizeX = (lowSizeX - 1) << 1;
        const int32_t tempSizeZ = (lowSizeZ - 1) << 1;

        std::vector<int32_t> temp(tempSizeX * tempSizeZ);

        {
            const Vec4i in_offsets(0 * lowSizeZ + 0, 1 * lowSizeZ + 0, 0 * lowSizeZ + 1, 1 * lowSizeZ + 1);
            fp2::pinned_int_array in(env, _in);

            for (int32_t tileX = 0; tileX < lowSizeX - 1; tileX++) {
                for (int32_t tileZ = 0; tileZ < lowSizeZ - 1; tileZ++) {
                    int4 v; //load values with single instruction using SSE
                    lookup<(1 << 30)>((tileX * lowSizeZ + tileZ) + in_offsets, &in[0]).store(v);

                    fp2::biome::fastlayer::rng rng(seed, (lowX + tileX) << 1, (lowZ + tileZ) << 1);
                    temp[((tileX << 1) + 0) * tempSizeZ + ((tileZ << 1) + 0)] = v[0];
                    temp[((tileX << 1) + 0) * tempSizeZ + ((tileZ << 1) + 1)] = v[rng.nextInt<2>() << 1];
                    temp[((tileX << 1) + 1) * tempSizeZ + ((tileZ << 1) + 0)] = v[rng.nextInt<2>()];
                    temp[((tileX << 1) + 1) * tempSizeZ + ((tileZ << 1) + 1)] = SELECT_CORNER(rng, v);
                }
            }
        }

        {
            fp2::pinned_int_array out(env, _out);

            for (int32_t dx = 0; dx < sizeX; dx++) {
                memcpy(&out[dx * sizeZ], &temp[(dx + (x & 1)) * tempSizeZ + (z & 1)], sizeZ * sizeof(int32_t));
            }
        }
    }

    template<int32_t(SELECT_CORNER)(rng&, int4&)> inline void zoom(JNIEnv* env,
            int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
        if (!((x | z | sizeX | sizeZ) & 1)) {
            zoom_aligned<SELECT_CORNER>(env, seed, x, z, sizeX, sizeZ, _out, _in);
        } else {
            zoom_unaligned<SELECT_CORNER>(env, seed, x, z, sizeX, sizeZ, _out, _in);
        }
    }

    template<int32_t(SELECT_CORNER)(rng&, int4&)> inline void zoom_multi_individual(JNIEnv* env,
            int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t count, jintArray _out, jintArray _in) {
        const int32_t lowSize = (size >> 1) + 2;
        const int32_t tempSize = (lowSize - 1) << 1;

        std::vector<int32_t> temp(count * count * tempSize * tempSize);

        {
            const Vec4i in_offsets(0 * lowSize + 0, 1 * lowSize + 0, 0 * lowSize + 1, 1 * lowSize + 1);
            fp2::pinned_int_array in(env, _in);

            for (int32_t inIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++, inIdx += lowSize * lowSize, tempIdx += tempSize * tempSize) {
                    const int32_t lowX = (x + gridX * dist) >> 1;
                    const int32_t lowZ = (z + gridZ * dist) >> 1;

                    for (int32_t tileX = 0; tileX < lowSize - 1; tileX++) {
                        for (int32_t tileZ = 0; tileZ < lowSize - 1; tileZ++) {
                            int4 v; //load values with single instruction using SSE
                            lookup<(1 << 30)>((inIdx + tileX * lowSize + tileZ) + in_offsets, &in[0]).store(v);

                            fp2::biome::fastlayer::rng rng(seed, (lowX + tileX) << 1, (lowZ + tileZ) << 1);
                            temp[tempIdx + ((tileX << 1) + 0) * tempSize + ((tileZ << 1) + 0)] = v[0];
                            temp[tempIdx + ((tileX << 1) + 0) * tempSize + ((tileZ << 1) + 1)] = v[rng.nextInt<2>() << 1];
                            temp[tempIdx + ((tileX << 1) + 1) * tempSize + ((tileZ << 1) + 0)] = v[rng.nextInt<2>()];
                            temp[tempIdx + ((tileX << 1) + 1) * tempSize + ((tileZ << 1) + 1)] = SELECT_CORNER(rng, v);
                        }
                    }
                }
            }
        }

        {
            fp2::pinned_int_array out(env, _out);

            for (int32_t outIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++, outIdx += size * size, tempIdx += tempSize * tempSize) {
                    const int32_t realX = x + gridX * dist;
                    const int32_t realZ = z + gridZ * dist;

                    for (int32_t dx = 0; dx < size; dx++) {
                        memcpy(&out[outIdx + dx * size], &temp[tempIdx + (dx + (realX & 1)) * tempSize + (realZ & 1)], size * sizeof(int32_t));
                    }
                }
            }
        }
    }
}

#endif //NATIVELAYER_NATIVEFASTLAYERZOOM_H
