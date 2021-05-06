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
import lombok.NonNull;

import static com.flowpowered.noise.module.source.Perlin.*;

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

    @Override
    public void generate3d(@NonNull double[] out, int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves, double scale) {
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    out[i] = this.generateSingle(baseX + (dx << level), baseY + (dy << level), baseZ + (dz << level), freqX, freqY, freqZ, seed, octaves, scale);
                }
            }
        }
    }

    @Override
    public void generate2d(@NonNull double[] out, int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves, double scale) {
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, i++) {
                out[i] = this.generateSingle(baseX + (dx << level), 0, baseZ + (dz << level), freqX, 0.0d, freqZ, seed, octaves, scale);
            }
        }
    }

    @Override
    public double generateSingle(int x, int y, int z, double freqX, double freqY, double freqZ, int seed, int octaves, double scale) {
        return perlin(seed, x * freqX, y * freqY, z * freqZ, octaves) * scale - 1.0d;
    }

    @Override
    public boolean isNative() {
        return false;
    }
}
