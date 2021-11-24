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
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import net.daporkchop.fp2.compat.cwg.noise.CWGNoiseProvider;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.world.gen.NoiseGeneratorImproved;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import util.FP2Test;

import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("deprecation")
public class TestCwgNoiseGen {
    protected static CWGNoiseProvider.Configured CONFIGURED_JAVA;
    protected static CWGNoiseProvider.Configured CONFIGURED_NATIVE;

    @BeforeClass
    public static void aaa_ensureNativeNoiseGenIsAvailable() {
        FP2Test.init();

        checkState(CWGNoiseProvider.INSTANCE.isNative(), "native noise generation must be available for testing!");

        CustomGeneratorSettings settings = new CustomGeneratorSettings();
        long seed = 102978420983752L;
        CONFIGURED_JAVA = CWGNoiseProvider.JAVA_INSTANCE.forSettings(settings, seed);
        CONFIGURED_NATIVE = CWGNoiseProvider.INSTANCE.forSettings(settings, seed);
    }

    /**
     * Copypasta of {@link ConversionUtils#initFlowNoiseHack()}, but accessing the gradient fields in {@link NoiseGeneratorImproved} directly (since the accessor mixin obviously
     * isn't being applied in a unit test environment).
     */
    @BeforeClass
    public static void bbb_initFlowNoiseHack() {
        SplittableRandom random = new SplittableRandom(123456789);
        for (int i = 0; i < Utils.RANDOM_VECTORS.length / 4; i++) {
            int j = random.nextInt(NoiseGeneratorImproved.GRAD_X.length);
            Utils.RANDOM_VECTORS[i * 4] = NoiseGeneratorImproved.GRAD_X[j] / 2;
            Utils.RANDOM_VECTORS[i * 4 + 1] = NoiseGeneratorImproved.GRAD_Y[j] / 2;
            Utils.RANDOM_VECTORS[i * 4 + 2] = NoiseGeneratorImproved.GRAD_Z[j] / 2;
        }
    }

    protected static boolean approxEquals(double a, double b) {
        return Math.abs(a - b) <= 0.000000001d;
    }

    @Test
    public void testNormal_3d() {
        SplittableRandom r = new SplittableRandom(67890L);

        this.testNormal_3d(2, 2, 2, 0, 0.2d, 0.2d, 0.2d, 32, 32, 32, 237582, 4).join();
        this.testNormal_3d(2, 2, 2, 0, 0.2d, 0.2d, 0.2d, 32, 32, 32, 237582, 5).join();

        CompletableFuture<?>[] futures = uncheckedCast(new CompletableFuture[256]);
        for (int i = 0; i < futures.length; i++) {
            futures[i] = this.testNormal_3d(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(4),
                    r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d),
                    r.nextInt(1, 65), r.nextInt(1, 65), r.nextInt(1, 65),
                    r.nextInt(), r.nextInt(1, 17));
        }
        CompletableFuture.allOf(futures).join();
    }

    protected CompletableFuture<Void> testNormal_3d(int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves) {
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
                        if (!approxEquals(javaVal, nativeVal)) {
                            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s, %s): %s (java) != %s (native)", (baseX + (dx << level)) * freqX, (baseY + (dy << level)) * freqY, (baseZ + (dz << level)) * freqZ, javaVal, nativeVal));
                        }
                    }
                }
            }
        });
    }

    @Test
    public void testNormal_2d() {
        SplittableRandom r = new SplittableRandom(12345L);

        this.testNormal_2d(2, 2, 0, 0.2d, 0.2d, 32, 32, 237582, 4).join();
        this.testNormal_2d(2, 2, 0, 0.2d, 0.2d, 32, 32, 237582, 5).join();

        CompletableFuture<?>[] futures = uncheckedCast(new CompletableFuture[256]);
        for (int i = 0; i < futures.length; i++) {
            futures[i] = this.testNormal_2d(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(4),
                    r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d),
                    r.nextInt(1, 257), r.nextInt(1, 257),
                    r.nextInt(), r.nextInt(1, 17));
        }
        CompletableFuture.allOf(futures).join();
    }

    protected CompletableFuture<Void> testNormal_2d(int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves) {
        return CompletableFuture.runAsync(() -> {
            double[] outJava = new double[sizeX * sizeZ];
            CWGNoiseProvider.JAVA_INSTANCE.generate2d(outJava, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves);

            double[] outNative = new double[sizeX * sizeZ];
            CWGNoiseProvider.INSTANCE.generate2d(outNative, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    double javaVal = outJava[i];
                    double nativeVal = outNative[i];
                    if (!approxEquals(javaVal, nativeVal)) {
                        throw new IllegalStateException(PStrings.fastFormat("@(%s, %s): %s (java) != %s (native)", (baseX + (dx << level)) * freqX, (baseZ + (dz << level)) * freqZ, javaVal, nativeVal));
                    }
                }
            }
        });
    }

    @Test
    public void testNormal_Single() {
        SplittableRandom r = new SplittableRandom(67890L);

        this.testNormal_Single(2, 2, 2, 0.2d, 0.2d, 0.2d, 237582, 4);
        this.testNormal_Single(2, 2, 2, 0.2d, 0.2d, 0.2d, 237582, 5);

        for (int i = 0; i < 256; i++) {
            this.testNormal_Single(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000),
                    r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d), r.nextDouble(Double.MIN_VALUE, 100000.0d),
                    r.nextInt(), r.nextInt(1, 17));
        }
    }

    protected void testNormal_Single(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves) {
        double javaVal = CWGNoiseProvider.JAVA_INSTANCE.generateSingle(x, y, z, freqX, freqY, freqZ, seed, octaves);
        double nativeVal = CWGNoiseProvider.INSTANCE.generateSingle(x, y, z, freqX, freqY, freqZ, seed, octaves);
        if (!approxEquals(javaVal, nativeVal)) {
            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s, %s): %s (java) != %s (native)", x * freqX, y * freqY, z * freqZ, javaVal, nativeVal));
        }
    }

    @Test
    public void testConfigured_depthSingle() {
        SplittableRandom r = new SplittableRandom(67890L);

        this.testConfigured_depthSingle(2, 2);

        for (int i = 0; i < 256; i++) {
            this.testConfigured_depthSingle(r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000));
        }
    }

    protected void testConfigured_depthSingle(int x, int z) {
        double javaVal = CONFIGURED_JAVA.generateDepthSingle(x, z);
        double nativeVal = CONFIGURED_NATIVE.generateDepthSingle(x, z);
        if (!approxEquals(javaVal, nativeVal)) {
            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s): %s (java) != %s (native)", x, z, javaVal, nativeVal));
        }
    }

    @Test
    public void testConfigured_single_noDepth() {
        SplittableRandom r = new SplittableRandom(67890L);

        this.testConfigured_single_noDepth(1.0d, 1.0d, 2, 2, 2);

        for (int i = 0; i < 256; i++) {
            this.testConfigured_single_noDepth(
                    r.nextDouble(-10, 10), r.nextDouble(-10, 10),
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000));
        }
    }

    protected void testConfigured_single_noDepth(double height, double variation, int x, int y, int z) {
        double javaVal = CONFIGURED_JAVA.generateSingle(height, variation, x, y, z);
        double nativeVal = CONFIGURED_NATIVE.generateSingle(height, variation, x, y, z);
        if (!approxEquals(javaVal, nativeVal)) {
            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s, %s): %s (java) != %s (native)", x, y, z, javaVal, nativeVal));
        }
    }

    @Test
    public void testConfigured_single_withDepth() {
        SplittableRandom r = new SplittableRandom(67890L);

        this.testConfigured_single_withDepth(1.0d, 1.0d, 0.0d, 2, 2, 2);

        for (int i = 0; i < 256; i++) {
            this.testConfigured_single_withDepth(
                    r.nextDouble(-10, 10), r.nextDouble(-10, 10), r.nextDouble(-1000, 1000),
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000));
        }
    }

    protected void testConfigured_single_withDepth(double height, double variation, double depth, int x, int y, int z) {
        double javaVal = CONFIGURED_JAVA.generateSingle(height, variation, depth, x, y, z);
        double nativeVal = CONFIGURED_NATIVE.generateSingle(height, variation, depth, x, y, z);
        if (!approxEquals(javaVal, nativeVal)) {
            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s, %s): %s (java) != %s (native)", x, y, z, javaVal, nativeVal));
        }
    }

    @Test
    public void testConfigured_depth2d() {
        SplittableRandom r = new SplittableRandom(12345L);

        this.testConfigured_depth2d(2, 2, 1, 1, 32, 32).join();
        this.testConfigured_depth2d(2, 2, 1, 1, 32, 32).join();

        CompletableFuture<?>[] futures = uncheckedCast(new CompletableFuture[256]);
        for (int i = 0; i < futures.length; i++) {
            futures[i] = this.testConfigured_depth2d(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000),
                    1 << r.nextInt(4), 1 << r.nextInt(4),
                    r.nextInt(1, 257), r.nextInt(1, 257));
        }
        CompletableFuture.allOf(futures).join();
    }

    protected CompletableFuture<Void> testConfigured_depth2d(int baseX, int baseZ, int scaleX, int scaleZ, int sizeX, int sizeZ) {
        return CompletableFuture.runAsync(() -> {
            double[] outJava = new double[sizeX * sizeZ];
            CONFIGURED_JAVA.generateDepth2d(outJava, baseX, baseZ, scaleX, scaleZ, sizeX, sizeZ);

            double[] outNative = new double[sizeX * sizeZ];
            CONFIGURED_NATIVE.generateDepth2d(outNative, baseX, baseZ, scaleX, scaleZ, sizeX, sizeZ);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    double javaVal = outJava[i];
                    double nativeVal = outNative[i];
                    if (!approxEquals(javaVal, nativeVal)) {
                        throw new IllegalStateException(PStrings.fastFormat("@(%s, %s): %s (java) != %s (native)", baseX + dx * scaleX, baseZ + dz * scaleZ, javaVal, nativeVal));
                    }
                }
            }
        });
    }

    @Test
    public void testConfigured_3d_withDepth() {
        SplittableRandom r = new SplittableRandom(12345L);

        this.testConfigured_3d_withDepth(2, 2, 2, 1, 1, 1, 32, 32, 32).join();
        this.testConfigured_3d_withDepth(2, 2, 2, 1, 1, 1, 32, 32, 32).join();

        CompletableFuture<?>[] futures = uncheckedCast(new CompletableFuture[256]);
        for (int i = 0; i < futures.length; i++) {
            futures[i] = this.testConfigured_3d_withDepth(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000),
                    1 << r.nextInt(4), 1 << r.nextInt(4), 1 << r.nextInt(4),
                    r.nextInt(1, 65), r.nextInt(1, 65), r.nextInt(1, 65));
        }
        CompletableFuture.allOf(futures).join();
    }

    protected CompletableFuture<Void> testConfigured_3d_withDepth(int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ) {
        return CompletableFuture.runAsync(() -> {
            double[] heights = new double[sizeX * sizeZ];
            double[] variation = new double[sizeX * sizeZ];
            double[] depth = new double[sizeX * sizeZ];
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < sizeX * sizeZ; i++) {
                heights[i] = r.nextDouble(-10, 10);
                variation[i] = r.nextDouble(-10, 10);
                depth[i] = r.nextDouble(-1000, 1000);
            }

            double[] outJava = new double[sizeX * sizeY * sizeZ];
            CONFIGURED_JAVA.generate3d(heights, variation, depth, outJava, baseX, baseY, baseZ, scaleX, scaleY, scaleZ, sizeX, sizeY, sizeZ);

            double[] outNative = new double[sizeX * sizeY * sizeZ];
            CONFIGURED_NATIVE.generate3d(heights, variation, depth, outNative, baseX, baseY, baseZ, scaleX, scaleY, scaleZ, sizeX, sizeY, sizeZ);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dy = 0; dy < sizeY; dy++) {
                    for (int dz = 0; dz < sizeZ; dz++, i++) {
                        double javaVal = outJava[i];
                        double nativeVal = outNative[i];
                        if (!approxEquals(javaVal, nativeVal)) {
                            throw new IllegalStateException(PStrings.fastFormat("%d@(%s, %s, %s): %s (java) != %s (native)", i, baseX + dx * scaleX, baseY + dy * scaleY, baseZ + dz * scaleZ, javaVal, nativeVal));
                        }
                    }
                }
            }
        });
    }

    @Test
    public void testConfigured_3d_noDepth() {
        SplittableRandom r = new SplittableRandom(12345L);

        this.testConfigured_3d_noDepth(2, 2, 2, 0, 32, 32, 32).join();
        this.testConfigured_3d_noDepth(2, 2, 2, 0, 32, 32, 32).join();

        CompletableFuture<?>[] futures = uncheckedCast(new CompletableFuture[256]);
        for (int i = 0; i < futures.length; i++) {
            futures[i] = this.testConfigured_3d_noDepth(
                    r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(4),
                    r.nextInt(1, 65), r.nextInt(1, 65), r.nextInt(1, 65));
        }
        CompletableFuture.allOf(futures).join();
    }

    protected CompletableFuture<Void> testConfigured_3d_noDepth(int baseX, int baseY, int baseZ, int level, int sizeX, int sizeY, int sizeZ) {
        return CompletableFuture.runAsync(() -> {
            double[] heights = new double[sizeX * sizeZ];
            double[] variation = new double[sizeX * sizeZ];
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < sizeX * sizeZ; i++) {
                heights[i] = r.nextDouble(-10, 10);
                variation[i] = r.nextDouble(-10, 10);
            }

            double[] outJava = new double[sizeX * sizeY * sizeZ];
            CONFIGURED_JAVA.generate3d(heights, variation, outJava, baseX, baseY, baseZ, 1 << level, 1 << level, 1 << level, sizeX, sizeY, sizeZ);

            double[] outNative = new double[sizeX * sizeY * sizeZ];
            CONFIGURED_NATIVE.generate3d(heights, variation, outNative, baseX, baseY, baseZ, 1 << level, 1 << level, 1 << level, sizeX, sizeY, sizeZ);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dy = 0; dy < sizeY; dy++) {
                    for (int dz = 0; dz < sizeZ; dz++, i++) {
                        double javaVal = outJava[i];
                        double nativeVal = outNative[i];
                        if (!approxEquals(javaVal, nativeVal)) {
                            throw new IllegalStateException(PStrings.fastFormat("@(%s, %s, %s): %s (java) != %s (native)", baseX + (dy << level), baseY + (dy << level), baseZ + (dz << level), javaVal, nativeVal));
                        }
                    }
                }
            }
        });
    }
}
