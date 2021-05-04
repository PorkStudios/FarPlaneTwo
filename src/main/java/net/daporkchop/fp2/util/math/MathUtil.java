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
}
