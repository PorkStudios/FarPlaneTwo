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

import static java.lang.Math.*;
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
    public final int cacheSize;

    public final int gbCacheStart;
    public final int gbCacheSize;
    public final Biome[] gbCache; //contains the cached biomes at generation scale

    public final Biome[] biomeCache; //contains the actual biomes for a chunk

    protected final float[] rawHeightVariation;
    protected final float[] processedHeightVariation;

    private final float[] nearBiomeWeightArray;
    private final int[] nearBiomeIndexOffsets;

    private final IBuilder delegateBuilder;

    //current state
    private int cubeX;
    private int cubeZ;
    private int level;

    private int xOffset;
    private int zOffset;

    public CWGContext(@NonNull World world, int padding, int smoothRadius) {
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

        this.gbCacheStart = max(((padding - 1) >> 2) + 1, smoothRadius);
        this.gbCacheSize = GT_SIZE + this.gbCacheStart * 2 + 1;
        this.gbCache = new Biome[this.gbCacheSize * this.gbCacheSize];

        this.cacheSize = T_VOXELS + padding * 2;

        this.biomeCache = new Biome[T_VOXELS * T_VOXELS];

        this.rawHeightVariation = new float[(this.gbCacheSize * this.gbCacheSize) << 1];
        this.processedHeightVariation = new float[this.cacheSize * this.cacheSize];

        int smoothDiameter = smoothRadius * 2 + 1;
        this.nearBiomeWeightArray = new float[smoothDiameter * smoothDiameter];
        this.nearBiomeIndexOffsets = new int[smoothDiameter * smoothDiameter];
        for (int i = 0, x = -smoothRadius; x <= smoothRadius; x++) {
            for (int z = -smoothRadius; z <= smoothRadius; z++) {
                this.nearBiomeIndexOffsets[i] = x * this.gbCacheSize + z;
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

        int baseGenTileX = (cubeX << (T_SHIFT - GT_SHIFT)) - this.gbCacheStart;
        int baseGenTileZ = (cubeZ << (T_SHIFT - GT_SHIFT)) - this.gbCacheStart;

        if (level == 0) { //base level, simply use vanilla system
            checkState(this.biomeProvider.getBiomesForGeneration(this.gbCache, baseGenTileX, baseGenTileZ, this.gbCacheSize, this.gbCacheSize) == this.gbCache);
            checkState(this.biomeProvider.getBiomes(this.biomeCache, (cubeX << T_SHIFT) - this.padding, (cubeZ << T_SHIFT) - this.padding, this.cacheSize, this.cacheSize, false) == this.biomeCache);
        } else { //not the base level, scale it all up
            //TODO: optimized method for generating biomes at low resolution?
            //TODO: this does not handle smoothing correctly, the smoothing radius should be unaffected by the detail level
            Biome[] tmpArr = new Biome[1];
            for (int i = 0, x = 0; x < this.gbCacheSize; x++) {
                for (int z = 0; z < this.gbCacheSize; z++) {
                    this.biomeProvider.getBiomesForGeneration(tmpArr, baseGenTileX + (x << level), baseGenTileZ + (z << level), 1, 1);
                    this.gbCache[i++] = tmpArr[0];
                }
            }

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0, x = 0; x < T_VOXELS; x++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    pos.setPos(((cubeX << T_SHIFT) + x) << level, 0, ((cubeZ << T_SHIFT) + z) << level);
                    this.biomeCache[i++] = this.biomeProvider.getBiome(pos, Biomes.PLAINS);
                }
            }
        }

        //compute biome height/variation
        //store raw values into array
        for (int i = 0, len = this.gbCache.length; i < len; i++) {
            this.rawHeightVariation[i << 1] = this.gbCache[i].getBaseHeight();
            this.rawHeightVariation[(i << 1) + 1] = this.gbCache[i].getHeightVariation();
        }
        for (int i = 0, x = this.gbCacheStart; x < this.gbCacheSize - this.gbCacheStart; x++) {
            for (int z = this.gbCacheStart; z < this.gbCacheSize - this.gbCacheStart; z++) {
                int centerIndex = x * this.gbCacheSize + z;
                float centerBaseHeight = this.rawHeightVariation[centerIndex << 1];

                float smoothVolatility = 0.0f;
                float smoothHeight = 0.0f;
                float totalWeight = 0.0f;

                for (int offsetIndex = 0, len = this.nearBiomeIndexOffsets.length; offsetIndex < len; offsetIndex++) {
                    int index = centerIndex + this.nearBiomeIndexOffsets[offsetIndex];
                    float biomeHeight = this.rawHeightVariation[index << 1];
                    float biomeVariation = this.rawHeightVariation[(index << 1) + 1];

                    float biomeWeight = abs(this.nearBiomeWeightArray[offsetIndex] / (biomeHeight + 2.0f));
                    if (biomeHeight > centerBaseHeight) {
                        biomeWeight *= 0.5f;
                    }

                    smoothHeight += biomeHeight * biomeWeight;
                    smoothVolatility += biomeVariation * biomeWeight;
                    totalWeight += biomeWeight;
                }

                this.processedHeightVariation[i++] = ConversionUtils.biomeHeightVanilla(smoothHeight / totalWeight);
                this.processedHeightVariation[i++] = ConversionUtils.biomeHeightVariationVanilla(smoothVolatility / totalWeight);
            }
        }
    }

    public double getBaseHeight(int x, int z) {
        return this.processedHeightVariation[this.getHeightVariationIndex(x, z) << 1];
    }

    public double getHeightVariation(int x, int z) {
        return this.processedHeightVariation[(this.getHeightVariationIndex(x, z) << 1) + 1];
    }

    private int getHeightVariationIndex(int x, int z) {
        return ((x - this.xOffset) >> this.level) * this.cacheSize + ((z - this.zOffset) >> this.level);
    }

    @Override
    public double get(int x, int y, int z) {
        return this.delegateBuilder.get(x, y, z); //TODO: implement this
    }
}
