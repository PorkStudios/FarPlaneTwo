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
import lombok.NonNull;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
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
    public void generateNoise(@NonNull double[] out, int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves, double scale) {
        this.generateNoise3d(out, baseX, baseY, baseZ, level, freqX, freqY, freqZ, sizeX, sizeY, sizeZ, seed, octaves, scale);
    }

    protected native void generateNoise3d(@NonNull double[] out, int baseX, int baseY, int baseZ, int level, double freqX, double freqY, double freqZ, int sizeX, int sizeY, int sizeZ, int seed, int octaves, double scale);

    @Override
    public void generateNoise(@NonNull double[] out, int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves, double scale) {
        this.generateNoise2d(out, baseX, baseZ, level, freqX, freqZ, sizeX, sizeZ, seed, octaves, scale);
    }

    protected native void generateNoise2d(@NonNull double[] out, int baseX, int baseZ, int level, double freqX, double freqZ, int sizeX, int sizeZ, int seed, int octaves, double scale);

    @Override
    public boolean isNative() {
        return true;
    }
}
