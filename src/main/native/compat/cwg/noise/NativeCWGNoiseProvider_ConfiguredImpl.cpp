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

#include <iostream>

class state_t {
public:
    double _heightVariationFactor;
    double _specialHeightVariationFactorBelowAverageY;
    double _heightVariationOffset;
    double _heightFactor;
    double _heightOffset;

    int32_t _selectorNoiseSeed;
    uint32_t _selectorNoiseOctaves;
    double _selectorNoiseFrequencyX;
    double _selectorNoiseFrequencyY;
    double _selectorNoiseFrequencyZ;
    double _selectorNoiseScale;
    double _selectorNoiseFactor;
    double _selectorNoiseOffset;

    int32_t _lowNoiseSeed;
    uint32_t _lowNoiseOctaves;
    double _lowNoiseFrequencyX;
    double _lowNoiseFrequencyY;
    double _lowNoiseFrequencyZ;
    double _lowNoiseScale;
    double _lowNoiseFactor;
    double _lowNoiseOffset;

    int32_t _highNoiseSeed;
    uint32_t _highNoiseOctaves;
    double _highNoiseFrequencyX;
    double _highNoiseFrequencyY;
    double _highNoiseFrequencyZ;
    double _highNoiseScale;
    double _highNoiseFactor;
    double _highNoiseOffset;

    int32_t _depthNoiseSeed;
    uint32_t _depthNoiseOctaves;
    double _depthNoiseFrequencyX;
    double _depthNoiseFrequencyZ;
    double _depthNoiseScale;
    double _depthNoiseFactor;
    double _depthNoiseOffset;

    fp2::simd::type_vec<int32_t, 4>::TYPE _allNoiseSeeds;
    fp2::simd::type_vec<uint32_t, 4>::TYPE _allNoiseOctaves;
    fp2::simd::type_vec<double, 4>::TYPE _allNoiseFrequenciesX;
    fp2::simd::type_vec<double, 4>::TYPE _allNoiseFrequenciesY;
    fp2::simd::type_vec<double, 4>::TYPE _allNoiseFrequenciesZ;
    fp2::simd::type_vec<double, 4>::TYPE _allNoiseScales;
    fp2::simd::type_vec<double, 4>::TYPE _allNoiseFactors;
    fp2::simd::type_vec<double, 4>::TYPE _allNoiseOffsets;

    inline void setupVectorFields() {
        _allNoiseScales = fp2::simd::type_vec<double, 4>::TYPE(_selectorNoiseScale, _lowNoiseScale, _highNoiseScale, _depthNoiseScale);
        _allNoiseFactors = fp2::simd::type_vec<double, 4>::TYPE(_selectorNoiseFactor, _lowNoiseFactor, _highNoiseFactor, _depthNoiseFactor);
        _allNoiseOffsets = fp2::simd::type_vec<double, 4>::TYPE(_selectorNoiseOffset, _lowNoiseOffset, _highNoiseOffset, _depthNoiseOffset);
        _allNoiseFrequenciesX = fp2::simd::type_vec<double, 4>::TYPE(_selectorNoiseFrequencyX, _lowNoiseFrequencyX, _highNoiseFrequencyX, _depthNoiseFrequencyX);
        _allNoiseFrequenciesY = fp2::simd::type_vec<double, 4>::TYPE(_selectorNoiseFrequencyY, _lowNoiseFrequencyY, _highNoiseFrequencyY, 0.0d);
        _allNoiseFrequenciesZ = fp2::simd::type_vec<double, 4>::TYPE(_selectorNoiseFrequencyZ, _lowNoiseFrequencyZ, _highNoiseFrequencyZ, _depthNoiseFrequencyZ);
        _allNoiseSeeds = fp2::simd::type_vec<int32_t, 4>::TYPE(_selectorNoiseSeed, _lowNoiseSeed, _highNoiseSeed, _depthNoiseSeed);
        _allNoiseOctaves = fp2::simd::type_vec<uint32_t, 4>::TYPE(_selectorNoiseOctaves, _lowNoiseOctaves, _highNoiseOctaves, _depthNoiseOctaves);
    }

    template<bool USE_DEPTH_ARG> inline double generateAndMixAllNoise(double height, double variation, double depth, int32_t x, int32_t y, int32_t z) {
        //there are 3 values to be computed, so there's no reason to use more than 4 lanes
        auto noise_v = fp2::cwg::noise::octaves3dVarying<4>(x * _allNoiseFrequenciesX, y * _allNoiseFrequenciesY, z * _allNoiseFrequenciesZ, _allNoiseSeeds, _allNoiseOctaves);
        noise_v = noise_v * _allNoiseScales - 1.0d;
        noise_v = noise_v * _allNoiseFactors + _allNoiseOffsets;

        struct {
            double selector;
            double low;
            double high;
            double depth;
        } noise;
        noise_v.store((double*) &noise);

        if constexpr (USE_DEPTH_ARG) {
            noise.depth = depth;
        } else {
            noise.depth = processDepthNoise<false>(noise.depth);
        }

        height = height * _heightFactor + _heightOffset;
        variation = variation * _heightVariationFactor * (height > y ? _specialHeightVariationFactorBelowAverageY : 1.0d) + _heightVariationOffset;

        double d = fp2::cwg::noise::lerp(noise.low, noise.high, fp2::cwg::noise::clamp(noise.selector, 0.0d, 1.0d)) + noise.depth;
        d = d * variation + height;
        return d - fp2::cwg::noise::signum(variation) * y;
    }

    template<size_t VEC_LANES, bool USE_DEPTH_ARG> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE generateAndMixAllNoise(
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE height, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE variation, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE depth,
            typename fp2::simd::type_vec<double, VEC_LANES>::TYPE x, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE y, typename fp2::simd::type_vec<double, VEC_LANES>::TYPE z) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;

        DOUBLE selector = (fp2::cwg::noise::octaves3d<VEC_LANES>(x * _selectorNoiseFrequencyX, y * _selectorNoiseFrequencyY, z * _selectorNoiseFrequencyZ, _selectorNoiseSeed, _selectorNoiseOctaves)
                * _selectorNoiseScale - 1.0d) * _selectorNoiseFactor + _selectorNoiseOffset;
        DOUBLE low = (fp2::cwg::noise::octaves3d<VEC_LANES>(x * _lowNoiseFrequencyX, y * _lowNoiseFrequencyY, z * _lowNoiseFrequencyZ, _lowNoiseSeed, _lowNoiseOctaves)
                * _lowNoiseScale - 1.0d) * _lowNoiseFactor + _lowNoiseOffset;
        DOUBLE high = (fp2::cwg::noise::octaves3d<VEC_LANES>(x * _highNoiseFrequencyX, y * _highNoiseFrequencyY, z * _highNoiseFrequencyZ, _highNoiseSeed, _highNoiseOctaves)
                * _highNoiseScale - 1.0d) * _highNoiseFactor + _highNoiseOffset;

        if constexpr (!USE_DEPTH_ARG) {
            depth = processDepthNoise<VEC_LANES>(fp2::cwg::noise::octaves2d<VEC_LANES>(x * _depthNoiseFrequencyX, z * _depthNoiseFrequencyZ, _depthNoiseSeed, _depthNoiseOctaves));
        }

        height = height * _heightFactor + _heightOffset;
        variation = if_mul(height > y, variation * _heightVariationFactor, _specialHeightVariationFactorBelowAverageY) + _heightVariationOffset;

        DOUBLE d = fp2::cwg::noise::lerp(low, high, max(min(selector, 1.0d), 0.0d)) + depth;
        d = d * variation + height;
        return d - select(variation == 0.0d, 0.0d, sign_combine(1.0d, variation)) * y;
    }

    template<bool SCALE = true> inline double processDepthNoise(double depth) {
        if constexpr (SCALE) {
            depth = depth * _depthNoiseScale - 1.0d;
            depth = depth * _depthNoiseFactor + _depthNoiseOffset;
        }
        depth *= depth < 0.0d ? -0.9d : 3.0d;
        depth -= 2.0d;
        depth = fp2::cwg::noise::clamp(depth * (depth < 0.0d ? 5.0d / 28.0d : 0.125d), -5.0d / 14.0d, 0.125d);
        depth *= 0.2d * 17.0d / 64.0d;
        return depth;
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE processDepthNoise(typename fp2::simd::type_vec<double, VEC_LANES>::TYPE depth) {
        depth = depth * _depthNoiseScale - 1.0d;
        depth = depth * _depthNoiseFactor + _depthNoiseOffset;
        depth *= select(depth < 0.0d, -0.9d, 3.0d);
        depth -= 2.0d;
        depth = min(max(depth * select(depth < 0.0d, 5.0d / 28.0d, 0.125d), -5.0d / 14.0d), 0.125d);
        depth *= 0.2d * 17.0d / 64.0d;
        return depth;
    }

    inline double generateDepthSingle(int32_t x, int32_t z) {
        constexpr size_t VEC_LANES = fp2::simd::LANES_32AND64;

        return processDepthNoise(fp2::cwg::noise::octaves2dPoint<VEC_LANES>(x * _depthNoiseFrequencyX, z * _depthNoiseFrequencyZ, _depthNoiseSeed, _depthNoiseOctaves));
    }

    inline void generateDepth2d(fp2::pinned_double_array& out, int32_t baseX, int32_t baseZ, int32_t level, int32_t sizeX, int32_t sizeZ) {
        constexpr size_t VEC_LANES = fp2::simd::LANES_32AND64;

        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
        using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
        using INT_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::INT::BOOL;

        if ((sizeZ < VEC_LANES && sizeX != 1)) { //if sizeZ is less than the number of vector lanes, we can't do vectorized multi-dimensional iteration in SIMD
            for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
                for (int32_t dz = 0; dz < sizeZ; dz++, i++) {
                    out[i] = processDepthNoise(fp2::cwg::noise::octaves2dPoint<VEC_LANES>((baseX + (dx << level)) * _depthNoiseFrequencyX, (baseZ + (dz << level)) * _depthNoiseFrequencyX, _depthNoiseSeed, _depthNoiseOctaves));
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
                processDepthNoise<VEC_LANES>(fp2::cwg::noise::octaves2d<VEC_LANES>(to_double(x) * _depthNoiseFrequencyX, to_double(z) * _depthNoiseFrequencyZ, _depthNoiseSeed, _depthNoiseOctaves))
                        .store(&out[index]);

                //increment z coordinates, resetting them and incrementing x if they reach the maximum value
                z += stepZ;
                INT_MASK ge = z >= maxZ;
                z = if_sub(ge, z, resetZ);
                x = if_add(ge, x, stepX);
            }

            if (index < totalCount) { //the number of samples remaining are less than the number of vector lanes, let's finish 'em up
                //x and z are already set up correctly
                processDepthNoise<VEC_LANES>(fp2::cwg::noise::octaves2d<VEC_LANES>(to_double(x) * _depthNoiseFrequencyX, to_double(z) * _depthNoiseFrequencyZ, _depthNoiseSeed, _depthNoiseOctaves))
                        .store_partial(totalCount & (VEC_LANES - 1), &out[index]);
            }
        }
    }

    template<size_t VEC_LANES> inline typename fp2::simd::type_vec<double, VEC_LANES>::TYPE loadWithWrap(
            fp2::pinned_double_array &arr, typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE i) {
        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_BOOL = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
        using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
        using INT_BOOL = typename fp2::simd::type_vec<double, VEC_LANES>::INT::BOOL;
        using LONG = typename fp2::simd::type_vec<double, VEC_LANES>::LONG::TYPE;

        //using intrinsics here because vectorclass doesn't have a way to lookup 64-bit values with 32-bit indices
        if constexpr (INSTRSET >= 8 && VEC_LANES == 4) { //AVX2
            return DOUBLE(_mm256_i32gather_pd(&arr[0], i, 8));
        } else if constexpr (INSTRSET >= 8 && VEC_LANES == 8) { //AVX512
            return DOUBLE(_mm512_i32gather_pd(&arr[0], i, 8));
        } else { //AVX or SSE
            static const INT INCREMENT = fp2::simd::increment<INT>();

            int32_t _i[VEC_LANES];
            i.store(&_i[0]);

            INT_BOOL equals = (i - _i[0]) == INCREMENT;
            if (horizontal_and(equals)) { //indices are sequential
                return DOUBLE().load(&arr[_i[0]]);
            } else { //slow non-sequential lookup
                return lookup<(1 << 30)>(extend(i), &arr[0]);
            }
        }
    }

    template<bool USE_DEPTH_ARG> inline void generate3d(
            fp2::pinned_double_array& height, fp2::pinned_double_array& variation, fp2::pinned_double_array& depth, fp2::pinned_double_array& out,
            int32_t baseX, int32_t baseY, int32_t baseZ, int32_t level, int32_t sizeX, int32_t sizeY, int32_t sizeZ) {
        constexpr size_t VEC_LANES = fp2::simd::LANES_32AND64;

        using DOUBLE = typename fp2::simd::type_vec<double, VEC_LANES>::TYPE;
        using DOUBLE_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::BOOL;
        using INT = typename fp2::simd::type_vec<double, VEC_LANES>::INT::TYPE;
        using INT_MASK = typename fp2::simd::type_vec<double, VEC_LANES>::INT::BOOL;

        if (sizeZ < VEC_LANES && (sizeY != 1 || sizeX != 1)) { //if sizeZ is less than the number of vector lanes, we can't do vectorized multi-dimensional iteration in SIMD
            for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
                for (int32_t dy = 0; dy < sizeY; dy++) {
                    for (int32_t inIdx = dx * sizeZ, dz = 0; dz < sizeZ; dz++, inIdx++, i++) {
                        out[i] = generateAndMixAllNoise<USE_DEPTH_ARG>(
                                height[inIdx], variation[inIdx], USE_DEPTH_ARG ? depth[inIdx] : 0.0d,
                                baseX + (dx << level), baseY + (dy << level), baseZ + (dz << level));
                    }
                }
            }
        } else {
            static const INT INCREMENT = fp2::simd::increment<INT>();

            INT x = baseX;
            INT y = baseY;
            INT z = baseZ + (INCREMENT << level);
            INT readIndex = INCREMENT;

            const INT maxZ = baseZ + (sizeZ << level);
            const INT stepZ = (int32_t) VEC_LANES << level;
            const INT resetZ = sizeZ << level;
            const INT stepXY = 1 << level;
            const INT maxY = baseY + (sizeY << level);
            const INT resetY = sizeY << level;
            const INT stepReadIndex = (int32_t) VEC_LANES;

            const size_t totalCount = sizeX * sizeY * sizeZ;
            size_t index = 0;

            for (; index < (totalCount & ~(VEC_LANES - 1)); index += VEC_LANES) {
                generateAndMixAllNoise<VEC_LANES, USE_DEPTH_ARG>(
                        loadWithWrap<VEC_LANES>(height, readIndex),
                        loadWithWrap<VEC_LANES>(variation, readIndex),
                        USE_DEPTH_ARG ? loadWithWrap<VEC_LANES>(depth, readIndex): 0.0d,
                        to_double(x), to_double(y), to_double(z)).store(&out[index]);

                //increment z coordinates, resetting them and incrementing y if they reach the maximum value
                z += stepZ;
                readIndex += stepReadIndex;
                INT_MASK ge = z >= maxZ;
                z = if_sub(ge, z, resetZ);
                readIndex = if_sub(ge, readIndex, sizeZ);

                //increment y coordinates, resetting them and incrementing x if they reach the maximum value
                y = if_add(ge, y, stepXY);
                ge = y >= maxY;
                y = if_sub(ge, y, resetY);
                x = if_add(ge, x, stepXY);
                readIndex = if_add(ge, readIndex, sizeZ);
            }

            if (index < totalCount) { //the number of samples remaining are less than the number of vector lanes, let's finish 'em up
                //x and z are already set up correctly
                size_t remaining = totalCount & (VEC_LANES - 1);
                size_t baseIndex = readIndex[0];
                generateAndMixAllNoise<VEC_LANES, USE_DEPTH_ARG>(
                        DOUBLE().load_partial(remaining, &height[baseIndex]),
                        DOUBLE().load_partial(remaining, &variation[baseIndex]),
                        USE_DEPTH_ARG ? DOUBLE().load_partial(remaining, &depth[baseIndex]) : 0.0d,
                        to_double(x), to_double(y), to_double(z)).store_partial(remaining, &out[index]);
            }
        }
    }

    inline double generateSingle(double height, double variation, int32_t x, int32_t y, int32_t z) {
        return generateAndMixAllNoise<false>(height, variation, 0.0d, x, y, z);
    }

    inline double generateSingle(double height, double variation, double depth, int32_t x, int32_t y, int32_t z) {
        return generateAndMixAllNoise<true>(height, variation, depth, x, y, z);
    }
};

FP2_JNI(jlong, NativeCWGNoiseProvider_00024ConfiguredImpl, createState0) (JNIEnv* env, jclass cla,
        jdouble heightVariationFactor, jdouble specialHeightVariationFactorBelowAverageY, jdouble heightVariationOffset, jdouble heightFactor, jdouble heightOffset,
        jdouble selectorNoiseFactor, jdouble selectorNoiseOffset, jdouble selectorNoiseFrequencyX, jdouble selectorNoiseFrequencyY, jdouble selectorNoiseFrequencyZ, jint selectorNoiseSeed, jint selectorNoiseOctaves, jdouble selectorNoiseScale,
        jdouble lowNoiseFactor, jdouble lowNoiseOffset, jdouble lowNoiseFrequencyX, jdouble lowNoiseFrequencyY, jdouble lowNoiseFrequencyZ, jint lowNoiseSeed, jint lowNoiseOctaves, jdouble lowNoiseScale,
        jdouble highNoiseFactor, jdouble highNoiseOffset, jdouble highNoiseFrequencyX, jdouble highNoiseFrequencyY, jdouble highNoiseFrequencyZ, jint highNoiseSeed, jint highNoiseOctaves, jdouble highNoiseScale,
        jdouble depthNoiseFactor, jdouble depthNoiseOffset, jdouble depthNoiseFrequencyX, jdouble depthNoiseFrequencyZ, jint depthNoiseSeed, jint depthNoiseOctaves, jdouble depthNoiseScale) {
    state_t* state = new state_t;

    state->_heightVariationFactor = heightVariationFactor;
    state->_specialHeightVariationFactorBelowAverageY = specialHeightVariationFactorBelowAverageY;
    state->_heightVariationOffset = heightVariationOffset;
    state->_heightFactor = heightFactor;
    state->_heightOffset = heightOffset;

    state->_depthNoiseFactor = depthNoiseFactor;
    state->_depthNoiseOffset = depthNoiseOffset;
    state->_depthNoiseFrequencyX = depthNoiseFrequencyX;
    state->_depthNoiseFrequencyZ = depthNoiseFrequencyZ;
    state->_depthNoiseSeed = depthNoiseSeed;
    state->_depthNoiseOctaves = (uint32_t) depthNoiseOctaves;
    state->_depthNoiseScale = depthNoiseScale;

    state->_selectorNoiseFactor = selectorNoiseFactor;
    state->_selectorNoiseOffset = selectorNoiseOffset;
    state->_selectorNoiseFrequencyX = selectorNoiseFrequencyX;
    state->_selectorNoiseFrequencyY = selectorNoiseFrequencyY;
    state->_selectorNoiseFrequencyZ = selectorNoiseFrequencyZ;
    state->_selectorNoiseSeed = selectorNoiseSeed;
    state->_selectorNoiseOctaves = (uint32_t) selectorNoiseOctaves;
    state->_selectorNoiseScale = selectorNoiseScale;

    state->_lowNoiseFactor = lowNoiseFactor;
    state->_lowNoiseOffset = lowNoiseOffset;
    state->_lowNoiseFrequencyX = lowNoiseFrequencyX;
    state->_lowNoiseFrequencyY = lowNoiseFrequencyY;
    state->_lowNoiseFrequencyZ = lowNoiseFrequencyZ;
    state->_lowNoiseSeed = lowNoiseSeed;
    state->_lowNoiseOctaves = (uint32_t) lowNoiseOctaves;
    state->_lowNoiseScale = lowNoiseScale;

    state->_highNoiseFactor = highNoiseFactor;
    state->_highNoiseOffset = highNoiseOffset;
    state->_highNoiseFrequencyX = highNoiseFrequencyX;
    state->_highNoiseFrequencyY = highNoiseFrequencyY;
    state->_highNoiseFrequencyZ = highNoiseFrequencyZ;
    state->_highNoiseSeed = highNoiseSeed;
    state->_highNoiseOctaves = (uint32_t) highNoiseOctaves;
    state->_highNoiseScale = highNoiseScale;

    state->setupVectorFields();

    return (jlong) state;
}

FP2_JNI(void, NativeCWGNoiseProvider_00024ConfiguredImpl, deleteState0) (JNIEnv* env, jclass cla,
        jlong _state) {
    delete (state_t*) _state;
}

FP2_JNI(void, NativeCWGNoiseProvider_00024ConfiguredImpl, generateDepth2d0) (JNIEnv* env, jobject obj,
        jdoubleArray _out, jint baseX, jint baseZ, jint level, jint sizeX, jint sizeZ, jlong _state) {
    fp2::pinned_double_array out(env, _out);
    ((state_t*) _state)->generateDepth2d(out, baseX, baseZ, level, sizeX, sizeZ);
}

FP2_JNI(jdouble, NativeCWGNoiseProvider_00024ConfiguredImpl, generateDepthSingle0) (JNIEnv* env, jobject obj,
        jint x, jint z, jlong _state) {
    return ((state_t*) _state)->generateDepthSingle(x, z);
}

FP2_JNI(jdouble, NativeCWGNoiseProvider_00024ConfiguredImpl, generateSingle0noDepth) (JNIEnv* env, jobject obj,
        jdouble height, jdouble variation, jint x, jint y, jint z, jlong _state) {
    return ((state_t*) _state)->generateSingle(height, variation, x, y, z);
}

FP2_JNI(void, NativeCWGNoiseProvider_00024ConfiguredImpl, generate3d0noDepth) (JNIEnv* env, jobject obj,
        jdoubleArray _height, jdoubleArray _variation, jdoubleArray _out, jint baseX, jint baseY, jint baseZ, jint level, jint sizeX, jint sizeY, jint sizeZ, jlong _state) {
    fp2::pinned_double_array height(env, _height);
    fp2::pinned_double_array variation(env, _variation);
    fp2::pinned_double_array out(env, _out);
    ((state_t*) _state)->generate3d<false>(height, variation, variation, out, baseX, baseY, baseZ, level, sizeX, sizeY, sizeZ);
}

FP2_JNI(void, NativeCWGNoiseProvider_00024ConfiguredImpl, generate3d0depth) (JNIEnv* env, jobject obj,
        jdoubleArray _height, jdoubleArray _variation, jdoubleArray _depth, jdoubleArray _out, jint baseX, jint baseY, jint baseZ, jint level, jint sizeX, jint sizeY, jint sizeZ, jlong _state) {
    fp2::pinned_double_array height(env, _height);
    fp2::pinned_double_array variation(env, _variation);
    fp2::pinned_double_array depth(env, _depth);
    fp2::pinned_double_array out(env, _out);
    ((state_t*) _state)->generate3d<true>(height, variation, depth, out, baseX, baseY, baseZ, level, sizeX, sizeY, sizeZ);
}

FP2_JNI(jdouble, NativeCWGNoiseProvider_00024ConfiguredImpl, generateSingle0depth) (JNIEnv* env, jobject obj,
        jdouble height, jdouble variation, jdouble depth, jint x, jint y, jint z, jlong _state) {
    return ((state_t*) _state)->generateSingle(height, variation, depth, x, y, z);
}
