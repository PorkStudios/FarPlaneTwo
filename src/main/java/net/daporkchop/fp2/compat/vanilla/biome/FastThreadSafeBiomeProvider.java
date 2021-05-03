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
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.world.biome.BiomeProvider;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelperCached.*;
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
        IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
        /*if (level == 0) { //max zoom level, the values are tightly packed so we just need to sample a single grid
            this.biomeLayer.getGrid(alloc, x, z, size, size, biomes);
        } else { //sample a "multigrid", with each sub-grid being a 1x1 grid - this is effectively the same as getGrid but with a customizable spacing between each sample point
            this.biomeLayer.multiGetGrids(alloc, x, z, 1, 1 << level, 0, size, biomes);
        }*/

        if (level == 0) { //max zoom level, the values are tightly packed so we just need to sample a single grid
            this.biomeLayer.getGrid(alloc, x, z, size, size, biomes);
        } else if (level == 1) {
            this.generateBiomesAtHalfResolution(alloc, x, z, size, biomes, this.biomeLayer);
        } else if (level == 2) {
            this.generationLayer.getGrid(alloc, x >> 2, z >> 2, size, size, biomes);
        } else if (level == 3) {
            this.generateBiomesAtHalfResolution(alloc, x >> 2, z >> 2, size, biomes, this.generationLayer);
        } else { //sample a "multigrid", with each sub-grid being a 1x1 grid - this is effectively the same as getGrid but with a customizable spacing between each sample point
            this.generationLayer.multiGetGrids(alloc, x >> 2, z >> 2, 1, 1 << (level - 2), 0, size, biomes);
        }
    }

    protected void generateBiomesAtHalfResolution(@NonNull IntArrayAllocator alloc, int x, int z, int size, @NonNull int[] biomes, @NonNull IFastLayer layer) {
        int[] temp = alloc.get(sq(size << 1));
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
        if (level < 2) { //zoom is low enough that each biome sample is smaller than the noise generation grid, so we'll have to generate them both individually
            this.generateBiomesAndWeightedHeightsVariations_highres(x, z, level, size, biomes, heights, variations, weightHelper);
        } else { //biome layer isn't needed, because the samples are spaced at least 4 blocks apart
            this.generateBiomesAndWeightedHeightsVariations_lowres(x, z, level, size, biomes, heights, variations, weightHelper);
        }
    }

    protected void generateBiomesAndWeightedHeightsVariations_highres(int x, int z, int level, int size, @NonNull int[] biomes, @NonNull double[] heights, @NonNull double[] variations, @NonNull BiomeWeightHelper weightHelper) {
        //generate biomes as per usual
        this.generateBiomes(x, z, level, size, biomes);

        //TODO: fix this
        //temp code: i don't feel like implementing this with weights atm lol
        for (int i = 0; i < sq(size); i++) {
            int b = biomes[i];
            heights[i] = getBiomeBaseHeight(b);
            variations[i] = getBiomeHeightVariation(b);
        }
    }

    protected void generateBiomesAndWeightedHeightsVariations_lowres(int x, int z, int level, int size, @NonNull int[] biomes, @NonNull double[] heights, @NonNull double[] variations, @NonNull BiomeWeightHelper weightHelper) {
        IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();

        int smoothRadius = weightHelper.smoothRadius();
        int smoothDiameter = weightHelper.smoothDiameter();
        int centerOffset = smoothRadius * smoothDiameter + smoothRadius;

        int[] tempBiomes = alloc.get(sq(size) * sq(smoothDiameter));
        try {
            //generate biomes on generation layer at low resolution
            this.generationLayer.multiGetGrids(alloc, (x >> 2) - smoothRadius, (z >> 2) - smoothRadius, smoothDiameter, 1 << (level - 2), 0, size, tempBiomes);

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
