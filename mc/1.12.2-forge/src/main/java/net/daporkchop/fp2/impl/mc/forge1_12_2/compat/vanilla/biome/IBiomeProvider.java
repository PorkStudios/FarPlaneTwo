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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome;

import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.weight.BiomeWeightHelper;
import net.minecraft.world.biome.BiomeProvider;

/**
 * A faster alternative to {@link BiomeProvider}.
 * <p>
 * Note that all methods use XZ coordinate order instead of vanilla's ZX.
 *
 * @author DaPorkchop_
 */
public interface IBiomeProvider {
    /**
     * Generates the IDs of the biomes in the given region.
     *
     * @param x      the X coordinate of the region to generate (in blocks)
     * @param z      the Z coordinate of the region to generate (in blocks)
     * @param level  the current zoom level
     * @param size   the size of the region along the X+Z axes (in samples)
     * @param biomes the array to write biomes to
     */
    void generateBiomes(int x, int z, int level, int size, @NonNull int[] biomes);

    /**
     * Generates the IDs of the biomes in the given region, as well as computing their weighted heights and height variations.
     *
     * @param x            the X coordinate of the region to generate (in blocks)
     * @param z            the Z coordinate of the region to generate (in blocks)
     * @param level        the current zoom level
     * @param size         the size of the region along the X+Z axes (in samples)
     * @param biomes       the array to write biomes to
     * @param heights      the array to write weighted biome heights to
     * @param variations   the array to write weighted biome height variations to
     * @param weightHelper an {@link BiomeWeightHelper} to use for computing weighted biome height (variations)
     */
    void generateBiomesAndWeightedHeightsVariations(int x, int z, int level, int size, @NonNull int[] biomes, @NonNull double[] heights, @NonNull double[] variations, @NonNull BiomeWeightHelper weightHelper);
}
