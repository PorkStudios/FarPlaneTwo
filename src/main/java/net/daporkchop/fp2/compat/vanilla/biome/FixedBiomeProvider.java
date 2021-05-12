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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.compat.vanilla.biome.weight.BiomeWeightHelper;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;

/**
 * Implementation of {@link IBiomeProvider} which uses a constant, fixed biome.
 *
 * @author DaPorkchop_
 */
@Getter
public class FixedBiomeProvider implements IBiomeProvider {
    protected final int biomeId;

    public FixedBiomeProvider(@NonNull Biome biome) {
        this.biomeId = FastRegistry.getId(biome);
    }

    @Override
    public void generateBiomes(int x, int z, int level, int size, @NonNull int[] biomes) {
        Arrays.fill(biomes, 0, size * size, this.biomeId);
    }

    @Override
    public void generateBiomesAndWeightedHeightsVariations(int x, int z, int level, int size, @NonNull int[] biomes, @NonNull double[] heights, @NonNull double[] variations, @NonNull BiomeWeightHelper weightHelper) {
        Arrays.fill(biomes, 0, size * size, this.biomeId);

        //compute weighted height+variation for single point, then copy it to all points
        int[] tempBiomeIds = new int[weightHelper.smoothDiameter()];
        Arrays.fill(tempBiomeIds, this.biomeId);
        weightHelper.compute(tempBiomeIds, weightHelper.smoothRadius(), 0, heights, variations, 0);

        Arrays.fill(heights, 0, size * size, heights[0]);
        Arrays.fill(variations, 0, size * size, variations[0]);
    }
}
