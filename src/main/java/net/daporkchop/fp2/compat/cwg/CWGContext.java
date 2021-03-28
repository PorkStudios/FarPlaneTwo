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
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper;
import net.daporkchop.fp2.compat.vanilla.biome.weight.BiomeWeightHelper;
import net.daporkchop.fp2.compat.vanilla.biome.weight.VanillaBiomeWeightHelper;
import net.daporkchop.lib.common.math.PMath;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.IntCache;

import java.lang.reflect.Field;
import java.util.Random;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.compat.cwg.CWGHelper.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Holds the initialized state for a CubicWorldGen emulation context.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public class CWGContext extends CustomGeneratorSettings implements IBuilder {
    public static final int GT_SHIFT = 2; //generation tile shift
    public static final int GT_SIZE = 1 << (T_SHIFT - GT_SHIFT); //generation tile size
    public static final int GT_COUNT = T_VOXELS >> GT_SHIFT;

    public final int size;
    public final int[] biomes;

    protected final int gSize;
    protected final double[] heights;
    protected final double[] variations;

    protected final BiomeProvider biomeProvider;
    protected final BiomeWeightHelper weightHelper;
    protected final IBiomeBlockReplacer[][] biomeBlockReplacers;

    //noisegen values
    protected final int selectorSeed;
    protected final int lowSeed;
    protected final int highSeed;
    protected final int randomHeight2dSeed;
    protected final double selectorScale;
    protected final double lowScale;
    protected final double highScale;
    protected final double randomHeight2dScale;

    //current initialization position
    protected int baseX;
    protected int baseZ;
    protected int level = -1;

    public CWGContext(@NonNull World world, int size, int smoothRadius) {
        this.size = notNegative(size, "size");
        this.weightHelper = new VanillaBiomeWeightHelper(0.0d, 1.0d, 0.0d, 1.0d, smoothRadius);

        CustomGeneratorSettings conf = CustomGeneratorSettings.getFromWorld(world);
        BiomeSource biomeSource = new BiomeSource(world, conf.createBiomeBlockReplacerConfig(), new BiomeProvider(world.getWorldInfo()), smoothRadius);

        try { //copy settings from actual generator settings
            for (Field field : CustomGeneratorSettings.class.getDeclaredFields()) {
                field.set(this, field.get(conf));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.biomeBlockReplacers = CWGHelper.blockReplacerMapToArray(CWGHelper.getReplacerMap(biomeSource));
        this.biomeProvider = CWGHelper.getBiomeGen(biomeSource);

        this.biomes = new int[this.size * this.size];

        this.gSize = (size >> GT_SHIFT) + 2;
        this.heights = new double[this.gSize * this.gSize];
        this.variations = new double[this.gSize * this.gSize];

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

        if (level == 0) { //base level, simply use vanilla system
            IntCache.resetIntCache();
            int[] biomes = this.biomeProvider.biomeIndexLayer.getInts(baseX, baseZ, this.size, this.size);
            for (int i = 0, x = 0; x < this.size; x++) { //convert ZX to XZ
                for (int z = 0; z < this.size; z++, i++) {
                    this.biomes[i] = biomes[z * this.size + x];
                }
            }
        } else if (level < 4) { //generate 16x16 tiles at a time and copy the individual samples into the target array
            //up to a certain scale, this is actually faster than generating individual biomes because the biome generation code
            int tiles = (((this.size - 1) << level) >> 4) + 1;
            int samplesPerTile = 16 >> level;
            int tileSize = 16;
            for (int tileX = 0; tileX < tiles; tileX++) {
                for (int tileZ = 0; tileZ < tiles; tileZ++) {
                    IntCache.resetIntCache();
                    int[] tile = this.biomeProvider.biomeIndexLayer.getInts(baseX + (tileX << 4), baseZ + (tileZ << 4), tileSize, tileSize);
                    for (int x = 0; x < samplesPerTile && tileX * samplesPerTile + x < this.size; x++) {
                        for (int z = 0; z < samplesPerTile && tileZ * samplesPerTile + z < this.size; z++) {
                            int dst = (tileX * samplesPerTile + x) * this.size + (tileZ * samplesPerTile + z);
                            int src = (z << level) * tileSize + (x << level);
                            this.biomes[dst] = tile[src];
                        }
                    }
                }
            }
        } else {
            //TODO: optimized method for generating biomes at low resolution?
            for (int i = 0, x = 0; x < this.size; x++) {
                for (int z = 0; z < this.size; z++) {
                    IntCache.resetIntCache();
                    this.biomes[i++] = this.biomeProvider.biomeIndexLayer.getInts(baseX + (x << level), baseZ + (z << level), 1, 1)[0];
                }
            }
        }

        this.weightHelper.compute(this.biomeProvider.genBiomes, baseX >> GT_SHIFT, baseZ >> GT_SHIFT, level, this.gSize, this.heights, this.variations);
        for (int i = 0, len = this.gSize * this.gSize; i < len; i++) { //convert to CWG format
            this.heights[i] = BiomeHelper.biomeHeightVanilla(this.heights[i]);
            this.variations[i] = BiomeHelper.biomeHeightVariationVanilla(this.variations[i]);
        }
    }

    @Override
    public double get(int x, int y, int z) {
        int i = ((x - this.baseX) >> (GT_SHIFT + this.level)) * this.gSize + ((z - this.baseZ) >> (GT_SHIFT + this.level));
        double height = this.heights[i] * this.heightFactor + this.heightOffset;
        double variation = this.variations[i] * (height > y ? this.specialHeightVariationFactorBelowAverageY : 1.0d) * this.heightVariationFactor + this.heightVariationOffset;

        double low = sample(this.lowSeed, x, y, z, this.lowNoiseOctaves, this.lowNoiseFrequencyX, this.lowNoiseFrequencyY, this.lowNoiseFrequencyZ, this.lowScale) * this.lowNoiseFactor + this.lowNoiseOffset;
        double high = sample(this.highSeed, x, y, z, this.highNoiseOctaves, this.highNoiseFrequencyX, this.highNoiseFrequencyY, this.highNoiseFrequencyZ, this.highScale) * this.highNoiseFactor + this.highNoiseOffset;
        double selector = sample(this.selectorSeed, x, y, z, this.selectorNoiseOctaves, this.selectorNoiseFrequencyX, this.selectorNoiseFrequencyY, this.selectorNoiseFrequencyZ, this.selectorScale) * this.selectorNoiseFactor + this.selectorNoiseOffset;

        double d = PMath.lerp(low, high, PMath.clamp(selector, 0.0d, 1.0d)) + this.randomHeight2d(x, y, z);
        d = d * variation + height;
        return d - Math.signum(variation) * y;
    }

    public double randomHeight2d(double x, double y, double z) {
        double d = sample(this.randomHeight2dSeed, x, y, z, this.depthNoiseOctaves, this.depthNoiseFrequencyX, 0.0d, this.depthNoiseFrequencyZ, this.randomHeight2dScale) * this.depthNoiseFactor + this.depthNoiseOffset;
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

    public int getBiome(int x, int z) {
        return this.biomes[((x - this.baseX) >> this.level) * this.size + ((z - this.baseZ) >> this.level)];
    }
}
