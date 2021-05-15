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

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.Utils;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastJavaPRandom;

import java.util.Random;

import static com.flowpowered.noise.module.source.Perlin.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * Pure-java implementation of {@link CWGNoiseProvider}.
 *
 * @author DaPorkchop_
 */
class JavaCWGNoiseProvider implements CWGNoiseProvider {
    protected static double perlin(int seed, double x, double y, double z, int octaves) {
        double value = 0.0d;
        double curPersistence = 1.0d;

        for (int curOctave = 0; curOctave < octaves; curOctave++) {
            // Make sure that these doubleing-point values have the same range as a 32-
            // bit integer so that we can pass them to the coherent-noise functions.
            double nx = Utils.makeInt32Range(x);
            double ny = Utils.makeInt32Range(y);
            double nz = Utils.makeInt32Range(z);

            // Get the coherent-noise value from the input value and add it to the
            // final result.
            value += Noise.gradientCoherentNoise3D(nx, ny, nz, seed + curOctave, DEFAULT_PERLIN_QUALITY) * curPersistence;

            // Prepare the next octave.
            x *= DEFAULT_PERLIN_LACUNARITY;
            y *= DEFAULT_PERLIN_LACUNARITY;
            z *= DEFAULT_PERLIN_LACUNARITY;
            curPersistence *= DEFAULT_PERLIN_PERSISTENCE;
        }

        return value;
    }

    protected static double sample(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves, double scale) {
        return perlin(seed, x * freqX, y * freqY, z * freqZ, octaves) * scale - 1.0d;
    }

    @Override
    public void generate3d(@NonNull double[] out, int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves, double scale) {
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    out[i] = sample(baseX + (dx << level), baseY + (dy << level), baseZ + (dz << level), freqX, freqY, freqZ, seed, octaves, scale);
                }
            }
        }
    }

    @Override
    public void generate2d(@NonNull double[] out, int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves, double scale) {
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, i++) {
                out[i] = sample(baseX + (dx << level), 0, baseZ + (dz << level), freqX, 0.0d, freqZ, seed, octaves, scale);
            }
        }
    }

    @Override
    public double generateSingle(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves, double scale) {
        return sample(x, y, z, freqX, freqY, freqZ, seed, octaves, scale);
    }

    @Override
    public Configured forSettings(@NonNull CustomGeneratorSettings settings, long seed) {
        return new ConfiguredImpl(settings, seed);
    }

    @Override
    public boolean isNative() {
        return false;
    }

    /**
     * Pure-java implementation of {@link Configured}.
     *
     * @author DaPorkchop_
     */
    protected static class ConfiguredImpl implements Configured {
        protected final double heightVariationFactor;
        protected final double specialHeightVariationFactorBelowAverageY;
        protected final double heightVariationOffset;
        protected final double heightFactor;
        protected final double heightOffset;

        protected final double depthNoiseFactor;
        protected final double depthNoiseOffset;
        protected final double depthNoiseFrequencyX;
        protected final double depthNoiseFrequencyZ;
        protected final int depthNoiseSeed;
        protected final int depthNoiseOctaves;
        protected final double depthNoiseScale;

        protected final double selectorNoiseFactor;
        protected final double selectorNoiseOffset;
        protected final double selectorNoiseFrequencyX;
        protected final double selectorNoiseFrequencyY;
        protected final double selectorNoiseFrequencyZ;
        protected final int selectorNoiseSeed;
        protected final int selectorNoiseOctaves;
        protected final double selectorNoiseScale;

        protected final double lowNoiseFactor;
        protected final double lowNoiseOffset;
        protected final double lowNoiseFrequencyX;
        protected final double lowNoiseFrequencyY;
        protected final double lowNoiseFrequencyZ;
        protected final int lowNoiseSeed;
        protected final int lowNoiseOctaves;
        protected final double lowNoiseScale;

        protected final double highNoiseFactor;
        protected final double highNoiseOffset;
        protected final double highNoiseFrequencyX;
        protected final double highNoiseFrequencyY;
        protected final double highNoiseFrequencyZ;
        protected final int highNoiseSeed;
        protected final int highNoiseOctaves;
        protected final double highNoiseScale;

        public ConfiguredImpl(@NonNull CustomGeneratorSettings settings, long seed) {
            this.heightVariationFactor = settings.heightVariationFactor;
            this.specialHeightVariationFactorBelowAverageY = settings.specialHeightVariationFactorBelowAverageY;
            this.heightVariationOffset = settings.heightVariationOffset;
            this.heightFactor = settings.heightFactor;
            this.heightOffset = settings.heightOffset;

            this.depthNoiseFactor = settings.depthNoiseFactor;
            this.depthNoiseOffset = settings.depthNoiseOffset;
            this.depthNoiseFrequencyX = settings.depthNoiseFrequencyX;
            this.depthNoiseFrequencyZ = settings.depthNoiseFrequencyZ;
            this.depthNoiseOctaves = settings.depthNoiseOctaves;

            this.selectorNoiseFactor = settings.selectorNoiseFactor;
            this.selectorNoiseOffset = settings.selectorNoiseOffset;
            this.selectorNoiseFrequencyX = settings.selectorNoiseFrequencyX;
            this.selectorNoiseFrequencyY = settings.selectorNoiseFrequencyY;
            this.selectorNoiseFrequencyZ = settings.selectorNoiseFrequencyZ;
            this.selectorNoiseOctaves = settings.selectorNoiseOctaves;

            this.lowNoiseFactor = settings.lowNoiseFactor;
            this.lowNoiseOffset = settings.lowNoiseOffset;
            this.lowNoiseFrequencyX = settings.lowNoiseFrequencyX;
            this.lowNoiseFrequencyY = settings.lowNoiseFrequencyY;
            this.lowNoiseFrequencyZ = settings.lowNoiseFrequencyZ;
            this.lowNoiseOctaves = settings.lowNoiseOctaves;

            this.highNoiseFactor = settings.highNoiseFactor;
            this.highNoiseOffset = settings.highNoiseOffset;
            this.highNoiseFrequencyX = settings.highNoiseFrequencyX;
            this.highNoiseFrequencyY = settings.highNoiseFrequencyY;
            this.highNoiseFrequencyZ = settings.highNoiseFrequencyZ;
            this.highNoiseOctaves = settings.highNoiseOctaves;

            this.depthNoiseScale = CWGNoiseProvider.scale(this.depthNoiseOctaves);
            this.lowNoiseScale = CWGNoiseProvider.scale(this.lowNoiseOctaves);
            this.highNoiseScale = CWGNoiseProvider.scale(this.highNoiseOctaves);
            this.selectorNoiseScale = CWGNoiseProvider.scale(this.selectorNoiseOctaves);

            Random rng = new Random(seed);
            this.selectorNoiseSeed = CWGNoiseProvider.packSeed(rng.nextLong());
            this.lowNoiseSeed = CWGNoiseProvider.packSeed(rng.nextLong());
            this.highNoiseSeed = CWGNoiseProvider.packSeed(rng.nextLong());
            this.depthNoiseSeed = CWGNoiseProvider.packSeed(rng.nextLong());
        }

        @Override
        public void generateDepth2d(@NonNull double[] out, int baseX, int baseZ, int scaleX, int scaleZ, int sizeX, int sizeZ) {
            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    out[i] = this.generateDepthSingle(baseX + dx * scaleX, baseZ + dz * scaleZ);
                }
            }
        }

        @Override
        public double generateDepthSingle(int x, int z) {
            double d = sample(x, 0, z, this.depthNoiseFrequencyX, 0.0d, this.depthNoiseFrequencyZ, this.depthNoiseSeed, this.depthNoiseOctaves, this.depthNoiseScale) * this.depthNoiseFactor + this.depthNoiseOffset;
            d *= d < 0.0d ? -0.9d : 3.0d;
            d -= 2.0d;
            d = clamp(d * (d < 0.0d ? 5.0d / 28.0d : 0.125d), -5.0d / 14.0d, 0.125d) * (0.2d * 17.0d / 64.0d);
            return d;
        }

        @Override
        public void generate3d(@NonNull double[] heightIn, @NonNull double[] variationIn, @NonNull double[] depthIn, @NonNull double[] out, int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ) {
            for (int i3 = 0, dx = 0; dx < sizeX; dx++) {
                for (int dy = 0; dy < sizeY; dy++) {
                    for (int i2 = dx * sizeZ, dz = 0; dz < sizeZ; dz++, i2++, i3++) {
                        out[i3] = this.generateSingle(heightIn[i2], variationIn[i2], depthIn[i2], baseX + dx * scaleX, baseY + dy * scaleY, baseZ + dz * scaleZ);
                    }
                }
            }
        }

        @Override
        public double generateSingle(double height, double variation, double depth, int x, int y, int z) {
            height = height * this.heightFactor + this.heightOffset;
            variation = variation * (height > y ? this.specialHeightVariationFactorBelowAverageY : 1.0d) * this.heightVariationFactor + this.heightVariationOffset;

            double selector = sample(x, y, z, this.selectorNoiseFrequencyX, this.selectorNoiseFrequencyY, this.selectorNoiseFrequencyZ, this.selectorNoiseSeed, this.selectorNoiseOctaves, this.selectorNoiseScale) * this.selectorNoiseFactor + this.selectorNoiseOffset;
            //TODO: benchmark and see whether this is actually faster with or without conditional
            double low = selector >= 1.0d ? 0.0d : sample(x, y, z, this.lowNoiseFrequencyX, this.lowNoiseFrequencyY, this.lowNoiseFrequencyZ, this.lowNoiseSeed, this.lowNoiseOctaves, this.lowNoiseScale) * this.lowNoiseFactor + this.lowNoiseOffset;
            double high = selector < 0.0d ? 0.0d : sample(x, y, z, this.highNoiseFrequencyX, this.highNoiseFrequencyY, this.highNoiseFrequencyZ, this.highNoiseSeed, this.highNoiseOctaves, this.highNoiseScale) * this.highNoiseFactor + this.highNoiseOffset;

            double d = lerp(low, high, clamp(selector, 0.0d, 1.0d)) + depth;
            d = d * variation + height;
            return d - Math.signum(variation) * y;
        }
    }
}
