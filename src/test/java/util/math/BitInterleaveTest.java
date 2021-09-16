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

package util.math;

import net.daporkchop.fp2.util.math.MathUtil;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class BitInterleaveTest {
    @Test
    public void test2d() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 10000; i++) {
            int x0 = random.nextInt();
            int z0 = random.nextInt();

            long interleaved = MathUtil.interleaveBits(x0, z0);

            int x1 = MathUtil.uninterleave2_0(interleaved);
            int z1 = MathUtil.uninterleave2_1(interleaved);

            checkState(x0 == x1 && z0 == z1);
        }
    }

    @Test
    public void test3d_small() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 10000; i++) {
            int x0 = random.nextInt() & ~(Integer.MIN_VALUE >> 10);
            int y0 = random.nextInt() & ~(Integer.MIN_VALUE >> 10);
            int z0 = random.nextInt() & ~(Integer.MIN_VALUE >> 10);

            long interleaved = MathUtil.interleaveBits(x0, y0, z0);

            int x1 = MathUtil.uninterleave3_0(interleaved);
            int y1 = MathUtil.uninterleave3_1(interleaved);
            int z1 = MathUtil.uninterleave3_2(interleaved);

            checkState(x0 == x1 && y0 == y1 && z0 == z1);
        }
    }

    @Test
    public void test3d_big() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 10000; i++) {
            int x0 = random.nextInt();
            int y0 = random.nextInt();
            int z0 = random.nextInt();

            long interleavedLow = MathUtil.interleaveBits(x0, y0, z0);
            int interleavedHigh = MathUtil.interleaveBitsHigh(x0, y0, z0);

            int x1 = MathUtil.uninterleave3_0(interleavedLow, interleavedHigh);
            int y1 = MathUtil.uninterleave3_1(interleavedLow, interleavedHigh);
            int z1 = MathUtil.uninterleave3_2(interleavedLow, interleavedHigh);

            checkState(x0 == x1 && y0 == y1 && z0 == z1);
        }
    }
}
