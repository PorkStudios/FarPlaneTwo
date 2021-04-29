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

#ifndef NATIVELAYER_NATIVEFASTLAYER_H
#define NATIVELAYER_NATIVEFASTLAYER_H

#include <fp2.h>

#include <lib/vectorclass-2.01.03/vectorclass.h>

namespace fp2::biome::fastlayer {
    /**
     * Faster re-implementation of the PRNG used in GenLayer.
     *
     * @author DaPorkchop_
     */
    template<typename T_64, typename T_32, T_32 (CAST_DOWN)(T_64), T_64 (CAST_UP)(T_32)> class base_rng {
    private:
        constexpr T_64 update(const T_64 state, const T_64 seed) {
            return state * (state * 6364136223846793005LL + 1442695040888963407LL) + seed;
        }

        constexpr T_64 start(const T_64 seed, const T_32 x, const T_32 z) {
            T_64 state = seed;
            state = update(state, CAST_UP(x));
            state = update(state, CAST_UP(z));
            state = update(state, CAST_UP(x));
            state = update(state, CAST_UP(z));
            return state;
        }

        const T_64 _seed;
        T_64 _state;

    public:
        base_rng(const T_64 seed, const T_32 x, const T_32 z):
            _seed(seed),
            _state(start(seed, x, z)) {}

        template<size_t COUNT = 1> inline void update() {
            for (size_t i = 0; i < COUNT; i++) {
                _state = update(_state, _seed);
            }
        }

        template<int32_t MAX> inline T_32 nextInt() {
            T_32 i;
            if constexpr ((MAX & (MAX - 1)) == 0) { //MAX is a power of two
                constexpr int32_t MASK = MAX - 1;
                i = CAST_DOWN((_state >> 24) & MASK);
            } else {
                i = CAST_DOWN((_state >> 24) % MAX);
                i += (i >> 31) & MAX; //equivalent to if (i < 0) { i += MAX; }
            }

            update(); //update PRNG state
            return i;
        }

        inline T_32 nextInt_fast(const fp2::fastmod_s64& fm, const T_32 max) {
            T_32 i = (T_32) ((_state >> 24) % fm);
            i += (i >> 31) & max; //equivalent to if (i < 0) { i += max; }

            update(); //update PRNG state
            return i;
        }

        inline T_32 nextInt(const T_32 max) {
            T_32 i;
            if (!(max & (max - 1))) {
                i = (T_32) (_state >> 24) & (max - 1);
            } else {
                i = (T_32) ((_state >> 24) % max);
                i += (i >> 31) & max; //equivalent to if (i < 0) { i += max; }
            }

            update(); //update PRNG state
            return i;
        }
    };

    using rng = base_rng<int64_t, int32_t, cast, cast>;

    enum padded_layer_mode {
        corners,
        sides,
        sides_final_two_reversed
    };

    /**
     * Base implementation of a layer which has an additional padding of one unit on each side.
     *
     * @param EVAL    a function which determines the output value based on the values at the center and neighbor points
     * @param CORNERS the mode to use for sampling neighbors
     * @author DaPorkchop_
     */
    template<int32_t(EVAL)(rng&, int32_t, Vec4i), padded_layer_mode MODE> class padded_layer {
        static const inline Vec4i OFFSETS_X = MODE == padded_layer_mode::corners
                ? Vec4i(-1, -1, 1, 1) : MODE == padded_layer_mode::sides ? Vec4i(-1, 0, 0, 1) : Vec4i(-1, 0, 1, 0);
        static const inline Vec4i OFFSETS_Z = MODE == padded_layer_mode::corners
                ? Vec4i(-1, 1, -1, 1) : MODE == padded_layer_mode::sides ? Vec4i(0, -1, 1, 0) : Vec4i(0, -1, 0, 1);

    public:
        inline void grid(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
            const int32_t inSizeX = sizeX + 2;
            const int32_t inSizeZ = sizeZ + 2;

            const Vec4i neighbor_offsets = OFFSETS_X * inSizeZ + OFFSETS_Z;

            fp2::pinned_int_array out(env, _out);
            fp2::pinned_int_array in(env, _in);

            for (int32_t outIdx = 0, dx = 0; dx < sizeX; dx++) {
                for (int32_t dz = 0; dz < sizeZ; dz++, outIdx++) {
                    const int32_t inIdx = (dx + 1) * inSizeZ + (dz + 1);
                    const Vec4i neighbors = lookup<(1 << 30)>(inIdx + neighbor_offsets, &in[0]);
                    const int32_t center = in[inIdx];

                    fp2::biome::fastlayer::rng rng(seed, x + dx, z + dz);

                    out[outIdx] = EVAL(rng, center, neighbors);
                }
            }
        }

        inline void grid_multi_combined(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t count, jintArray _out, jintArray _in) {
            const int32_t inSize = (dist * count) + 2;

            const Vec4i neighbor_offsets = OFFSETS_X * inSize + OFFSETS_Z;

            fp2::pinned_int_array out(env, _out);
            fp2::pinned_int_array in(env, _in);

            for (int32_t outIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++) {
                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, outIdx++) {
                            const int32_t inIdx = (gridX * dist + dx + 1) * inSize + (gridZ * dist + dz + 1);
                            const Vec4i neighbors = lookup<(1 << 30)>(inIdx + neighbor_offsets, &in[0]);
                            const int32_t center = in[inIdx];

                            fp2::biome::fastlayer::rng rng(seed, x + gridX * dist + dx, z + gridZ * dist + dz);

                            out[outIdx] = EVAL(rng, center, neighbors);
                        }
                    }
                }
            }
        }

        inline void grid_multi_individual(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t count, jintArray _out, jintArray _in) {
            const int32_t inSize = size + 2;

            const Vec4i neighbor_offsets = OFFSETS_X * inSize + OFFSETS_Z;

            fp2::pinned_int_array out(env, _out);
            fp2::pinned_int_array in(env, _in);

            for (int32_t outIdx = 0, inBase = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++, inBase += inSize * inSize) {
                    const int32_t baseX = x + gridX * dist;
                    const int32_t baseZ = z + gridZ * dist;

                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, outIdx++) {
                            const int32_t inIdx = inBase + (dx + 1) * inSize + (dz + 1);
                            const Vec4i neighbors = lookup<(1 << 30)>(inIdx + neighbor_offsets, &in[0]);
                            const int32_t center = in[inIdx];

                            fp2::biome::fastlayer::rng rng(seed, baseX + dx, baseZ + dz);

                            out[outIdx] = EVAL(rng, center, neighbors);
                        }
                    }
                }
            }
        }
    };

    /**
     * Base implementation of a layer which simply replaces a single value type in the output.
     *
     * @param EVAL a function which determines the output value based on the input value
     * @author DaPorkchop_
     */
    template<int32_t(EVAL)(rng&, int32_t)> class translation_layer {
    public:
        inline void grid(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _inout) {
            fp2::pinned_int_array inout(env, _inout);

            for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
                for (int32_t dz = 0; dz < sizeZ; dz++, i++) {
                    fp2::biome::fastlayer::rng rng(seed, x + dx, z + dz);

                    inout[i] = EVAL(rng, inout[i]);
                }
            }
        }

        inline void grid_multi(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t count, jintArray _inout) {
            fp2::pinned_int_array inout(env, _inout);

            for (int32_t i = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++) {
                    const int32_t baseX = x + gridX * dist;
                    const int32_t baseZ = z + gridZ * dist;

                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, i++) {
                            fp2::biome::fastlayer::rng rng(seed, baseX + dx, baseZ + dz);

                            inout[i] = EVAL(rng, inout[i]);
                        }
                    }
                }
            }
        }
    };

    /**
     * Base implementation of a layer which has no inputs.
     *
     * @param EVAL a function which determines the output value
     * @author DaPorkchop_
     */
    template<int32_t(EVAL)(rng&, int32_t, int32_t)> class source_layer {
    public:
        inline void grid(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out) {
            fp2::pinned_int_array out(env, _out);

            for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
                for (int32_t dz = 0; dz < sizeZ; dz++, i++) {
                    fp2::biome::fastlayer::rng rng(seed, x + dx, z + dz);

                    out[i] = EVAL(rng, x + dx, z + dz);
                }
            }
        }

        inline void grid_multi(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t count, jintArray _out) {
            fp2::pinned_int_array out(env, _out);

            for (int32_t i = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++) {
                    const int32_t baseX = x + gridX * dist;
                    const int32_t baseZ = z + gridZ * dist;

                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, i++) {
                            fp2::biome::fastlayer::rng rng(seed, baseX + dx, baseZ + dz);

                            out[i] = EVAL(rng, baseX + dx, baseZ + dz);
                        }
                    }
                }
            }
        }
    };
}

#endif //NATIVELAYER_NATIVEFASTLAYER_H
