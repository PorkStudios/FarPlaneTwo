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

#ifndef CWG_NOISE_H
#define CWG_NOISE_H

#include <fp2.h>
#include <fp2/simd.h>

#include <array>
#include <cassert>
#include <immintrin.h>

namespace FP2_ROOT_NAMESPACE { namespace fp2::cwg::noise {
    /**
     * An array of random vectors used for noise generation. This corresponds directly to the
     * com.flowpowered.noise.Utils.RANDOM_VECTORS double[] on the Java side.
     *
     * In order to minimize CPU cache utilization and improve lookup performance, the original vectors are packed
     * into 64-bit integers, such that each vector component is stored using only 16 bits. This works because CWG
     * changes the RANDOM_VECTORS array to only contain 0, 0.5 and -0.5, all of which differ only in the upper 16 bits
     * (and all the lower bit are 0) when represented as double-precision IEEE 745 floating-point values.
     *
     * This means that obtaining three SIMD vectors containing the x,y,z components of the random vectors referenced by
     * a SIMD vector of random vector indices can be implemented using a single gathering load and some simple
     * shifting+masking. In fact, clang (LLVM) is able to optimize it into a single gathering load followed by three
     * vector shuffles (one shuffle for each of the x,y,z components).
     */
    __attribute__((aligned(sizeof(jlong) * 256))) //align to ensure the table doesn't go over a page boundary
    inline jlong RANDOM_VECTORS[256];

    constexpr double raw_to_double(uint64_t in) {
        const union {
            uint64_t i;
            double d;
        } u = {in};
        return u.d;
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE makeInt32Range(
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE n) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;

        DOUBLE nn = n + n;
        DOUBLE magic0 = raw_to_double(0x3E10000000000000);
        DOUBLE magic1 = raw_to_double(0x41D0000000000000);

        return select(abs(n) >= 1073741824.0, nn - truncate(nn * magic0) * magic1 - sign_combine(magic1, n), n);

        // equivalent code:
        /*double d0 = n, d1, d2;
        if (n >= (d1 = 1073741824.0)) {
            d0 = d0 + d0;
            d1 = raw_to_double(0x3E10000000000000);
            d1 = d0 * d1;
            d1 = trunc(d1); // frintz d1, d1
            d2 = raw_to_double(0x41D0000000000000);
            d1 = d1 * d2;
            d0 = d0 - d1;
            d0 = d0 - d2;
        } else if (n <= (d1 = -1073741824.0)) {
            d0 = d0 + d0;
            d1 = raw_to_double(0x3E10000000000000);
            d1 = d0 * d1;
            d1 = trunc(d1); //frintz d1, d1
            d2 = raw_to_double(0x41D0000000000000);
            d1 = d1 * d2;
            d0 = d0 - d1;
            d0 = d0 + d2;
        }
        return d0;*/

        //equivalent code:
        /*if (n >= 1073741824.0) {
            return (fmod(2.0 * n, 1073741824.0)) - 1073741824.0;
        } else if (n <= -1073741824.0) {
            return (fmod(2.0 * n, 1073741824.0)) + 1073741824.0;
        } else {
            return n;
        }*/
    }

    template<typename T> constexpr T min(T a, T b) {
        return a < b ? a : b;
    }

    template<typename T> constexpr T max(T a, T b) {
        return a > b ? a : b;
    }

    template<typename T> constexpr T clamp(T val, T min, T max) {
        return val > min ? val < max ? val : max : min;
    }

    template<typename T> constexpr T abs(T a) {
        return a < 0 ? -a : a;
    }

    template<typename T> constexpr T copysign(T src, T dst) {
        return src < 0 ? -abs(dst) : abs(dst);
    }

    template<typename T> constexpr T signum(T n) {
        return n < 0 ? -1 : n > 0 ? 1 : 0;
    }

    template<typename T> constexpr T sCurve3(T a) {
        return a * a * (3.0 - 2.0 * a);
    }

    template<typename T> constexpr T vectorIndex(T ix, T iy, T iz, T seed) {
        T vectorIndex = 1619 * ix + 31337 * iy + 6971 * iz + 1013 * seed;
        return (vectorIndex ^ (vectorIndex >> 8)) & 0xFF;
    }

    template<typename T> constexpr T lerp(T n0, T n1, T a) {
        return n0 + (n1 - n0) * a;
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE gradientNoise3d(
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE fx, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE fy, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE fz,
            typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE ix, typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE iy, typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE iz,
            typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE seed) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
        using LONG = typename fp2::simd::type_vec<double, VEC_LANES>::LONG::TYPE;

        INT vi = vectorIndex(ix, iy, iz, seed);
        LONG vector = lookup<256>(extend(vi), RANDOM_VECTORS);

        //clang is able to optimize this into a sequence of shuffles
        DOUBLE xvGradient = reinterpret_d(((vector >> 48) & 0xFFFF) << 48);
        DOUBLE yvGradient = reinterpret_d(((vector >> 32) & 0xFFFF) << 48);
        DOUBLE zvGradient = reinterpret_d(((vector >> 16) & 0xFFFF) << 48);

        return xvGradient * fx + yvGradient * fy + zvGradient * fz + 0.5;
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE noise3d(
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE x, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE y, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE z,
            typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE seed) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;

        //floor coordinates
        DOUBLE floorX = if_sub(x == 0.0, floor(x), 1.0);
        DOUBLE floorY = if_sub(y == 0.0, floor(y), 1.0);
        DOUBLE floorZ = if_sub(z == 0.0, floor(z), 1.0);

        //convert floored coordinates to ints to get the integer coordinates at the corners
        INT x0 = round_to_int32(floorX);
        INT x1 = x0 + 1;
        INT y0 = round_to_int32(floorY);
        INT y1 = y0 + 1;
        INT z0 = round_to_int32(floorZ);
        INT z1 = z0 + 1;

        //get coordinate's fractional parts
        DOUBLE fx = x - floorX;
        DOUBLE fy = y - floorY;
        DOUBLE fz = z - floorZ;

        //compute gradient vectors at each corner
        DOUBLE n000 = gradientNoise3d<VEC_LANES>(fx, fy, fz, x0, y0, z0, seed);
        DOUBLE n001 = gradientNoise3d<VEC_LANES>(fx, fy, fz - 1.0, x0, y0, z1, seed);
        DOUBLE n010 = gradientNoise3d<VEC_LANES>(fx, fy - 1.0, fz, x0, y1, z0, seed);
        DOUBLE n011 = gradientNoise3d<VEC_LANES>(fx, fy - 1.0, fz - 1.0, x0, y1, z1, seed);
        DOUBLE n100 = gradientNoise3d<VEC_LANES>(fx - 1.0, fy, fz, x1, y0, z0, seed);
        DOUBLE n101 = gradientNoise3d<VEC_LANES>(fx - 1.0, fy, fz - 1.0, x1, y0, z1, seed);
        DOUBLE n110 = gradientNoise3d<VEC_LANES>(fx - 1.0, fy - 1.0, fz, x1, y1, z0, seed);
        DOUBLE n111 = gradientNoise3d<VEC_LANES>(fx - 1.0, fy - 1.0, fz - 1.0, x1, y1, z1, seed);

        //smooth fractional coordinates
        DOUBLE xs = sCurve3(fx);
        DOUBLE ys = sCurve3(fy);
        DOUBLE zs = sCurve3(fz);

        //lerp between corners to get a single output sample
        DOUBLE n000_n100 = lerp(n000, n100, xs);
        DOUBLE n001_n101 = lerp(n001, n101, xs);
        DOUBLE n010_n110 = lerp(n010, n110, xs);
        DOUBLE n011_n111 = lerp(n011, n111, xs);

        DOUBLE n00x_n01x = lerp(n000_n100, n010_n110, ys);
        DOUBLE n10x_n11x = lerp(n001_n101, n011_n111, ys);

        return lerp(n00x_n01x, n10x_n11x, zs);
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE octaves3d(
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE x, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE y, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE z,
            typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE seed, size_t octaves) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;

        static const DOUBLE INITIAL_PERSISTENCE = 1.0;
        static const DOUBLE PERSISTENCE = 0.5;

        static const DOUBLE LANCULARITY = 2.0;

        DOUBLE value = 0.0;
        DOUBLE persistence = INITIAL_PERSISTENCE;

        for (size_t curOctave = 0; curOctave < octaves; curOctave++, persistence *= PERSISTENCE, x *= LANCULARITY, y *= LANCULARITY, z *= LANCULARITY) {
            DOUBLE nx = makeInt32Range<VEC_LANES>(x);
            DOUBLE ny = makeInt32Range<VEC_LANES>(y);
            DOUBLE nz = makeInt32Range<VEC_LANES>(z);

            value += noise3d<VEC_LANES>(nx, ny, nz, seed + curOctave) * persistence;
        }

        return value;
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE octaves3dVarying(
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE x, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE y, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE z,
            typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE seed, typename fp2::simd::type_vec<double, VEC_LANES>::UINT::TYPE octaves) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
        using ULONG = typename fp2::simd::type_vec<double, VEC_LANES>::ULONG::TYPE;

        static const DOUBLE INITIAL_PERSISTENCE = 1.0;
        static const DOUBLE PERSISTENCE = 0.5;

        static const DOUBLE LANCULARITY = 2.0;

        DOUBLE value = 0.0;
        DOUBLE persistence = INITIAL_PERSISTENCE;

        for (size_t curOctave = 0, maxOctaves = horizontal_max(octaves); curOctave < maxOctaves; curOctave++, persistence *= PERSISTENCE, x *= LANCULARITY, y *= LANCULARITY, z *= LANCULARITY) {
            DOUBLE nx = makeInt32Range<VEC_LANES>(x);
            DOUBLE ny = makeInt32Range<VEC_LANES>(y);
            DOUBLE nz = makeInt32Range<VEC_LANES>(z);

            value = if_add(ULONG(curOctave) < extend(octaves), value, noise3d<VEC_LANES>(nx, ny, nz, seed + curOctave) * persistence);
        }

        return value;
    }

    template<size_t VEC_LANES> inline double octaves3dPoint(double _x, double _y, double _z, int32_t seed, size_t octaves) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
        using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
        using ULONG = typename fp2::simd::type_vec<double, VEC_LANES>::ULONG::TYPE;

        static const DOUBLE INITIAL_LACUNARITY = fp2::simd::increment_shift<DOUBLE>();
        static const DOUBLE LANCULARITY = 1 << VEC_LANES;

        static const DOUBLE INITIAL_PERSISTENCE = 1.0 / fp2::simd::increment_shift<DOUBLE>();
        static const DOUBLE PERSISTENCE = 1.0 / (1 << VEC_LANES);

        static const INT INCREMENT = fp2::simd::increment<INT>();
        static const ULONG INCREMENT_L = fp2::simd::increment<ULONG>();

        DOUBLE x = _x * INITIAL_LACUNARITY;
        DOUBLE y = _y * INITIAL_LACUNARITY;
        DOUBLE z = _z * INITIAL_LACUNARITY;

        DOUBLE value = 0.0;
        DOUBLE persistence = INITIAL_PERSISTENCE;

        size_t curOctave = 0;
        for (; curOctave < (octaves & ~(VEC_LANES - 1)); curOctave += VEC_LANES, persistence *= PERSISTENCE, x *= LANCULARITY, y *= LANCULARITY, z *= LANCULARITY) {
            DOUBLE nx = makeInt32Range<VEC_LANES>(x);
            DOUBLE ny = makeInt32Range<VEC_LANES>(y);
            DOUBLE nz = makeInt32Range<VEC_LANES>(z);

            value += noise3d<VEC_LANES>(nx, ny, nz, seed + curOctave + INCREMENT) * persistence;
        }

        if (curOctave < octaves) { //there are some number of octaves left, let's do them with a mask
            DOUBLE nx = makeInt32Range<VEC_LANES>(x);
            DOUBLE ny = makeInt32Range<VEC_LANES>(y);
            DOUBLE nz = makeInt32Range<VEC_LANES>(z);

            value = if_add(curOctave + INCREMENT_L < octaves, value, noise3d<VEC_LANES>(nx, ny, nz, seed + curOctave + INCREMENT) * persistence);
        }

        return horizontal_add(value);
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE octaves2d(
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE x, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE z,
            typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE seed, size_t octaves) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;

        static const DOUBLE INITIAL_PERSISTENCE = 1.0;
        static const DOUBLE PERSISTENCE = 0.5;

        static const DOUBLE LANCULARITY = 2.0;

        DOUBLE value = 0.0;
        DOUBLE persistence = INITIAL_PERSISTENCE;

        for (size_t curOctave = 0; curOctave < octaves; curOctave++, persistence *= PERSISTENCE, x *= LANCULARITY, z *= LANCULARITY) {
            DOUBLE nx = makeInt32Range<VEC_LANES>(x);
            DOUBLE nz = makeInt32Range<VEC_LANES>(z);

            value += noise3d<VEC_LANES>(nx, 0.0, nz, seed + curOctave) * persistence;
        }

        return value;
    }

    template<size_t VEC_LANES> inline double octaves2dPoint(double _x, double _z, int32_t seed, size_t octaves) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
        using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
        using ULONG = typename fp2::simd::type_vec<double, VEC_LANES>::ULONG::TYPE;

        static const DOUBLE INITIAL_LACUNARITY = fp2::simd::increment_shift<DOUBLE>();
        static const DOUBLE LANCULARITY = 1 << VEC_LANES;

        static const DOUBLE INITIAL_PERSISTENCE = 1.0 / fp2::simd::increment_shift<DOUBLE>();
        static const DOUBLE PERSISTENCE = 1.0 / (1 << VEC_LANES);

        static const INT INCREMENT = fp2::simd::increment<INT>();
        static const ULONG INCREMENT_L = fp2::simd::increment<ULONG>();

        DOUBLE x = _x * INITIAL_LACUNARITY;
        DOUBLE z = _z * INITIAL_LACUNARITY;

        DOUBLE value = 0.0;
        DOUBLE persistence = INITIAL_PERSISTENCE;

        size_t curOctave = 0;
        for (; curOctave < (octaves & ~(VEC_LANES - 1)); curOctave += VEC_LANES, persistence *= PERSISTENCE, x *= LANCULARITY, z *= LANCULARITY) {
            DOUBLE nx = makeInt32Range<VEC_LANES>(x);
            DOUBLE nz = makeInt32Range<VEC_LANES>(z);

            value += noise3d<VEC_LANES>(nx, 0.0, nz, seed + curOctave + INCREMENT) * persistence;
        }

        if (curOctave < octaves) { //there are some number of octaves left, let's do them with a mask
            DOUBLE nx = makeInt32Range<VEC_LANES>(x);
            DOUBLE nz = makeInt32Range<VEC_LANES>(z);

            value = if_add(curOctave + INCREMENT_L < octaves, value, noise3d<VEC_LANES>(nx, 0.0, nz, seed + curOctave + INCREMENT) * persistence);
        }

        return horizontal_add(value);
    }
}}

#endif //CWG_NOISE_H
