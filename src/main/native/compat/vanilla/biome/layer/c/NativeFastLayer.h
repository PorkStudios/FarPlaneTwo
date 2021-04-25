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
    class rng {
    private:
        constexpr int64_t update(const int64_t state, const int64_t seed) {
            return state * (state * 6364136223846793005LL + 1442695040888963407LL) + seed;
        }

        constexpr int64_t start(const int64_t seed, const int32_t x, const int32_t z) {
            int64_t state = seed;
            state = update(state, x);
            state = update(state, z);
            state = update(state, x);
            state = update(state, z);
            return state;
        }

        const int64_t _seed;
        int64_t _state;

    public:
        rng(const int64_t seed, const int32_t x, const int32_t z):
            _seed(seed),
            _state(start(seed, x, z)) {}

        template<int32_t MAX> inline int32_t nextInt() {
            constexpr fp2::fastmod_s64 fm(MAX);
            int32_t i = (int32_t) ((_state >> 24) % fm);
            i += (i >> 31) & MAX; //equivalent to if (i < 0) { i += MAX; }

            //update PRNG state
            _state = update(_state, _seed);
            return i;
        }
    };
}

#endif //NATIVELAYER_NATIVEFASTLAYER_H
