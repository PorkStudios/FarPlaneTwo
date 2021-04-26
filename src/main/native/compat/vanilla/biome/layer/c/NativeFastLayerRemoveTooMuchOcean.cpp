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

FP2_JNI(void, NativeFastLayerRemoveTooMuchOcean, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
    fp2::pinned_int_array out(env, _out);
    fp2::pinned_int_array in(env, _in);

    const Vec4i neighbor_offsets(-1 * (sizeZ + 2) + 0, 0 * (sizeZ + 2) + -1, 0 * (sizeZ + 2) + 1, 1 * (sizeZ + 2) + 0);

    for (int32_t outIdx = 0, dx = 0; dx < sizeX; dx++) {
        for (int32_t inIdx = (dx + 1) * (sizeZ + 2) + 1, dz = 0; dz < sizeZ; inIdx++, dz++, outIdx++) {
            auto center = in[inIdx];

            out[outIdx] = center == 0
                        && horizontal_or(lookup<(1 << 30)>(inIdx + neighbor_offsets, &in[0])) == 0
                        && fp2::biome::fastlayer::rng(seed, x + dx, z + dz).nextInt<2>() == 0
                    ? 1
                    : center;
        }
    }
}
