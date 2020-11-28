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

    public final int smoothRadius;
    private final int smoothDiameter;
    private final float[] nearBiomeWeightArray;

    public final int size;
    public final Biome[] biomes;

    private final float[] heights;
    private final float[] variations;

    private final IBuilder delegateBuilder;

    //current state
    private int baseX;
    private int baseZ;
    private int level = -1;

    public CWGContext(@NonNull World world, int size, int smoothRadius) {
        this.size = notNegative(size, "size");
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

        this.biomes = new Biome[this.size * this.size];

        this.heights = new float[this.size * this.size];
        this.variations = new float[this.size * this.size];

        this.smoothDiameter = smoothRadius * 2 + 1;
        this.nearBiomeWeightArray = new float[this.smoothDiameter * this.smoothDiameter];
        for (int i = 0, z = -this.smoothRadius; z <= this.smoothRadius; z++) {
            for (int x = -this.smoothRadius; x <= this.smoothRadius; x++) {
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
            checkState(this.biomeProvider.getBiomes(this.biomes, baseX, baseZ, this.size, this.size, false) == this.biomes);
            //convert ZX to XZ
            for (int x = 0; x < this.size - 1; x++) {
                for (int z = 1 + x; z < this.size; z++) {
                    int src = z * this.size + x;
                    int dst = x * this.size + z;
                    Biome temp = this.biomes[src];
                    this.biomes[src] = this.biomes[dst];
                    this.biomes[dst] = temp;
                }
            }
        } else { //not the base level, scale it all up
            //TODO: optimized method for generating biomes at low resolution?
            //TODO: this does not handle smoothing correctly, the smoothing radius should be unaffected by the detail level
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0, x = 0; x < this.size; x++) {
                for (int z = 0; z < this.size; z++) {
                    this.biomes[i++] = this.biomeProvider.getBiome(pos.setPos(baseX + (x << level), 0, baseZ + (z << level)), Biomes.PLAINS);
                }
            }
        }

        Biome[] gBiomes = null;
        for (int i = 0, x = 0; x < this.size; x++) {
            for (int z = 0; z < this.size; z++, i++) {
                gBiomes = this.biomeProvider.getBiomesForGeneration(gBiomes, ((baseX + x) >> GT_SHIFT) - this.smoothRadius, ((baseZ + z) >> GT_SHIFT) - this.smoothRadius, this.smoothDiameter, this.smoothDiameter);

                float centerBiomeHeight = gBiomes[this.smoothRadius * this.smoothDiameter + this.smoothRadius].getBaseHeight();

                float smoothVariation = 0.0f;
                float smoothHeight = 0.0f;
                float biomeWeightSum = 0.0f;

                for (int j = 0; j < this.smoothDiameter * this.smoothDiameter; j++) {
                    Biome biome = gBiomes[j];

                    float biomeHeight = biome.getBaseHeight();
                    float biomeVolatility = biome.getHeightVariation();

                    float biomeWeight = Math.abs(this.nearBiomeWeightArray[j] / (biomeHeight + 2.0f));

                    if (biomeHeight > centerBiomeHeight) {
                        biomeWeight *= 0.5f;
                    }
                    smoothVariation += biomeVolatility * biomeWeight;
                    smoothHeight += biomeHeight * biomeWeight;

                    biomeWeightSum += biomeWeight;
                }

                this.heights[i] = ConversionUtils.biomeHeightVanilla(smoothHeight / biomeWeightSum);
                this.variations[i] = ConversionUtils.biomeHeightVariationVanilla(smoothVariation / biomeWeightSum);
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
        return (x - this.baseX) * this.size + (z - this.baseZ);
    }

    @Override
    public double get(int x, int y, int z) {
        return this.delegateBuilder.get(x, y, z); //TODO: implement this
    }
}
