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

package net.daporkchop.fp2.util.math;

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
    public static int asrRound(int val, int shift) {
        return shift == 0
                ? val
                : (val >> shift) + ((val >> (shift - 1)) & 1);
    }

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
     * Note that this will discard the upper 16 bits of all parameters.
     * <p>
     * Based on <a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">Bit Twiddling Hacks - Interleave bits by Binary Magic Numbers</a>.
     *
     * @return the interleaved bits
     */
    public static int interleaveBits(int i0, int i1) {
        return spreadBits1(i0) | (spreadBits1(i1) << 1);
    }

    /**
     * Interleaves the bits of 3 {@code int}s.
     * <p>
     * Note that this will discard the upper 22 bits of all parameters.
     * <p>
     * Based on <a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">Bit Twiddling Hacks - Interleave bits by Binary Magic Numbers</a>.
     *
     * @return the interleaved bits
     */
    public static int interleaveBits(int i0, int i1, int i2) {
        return spreadBits2(i0) | (spreadBits2(i1) << 1) | (spreadBits2(i2) << 2);
    }

    /**
     * Inserts a single bit between every bit in the input parameter.
     * <p>
     * Note that this will discard the upper 16 bits of the parameter.
     * <p>
     * Based on <a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">Bit Twiddling Hacks - Interleave bits by Binary Magic Numbers</a>.
     *
     * @return the spread bits
     */
    public static int spreadBits1(int i) {
        //clear upper bits
        i &= (1 << 16) - 1;

        //basically magic
        i = (i | (i << 8)) & 0x00FF00FF;
        i = (i | (i << 4)) & 0x0F0F0F0F;
        i = (i | (i << 2)) & 0x33333333;
        i = (i | (i << 1)) & 0x55555555;
        return i;
    }

    /**
     * Inserts two bits between every bit in the input parameter.
     * <p>
     * Note that this will discard the upper 22 bits of the parameter.
     * <p>
     * Based on <a href="https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN">Bit Twiddling Hacks - Interleave bits by Binary Magic Numbers</a>.
     *
     * @return the spread bits
     */
    public static int spreadBits2(int i) {
        //clear upper bits
        i &= (1 << 10) - 1;

        //basically magic
        i = (i | (i << 16)) & 0x00FF00FF;
        i = (i | (i << 8)) & 0x0300F00F;
        i = (i | (i << 4)) & 0x030C30C3;
        i = (i | (i << 2)) & 0x09249249;
        return i;
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
