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

#pragma once

#include <vectorclass-2.01.03/vectorclass.h>

#ifndef _FP2_VEC_SIZE
#define _FP2_VEC_SIZE 128
#warning "_FP2_VEC_SIZE not set, defaulting to 128!"
#endif

#define _FP2_VEC_MIN_SIZE 128

namespace FP2_ROOT_NAMESPACE { namespace fp2::simd {
    constexpr size_t LANES_16 = _FP2_VEC_SIZE / 16;
    constexpr size_t LANES_32 = _FP2_VEC_SIZE / 32;
    constexpr size_t LANES_64 = _FP2_VEC_SIZE / 64;
    constexpr size_t LANES_32AND64 = _FP2_VEC_MIN_SIZE < _FP2_VEC_SIZE ? LANES_64 : LANES_32;

    /**
     * Base class for all type-specific vector species.
     * @tparam ELEM  the element type
     * @tparam LANES the number of vector lanes
     */
    template<typename ELEM, size_t _LANES> class type_vec {
    public:
        constexpr static size_t LANES = _LANES;
    };

#define _FP2_VEC_SHORT(SIZE) template<> class type_vec<int16_t, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## s; \
    using BOOL = Vec ## SIZE ## sb; \
    using INT = type_vec<int32_t, SIZE>; \
    using LONG = type_vec<int64_t, SIZE>; \
    using FLOAT = type_vec<float, SIZE>; \
    using DOUBLE = type_vec<double, SIZE>; \
};
    _FP2_VEC_SHORT(8)
    _FP2_VEC_SHORT(16)
    _FP2_VEC_SHORT(32)
#undef _FP2_VEC_SHORT

#define _FP2_VEC_USHORT(SIZE) template<> class type_vec<uint16_t, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## us; \
    using BOOL = Vec ## SIZE ## sb; \
    using INT = type_vec<uint32_t, SIZE>; \
    using LONG = type_vec<uint64_t, SIZE>; \
    using FLOAT = type_vec<float, SIZE>; \
    using DOUBLE = type_vec<double, SIZE>; \
};
    _FP2_VEC_USHORT(8)
    _FP2_VEC_USHORT(16)
    _FP2_VEC_USHORT(32)
#undef _FP2_VEC_USHORT

#define _FP2_VEC_INT(SIZE) template<> class type_vec<int32_t, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## i; \
    using BOOL = Vec ## SIZE ## ib; \
    using SHORT = type_vec<int16_t, SIZE>; \
    using LONG = type_vec<int64_t, SIZE>; \
    using FLOAT = type_vec<float, SIZE>; \
    using DOUBLE = type_vec<double, SIZE>; \
};
    _FP2_VEC_INT(4)
    _FP2_VEC_INT(8)
    _FP2_VEC_INT(16)
#undef _FP2_VEC_INT

#define _FP2_VEC_UINT(SIZE) template<> class type_vec<uint32_t, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## ui; \
    using BOOL = Vec ## SIZE ## ib; \
    using SHORT = type_vec<uint16_t, SIZE>; \
    using LONG = type_vec<uint64_t, SIZE>; \
    using FLOAT = type_vec<float, SIZE>; \
    using DOUBLE = type_vec<double, SIZE>; \
};
    _FP2_VEC_UINT(4)
    _FP2_VEC_UINT(8)
    _FP2_VEC_UINT(16)
#undef _FP2_VEC_UINT

#define _FP2_VEC_LONG(SIZE) template<> class type_vec<int64_t, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## q; \
    using BOOL = Vec ## SIZE ## qb; \
    using SHORT = type_vec<int16_t, SIZE>; \
    using INT = type_vec<int32_t, SIZE>; \
    using FLOAT = type_vec<float, SIZE>; \
    using DOUBLE = type_vec<double, SIZE>; \
};
    _FP2_VEC_LONG(2)
    _FP2_VEC_LONG(4)
    _FP2_VEC_LONG(8)
#undef _FP2_VEC_LONG

#define _FP2_VEC_ULONG(SIZE) template<> class type_vec<uint64_t, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## uq; \
    using BOOL = Vec ## SIZE ## qb; \
    using SHORT = type_vec<uint16_t, SIZE>; \
    using INT = type_vec<uint32_t, SIZE>; \
    using FLOAT = type_vec<float, SIZE>; \
    using DOUBLE = type_vec<double, SIZE>; \
};
    _FP2_VEC_ULONG(2)
    _FP2_VEC_ULONG(4)
    _FP2_VEC_ULONG(8)
#undef _FP2_VEC_ULONG

#define _FP2_VEC_FLOAT(SIZE) template<> class type_vec<float, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## f; \
    using BOOL = Vec ## SIZE ## fb; \
    using SHORT = type_vec<int16_t, SIZE>; \
    using USHORT = type_vec<uint16_t, SIZE>; \
    using INT = type_vec<int32_t, SIZE>; \
    using UINT = type_vec<uint32_t, SIZE>; \
    using LONG = type_vec<int64_t, SIZE>; \
    using ULONG = type_vec<uint64_t, SIZE>; \
    using DOUBLE = type_vec<double, SIZE>; \
};
    _FP2_VEC_FLOAT(4)
    _FP2_VEC_FLOAT(8)
    _FP2_VEC_FLOAT(16)
#undef _FP2_VEC_FLOAT

#define _FP2_VEC_DOUBLE(SIZE) template<> class type_vec<double, SIZE> { \
public: \
    using TYPE = Vec ## SIZE ## d; \
    using BOOL = Vec ## SIZE ## db; \
    using SHORT = type_vec<int16_t, SIZE>; \
    using USHORT = type_vec<uint16_t, SIZE>; \
    using INT = type_vec<int32_t, SIZE>; \
    using UINT = type_vec<uint32_t, SIZE>; \
    using LONG = type_vec<int64_t, SIZE>; \
    using ULONG = type_vec<uint64_t, SIZE>; \
    using FLOAT = type_vec<float, SIZE>; \
};
    _FP2_VEC_DOUBLE(2)
    _FP2_VEC_DOUBLE(4)
    _FP2_VEC_DOUBLE(8)
#undef _FP2_VEC_DOUBLE

    /**
     * Base class for all vector species.
     * @tparam SIZE the number of bits
     */
    template<size_t SIZE> class vec;

#define _FP2_VEC(SIZE) template<> class vec<SIZE> { \
public: \
    using SHORT = type_vec<int16_t, ((SIZE) / 16)>::TYPE; \
    using SHORT_BOOL = type_vec<int16_t, ((SIZE) / 16)>::BOOL; \
    using USHORT = type_vec<uint16_t, ((SIZE) / 16)>::TYPE; \
    using USHORT_BOOL = type_vec<uint16_t, ((SIZE) / 16)>::BOOL; \
    using INT = type_vec<int32_t, ((SIZE) / 32)>::TYPE; \
    using INT_BOOL = type_vec<int32_t, ((SIZE) / 32)>::BOOL; \
    using UINT = type_vec<uint32_t, ((SIZE) / 32)>::TYPE; \
    using UINT_BOOL = type_vec<uint32_t, ((SIZE) / 32)>::BOOL; \
    using LONG = type_vec<int64_t, ((SIZE) / 64)>::TYPE; \
    using LONG_BOOL = type_vec<int64_t, ((SIZE) / 64)>::BOOL; \
    using ULONG = type_vec<uint64_t, ((SIZE) / 64)>::TYPE; \
    using ULONG_BOOL = type_vec<uint64_t, ((SIZE) / 64)>::BOOL; \
    using FLOAT = type_vec<float, ((SIZE) / 32)>::TYPE; \
    using FLOAT_BOOL = type_vec<float, ((SIZE) / 32)>::BOOL; \
    using DOUBLE = type_vec<double, ((SIZE) / 64)>::TYPE; \
    using DOUBLE_BOOL = type_vec<double, ((SIZE) / 64)>::BOOL; \
};
    _FP2_VEC(128)
    _FP2_VEC(256)
    _FP2_VEC(512)
#undef _FP2_VEC

    template<typename T> inline T increment() {
        T out = 0;
        for (size_t i = 0; i < out.size(); i++) {
            out.insert(i, i);
        }
        return out;
    }

    template<typename T> inline T increment_shift() {
        T out = 0;
        for (size_t i = 0; i < out.size(); i++) {
            out.insert(i, 1 << i);
        }
        return out;
    }
}}
