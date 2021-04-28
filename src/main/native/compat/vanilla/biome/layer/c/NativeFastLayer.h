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
                constexpr fp2::fastmod_s64 fm(MAX);
                i = CAST_DOWN((_state >> 24) % fm);
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
            T_32 i = (T_32) ((_state >> 24) % max);
            i += (i >> 31) & max; //equivalent to if (i < 0) { i += max; }

            update(); //update PRNG state
            return i;
        }
    };

    using rng = base_rng<int64_t, int32_t, cast, cast>;
}

#endif //NATIVELAYER_NATIVEFASTLAYER_H
