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

package net.daporkchop.fp2.compat.cwg.noise;

import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import lombok.NonNull;
import net.daporkchop.fp2.compat.x86.x86FeatureDetector;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.natives.Feature;
import net.daporkchop.lib.natives.FeatureBuilder;

import static com.flowpowered.noise.module.source.Perlin.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * Faster generation of CWG-style noise.
 *
 * @author DaPorkchop_
 */
public interface CWGNoiseProvider extends Feature<CWGNoiseProvider> {
    CWGNoiseProvider INSTANCE = FeatureBuilder.<CWGNoiseProvider>create(CWGNoiseProvider.class)
            .addNative("net.daporkchop.fp2.compat.cwg.noise.NativeCWGNoiseProvider", x86FeatureDetector.INSTANCE.maxSupportedVectorExtension())
            .addJava("net.daporkchop.fp2.compat.cwg.noise.JavaCWGNoiseProvider")
            .build(true);

    CWGNoiseProvider JAVA_INSTANCE = INSTANCE.isNative() ? new JavaCWGNoiseProvider() : INSTANCE;

    static double scale(int octaves) {
        double maxValue = ((1.0d / (1 << octaves)) - 1.0d) * (1.0d / (DEFAULT_PERLIN_PERSISTENCE - 1.0d));
        return 2.0d / maxValue;
    }

    static int packSeed(long seed) {
        return (int) ((seed) ^ (seed >>> 32L));
    }

    /**
     * @see #scale(int)
     * @see #generate3d(double[], int, int, int, int, double, double, double, int, int, int, int, int, double)
     */
    @Deprecated
    default void generate3d(@NonNull double[] out, int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves) {
        this.generate3d(out, baseX, baseY, baseZ, level, freqX, freqY, freqZ, sizeX, sizeY, sizeZ, seed, octaves, scale(octaves));
    }

    void generate3d(@NonNull double[] out, int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves, double scale);

    /**
     * @see #scale(int)
     * @see #generate2d(double[], int, int, int, double, double, int, int, int, int, double)
     */
    @Deprecated
    default void generate2d(@NonNull double[] out, int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves) {
        this.generate2d(out, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves, scale(octaves));
    }

    void generate2d(@NonNull double[] out, int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves, double scale);

    /**
     * @see #scale(int)
     * @see #generateSingle(int, int, int, double, double, double, int, int, double)
     */
    @Deprecated
    default double generateSingle(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves) {
        return this.generateSingle(x, y, z, freqX, freqY, freqZ, seed, octaves, scale(octaves));
    }

    double generateSingle(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves, double scale);

    Configured forSettings(@NonNull CustomGeneratorSettings settings, long seed);

    /**
     * Alternative to {@link CWGNoiseProvider} which computes all the different noise generator outputs at once and combines them.
     * <p>
     * Instances of this class are pre-configured with a given {@link CustomGeneratorSettings} at construction time.
     *
     * @author DaPorkchop_
     * @see #forSettings(CustomGeneratorSettings, long)
     */
    interface Configured {
        //depth noise generation

        void generateDepth2d(@NonNull double[] out, int baseX, int baseZ, int scaleX, int scaleZ, int sizeX, int sizeZ);

        double generateDepthSingle(int x, int z);

        //full noise generation

        default void generate3d(@NonNull double[] heightIn, @NonNull double[] variationIn, @NonNull double[] out, int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ) {
            ArrayAllocator<double[]> alloc = ALLOC_DOUBLE.get();
            double[] depth = alloc.atLeast(sizeX * sizeZ);
            try {
                //generate depth noise
                this.generateDepth2d(depth, baseX, baseZ, scaleX, scaleZ, sizeX, sizeZ);

                //use generated depth noise for full noise generation
                this.generate3d(heightIn, variationIn, depth, out, baseX, baseY, baseZ, scaleX, scaleY, scaleZ, sizeX, sizeY, sizeZ);
            } finally {
                alloc.release(depth);
            }
        }

        void generate3d(@NonNull double[] heightIn, @NonNull double[] variationIn, @NonNull double[] depthIn, @NonNull double[] out, int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ);

        default double generateSingle(double height, double variation, int x, int y, int z) {
            return this.generateSingle(height, variation, this.generateDepthSingle(x, z), x, y, z);
        }

        double generateSingle(double height, double variation, double depth, int x, int y, int z);
    }
}
