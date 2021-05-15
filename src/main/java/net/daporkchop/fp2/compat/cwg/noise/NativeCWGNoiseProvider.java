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

import com.flowpowered.noise.Utils;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PCleaner;

import java.util.Random;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Native (and vectorized) implementation of {@link CWGNoiseProvider}.
 *
 * @author DaPorkchop_
 */
class NativeCWGNoiseProvider extends JavaCWGNoiseProvider {
    public NativeCWGNoiseProvider() {
        float[] randomVectors = new float[Utils.RANDOM_VECTORS.length];
        for (int i = 0; i < Utils.RANDOM_VECTORS.length; i++) {
            double d = Utils.RANDOM_VECTORS[i];
            float f = (float) d;
            checkState(d == (double) f, "RANDOM_VECTORS[%d]: original value %s cannot be converted to float %s without losing precision!", (Integer) i, (Double) d, (Float) f);
            randomVectors[i] = f;
        }
        this.setRandomVectors(randomVectors);
    }

    protected native void setRandomVectors(@NonNull float[] randomVectors);

    @Override
    public native void generate3d(@NonNull double[] out, int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves, double scale);

    @Override
    public native void generate2d(@NonNull double[] out, int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves, double scale);

    @Override
    public native double generateSingle(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves, double scale);

    @Override
    public Configured forSettings(@NonNull CustomGeneratorSettings settings, long seed) {
        return new ConfiguredImpl(settings, seed);
    }

    @Override
    public boolean isNative() {
        return true;
    }

    /**
     * Native (and vectorized) implementation of {@link Configured}.
     *
     * @author DaPorkchop_
     */
    protected static class ConfiguredImpl extends JavaCWGNoiseProvider.ConfiguredImpl {
        //ew gross look at all those parameters
        //absolutely disgusting
        //what kind of horrible person would do such a thing

        protected static native long createState0(
                double heightVariationFactor, double specialHeightVariationFactorBelowAverageY, double heightVariationOffset, double heightFactor, double heightOffset,
                double selectorNoiseFactor, double selectorNoiseOffset, double selectorNoiseFrequencyX, double selectorNoiseFrequencyY, double selectorNoiseFrequencyZ, int selectorNoiseSeed, int selectorNoiseOctaves, double selectorNoiseScale,
                double lowNoiseFactor, double lowNoiseOffset, double lowNoiseFrequencyX, double lowNoiseFrequencyY, double lowNoiseFrequencyZ, int lowNoiseSeed, int lowNoiseOctaves, double lowNoiseScale,
                double highNoiseFactor, double highNoiseOffset, double highNoiseFrequencyX, double highNoiseFrequencyY, double highNoiseFrequencyZ, int highNoiseSeed, int highNoiseOctaves, double highNoiseScale,
                double depthNoiseFactor, double depthNoiseOffset, double depthNoiseFrequencyX, double depthNoiseFrequencyZ, int depthNoiseSeed, int depthNoiseOctaves, double depthNoiseScale);

        protected static native void deleteState0(long state);

        protected final long state;

        public ConfiguredImpl(@NonNull CustomGeneratorSettings settings, long seed) {
            super(settings, seed);

            Random rng = new Random(seed);
            this.state = createState0(
                    this.heightVariationFactor, this.specialHeightVariationFactorBelowAverageY, this.heightVariationOffset, this.heightFactor, this.heightOffset,
                    this.selectorNoiseFactor, this.selectorNoiseOffset, this.selectorNoiseFrequencyX, this.selectorNoiseFrequencyY, this.selectorNoiseFrequencyZ, this.selectorNoiseSeed, this.selectorNoiseOctaves, this.selectorNoiseScale,
                    this.lowNoiseFactor, this.lowNoiseOffset, this.lowNoiseFrequencyX, this.lowNoiseFrequencyY, this.lowNoiseFrequencyZ, this.lowNoiseSeed, this.lowNoiseOctaves, this.lowNoiseScale,
                    this.highNoiseFactor, this.highNoiseOffset, this.highNoiseFrequencyX, this.highNoiseFrequencyY, this.highNoiseFrequencyZ, this.highNoiseSeed, this.highNoiseOctaves, this.highNoiseScale,
                    this.depthNoiseFactor, this.depthNoiseOffset, this.depthNoiseFrequencyX, this.depthNoiseFrequencyZ, this.depthNoiseSeed, this.depthNoiseOctaves, this.depthNoiseScale);

            //register cleaner for off-heap state
            PCleaner.cleaner(this, new Releaser(this.state));
        }

        @Override
        public void generateDepth2d(@NonNull double[] out, int baseX, int baseZ, int scaleX, int scaleZ, int sizeX, int sizeZ) {
            this.generateDepth2d0(out, baseX, baseZ, scaleX, scaleZ, sizeX, sizeZ, this.state);
        }

        protected native void generateDepth2d0(@NonNull double[] out, int baseX, int baseZ, int scaleX, int scaleZ, int sizeX, int sizeZ, long state);

        @Override
        public double generateDepthSingle(int x, int z) {
            return this.generateDepthSingle0(x, z, this.state);
        }

        protected native double generateDepthSingle0(int x, int z, long state);

        @Override
        public void generate3d(@NonNull double[] heightIn, @NonNull double[] variationIn, @NonNull double[] out, int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ) {
            this.generate3d0noDepth(heightIn, variationIn, out, baseX, baseY, baseZ, scaleX, scaleY, scaleZ, sizeX, sizeY, sizeZ, this.state);
        }

        protected native void generate3d0noDepth(@NonNull double[] heightIn, @NonNull double[] variationIn, @NonNull double[] out, int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ, long state);

        @Override
        public void generate3d(@NonNull double[] heightIn, @NonNull double[] variationIn, @NonNull double[] depthIn, @NonNull double[] out, int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ) {
            this.generate3d0depth(heightIn, variationIn, depthIn, out, baseX, baseY, baseZ, scaleX, scaleY, scaleZ, sizeX, sizeY, sizeZ, this.state);
        }

        protected native void generate3d0depth(@NonNull double[] heightIn, @NonNull double[] variationIn, @NonNull double[] depthIn, @NonNull double[] out, int baseX, int baseY, int baseZ, int scaleX, int scaleY, int scaleZ, int sizeX, int sizeY, int sizeZ, long state);

        @Override
        public double generateSingle(double height, double variation, int x, int y, int z) {
            return this.generateSingle0noDepth(height, variation, x, y, z, this.state);
        }

        protected native double generateSingle0noDepth(double height, double variation, int x, int y, int z, long state);

        @Override
        public double generateSingle(double height, double variation, double depth, int x, int y, int z) {
            return this.generateSingle0depth(height, variation, depth, x, y, z, this.state);
        }

        protected native double generateSingle0depth(double height, double variation, double depth, int x, int y, int z, long state);

        /**
         * A function which deletes a {@link ConfiguredImpl}'s off-heap state.
         *
         * @author DaPorkchop_
         */
        @RequiredArgsConstructor
        protected static class Releaser implements Runnable {
            protected final long state;

            @Override
            public void run() {
                deleteState0(this.state);
            }
        }
    }
}
