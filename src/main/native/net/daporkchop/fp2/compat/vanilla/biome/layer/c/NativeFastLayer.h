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
#include <fp2/fastmod.h>

#include <lib/vectorclass-2.01.03/vectorclass.h>

#include <cstring>
#include <vector>

#define ALWAYS_INLINE inline __attribute__((always_inline,flatten))

namespace fp2::biome::fastlayer {
    /**
     * Faster re-implementation of the PRNG used in GenLayer.
     *
     * @author DaPorkchop_
     */
    template<typename T_64, typename T_32, T_32 (CAST_DOWN)(T_64), T_64 (CAST_UP)(T_32)> class base_rng {
    private:
        constexpr ALWAYS_INLINE T_64 update(const T_64 state, const T_64 seed) {
            return state * (state * 6364136223846793005LL + 1442695040888963407LL) + seed;
        }

        constexpr ALWAYS_INLINE T_64 start(const T_64 seed, const T_32 x, const T_32 z) {
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
        ALWAYS_INLINE base_rng(const T_64 seed, const T_32 x, const T_32 z):
            _seed(seed),
            _state(start(seed, x, z)) {}

        template<size_t COUNT = 1> ALWAYS_INLINE void update() {
            for (size_t i = 0; i < COUNT; i++) {
                _state = update(_state, _seed);
            }
        }

        //TODO: i can do vectorized fast modulo by constant using https://www.felixcloutier.com/x86/pmulld:pmullq (needs AVX512)

        template<int32_t MAX> ALWAYS_INLINE T_32 nextInt() {
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

        ALWAYS_INLINE T_32 nextInt(const fp2::fastmod_s64& fm) {
            const int32_t max = fm._d;
            T_32 i;
            if (!(max & (max - 1))) {
                i = (T_32) (_state >> 24) & (max - 1);
            } else {
                i = (T_32) ((_state >> 24) % fm);
                i += (i >> 31) & max; //equivalent to if (i < 0) { i += max; }
            }

            update(); //update PRNG state
            return i;
        }

        ALWAYS_INLINE T_32 nextInt(const T_32 max) {
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

    constexpr int32_t mulAddShift(int32_t a, int32_t b, int32_t c, int32_t shift) {
        return (int32_t) (((int64_t) a * (int64_t) b + (int64_t) c) >> shift);
    }

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
    template<typename... ARGS> class padded_layer {
    public:
    template<int32_t(EVAL)(int64_t, int32_t, int32_t, int32_t, Vec4i, ARGS...), padded_layer_mode MODE> class impl {
        static const inline Vec4i OFFSETS_X = MODE == padded_layer_mode::corners
                ? Vec4i(-1, 1, -1, 1) : MODE == padded_layer_mode::sides ? Vec4i(-1, 0, 0, 1) : Vec4i(-1, 0, 1, 0);
        static const inline Vec4i OFFSETS_Z = MODE == padded_layer_mode::corners
                ? Vec4i(-1, -1, 1, 1) : MODE == padded_layer_mode::sides ? Vec4i(0, -1, 1, 0) : Vec4i(0, -1, 0, 1);

    public:
        ALWAYS_INLINE void grid(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in, ARGS... args) {
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

                    out[outIdx] = EVAL(seed, x + dx, z + dz, center, neighbors, args...);
                }
            }
        }

        ALWAYS_INLINE void grid_multi_combined(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t depth, int32_t count, jintArray _out, jintArray _in, ARGS... args) {
            const int32_t inSize = (((dist >> depth) + 1) * count) + 2;
            const int32_t mask = depth != 0;

            const Vec4i neighbor_offsets = OFFSETS_X * inSize + OFFSETS_Z;

            fp2::pinned_int_array out(env, _out);
            fp2::pinned_int_array in(env, _in);

            for (int32_t outIdx = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++) {
                    const int32_t baseX = mulAddShift(gridX, dist, x, depth);
                    const int32_t baseZ = mulAddShift(gridZ, dist, z, depth);
                    const int32_t offsetX = mulAddShift(gridX, dist, gridX & mask, depth);
                    const int32_t offsetZ = mulAddShift(gridZ, dist, gridZ & mask, depth);

                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, outIdx++) {
                            const int32_t inIdx = (offsetX + dx + 1) * inSize + (offsetZ + dz + 1);
                            const Vec4i neighbors = lookup<(1 << 30)>(inIdx + neighbor_offsets, &in[0]);
                            const int32_t center = in[inIdx];

                            out[outIdx] = EVAL(seed, baseX + dx, baseZ + dz, center, neighbors, args...);
                        }
                    }
                }
            }
        }

        ALWAYS_INLINE void grid_multi_individual(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t depth, int32_t count, jintArray _out, jintArray _in, ARGS... args) {
            const int32_t inSize = size + 2;

            const Vec4i neighbor_offsets = OFFSETS_X * inSize + OFFSETS_Z;

            fp2::pinned_int_array out(env, _out);
            fp2::pinned_int_array in(env, _in);

            for (int32_t outIdx = 0, inBase = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++, inBase += inSize * inSize) {
                    const int32_t baseX = mulAddShift(gridX, dist, x, depth);
                    const int32_t baseZ = mulAddShift(gridZ, dist, z, depth);

                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, outIdx++) {
                            const int32_t inIdx = inBase + (dx + 1) * inSize + (dz + 1);
                            const Vec4i neighbors = lookup<(1 << 30)>(inIdx + neighbor_offsets, &in[0]);
                            const int32_t center = in[inIdx];

                            out[outIdx] = EVAL(seed, baseX + dx, baseZ + dz, center, neighbors, args...);
                        }
                    }
                }
            }
        }
    }; };

    /**
     * Base implementation of a layer which simply replaces a single value type in the output.
     *
     * @param EVAL a function which determines the output value based on the input value
     * @author DaPorkchop_
     */
    template<typename... ARGS> class translation_layer {
    public:
    template<int32_t(EVAL)(int64_t, int32_t, int32_t, int32_t, ARGS...)> class impl {
    public:
        ALWAYS_INLINE void grid(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _inout, ARGS... args) {
            fp2::pinned_int_array inout(env, _inout);

            for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
                for (int32_t dz = 0; dz < sizeZ; dz++, i++) {
                    inout[i] = EVAL(seed, x + dx, z + dz, inout[i], args...);
                }
            }
        }

        ALWAYS_INLINE void grid_multi(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t depth, int32_t count, jintArray _inout, ARGS... args) {
            fp2::pinned_int_array inout(env, _inout);

            for (int32_t i = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++) {
                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, i++) {
                            inout[i] = EVAL(seed, mulAddShift(gridX, dist, x, depth) + dx, mulAddShift(gridZ, dist, z, depth) + dz, inout[i], args...);
                        }
                    }
                }
            }
        }
    }; };

    /**
     * Base implementation of a layer which has no inputs.
     *
     * @param EVAL a function which determines the output value
     * @author DaPorkchop_
     */
    template<int32_t(EVAL)(int64_t, int32_t, int32_t)> class source_layer {
    public:
        ALWAYS_INLINE void grid(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out) {
            fp2::pinned_int_array out(env, _out);

            for (int32_t i = 0, dx = 0; dx < sizeX; dx++) {
                for (int32_t dz = 0; dz < sizeZ; dz++, i++) {
                    out[i] = EVAL(seed, x + dx, z + dz);
                }
            }
        }

        ALWAYS_INLINE void grid_multi(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t depth, int32_t count, jintArray _out) {
            fp2::pinned_int_array out(env, _out);

            for (int32_t i = 0, gridX = 0; gridX < count; gridX++) {
                for (int32_t gridZ = 0; gridZ < count; gridZ++) {
                    for (int32_t dx = 0; dx < size; dx++) {
                        for (int32_t dz = 0; dz < size; dz++, i++) {
                            out[i] = EVAL(seed, mulAddShift(gridX, dist, x, depth) + dx, mulAddShift(gridZ, dist, z, depth) + dz);
                        }
                    }
                }
            }
        }
    };

    /**
     * Base implementation of a layer which zooms in by a power of two.
     *
     * @param EVAL a function which determines the output values for an entire zoomed-in tile based on the input values at the corner points
     * @param ZOOM the number of bits to shift zoomed coordinates by
     * @author DaPorkchop_
     */
    template<void(EVAL)(int64_t, int32_t, int32_t, Vec4i, int32_t**), uint32_t ZOOM> class zooming_layer {
        static const inline Vec4i OFFSETS_X = Vec4i(0, 1, 0, 1);
        static const inline Vec4i OFFSETS_Z = Vec4i(0, 0, 1, 1);

        static constexpr uint32_t SIZE = 1 << ZOOM;
        static constexpr uint32_t MASK = SIZE - 1;

        constexpr ALWAYS_INLINE bool is_aligned(int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ) {
            return ((x | z | sizeX | sizeZ) & MASK) == 0;
        }

        ALWAYS_INLINE void grid_aligned(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
            const int32_t inX = x >> ZOOM;
            const int32_t inZ = z >> ZOOM;
            const int32_t inSizeX = (sizeX >> ZOOM) + 1;
            const int32_t inSizeZ = (sizeZ >> ZOOM) + 1;

            const Vec4i in_offsets = OFFSETS_X * inSizeZ + OFFSETS_Z;

            fp2::pinned_int_array out(env, _out);
            fp2::pinned_int_array in(env, _in);

            for (int32_t tileX = 0; tileX < inSizeX - 1; tileX++) {
                for (int32_t tileZ = 0; tileZ < inSizeZ - 1; tileZ++) {
                    Vec4i values = lookup<(1 << 30)>((tileX * inSizeZ + tileZ) + in_offsets, &in[0]);

                    int32_t* pointers[SIZE];
                    for (int32_t i = 0; i < SIZE; i++) {
                        pointers[i] = &out[((tileX << ZOOM) + i) * sizeZ + (tileZ << ZOOM)];
                    }

                    EVAL(seed, (inX + tileX) << ZOOM, (inZ + tileZ) << ZOOM, values, pointers);
                }
            }
        }

        ALWAYS_INLINE void grid_unaligned(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
            const int32_t inX = x >> ZOOM;
            const int32_t inZ = z >> ZOOM;
            const int32_t inSizeX = (sizeX >> ZOOM) + 2;
            const int32_t inSizeZ = (sizeZ >> ZOOM) + 2;
            const int32_t tempSizeX = (inSizeX - 1) << ZOOM;
            const int32_t tempSizeZ = (inSizeZ - 1) << ZOOM;
    
            std::vector<int32_t> temp(tempSizeX * tempSizeZ);

            {
                const Vec4i in_offsets = OFFSETS_X * inSizeZ + OFFSETS_Z;
                fp2::pinned_int_array in(env, _in);
    
                for (int32_t tileX = 0; tileX < inSizeX - 1; tileX++) {
                    for (int32_t tileZ = 0; tileZ < inSizeZ - 1; tileZ++) {
                        Vec4i values = lookup<(1 << 30)>((tileX * inSizeZ + tileZ) + in_offsets, &in[0]);

                        int32_t* pointers[SIZE];
                        for (int32_t i = 0; i < SIZE; i++) {
                            pointers[i] = &temp[((tileX << ZOOM) + i) * tempSizeZ + (tileZ << ZOOM)];
                        }

                        EVAL(seed, (inX + tileX) << ZOOM, (inZ + tileZ) << ZOOM, values, pointers);
                    }
                }
            }

            {
                fp2::pinned_int_array out(env, _out);
    
                for (int32_t dx = 0; dx < sizeX; dx++) {
                    memcpy(&out[dx * sizeZ], &temp[(dx + (x & MASK)) * tempSizeZ + (z & MASK)], sizeZ * sizeof(int32_t));
                }
            }
        }

    public:
        ALWAYS_INLINE void grid(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t sizeX, int32_t sizeZ, jintArray _out, jintArray _in) {
            if (is_aligned(x, z, sizeX, sizeZ)) {
                grid_aligned(env, seed, x, z, sizeX, sizeZ, _out, _in);
            } else {
                grid_unaligned(env, seed, x, z, sizeX, sizeZ, _out, _in);
            }
        }

        ALWAYS_INLINE void grid_multi_combined(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t depth, int32_t count, jintArray _out, jintArray _in) {
            const int32_t inSize = ((((dist >> depth) + 1) * count) >> ZOOM) + 2;
            const int32_t inTileSize = (size >> ZOOM) + 2;
            const int32_t tempTileSize = (inTileSize - 1) << ZOOM;

            std::vector<int32_t> temp(count * count * tempTileSize * tempTileSize);

            {
                const Vec4i in_offsets = OFFSETS_X * inSize + OFFSETS_Z;
                fp2::pinned_int_array in(env, _in);

                for (int32_t tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                    for (int32_t gridZ = 0; gridZ < count; gridZ++, tempIdx += tempTileSize * tempTileSize) {
                        const int32_t inX = mulAddShift(gridX, dist, x, depth) >> ZOOM;
                        const int32_t inZ = mulAddShift(gridZ, dist, z, depth) >> ZOOM;
                        const int32_t offsetX = mulAddShift(gridX, dist, gridX & MASK, depth) >> ZOOM;
                        const int32_t offsetZ = mulAddShift(gridZ, dist, gridZ & MASK, depth) >> ZOOM;

                        for (int32_t tileX = 0; tileX < inTileSize - 1; tileX++) {
                            for (int32_t tileZ = 0; tileZ < inTileSize - 1; tileZ++) {
                                Vec4i values = lookup<(1 << 30)>(((offsetX + tileX) * inSize + (offsetZ + tileZ)) + in_offsets, &in[0]);

                                int32_t* pointers[SIZE];
                                for (int32_t i = 0; i < SIZE; i++) {
                                    pointers[i] = &temp[tempIdx + ((tileX << ZOOM) + i) * tempTileSize + (tileZ << ZOOM)];
                                }

                                EVAL(seed, (inX + tileX) << ZOOM, (inZ + tileZ) << ZOOM, values, pointers);
                            }
                        }
                    }
                }
            }

            {
                fp2::pinned_int_array out(env, _out);

                for (int32_t outIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                    for (int32_t gridZ = 0; gridZ < count; gridZ++, outIdx += size * size, tempIdx += tempTileSize * tempTileSize) {
                        const int32_t realX = mulAddShift(gridX, dist, x, depth);
                        const int32_t realZ = mulAddShift(gridZ, dist, z, depth);

                        for (int32_t dx = 0; dx < size; dx++) {
                            memcpy(&out[outIdx + dx * size], &temp[tempIdx + (dx + (realX & MASK)) * tempTileSize + (realZ & MASK)], size * sizeof(int32_t));
                        }
                    }
                }
            }
        }

        ALWAYS_INLINE void grid_multi_individual(JNIEnv* env,
                int64_t seed, int32_t x, int32_t z, int32_t size, int32_t dist, int32_t depth, int32_t count, jintArray _out, jintArray _in) {
            const int32_t inSize = (size >> ZOOM) + 2;
            const int32_t tempSize = (inSize - 1) << ZOOM;
    
            std::vector<int32_t> temp(count * count * tempSize * tempSize);
    
            {
                const Vec4i in_offsets = OFFSETS_X * inSize + OFFSETS_Z;
                fp2::pinned_int_array in(env, _in);
    
                for (int32_t inIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                    for (int32_t gridZ = 0; gridZ < count; gridZ++, inIdx += inSize * inSize, tempIdx += tempSize * tempSize) {
                        const int32_t baseX = mulAddShift(gridX, dist, x, depth);
                        const int32_t baseZ = mulAddShift(gridZ, dist, z, depth);
                        const int32_t inX = baseX >> ZOOM;
                        const int32_t inZ = baseZ >> ZOOM;
    
                        for (int32_t tileX = 0; tileX < inSize - 1; tileX++) {
                            for (int32_t tileZ = 0; tileZ < inSize - 1; tileZ++) {
                                Vec4i values = lookup<(1 << 30)>((tileX * inSize + tileZ) + in_offsets, &in[inIdx]);
    
                                int32_t* pointers[SIZE];
                                for (int32_t i = 0; i < SIZE; i++) {
                                    pointers[i] = &temp[tempIdx + ((tileX << ZOOM) + i) * tempSize + (tileZ << ZOOM)];
                                }

                                EVAL(seed, (inX + tileX) << ZOOM, (inZ + tileZ) << ZOOM, values, pointers);
                            }
                        }
                    }
                }
            }

            {
                fp2::pinned_int_array out(env, _out);
    
                for (int32_t outIdx = 0, tempIdx = 0, gridX = 0; gridX < count; gridX++) {
                    for (int32_t gridZ = 0; gridZ < count; gridZ++, outIdx += size * size, tempIdx += tempSize * tempSize) {
                        const int32_t realX = mulAddShift(gridX, dist, x, depth);
                        const int32_t realZ = mulAddShift(gridZ, dist, z, depth);
    
                        for (int32_t dx = 0; dx < size; dx++) {
                            memcpy(&out[outIdx + dx * size], &temp[tempIdx + (dx + (realX & MASK)) * tempSize + (realZ & MASK)], size * sizeof(int32_t));
                        }
                    }
                }
            }
        }
    };

    struct biome_ids_t {
        int32_t OCEAN;
        int32_t DEFAULT;
        int32_t PLAINS;
        int32_t DESERT;
        int32_t EXTREME_HILLS;
        int32_t FOREST;
        int32_t TAIGA;
        int32_t SWAMPLAND;
        int32_t RIVER;
        int32_t HELL;
        int32_t SKY;
        int32_t FROZEN_OCEAN;
        int32_t FROZEN_RIVER;
        int32_t ICE_PLAINS;
        int32_t ICE_MOUNTAINS;
        int32_t MUSHROOM_ISLAND;
        int32_t MUSHROOM_ISLAND_SHORE;
        int32_t BEACH;
        int32_t DESERT_HILLS;
        int32_t FOREST_HILLS;
        int32_t TAIGA_HILLS;
        int32_t EXTREME_HILLS_EDGE;
        int32_t JUNGLE;
        int32_t JUNGLE_HILLS;
        int32_t JUNGLE_EDGE;
        int32_t DEEP_OCEAN;
        int32_t STONE_BEACH;
        int32_t COLD_BEACH;
        int32_t BIRCH_FOREST;
        int32_t BIRCH_FOREST_HILLS;
        int32_t ROOFED_FOREST;
        int32_t COLD_TAIGA;
        int32_t COLD_TAIGA_HILLS;
        int32_t REDWOOD_TAIGA;
        int32_t REDWOOD_TAIGA_HILLS;
        int32_t EXTREME_HILLS_WITH_TREES;
        int32_t SAVANNA;
        int32_t SAVANNA_PLATEAU;
        int32_t MESA;
        int32_t MESA_ROCK;
        int32_t MESA_CLEAR_ROCK;
        int32_t VOID;
        int32_t MUTATED_PLAINS;
        int32_t MUTATED_DESERT;
        int32_t MUTATED_EXTREME_HILLS;
        int32_t MUTATED_FOREST;
        int32_t MUTATED_TAIGA;
        int32_t MUTATED_SWAMPLAND;
        int32_t MUTATED_ICE_FLATS;
        int32_t MUTATED_JUNGLE;
        int32_t MUTATED_JUNGLE_EDGE;
        int32_t MUTATED_BIRCH_FOREST;
        int32_t MUTATED_BIRCH_FOREST_HILLS;
        int32_t MUTATED_ROOFED_FOREST;
        int32_t MUTATED_TAIGA_COLD;
        int32_t MUTATED_REDWOOD_TAIGA;
        int32_t MUTATED_REDWOOD_TAIGA_HILLS;
        int32_t MUTATED_EXTREME_HILLS_WITH_TREES;
        int32_t MUTATED_SAVANNA;
        int32_t MUTATED_SAVANNA_ROCK;
        int32_t MUTATED_MESA;
        int32_t MUTATED_MESA_ROCK;
        int32_t MUTATED_MESA_CLEAR_ROCK;
    };

    inline biome_ids_t biome_ids;

    template<size_t BIOME_COUNT> struct biomes_t {
    private:
        uint8_t _flags[BIOME_COUNT];
        int32_t _mutations[BIOME_COUNT];
        uint8_t _equals[BIOME_COUNT * BIOME_COUNT];

        uint32_t _flags_simd[BIOME_COUNT];
        uint32_t _equals_simd[BIOME_COUNT * BIOME_COUNT];
        
        static constexpr uint8_t FLAG_VALID = 1 << 0;
        static constexpr uint8_t FLAG_IS_JUNGLE_COMPATIBLE = 1 << 1;
        static constexpr uint8_t FLAG_IS_BIOME_OCEANIC = 1 << 2;
        static constexpr uint8_t FLAG_IS_MESA = 1 << 3;
        static constexpr uint8_t FLAG_IS_MUTATION = 1 << 4;
        static constexpr uint8_t FLAG_IS_JUNGLE = 1 << 5;
        static constexpr uint8_t FLAG_IS_SNOWY_BIOME = 1 << 6;
        
        static constexpr uint8_t EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU = 1 << 0;
        static constexpr uint8_t EQUALS_CAN_BIOMES_BE_NEIGHBORS = 1 << 1;

    public:
        ALWAYS_INLINE void reload(JNIEnv* env, jint count, jbyteArray flags, jbyteArray equals, jintArray mutations) {
            if (count != BIOME_COUNT) {
                throw fp2::error("invalid BIOME_COUNT", count);
            }

            env->GetByteArrayRegion(flags, 0, BIOME_COUNT, (jbyte*) &_flags);
            env->GetIntArrayRegion(mutations, 0, BIOME_COUNT, (jint*) &_mutations);
            env->GetByteArrayRegion(equals, 0, BIOME_COUNT * BIOME_COUNT, (jbyte*) &_equals);

            //unpack _flags and _equals to 32-bit (less cache-friendly, but allows checks to be vectorized)
            for (size_t i = 0; i < BIOME_COUNT; i++) {
                _flags_simd[i] = _flags[i];
            }
            for (size_t i = 0; i < BIOME_COUNT * BIOME_COUNT; i++) {
                _equals_simd[i] = _equals[i];
            }
        }

        ALWAYS_INLINE bool isValid(int32_t id) {
            return id >= 0 && (_flags[id] & FLAG_VALID);
        }

        ALWAYS_INLINE Vec4ib isValid(Vec4i id) {
            return (id >= 0) & ((lookup<BIOME_COUNT>(max(id, 0), &_flags_simd) & FLAG_VALID) != 0);
        }

        ALWAYS_INLINE bool isJungleCompatible(int32_t id) {
            return id >= 0 && (_flags[id] & FLAG_IS_JUNGLE_COMPATIBLE);
        }

        ALWAYS_INLINE Vec4ib isJungleCompatible(Vec4i id) {
            return (id >= 0) & ((lookup<BIOME_COUNT>(max(id, 0), &_flags_simd) & FLAG_IS_JUNGLE_COMPATIBLE) != 0);
        }

        ALWAYS_INLINE bool isBiomeOceanic(int32_t id) {
            return id >= 0 && (_flags[id] & FLAG_IS_BIOME_OCEANIC);
        }

        ALWAYS_INLINE Vec4ib isBiomeOceanic(Vec4i id) {
            return (id >= 0) & ((lookup<BIOME_COUNT>(max(id, 0), &_flags_simd) & FLAG_IS_BIOME_OCEANIC) != 0);
        }

        ALWAYS_INLINE bool isMesa(int32_t id) {
            return id >= 0 && (_flags[id] & FLAG_IS_MESA);
        }

        ALWAYS_INLINE Vec4ib isMesa(Vec4i id) {
            return (id >= 0) & ((lookup<BIOME_COUNT>(max(id, 0), &_flags_simd) & FLAG_IS_MESA) != 0);
        }

        ALWAYS_INLINE bool isMutation(int32_t id) {
            return id >= 0 && (_flags[id] & FLAG_IS_MUTATION);
        }

        ALWAYS_INLINE Vec4ib isMutation(Vec4i id) {
            return (id >= 0) & ((lookup<BIOME_COUNT>(max(id, 0), &_flags_simd) & FLAG_IS_MUTATION) != 0);
        }

        ALWAYS_INLINE bool isJungle(int32_t id) {
            return id >= 0 && (_flags[id] & FLAG_IS_JUNGLE);
        }

        ALWAYS_INLINE Vec4ib isJungle(Vec4i id) {
            return (id >= 0) & ((lookup<BIOME_COUNT>(max(id, 0), &_flags_simd) & FLAG_IS_JUNGLE) != 0);
        }

        ALWAYS_INLINE bool isSnowyBiome(int32_t id) {
            return id >= 0 && (_flags[id] & FLAG_IS_SNOWY_BIOME);
        }

        ALWAYS_INLINE Vec4ib isSnowyBiome(Vec4i id) {
            return (id >= 0) & ((lookup<BIOME_COUNT>(max(id, 0), &_flags_simd) & FLAG_IS_SNOWY_BIOME) != 0);
        }

        ALWAYS_INLINE bool biomesEqualOrMesaPlateau(int32_t a, int32_t b) {
            return a >= 0 && b >= 0 && (_equals[b * BIOME_COUNT + a] & EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU);
        }

        ALWAYS_INLINE Vec4ib biomesEqualOrMesaPlateau(Vec4i a, int32_t b) {
            return b < 0 ? Vec4ib(false) : ((a >= 0) & ((lookup<BIOME_COUNT>(max(a, 0), &_equals_simd[b * BIOME_COUNT]) & EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU) != 0));
        }

        ALWAYS_INLINE bool canBiomesBeNeighbors(int32_t a, int32_t b) {
            return a >= 0 && b >= 0 && (_equals[b * BIOME_COUNT + a] & EQUALS_CAN_BIOMES_BE_NEIGHBORS);
        }

        ALWAYS_INLINE Vec4ib canBiomesBeNeighbors(Vec4i a, int32_t b) {
            return b < 0 ? Vec4ib(false) : ((a >= 0) & ((lookup<BIOME_COUNT>(max(a, 0), &_equals_simd[b * BIOME_COUNT]) & EQUALS_CAN_BIOMES_BE_NEIGHBORS) != 0));
        }

        ALWAYS_INLINE int32_t getMutationForBiome(int32_t id) {
            return id >= 0 ? _mutations[id] : id;
        }
    };

    inline biomes_t<256> biomes;
}

using fp2::biome::fastlayer::biome_ids;
using fp2::biome::fastlayer::biomes;

#endif //NATIVELAYER_NATIVEFASTLAYER_H
