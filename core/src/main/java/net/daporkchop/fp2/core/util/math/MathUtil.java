/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.util.math;

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;

import static java.lang.Math.*;

/**
 * Various math helper functions.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class MathUtil {
    /**
     * Divides the given value by {@code 2^shift}, rounding down towards negative infinity.
     *
     * @param val   the value
     * @param shift {@code log2} of the divisor. Undefined behavior if negative or greater than or equal to {@link Integer#SIZE}
     * @return the given value divided by {@code 2^shift}
     */
    public static int asrFloor(int val, int shift) {
        //lol this is totally useless, i'm just adding it here because it keeps the code cleaner when asrRound()/asrCeil() are used alongside it
        return val >> shift;
    }

    /**
     * Divides the given value by {@code 2^shift}, rounding results with a fractional part less than {@code 0.5} towards negative infinity and results with
     * a fractional part greater than or equal to {@code 0.5} towards positive infinity.
     *
     * @param val   the value
     * @param shift {@code log2} of the divisor. Undefined behavior if negative or greater than or equal to {@link Integer#SIZE}
     * @return the given value divided by {@code 2^shift}
     */
    public static int asrRound(int val, int shift) {
        return shift == 0
                ? val
                : (val >> shift) + ((val >> (shift - 1)) & 1);
    }

    /**
     * Divides the given value by {@code 2^shift}, rounding up towards positive infinity.
     *
     * @param val   the value
     * @param shift {@code log2} of the divisor. Undefined behavior if negative or greater than or equal to {@link Integer#SIZE}
     * @return the given value divided by {@code 2^shift}
     */
    public static int asrCeil(int val, int shift) {
        return shift == 0
                ? val
                : (val >> shift) + ((val & ((1 << shift) - 1)) != 0 ? 1 : 0);
    }

    public static int mulAddShift(int a, int b, int c, int shift) {
        return (int) (((long) a * (long) b + c) >> shift);
    }

    public static int gcd(int a, int b) {
        while (a != b) {
            if (a > b) {
                a -= b;
            } else {
                b -= a;
            }
        }
        return a;
    }

    public static int sq(int d) {
        return d * d;
    }

    public static int cb(int d) {
        return d * d * d;
    }

    public static long sq(long d) {
        return d * d;
    }

    public static long cb(long d) {
        return d * d * d;
    }

    public static float sq(float d) {
        return d * d;
    }

    public static double sq(double d) {
        return d * d;
    }

    /**
     * Computes the X coordinate of the point where the line defined by the two given values (at x=0 and x=1, respectively) intersects the X axis.
     *
     * @param d0 the value at x=0
     * @param d1 the value at x=1
     */
    public static double minimize(double d0, double d1) {
        return d0 / (d0 - d1);
    }

    /**
     * Interleaves the bits of 2 {@code int}s.
     * <p>
     * Based on <a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">Bit Twiddling Hacks - Interleave bits by Binary Magic Numbers</a>.
     *
     * @return the interleaved bits
     */
    public static long interleaveBits(int i0, int i1) {
        return spreadBits1(i0) | (spreadBits1(i1) << 1L);
    }

    /**
     * Extracts the {@code i0} parameter from the value returned by {@link #interleaveBits(int, int)}.
     *
     * @param interleaved the interleaved bits
     * @return {@code i0}
     */
    public static int uninterleave2_0(long interleaved) {
        return (int) gatherBits1(interleaved);
    }

    /**
     * Extracts the {@code i1} parameter from the value returned by {@link #interleaveBits(int, int)}.
     *
     * @param interleaved the interleaved bits
     * @return {@code i1}
     */
    public static int uninterleave2_1(long interleaved) {
        return (int) gatherBits1(interleaved >>> 1L);
    }

    /**
     * Interleaves the lower bits of 3 {@code int}s.
     * <p>
     * Note that this will discard the upper 10 bits of {@code i0}, and 11 bits of {@code i1} and {@code i2}.
     *
     * @return the interleaved bits
     */
    public static long interleaveBits(int i0, int i1, int i2) {
        return spreadBits2(i0) | (spreadBits2(i1) << 1L) | (spreadBits2(i2) << 2L);
    }

    /**
     * Interleaves the upper bits of 3 {@code int}s.
     * <p>
     * Note that this will discard the lower 22 bits of {@code i0}, and 21 bits of {@code i1} and {@code i2}.
     *
     * @return the interleaved bits
     */
    public static int interleaveBitsHigh(int i0, int i1, int i2) {
        return (int) ((spreadBits2(i0 >>> 22) << 2L) | spreadBits2(i1 >>> 21) | (spreadBits2(i2 >>> 21) << 1L));
    }

    /**
     * Extracts the {@code i0} parameter from the value returned by {@link #interleaveBits(int, int, int)}.
     *
     * @param interleavedLow  the low interleaved bits
     * @return {@code i0}
     */
    public static int uninterleave3_0(long interleavedLow) {
        return (int) gatherBits2(interleavedLow);
    }

    /**
     * Extracts the {@code i1} parameter from the value returned by {@link #interleaveBits(int, int, int)}.
     *
     * @param interleavedLow  the low interleaved bits
     * @return {@code i1}
     */
    public static int uninterleave3_1(long interleavedLow) {
        return (int) gatherBits2(interleavedLow >>> 1L);
    }

    /**
     * Extracts the {@code i2} parameter from the value returned by {@link #interleaveBits(int, int, int)}.
     *
     * @param interleavedLow  the low interleaved bits
     * @return {@code i2}
     */
    public static int uninterleave3_2(long interleavedLow) {
        return (int) gatherBits2(interleavedLow >>> 2L);
    }

    /**
     * Extracts the {@code i0} parameter from the values returned by {@link #interleaveBits(int, int, int)} and {@link #interleaveBitsHigh(int, int, int)}.
     *
     * @param interleavedLow  the low interleaved bits
     * @param interleavedHigh the high interleaved bits
     * @return {@code i0}
     */
    public static int uninterleave3_0(long interleavedLow, int interleavedHigh) {
        return (int) (gatherBits2(interleavedLow) | (gatherBits2(interleavedHigh >>> 2L) << 22L));
    }

    /**
     * Extracts the {@code i1} parameter from the values returned by {@link #interleaveBits(int, int, int)} and {@link #interleaveBitsHigh(int, int, int)}.
     *
     * @param interleavedLow  the low interleaved bits
     * @param interleavedHigh the high interleaved bits
     * @return {@code i1}
     */
    public static int uninterleave3_1(long interleavedLow, int interleavedHigh) {
        return (int) (gatherBits2(interleavedLow >>> 1L) | (gatherBits2(interleavedHigh) << 21L));
    }

    /**
     * Extracts the {@code i2} parameter from the values returned by {@link #interleaveBits(int, int, int)} and {@link #interleaveBitsHigh(int, int, int)}.
     *
     * @param interleavedLow  the low interleaved bits
     * @param interleavedHigh the high interleaved bits
     * @return {@code i2}
     */
    public static int uninterleave3_2(long interleavedLow, int interleavedHigh) {
        return (int) (gatherBits2(interleavedLow >>> 2L) | (gatherBits2(interleavedHigh >>> 1L) << 21L));
    }

    /**
     * Inserts a single bit between every bit in the input parameter.
     * <p>
     * Note that this will discard the upper 32 bits of the parameter.
     * <p>
     * Based on <a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">Bit Twiddling Hacks - Interleave bits by Binary Magic Numbers</a>.
     *
     * @return the spread bits
     */
    public static long spreadBits1(long l) {
        //clear upper bits
        l &= 0x00000000FFFFFFFFL;

        //basically magic
        l = (l | (l << 16L)) & 0x0000FFFF0000FFFFL;
        l = (l | (l << 8L)) & 0x00FF00FF00FF00FFL;
        l = (l | (l << 4L)) & 0x0F0F0F0F0F0F0F0FL;
        l = (l | (l << 2L)) & 0x3333333333333333L;
        l = (l | (l << 1L)) & 0x5555555555555555L;
        return l;
    }

    /**
     * Discards a single bit between every bit in the input parameter.
     *
     * @return the gathered bits
     */
    public static long gatherBits1(long l) {
        //clear unneeded bits
        l &= 0x5555555555555555L;

        //basically magic
        l = (l | (l >>> 1L)) & 0x3333333333333333L;
        l = (l | (l >>> 2L)) & 0x0F0F0F0F0F0F0F0FL;
        l = (l | (l >>> 4L)) & 0x00FF00FF00FF00FFL;
        l = (l | (l >>> 8L)) & 0x0000FFFF0000FFFFL;
        l = (l | (l >>> 16L)) & 0x00000000FFFFFFFFL;
        return l;
    }

    /**
     * Inserts two bits between every bit in the input parameter.
     * <p>
     * Note that this will discard the upper 43 bits of the parameter.
     * <p>
     * Based on <a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">Bit Twiddling Hacks - Interleave bits by Binary Magic Numbers</a>.
     *
     * @return the spread bits
     */
    public static long spreadBits2(long l) {
        //clear upper bits
        l &= 0x00000000003FFFFFL;

        //basically magic
        l = (l | (l << 32L)) & 0x003F00000000FFFFL;
        l = (l | (l << 16L)) & 0x003F0000FF0000FFL;
        l = (l | (l << 8L)) & 0x300F00F00F00F00FL;
        l = (l | (l << 4L)) & 0x30C30C30C30C30C3L;
        l = (l | (l << 2L)) & 0x9249249249249249L;
        return l;
    }

    /**
     * Discards two bits between every bit in the input parameter.
     *
     * @return the gathered bits
     */
    public static long gatherBits2(long l) {
        //clear unneeded bits
        l &= 0x9249249249249249L;

        //basically magic
        l = (l | (l >>> 2L)) & 0x30C30C30C30C30C3L;
        l = (l | (l >>> 4L)) & 0x300F00F00F00F00FL;
        l = (l | (l >>> 8L)) & 0x003F0000FF0000FFL;
        l = (l | (l >>> 16L)) & 0x003F00000000FFFFL;
        l = (l | (l >>> 32L)) & 0x00000000003FFFFFL;
        return l;
    }

    public static long lcm(long a, long b) {
        return multiplyExact(a, b) / gcd(a, b);
    }

    public static long gcd(long a, long b) {
        while (b != 0L) {
            long t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    public static boolean isPrime(long n) { //O(sqrt(N))
        if (n <= 1L) {
            return false;
        } else if (n <= 3L) {
            return true;
        }

        //make search about 3x faster by testing these outside of main loop
        if (n % 2L == 0L || n % 3L == 0L) {
            return false;
        }

        for (long i = 5L; i * i <= n; i += 6L) {
            if (n % i == 0L || n % (i + 2L) == 0L) {
                return false;
            }
        }
        return true;
    }

    public static long[] primeFactors(long n) { //O(N) (i think)
        LongList out = new LongArrayList();

        for (long prime = 2L; n != 1L; prime++) {
            while (n % prime == 0L) {
                n /= prime;
                out.add(prime);
            }
        }

        return out.toArray();
    }
}
