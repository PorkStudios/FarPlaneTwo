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

package net.daporkchop.fp2.compat.cwg;

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.Utils;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.replacer.SwampWaterWithLilypadReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.List;
import java.util.Map;

import static com.flowpowered.noise.module.source.Perlin.*;
import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class CWGHelper {
    protected static final long BIOMEBLOCKREPLACERS_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeBlockReplacers");
    protected static final long BIOMEGEN_OFFSET = PUnsafe.pork_getOffset(BiomeSource.class, "biomeGen");

    public static Map<Biome, List<IBiomeBlockReplacer>> getReplacerMap(@NonNull BiomeSource biomeSource) {
        return PUnsafe.getObject(biomeSource, BIOMEBLOCKREPLACERS_OFFSET);
    }

    public static BiomeProvider getBiomeGen(@NonNull BiomeSource biomeSource) {
        return PUnsafe.getObject(biomeSource, BIOMEGEN_OFFSET);
    }

    public static IBiomeBlockReplacer[][] blockReplacerMapToArray(@NonNull Map<Biome, List<IBiomeBlockReplacer>> replacerMap) {
        int maxBiomeId = replacerMap.keySet().stream().mapToInt(Biome::getIdForBiome).max().orElse(0);
        IBiomeBlockReplacer[][] biomeReplacers = new IBiomeBlockReplacer[maxBiomeId + 1][];
        replacerMap.forEach((biome, list) -> biomeReplacers[Biome.getIdForBiome(biome)]
                = list.stream().filter(r -> !(r instanceof SwampWaterWithLilypadReplacer)).toArray(IBiomeBlockReplacer[]::new));
        return biomeReplacers;
    }

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

    /**
     * Estimates the terrain height at the given X and Z coordinates using the given {@link IBuilder}.
     *
     * @param builder the {@link IBuilder} to get density values from
     * @param x       the X coordinate (in blocks)
     * @param z       the Z coordinate (in blocks)
     * @return the estimated terrain height value
     */
    public static int getHeight(@NonNull IBuilder builder, int x, int z) {
        int y = Integer.MIN_VALUE;
        int step = Integer.MAX_VALUE >> 1;

        do {
            for (int yyPrev = y, yy = y + step; yy > yyPrev; yyPrev = yy, yy += step) {
                if (builder.get(x, yy, z) <= 0.0d) { //non-solid
                    y = yyPrev;
                    break;
                }
            }
        } while ((step >>= 1) != 0);
        return y;
    }
}
