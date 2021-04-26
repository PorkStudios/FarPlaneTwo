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

#include <fp2.h>
#include "NativeFastLayer.h"

#include <lib/vectorclass-2.01.03/vectorclass.h>

constexpr fp2::fastmod_s64 MODULOS[] = {
    fp2::fastmod_s64(1),
    fp2::fastmod_s64(2),
    fp2::fastmod_s64(3),
    fp2::fastmod_s64(4)
};

FP2_JNI(void, NativeFastLayerAddIsland, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    fp2::pinned_int_array out(env, _out);
    fp2::pinned_int_array in(env, _in);

    const Vec4i neighbor_offsets(-1 * (sizeZ + 2) + -1, -1 * (sizeZ + 2) + 1, 1 * (sizeZ + 2) + -1, 1 * (sizeZ + 2) + 1);

    for (int32_t outIdx = 0, dx = 0; dx < sizeX; dx++) {
        Vec4i neighbor_indices = ((dx + 1) * (sizeZ + 2) + 1) + neighbor_offsets;

        for (int32_t inIdx = (dx + 1) * (sizeZ + 2) + 1, dz = 0; dz < sizeZ; neighbor_indices++, inIdx++, dz++, outIdx++) {
            fp2::biome::fastlayer::rng rng(seed, x + dx, z + dz);

            auto center = in[inIdx];
            const Vec4i neighbors = lookup<(1 << 30)>(neighbor_indices, &in[0]);

            if (center != 0) {
                out[outIdx] = horizontal_or(neighbors == 0) && !rng.nextInt<5>()
                    ? center == 4 ? 4 : 0
                    : center;
            } else if (horizontal_and(neighbors == 0)) {
                out[outIdx] = center;
            } else {
                int32_t next = 1;

                int32_t neighbor_values[4]; //store vector into normal c array to avoid extracting individual element 4 times
                neighbors.store(neighbor_values);

                for (int32_t limit = 0, i = 0; i < 4; i++) {
                    if (neighbor_values[i] != 0 && rng.nextInt_fast(MODULOS[limit++], limit) == 0) {
                        next = neighbor_values[i];
                    }
                }

                out[outIdx] = rng.nextInt<3>() == 0
                        ? next
                        : next == 4 ? 4 : 0;
            }
        }
    }
}
