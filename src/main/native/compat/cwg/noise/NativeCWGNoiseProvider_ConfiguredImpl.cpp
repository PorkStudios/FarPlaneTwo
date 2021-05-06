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

class state_t {
public:
    double _heightVariationFactor;
    double _specialHeightVariationFactorBelowAverageY;
    double _heightVariationOffset;
    double _heightFactor;
    double _heightOffset;

    double _depthNoiseFactor;
    double _depthNoiseOffset;
    double _depthNoiseFrequencyX;
    double _depthNoiseFrequencyZ;
    int32_t _depthNoiseSeed;
    uint32_t _depthNoiseOctaves;
    double _depthNoiseScale;

    double _selectorNoiseFactor;
    double _selectorNoiseOffset;
    double _selectorNoiseFrequencyX;
    double _selectorNoiseFrequencyY;
    double _selectorNoiseFrequencyZ;
    int32_t _selectorNoiseSeed;
    uint32_t _selectorNoiseOctaves;
    double _selectorNoiseScale;

    double _lowNoiseFactor;
    double _lowNoiseOffset;
    double _lowNoiseFrequencyX;
    double _lowNoiseFrequencyY;
    double _lowNoiseFrequencyZ;
    int32_t _lowNoiseSeed;
    uint32_t _lowNoiseOctaves;
    double _lowNoiseScale;

    double _highNoiseFactor;
    double _highNoiseOffset;
    double _highNoiseFrequencyX;
    double _highNoiseFrequencyY;
    double _highNoiseFrequencyZ;
    int32_t _highNoiseSeed;
    uint32_t _highNoiseOctaves;
    double _highNoiseScale;

    inline double processDepthNoise(double depth) {
        depth = depth * _depthNoiseScale - 1.0d;
        depth = depth * _depthNoiseFactor + _depthNoiseOffset;
        depth *= depth < 0.0d ? -0.9d : 3.0d;
        depth -= 2.0d;
        depth = fp2::clamp(depth * (depth < 0.0d ? 5.0d / 28.0d : 0.125d), -5.0d / 14.0d, 0.125d);
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

FP2_JNI(void, NativeCWGNoiseProvider_00024ConfiguredImpl, generateSingle0_depth) (JNIEnv* env, jobject obj,
        jlong seed, jint x, jint z, jint sizeX, jint sizeZ, jintArray _out, jintArray _in) {
}
