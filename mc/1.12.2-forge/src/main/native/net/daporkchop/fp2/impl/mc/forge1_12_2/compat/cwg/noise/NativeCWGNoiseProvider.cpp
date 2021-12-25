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

#include "cwg_noise.h"

FP2_JNI(void, NativeCWGNoiseProvider, setRandomVectors)(JNIEnv* env, jobject obj,
        jfloatArray random_vectors) FP2_JNI_HEAD
    fp2::cwg::noise::setRandomVectors(env, random_vectors);
FP2_JNI_TAIL

FP2_JNI(void, NativeCWGNoiseProvider, generate3d)(JNIEnv* env, jobject obj,
        jdoubleArray _out, jint baseX, jint baseY, jint baseZ, jint level, jdouble freqX, jdouble freqY, jdouble freqZ, jint sizeX, jint sizeY, jint sizeZ, jint seed, jint octaves, jdouble scale) {
    constexpr size_t VEC_LANES = fp2::simd::LANES_32AND64;

    using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
    using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
    using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
    using INT_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::INT::BOOL;

    fp2::pinned_double_array out(env, _out);

    if (!(octaves & (VEC_LANES - 1)) //if the number of octaves is a multiple of the number of vector lanes, there will be no wasted samples if we vectorize on octaves
            || (sizeZ < VEC_LANES && (sizeY != 1 || sizeX != 1))) { //if sizeZ is less than the number of vector lanes, we can't do vectorized multi-dimensional iteration in SIMD
        for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
            for (int32_t dy = 0; dy < sizeY; dy++) {
                for (int32_t dz = 0; dz < sizeZ; dz++, i++) {
                    out[i] = fp2::cwg::noise::octaves3dPoint<VEC_LANES>((baseX + (dx << level)) * freqX, (baseY + (dy << level)) * freqY, (baseZ + (dz << level)) * freqZ, seed, octaves) * scale - 1.0;
                }
            }
        }
    } else {
        static const INT INCREMENT = fp2::simd::increment<INT>();

        INT x = baseX;
        INT y = baseY;
        INT z = baseZ + (INCREMENT << level);

        const INT maxZ = baseZ + (sizeZ << level);
        const INT stepZ = (int32_t) VEC_LANES << level;
        const INT resetZ = sizeZ << level;
        const INT stepXY = 1 << level;
        const INT maxY = baseY + (sizeY << level);
        const INT resetY = sizeY << level;

        const size_t totalCount = sizeX * sizeY * sizeZ;
        size_t index = 0;

        for (; index < (totalCount & ~(VEC_LANES - 1)); index += VEC_LANES) {
            (fp2::cwg::noise::octaves3d<VEC_LANES>(to_double(x) * freqX, to_double(y) * freqY, to_double(z) * freqZ, seed, octaves) * scale - 1.0).store(&out[index]);

            //increment z coordinates, resetting them and incrementing y if they reach the maximum value
            z += stepZ;
            INT_MASK ge = z >= maxZ;
            z = if_sub(ge, z, resetZ);

            //increment y coordinates, resetting them and incrementing x if they reach the maximum value
            y = if_add(ge, y, stepXY);
            ge = y >= maxY;
            y = if_sub(ge, y, resetY);
            x = if_add(ge, x, stepXY);
        }

        if (index < totalCount) { //the number of samples remaining are less than the number of vector lanes, let's finish 'em up
            //x and z are already set up correctly
            (fp2::cwg::noise::octaves3d<VEC_LANES>(to_double(x) * freqX, to_double(y) * freqY, to_double(z) * freqZ, seed, octaves) * scale - 1.0).store_partial(totalCount & (VEC_LANES - 1), &out[index]);
        }
    }
}

FP2_JNI(void, NativeCWGNoiseProvider, generate2d)(JNIEnv* env, jobject obj,
        jdoubleArray _out, jint baseX, jint baseZ, jint level, jdouble freqX, jdouble freqZ, jint sizeX, jint sizeZ, jint seed, jint octaves, jdouble scale) {
    constexpr size_t VEC_LANES = fp2::simd::LANES_32AND64;

    using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
    using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
    using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
    using INT_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::INT::BOOL;

    fp2::pinned_double_array out(env, _out);

    if (!(octaves & (VEC_LANES - 1)) //if the number of octaves is a multiple of the number of vector lanes, there will be no wasted samples if we vectorize on octaves
            || (sizeZ < VEC_LANES && sizeX != 1)) { //if sizeZ is less than the number of vector lanes, we can't do vectorized multi-dimensional iteration in SIMD
        for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
            for (int32_t dz = 0; dz < sizeZ; dz++, i++) {
                out[i] = fp2::cwg::noise::octaves2dPoint<VEC_LANES>((baseX + (dx << level)) * freqX, (baseZ + (dz << level)) * freqZ, seed, octaves) * scale - 1.0;
            }
        }
    } else {
        static const INT INCREMENT = fp2::simd::increment<INT>();

        INT x = baseX;
        INT z = baseZ + (INCREMENT << level);

        const INT maxZ = baseZ + (sizeZ << level);
        const INT stepZ = (int32_t) VEC_LANES << level;
        const INT resetZ = sizeZ << level;
        const INT stepX = 1 << level;

        const size_t totalCount = sizeX * sizeZ;
        size_t index = 0;

        for (; index < (totalCount & ~(VEC_LANES - 1)); index += VEC_LANES) {
            (fp2::cwg::noise::octaves2d<VEC_LANES>(to_double(x) * freqX, to_double(z) * freqZ, seed, octaves) * scale - 1.0).store(&out[index]);

            //increment z coordinates, resetting them and incrementing x if they reach the maximum value
            z += stepZ;
            INT_MASK ge = z >= maxZ;
            z = if_sub(ge, z, resetZ);
            x = if_add(ge, x, stepX);
        }

        if (index < totalCount) { //the number of samples remaining are less than the number of vector lanes, let's finish 'em up
            //x and z are already set up correctly
            (fp2::cwg::noise::octaves2d<VEC_LANES>(to_double(x) * freqX, to_double(z) * freqZ, seed, octaves) * scale - 1.0).store_partial(totalCount & (VEC_LANES - 1), &out[index]);
        }
    }
}

FP2_JNI(jdouble, NativeCWGNoiseProvider, generateSingle)(JNIEnv* env, jobject obj,
        jint x, jint y, jint z, jdouble freqX, jdouble freqY, jdouble freqZ, jint seed, jint octaves, jdouble scale) {
    constexpr size_t VEC_LANES = fp2::simd::LANES_32AND64;

    return fp2::cwg::noise::octaves3dPoint<VEC_LANES>(x * freqX, y * freqY, z * freqZ, seed, octaves) * scale - 1.0;
}
