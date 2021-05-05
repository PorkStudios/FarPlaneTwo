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

package compat.cwg.noise;

import net.daporkchop.fp2.compat.cwg.noise.CWGNoiseProvider;
import net.daporkchop.lib.common.misc.string.PStrings;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.SplittableRandom;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("deprecation")
public class TestCwgNoiseGen {
    @BeforeClass
    public static void ensureNativeNoiseGenIsAvailable() {
        checkState(CWGNoiseProvider.INSTANCE.isNative(), "native noise generation must be available for testing!");
    }

    @Test
    public void testNativeIsIdenticalToJava() {
        SplittableRandom r = new SplittableRandom(12345L);

        this.test2d(
                2, 2, 0,
                0.2d, 0.2d,
                2, 2,
                237582, 4);

        for (int i = 0; i < 256; i++) {
            this.test2d(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(4),
                    r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d),
                    r.nextInt(1, 257), r.nextInt(1, 257),
                    r.nextInt(), r.nextInt(1, 17));
        }
    }

    protected void test2d(int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves) {
        double[] outJava = new double[sizeX * sizeZ];
        CWGNoiseProvider.JAVA_INSTANCE.generateNoise(outJava, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves);

        double[] outNative = new double[sizeX * sizeZ];
        CWGNoiseProvider.INSTANCE.generateNoise(outNative, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves);

        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, i++) {
                double javaVal = outJava[i];
                double nativeVal = outNative[i];
                if (Math.round(javaVal * 100000.0d) != Math.round(nativeVal * 100000.0d)) {
                    throw new IllegalStateException(PStrings.fastFormat("@(%s, %s): %s (java) != %s (native)", (baseX + (dx << level)) * freqX, (baseZ + (dz << level)) * freqZ, javaVal, nativeVal));
                }
            }
        }
    }
}
