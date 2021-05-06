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

import com.flowpowered.noise.Utils;
import io.github.opencubicchunks.cubicchunks.cubicgen.ConversionUtils;
import net.daporkchop.fp2.compat.cwg.noise.CWGNoiseProvider;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastJavaPRandom;
import net.minecraft.world.gen.NoiseGeneratorImproved;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

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

    /**
     * Copypasta of {@link ConversionUtils#initFlowNoiseHack()}, but accessing the gradient fields in {@link NoiseGeneratorImproved} directly (since the accessor mixin obviously
     * isn't being applied in a unit test environment).
     */
    @BeforeClass
    public static void initFlowNoiseHack() {
        PRandom random = new FastJavaPRandom(123456789);
        for (int i = 0; i < Utils.RANDOM_VECTORS.length / 4; i++) {
            int j = random.nextInt(NoiseGeneratorImproved.GRAD_X.length);
            Utils.RANDOM_VECTORS[i * 4] = NoiseGeneratorImproved.GRAD_X[j] / 2;
            Utils.RANDOM_VECTORS[i * 4 + 1] = NoiseGeneratorImproved.GRAD_Y[j] / 2;
            Utils.RANDOM_VECTORS[i * 4 + 2] = NoiseGeneratorImproved.GRAD_Z[j] / 2;
        }
    }

    @Test
    public void test3d() {
        SplittableRandom r = new SplittableRandom(67890L);

        this.test3d(
                2, 2, 2, 0,
                0.2d, 0.2d, 0.2d,
                32, 32, 32,
                237582, 4).join();

        this.test3d(
                2, 2, 2, 0,
                0.2d, 0.2d, 0.2d,
                32, 32, 32,
                237582, 5).join();

        CompletableFuture<?>[] futures = uncheckedCast(new CompletableFuture[256]);
        for (int i = 0; i < futures.length; i++) {
            futures[i] = this.test3d(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(4),
                    r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d),
                    r.nextInt(1, 65), r.nextInt(1, 65), r.nextInt(1, 65),
                    r.nextInt(), r.nextInt(1, 17));
        }
        CompletableFuture.allOf(futures).join();
    }

    protected CompletableFuture<Void> test3d(int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves) {
        return CompletableFuture.runAsync(() -> {
            double[] outJava = new double[sizeX * sizeY * sizeZ];
            CWGNoiseProvider.JAVA_INSTANCE.generate3d(outJava, baseX, baseY, baseZ, level, freqX, freqY, freqZ, sizeX, sizeY, sizeZ, seed, octaves);

            double[] outNative = new double[sizeX * sizeY * sizeZ];
            CWGNoiseProvider.INSTANCE.generate3d(outNative, baseX, baseY, baseZ, level, freqX, freqY, freqZ, sizeX, sizeY, sizeZ, seed, octaves);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dy = 0; dy < sizeY; dy++) {
                    for (int dz = 0; dz < sizeZ; dz++, i++) {
                        double javaVal = outJava[i];
                        double nativeVal = outNative[i];
                        if (Math.round(javaVal * 100000.0d) != Math.round(nativeVal * 100000.0d)) {
                            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s, %s): %s (java) != %s (native)", (baseX + (dx << level)) * freqX, (baseY + (dy << level)) * freqY, (baseZ + (dz << level)) * freqZ, javaVal, nativeVal));
                        }
                    }
                }
            }
        });
    }

    @Test
    public void test2d() {
        SplittableRandom r = new SplittableRandom(12345L);

        this.test2d(
                2, 2, 0,
                0.2d, 0.2d,
                32, 32,
                237582, 4).join();

        this.test2d(
                2, 2, 0,
                0.2d, 0.2d,
                32, 32,
                237582, 5).join();

        CompletableFuture<?>[] futures = uncheckedCast(new CompletableFuture[256]);
        for (int i = 0; i < futures.length; i++) {
            futures[i] = this.test2d(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(4),
                    r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d),
                    r.nextInt(1, 257), r.nextInt(1, 257),
                    r.nextInt(), r.nextInt(1, 17));
        }
        CompletableFuture.allOf(futures).join();
    }

    protected CompletableFuture<Void> test2d(int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves) {
        return CompletableFuture.runAsync(() -> {
            double[] outJava = new double[sizeX * sizeZ];
            CWGNoiseProvider.JAVA_INSTANCE.generate2d(outJava, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves);

            double[] outNative = new double[sizeX * sizeZ];
            CWGNoiseProvider.INSTANCE.generate2d(outNative, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    double javaVal = outJava[i];
                    double nativeVal = outNative[i];
                    if (Math.round(javaVal * 100000.0d) != Math.round(nativeVal * 100000.0d)) {
                        throw new IllegalStateException(PStrings.fastFormat("@(%s, %s): %s (java) != %s (native)", (baseX + (dx << level)) * freqX, (baseZ + (dz << level)) * freqZ, javaVal, nativeVal));
                    }
                }
            }
        });
    }

    @Test
    public void testSingle() {
        SplittableRandom r = new SplittableRandom(67890L);

        this.testSingle(
                2, 2, 2,
                0.2d, 0.2d, 0.2d,
                237582, 4);

        this.testSingle(
                2, 2, 2,
                0.2d, 0.2d, 0.2d,
                237582, 5);

        for (int i = 0; i < 256; i++) {
            this.testSingle(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000),
                    r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d),
                    r.nextInt(), r.nextInt(1, 17));
        }
    }

    protected void testSingle(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves) {
        double javaVal = CWGNoiseProvider.JAVA_INSTANCE.generateSingle(x, y, z, freqX, freqY, freqZ, seed, octaves);

        double nativeVal = CWGNoiseProvider.INSTANCE.generateSingle(x, y, z, freqX, freqY, freqZ, seed, octaves);

        if (Math.round(javaVal * 100000.0d) != Math.round(nativeVal * 100000.0d)) {
            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s, %s): %s (java) != %s (native)", x * freqX, y * freqY, z * freqZ, javaVal, nativeVal));
        }
    }
}
