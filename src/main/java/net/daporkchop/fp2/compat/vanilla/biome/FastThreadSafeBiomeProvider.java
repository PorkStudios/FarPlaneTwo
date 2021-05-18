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

package net.daporkchop.fp2.compat.vanilla.biome;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.weight.BiomeWeightHelper;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.biome.BiomeProvider;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * @author DaPorkchop_
 */
public class FastThreadSafeBiomeProvider implements IBiomeProvider {
    protected final IFastLayer biomeLayer;
    protected final IFastLayer generationLayer;

    public FastThreadSafeBiomeProvider(@NonNull BiomeProvider provider) {
        IFastLayer[] fastLayers = FastLayerProvider.INSTANCE.makeFast(provider.genBiomes, provider.biomeIndexLayer);
        this.biomeLayer = fastLayers[1];
        this.generationLayer = fastLayers[0];
    }

    @Override
    public void generateBiomes(int x, int z, int level, int size, @NonNull int[] biomes) {
        ArrayAllocator<int[]> alloc = ALLOC_INT.get();
        if (level == 0) { //max zoom level, the values are tightly packed so we just need to sample a single grid
            this.biomeLayer.getGrid(alloc, x, z, size, size, biomes);
        } else if (level == 1) {
            this.generateBiomesAtHalfResolution(alloc, x, z, size, biomes, this.biomeLayer);
        } else if (level < GTH_SHIFT) {
            this.biomeLayer.multiGetGrids(alloc, x, z, 1, 1 << level, 0, size, biomes);
        } else if (level == GTH_SHIFT) {
            this.generationLayer.getGrid(alloc, x >> GTH_SHIFT, z >> GTH_SHIFT, size, size, biomes);
        } else if (level == GTH_SHIFT + 1) {
            this.generateBiomesAtHalfResolution(alloc, x >> GTH_SHIFT, z >> GTH_SHIFT, size, biomes, this.generationLayer);
        } else { //sample a "multigrid", with each sub-grid being a 1x1 grid - this is effectively the same as getGrid but with a customizable spacing between each sample point
            this.generationLayer.multiGetGrids(alloc, x >> GTH_SHIFT, z >> GTH_SHIFT, 1, 1 << (level - GTH_SHIFT), 0, size, biomes);
        }
    }

    protected void generateBiomesAtHalfResolution(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, @NonNull int[] biomes, @NonNull IFastLayer layer) {
        int[] temp = alloc.atLeast(sq(size << 1));
        try {
            //generate biome grid at twice the ordinary size
            layer.getGrid(alloc, x, z, size << 1, size << 1, temp);

            //copy every second sample into output array
            for (int inIdx = 0, outIdx = 0, dx = 0; dx < size; dx++, inIdx += size << 1) {
                for (int dz = 0; dz < size; dz++, inIdx += 2, outIdx++) {
                    biomes[outIdx] = temp[inIdx];
                }
            }
        } finally {
            alloc.release(temp);
        }
    }

    @Override
    public void generateBiomesAndWeightedHeightsVariations(int x, int z, int level, int size, @NonNull int[] biomes, @NonNull double[] heights, @NonNull double[] variations, @NonNull BiomeWeightHelper weightHelper) {
        if (level < GTH_SHIFT) { //zoom is low enough that each biome sample is smaller than the noise generation grid, so we'll have to generate them both individually
            this.generateBiomesAndWeightedHeightsVariations_highres(x, z, level, size, biomes, heights, variations, weightHelper);
        } else { //biome layer isn't needed, because the samples are spaced at least 4 blocks apart
            this.generateBiomesAndWeightedHeightsVariations_lowres(x, z, level, size, biomes, heights, variations, weightHelper);
        }
    }

    protected void generateBiomesAndWeightedHeightsVariations_highres(int x, int z, int level, int size, @NonNull int[] biomes, @NonNull double[] heights, @NonNull double[] variations, @NonNull BiomeWeightHelper weightHelper) {
        //generate biomes as per usual
        this.generateBiomes(x, z, level, size, biomes);

        //generate biomes on generation-scale grid to compute weights
        ArrayAllocator<int[]> alloc = ALLOC_INT.get();

        int shift = GTH_SHIFT - level;
        int smoothRadius = weightHelper.smoothRadius();
        int tempBiomesSize = asrCeil(size, shift) + (smoothRadius << 1);
        int[] tempBiomes = alloc.atLeast(sq(tempBiomesSize));
        try {
            //generate biomes
            this.generationLayer.getGrid(alloc, (x >> GTH_SHIFT) - smoothRadius, (z >> GTH_SHIFT) - smoothRadius, tempBiomesSize, tempBiomesSize, tempBiomes);

            //compute weights from biome IDs
            for (int outIdx = 0, dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++, outIdx++) {
                    weightHelper.compute(tempBiomes, (dx >> shift) * tempBiomesSize + (dz >> shift), tempBiomesSize, heights, variations, outIdx);
                }
            }
        } finally {
            alloc.release(tempBiomes);
        }
    }

    protected void generateBiomesAndWeightedHeightsVariations_lowres(int x, int z, int level, int size, @NonNull int[] biomes, @NonNull double[] heights, @NonNull double[] variations, @NonNull BiomeWeightHelper weightHelper) {
        ArrayAllocator<int[]> alloc = ALLOC_INT.get();

        int smoothRadius = weightHelper.smoothRadius();
        int smoothDiameter = weightHelper.smoothDiameter();
        int centerOffset = smoothRadius * smoothDiameter + smoothRadius;

        int[] tempBiomes = alloc.atLeast(sq(size) * sq(smoothDiameter));
        try {
            //generate biomes on generation layer at low resolution
            this.generationLayer.multiGetGrids(alloc, (x >> GTH_SHIFT) - smoothRadius, (z >> GTH_SHIFT) - smoothRadius, smoothDiameter, 1 << (level - GTH_SHIFT), 0, size, tempBiomes);

            //copy biomes into output array
            for (int inIdx = centerOffset, outIdx = 0, dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++, inIdx += sq(smoothDiameter), outIdx++) {
                    biomes[outIdx] = tempBiomes[inIdx];
                }
            }

            //compute weighted values at each point
            for (int inIdx = 0, outIdx = 0, dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++, inIdx += sq(smoothDiameter), outIdx++) {
                    weightHelper.compute(tempBiomes, inIdx, smoothDiameter, heights, variations, outIdx);
                }
            }
        } finally {
            alloc.release(tempBiomes);
        }
    }
}
