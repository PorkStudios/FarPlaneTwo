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

package net.daporkchop.fp2.compat.vanilla.biome.weight;

import lombok.Getter;
import lombok.NonNull;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;
import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelperCached.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class VanillaBiomeWeightHelper implements BiomeWeightHelper {
    protected final double[] heights;
    protected final double[] variations;
    protected final double[] weightFactors;

    protected final double[] smoothWeights;
    @Getter
    protected final int smoothRadius;
    @Getter
    protected final int smoothDiameter;

    public VanillaBiomeWeightHelper(double depthOffset, double depthFactor, double scaleOffset, double scaleFactor, int smoothRadius) {
        this.smoothRadius = notNegative(smoothRadius, "smoothRadius");
        this.smoothDiameter = smoothRadius * 2 + 1;
        this.smoothWeights = new double[this.smoothDiameter * this.smoothDiameter];
        for (int i = 0, x = -this.smoothRadius; x <= this.smoothRadius; x++) {
            for (int z = -this.smoothRadius; z <= this.smoothRadius; z++) {
                this.smoothWeights[i++] = 10.0d / Math.sqrt(z * z + x * x + 0.2d);
            }
        }

        this.heights = new double[BIOME_COUNT];
        this.variations = new double[BIOME_COUNT];
        this.weightFactors = new double[BIOME_COUNT];

        for (int id = 0; id < BIOME_COUNT; id++) {
            this.weightFactors[id] = weightFactor(this.heights[id] = getBiomeBaseHeight(id) * depthFactor + depthOffset);
            this.variations[id] = getBiomeHeightVariation(id) * scaleFactor + scaleOffset;
        }
    }

    @Override
    public void compute(@NonNull int[] biomesIn, int inOffset, int inScaleX, @NonNull double[] heightsOut, @NonNull double[] variationsOut, int outIdx) {
        final int smoothRadius = this.smoothRadius;
        final int smoothDiameter = this.smoothDiameter;

        final double centerBiomeRawHeight = getBiomeBaseHeight(biomesIn[inOffset + smoothRadius * smoothDiameter + smoothRadius]);

        double smoothHeight = 0.0d;
        double smoothVariation = 0.0d;
        double biomeWeightSum = 0.0d;

        for (int i = 0, dx = -smoothRadius; dx <= smoothRadius; dx++) {
            for (int j = inOffset + (dx + smoothRadius) * inScaleX, dz = -smoothRadius; dz <= smoothRadius; dz++, i++, j++) {
                int id = biomesIn[j];

                double weight = this.smoothWeights[i] * this.weightFactors[id];
                if (getBiomeBaseHeight(id) > centerBiomeRawHeight) {
                    weight *= 0.5d;
                }

                smoothHeight += this.heights[id] * weight;
                smoothVariation += this.variations[id] * weight;
                biomeWeightSum += weight;
            }
        }

        double f = 1.0d / biomeWeightSum;
        heightsOut[outIdx] = smoothHeight * f;
        variationsOut[outIdx] = smoothVariation * f;
    }
}
