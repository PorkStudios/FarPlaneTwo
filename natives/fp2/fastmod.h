/*
 * Adapted from https://github.com/lemire/fastmod/blob/master/include/fastmod.h
 */

#pragma once

#include <cstdint>

namespace fp2 {
    namespace _fastmod {
        template<typename T> constexpr T abs_constexpr(const T val) {
            return val < 0 ? -val : val;
        }
    
        constexpr uint64_t mul128_u32(uint64_t lowbits, uint32_t d) {
            return ((__uint128_t)lowbits * d) >> 64;
        }

        constexpr uint64_t mul128_s32(uint64_t lowbits, int32_t d) {
            return ((__int128_t)lowbits * d) >> 64;
        }

        constexpr uint64_t mul128_u64(__uint128_t lowbits, uint64_t d) {
            __uint128_t bottom_half = (lowbits & UINT64_C(0xFFFFFFFFFFFFFFFF)) * d;
            bottom_half >>= 64;
            __uint128_t top_half = (lowbits >> 64) * d;
            __uint128_t both_halves = bottom_half + top_half;
            both_halves >>= 64;
            return (uint64_t)both_halves;
        }
    }

    class fastmod_u32 {
    public:
        uint64_t _m;
        uint32_t _d;

        constexpr fastmod_u32(const uint32_t d):
            _m(((uint64_t) -1) / d + 1),
            _d(d) {}

        friend uint32_t operator %(const uint32_t& a, const fastmod_u32& fm) {
            return (uint32_t) _fastmod::mul128_u32(fm._m * a, fm._d);
        }

        friend uint32_t operator /(const uint32_t& a, const fastmod_u32& fm) {
            return (uint32_t) _fastmod::mul128_u32(fm._m, a);
        }
    };

    class fastmod_s32 {
    public:
        uint64_t _m;
        int32_t _d;
        int32_t _positive_d;

        constexpr fastmod_s32(const int32_t d):
            _m(((uint64_t) -1) / _fastmod::abs_constexpr(d) + 1 + !(_fastmod::abs_constexpr(d) & (_fastmod::abs_constexpr(d) - 1))),
            _d(d),
            _positive_d(_fastmod::abs_constexpr(d)) {}

        friend int32_t operator %(const int32_t& a, const fastmod_s32& fm) {
            return ((int32_t) _fastmod::mul128_u32(fm._m * a, fm._positive_d)) - ((fm._positive_d - 1) & (a >> 31));
        }

        friend int32_t operator /(const int32_t& a, const fastmod_s32& fm) {
            int32_t tmp = (int32_t) (_fastmod::mul128_s32(fm._m, a) + (a < 0));
            return fm._d < 0 ? -tmp : tmp;
        }
    };

    class fastmod_u64 {
    public:
        __uint128_t _m;
        uint64_t _d;

        constexpr fastmod_u64(const uint64_t d):
            _m(((__uint128_t) -1) / d + 1),
            _d(d) {}

        friend uint64_t operator %(const uint64_t& a, const fastmod_u64& fm) {
            return (uint64_t) _fastmod::mul128_u64(fm._m * a, fm._d);
        }

        friend uint64_t operator /(const uint64_t& a, const fastmod_u64& fm) {
            return (uint64_t) _fastmod::mul128_u64(fm._m, a);
        }
    };

    class fastmod_s64 {
    public:
        __uint128_t _m;
        int64_t _d;
        int64_t _positive_d;

        constexpr fastmod_s64(const int64_t d):
            _m(((__uint128_t) -1) / _fastmod::abs_constexpr(d) + 1 + !(_fastmod::abs_constexpr(d) & (_fastmod::abs_constexpr(d) - 1))),
            _d(d),
            _positive_d(_fastmod::abs_constexpr(d)) {}

        friend int64_t operator %(const int64_t& a, const fastmod_s64& fm) {
            return ((uint64_t) _fastmod::mul128_u64(fm._m * a, fm._positive_d)) - ((fm._positive_d - 1) & (a >> 63));
        }

        /*friend int64_t operator /(const int64_t& a, const fastmod_s64& fm) {
            int64_t tmp = (int64_t) (_fastmod::mul128_u64(fm._m, a) + (a < 0));
            return fm._d < 0 ? -tmp : tmp;
        }*/
    };
}
