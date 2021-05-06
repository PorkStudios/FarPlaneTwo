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

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import lombok.NonNull;
import net.daporkchop.fp2.compat.cwg.noise.CWGNoiseProvider;
import net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper;
import net.daporkchop.fp2.compat.vanilla.biome.IBiomeProvider;
import net.daporkchop.fp2.compat.vanilla.biome.weight.BiomeWeightHelper;
import net.daporkchop.fp2.compat.vanilla.biome.weight.VanillaBiomeWeightHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.lang.reflect.Field;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Holds the initialized state for a CubicWorldGen emulation context.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public class CWGContext {
    public final int size;
    public final int[] biomes;

    protected final IBiomeProvider biomeProvider;
    protected final BiomeWeightHelper weightHelper;
    protected final IBiomeBlockReplacer[][] biomeBlockReplacers;

    protected final CWGNoiseProvider.Configured configuredNoiseGen;

    protected final double[] heights;
    protected final double[] variations;
    protected final double[] depth;

    protected final int expectedBaseHeight;

    //current initialization position
    protected int baseX;
    protected int baseZ;
    protected int level = -1;

    public CWGContext(@NonNull World world, int size, int smoothRadius) {
        this.size = notNegative(size, "size");
        this.biomes = new int[this.size * this.size];

        CustomGeneratorSettings conf = CustomGeneratorSettings.getFromWorld(world);
        BiomeSource biomeSource = new BiomeSource(world, conf.createBiomeBlockReplacerConfig(), new BiomeProvider(world.getWorldInfo()), smoothRadius);

        this.biomeProvider = BiomeHelper.from(CWGHelper.getBiomeGen(biomeSource));
        this.weightHelper = new VanillaBiomeWeightHelper(0.0d, 1.0d, 0.0d, 1.0d, smoothRadius);
        this.biomeBlockReplacers = CWGHelper.blockReplacerMapToArray(CWGHelper.getReplacerMap(biomeSource));

        this.configuredNoiseGen = CWGNoiseProvider.INSTANCE.forSettings(conf, world.getSeed());

        this.heights = new double[this.size * this.size];
        this.variations = new double[this.size * this.size];
        this.depth = new double[this.size * this.size];

        this.expectedBaseHeight = (int) conf.expectedBaseHeight;
    }

    /**
     * Gets the block replacers for the given biome.
     *
     * @param biomeId the {@link Biome}
     * @return an array of {@link IBiomeBlockReplacer}s used by the biome
     */
    public IBiomeBlockReplacer[] replacersForBiome(int biomeId) {
        return this.biomeBlockReplacers[biomeId];
    }

    /**
     * Initializes this context at the given position.
     *
     * @param baseX the base X coordinate (in blocks)
     * @param baseZ the base Z coordinate (in blocks)
     * @param level the detail level
     */
    public void init(int baseX, int baseZ, int level) {
        if (this.baseX == baseX && this.baseZ == baseZ && this.level == level) {
            return; //already initialized to the given position, we don't need to do anything
        }

        this.baseX = baseX;
        this.baseZ = baseZ;
        this.level = level;

        this.biomeProvider.generateBiomesAndWeightedHeightsVariations(baseX, baseZ, level, this.size, this.biomes, this.heights, this.variations, this.weightHelper);

        //convert biome heights/variations to CWG forms
        for (int i = 0; i < sq(this.size); i++) {
            this.heights[i] = BiomeHelper.biomeHeightVanilla(this.heights[i]);
            this.variations[i] = BiomeHelper.biomeHeightVariationVanilla(this.variations[i]);
        }

        //precompute depth noise
        this.configuredNoiseGen.generateDepth2d(this.depth, baseX, baseZ, level, this.size, this.size);
    }

    /**
     * Estimates the terrain height at the given X and Z coordinates.
     *
     * @param x the X coordinate (in blocks)
     * @param z the Z coordinate (in blocks)
     * @return the estimated terrain height value
     */
    public int getHeight(int x, int z) {
        int initialY = (int) this.expectedBaseHeight;

        //find minimum and maximum bounds
        int minY = Integer.MIN_VALUE, maxY = Integer.MAX_VALUE;
        if (this.get(x, initialY, z) > 0.0d) { //initial point is solid
            minY = initialY;
            for (int shift = T_SHIFT; shift < Integer.SIZE; shift++) {
                if (this.get(x, initialY + (1 << shift), z) <= 0.0d) {
                    maxY = initialY + (1 << shift);
                    break;
                } else {
                    minY = initialY + (1 << shift);
                }
            }
        } else {
            maxY = initialY;
            for (int shift = T_SHIFT; shift < Integer.SIZE; shift++) {
                if (this.get(x, initialY - (1 << shift), z) > 0.0d) {
                    minY = initialY - (1 << shift);
                    break;
                } else {
                    maxY = initialY - (1 << shift);
                }
            }
        }

        //binary search
        for (int error = 8 << this.level; maxY - minY > error; ) {
            int middle = (minY + maxY) >>> 1;
            if (this.get(x, middle, z) > 0.0d) { //middle point is solid, move search up
                minY = middle;
            } else {
                maxY = middle;
            }
        }

        double d0 = this.get(x, minY, z);
        double d1 = this.get(x, maxY, z);
        return lerpI(minY, maxY, minimize(d0, d1));
    }

    protected int cacheIndex(int x, int z) {
        return ((x - this.baseX) >> this.level) * this.size + ((z - this.baseZ) >> this.level);
    }

    public double get(int x, int y, int z) {
        int i = this.cacheIndex(x, z);
        return this.configuredNoiseGen.generateSingle(this.heights[i], this.variations[i], this.depth[i], x, y, z);
    }

    public int getBiome(int x, int z) {
        return this.biomes[this.cacheIndex(x, z)];
    }
}
