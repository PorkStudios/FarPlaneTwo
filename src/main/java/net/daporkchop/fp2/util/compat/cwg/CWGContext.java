/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.util.compat.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.ConversionUtils;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import lombok.NonNull;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.lang.reflect.Field;

import static net.daporkchop.fp2.util.Constants.*;
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

    protected final IBiomeBlockReplacer[][] biomeBlockReplacers;
    protected final BiomeProvider biomeProvider;

    public final int padding;
    public final int smoothRadius;
    private final int smoothDiameter;

    public final int cacheSize;
    public final int cacheOff;
    public final Biome[] biomes;

    private final float[] heights;
    private final float[] variations;
    private final float[] nearBiomeWeightArray;

    private final IBuilder delegateBuilder;

    //current state
    private int cubeX;
    private int cubeZ;
    private int level;

    private int xOffset;
    private int zOffset;

    public CWGContext(@NonNull World world, int size, int padding, int smoothRadius) {
        this.padding = notNegative(padding, "padding");
        this.smoothRadius = notNegative(smoothRadius, "smoothRadius");

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

        this.cacheOff = this.padding;
        this.cacheSize = size + this.padding * 2;
        this.biomes = new Biome[this.cacheSize * this.cacheSize];

        this.heights = new float[this.cacheSize * this.cacheSize];
        this.variations = new float[this.cacheSize * this.cacheSize];

        this.smoothDiameter = smoothRadius * 2 + 1;
        this.nearBiomeWeightArray = new float[this.smoothDiameter * this.smoothDiameter];
        for (int i = 0, x = -this.smoothRadius; x <= this.smoothRadius; x++) {
            for (int z = -this.smoothRadius; z <= this.smoothRadius; z++) {
                this.nearBiomeWeightArray[i++] = (float) (10.0d / Math.sqrt(x * x + z * z + 0.2d));
            }
        }

        this.delegateBuilder = new CWGBuilderFast(this, world, biomeSource);
    }

    /**
     * Gets the block replacers for the given biome.
     *
     * @param biome the {@link Biome}
     * @return an array of {@link IBiomeBlockReplacer}s used by the biome
     */
    public IBiomeBlockReplacer[] replacersForBiome(Biome biome) {
        return this.biomeBlockReplacers[Biome.getIdForBiome(biome)];
    }

    /**
     * Initializes this context at the given position.
     *
     * @param cubeX the base X coordinate (in cubes)
     * @param cubeZ the base Z coordinate (in cubes)
     * @param level the detail level
     */
    public void init(int cubeX, int cubeZ, int level) {
        this.cubeX = cubeX;
        this.cubeZ = cubeZ;
        this.level = level;

        this.xOffset = (cubeX << (T_SHIFT + level)) - this.padding;
        this.zOffset = (cubeZ << (T_SHIFT + level)) - this.padding;

        if (false && level == 0) { //base level, simply use vanilla system
            checkState(this.biomeProvider.getBiomes(this.biomes, (cubeX << T_SHIFT) - this.padding, (cubeZ << T_SHIFT) - this.padding, this.cacheSize, this.cacheSize, false) == this.biomes);
        } else { //not the base level, scale it all up
            //TODO: optimized method for generating biomes at low resolution?
            //TODO: this does not handle smoothing correctly, the smoothing radius should be unaffected by the detail level
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0, x = 0; x < this.cacheSize; x++) {
                for (int z = 0; z < this.cacheSize; z++) {
                    pos.setPos(((cubeX << 4) - this.padding + x) << level, 0, ((cubeZ << 4) - this.padding + z) << level);
                    Biome centerBiome = this.biomes[i] = this.biomeProvider.getBiome(pos, Biomes.PLAINS);

                    float smoothVariation = 0.0f;
                    float smoothHeight = 0.0f;

                    if (this.smoothRadius > 0) {
                        float centerBiomeHeight = centerBiome.getBaseHeight();
                        float biomeWeightSum = 0.0f;

                        int xx = ((cubeX << 4) - this.padding + x) & ~3;
                        int zz = ((cubeZ << 4) - this.padding + z) & ~3;
                        for (int j = 0, dx = -this.smoothRadius; dx <= this.smoothRadius; dx++) {
                            for (int dz = -this.smoothRadius; dz <= this.smoothRadius; dz++) {
                                pos.setPos(xx + (dx << GT_SHIFT), 0, zz + (dz << GT_SHIFT));
                                Biome biome = this.biomeProvider.getBiome(pos, Biomes.PLAINS);

                                float biomeHeight = biome.getBaseHeight();
                                float biomeVolatility = biome.getHeightVariation();

                                float biomeWeight = Math.abs(this.nearBiomeWeightArray[j++] / (biomeHeight + 2.0f));

                                if (biomeHeight > centerBiomeHeight) {
                                    biomeWeight *= 0.5f;
                                }
                                smoothVariation += biomeVolatility * biomeWeight;
                                smoothHeight += biomeHeight * biomeWeight;

                                biomeWeightSum += biomeWeight;
                            }
                        }
                        smoothHeight = ConversionUtils.biomeHeightVanilla(smoothHeight / biomeWeightSum);
                        smoothVariation = ConversionUtils.biomeHeightVariationVanilla(smoothVariation / biomeWeightSum);
                    } else {
                        smoothHeight = ConversionUtils.biomeHeightVanilla(centerBiome.getBaseHeight());
                        smoothVariation = ConversionUtils.biomeHeightVariationVanilla(centerBiome.getHeightVariation());
                    }
                    this.heights[i] = smoothHeight;
                    this.variations[i] = smoothVariation;
                    i++;
                }
            }
        }
    }

    public double getBaseHeight(int x, int z) {
        //return ConversionUtils.biomeHeightVanilla(this.biomes[this.getHeightVariationIndex(x, z)].getBaseHeight());
        try {
            return this.heights[this.getHeightVariationIndex(x, z)];
        } catch (ArrayIndexOutOfBoundsException ioobe) {
            return 0.0d;
        }
    }

    public double getHeightVariation(int x, int z) {
        //return ConversionUtils.biomeHeightVariationVanilla(this.biomes[this.getHeightVariationIndex(x, z)].getHeightVariation());
        return this.variations[this.getHeightVariationIndex(x, z)];
    }

    private int getHeightVariationIndex(int x, int z) {
        return (x - this.xOffset) * this.cacheSize + (z - this.zOffset);
    }

    @Override
    public double get(int x, int y, int z) {
        return this.delegateBuilder.get(x, y, z); //TODO: implement this
    }
}
