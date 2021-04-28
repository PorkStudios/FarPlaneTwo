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

FP2_JNI(void, NativeFastLayerIsland, getGrid0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out) {
    fp2::pinned_int_array out(env, _out);

    for (int32_t outIdx = 0, dx = 0; dx < sizeX; dx++) {
        for (int32_t dz = 0; dz < sizeZ; dz++, outIdx++) {
            fp2::biome::fastlayer::rng rng(seed, x + dx, z + dz);
            out[outIdx] = rng.nextInt<10>() == 0;
        }
    }

    if (x <= 0 && z <= 0 && x + sizeX >= 0 && z + sizeZ >= 0) { //(0,0) is always set to 1
        out[-x * sizeZ - z] = 1;
    }
}

FP2_JNI(void, NativeFastLayerIsland, multiGetGrids0) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint size, jint dist, jint count, jintArray _out) {
    fp2::pinned_int_array out(env, _out);

    for (int32_t outIdx = 0, gridX = 0; gridX < count; gridX++) {
        for (int32_t gridZ = 0; gridZ < count; gridZ++, outIdx += size * size) {
            int32_t gridBaseX = x + gridX * dist;
            int32_t gridBaseZ = z + gridZ * dist;
            for (int32_t i = outIdx, dx = 0; dx < size; dx++) {
                for (int32_t dz = 0; dz < size; dz++, i++) {
                    fp2::biome::fastlayer::rng rng(seed, gridBaseX + dx, gridBaseZ + dz);
                    out[i] = rng.nextInt<10>() == 0;
                }
            }

            if (gridBaseX <= 0 && gridBaseZ <= 0 && gridBaseX + size >= 0 && gridBaseZ + size >= 0) { //(0,0) is always set to 1
                out[outIdx + (-x * size - z)] = 1;
            }
        }
    }
}
