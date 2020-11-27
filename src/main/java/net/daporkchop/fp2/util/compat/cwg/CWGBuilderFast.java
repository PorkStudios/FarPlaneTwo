/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.util.compat.cwg;

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.Utils;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomTerrainGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import lombok.NonNull;
import net.daporkchop.lib.common.math.PMath;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.Random;

import static com.flowpowered.noise.module.source.Perlin.*;
import static java.lang.Math.*;

/**
 * Functionally equivalent to {@link CustomTerrainGenerator}'s massive tree of chained {@link IBuilder} lambdas, but with everything in a single
 * object.
 *
 * @author DaPorkchop_
 */
public class CWGBuilderFast extends CustomGeneratorSettings implements IBuilder { //extending CustomGeneratorSettings eliminates an indirection
    protected static double perlin(int seed, double x, double y, double z, int octaves) {
        double value = 0.0d;
        double curPersistence = 1.0d;

        for (int curOctave = 0; curOctave < octaves; curOctave++) {
            // Make sure that these floating-point values have the same range as a 32-
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

    protected static double normalize(double value, double scale) {
        return value * scale - 1.0d;
    }

    protected static double sample(int seed, double x, double y, double z, int octaves, double freqX, double freqY, double freqZ, double scale) {
        return normalize(perlin(seed, x * freqX, y * freqY, z * freqZ, octaves), scale);
    }

    protected static int packSeed(long seed) {
        return (int) ((seed) ^ (seed >>> 32L));
    }

    protected static double scale(int octaves) {
        double maxValue = (pow(DEFAULT_PERLIN_PERSISTENCE, octaves) - 1.0d) / (DEFAULT_PERLIN_PERSISTENCE - 1.0d);
        return 2.0d / maxValue;
    }

    protected final int selectorSeed;
    protected final int lowSeed;
    protected final int highSeed;
    protected final int randomHeight2dSeed;
    protected final double selectorScale;
    protected final double lowScale;
    protected final double highScale;
    protected final double randomHeight2dScale;

    protected final CWGContext cwgContext;
    protected final BiomeSource biomes;

    public CWGBuilderFast(@NonNull CWGContext cwgContext, @NonNull World world, @NonNull BiomeSource biomes) {
        this.cwgContext = cwgContext;
        this.biomes = biomes;

        try {
            for (Field field : CustomGeneratorSettings.class.getDeclaredFields()) {
                field.set(this, field.get(cwgContext));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Random rnd = new Random(world.getSeed());
        this.selectorSeed = packSeed(rnd.nextLong());
        this.lowSeed = packSeed(rnd.nextLong());
        this.highSeed = packSeed(rnd.nextLong());
        this.randomHeight2dSeed = packSeed(rnd.nextLong());

        this.selectorScale = scale(this.selectorNoiseOctaves);
        this.lowScale = scale(this.lowNoiseOctaves);
        this.highScale = scale(this.highNoiseOctaves);
        this.randomHeight2dScale = scale(this.depthNoiseOctaves);
    }

    @Override
    public double get(int x, int y, int z) {
        double height = this.cwgContext.getBaseHeight(x, z) * this.heightFactor + this.heightOffset;
        double variation = this.cwgContext.getHeightVariation(x, z) * (height > y ? this.specialHeightVariationFactorBelowAverageY : 1.0d) * this.heightVariationFactor + this.heightVariationOffset;
        double d = PMath.lerp(this.low(x, y, z), this.high(x, y, z), this.selector(x, y, z)) + this.randomHeight2d(x, y, z);
        d = d * variation + height;
        return d - Math.signum(variation) * y;
    }

    public double selector(double x, double y, double z) {
        double d = sample(this.selectorSeed, x, y, z, this.selectorNoiseOctaves, this.selectorNoiseFrequencyX, this.selectorNoiseFrequencyY, this.selectorNoiseFrequencyZ, this.selectorScale);
        return PMath.clamp(d * this.selectorNoiseFactor + this.selectorNoiseOffset, 0.0d, 1.0d);
    }

    public double low(double x, double y, double z) {
        double d = sample(this.lowSeed, x, y, z, this.lowNoiseOctaves, this.lowNoiseFrequencyX, this.lowNoiseFrequencyY, this.lowNoiseFrequencyZ, this.lowScale);
        return d * this.lowNoiseFactor + this.lowNoiseOffset;
    }

    public double high(double x, double y, double z) {
        double d = sample(this.highSeed, x, y, z, this.highNoiseOctaves, this.highNoiseFrequencyX, this.highNoiseFrequencyY, this.highNoiseFrequencyZ, this.highScale);
        return d * this.highNoiseFactor + this.highNoiseOffset;
    }

    public double randomHeight2d(double x, double y, double z) {
        double d = sample(this.randomHeight2dSeed, x, y, z, this.depthNoiseOctaves, this.depthNoiseFrequencyX, 0.0d, this.depthNoiseFrequencyZ, this.randomHeight2dScale);
        d = d * this.depthNoiseFactor + this.depthNoiseOffset;
        if (d < 0.0d) {
            d *= -0.3d;
        }
        d = PMath.clamp(d * 3.0d - 2.0d, -2.0d, 1.0d);
        if (d < 0.0d) {
            d /= 2.0d * 2.0d * 1.4d;
        } else {
            d /= 8.0d;
        }
        return d * 0.2d * 17.0d / 64.0d;
    }
}
