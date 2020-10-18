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

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.IBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.lang.reflect.Field;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Holds the initialized state for a CubicWorldGen emulation context.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
@Getter
public class CWGContext extends CustomGeneratorSettings implements IBuilder {
    public static final int GT_SHIFT = 2; //generation tile shift
    public static final int GT_SIZE = 1 << GT_SHIFT; //generation tile size

    protected final IBiomeBlockReplacer[][] biomeBlockReplacers;
    protected final BiomeProvider biomeProvider;

    protected final int gbCacheStart;
    protected final int gbCacheSize;
    protected final Biome[] gbCache;

    @Getter(AccessLevel.NONE)
    private final IBuilder delegateBuilder;

    public CWGContext(@NonNull World world, int padding, int smoothRadius) {
        notNegative(padding, "padding");
        notNegative(smoothRadius, "smoothRadius");

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
        this.gbCacheSize = GT_SIZE + 1 + this.gbCacheStart * 2;
        this.gbCache = new Biome[this.gbCacheSize * this.gbCacheSize];

        this.delegateBuilder = new CWGBuilderFast(world, conf, biomeSource);
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
     * @param baseCubeX the base X coordinate (in cubes)
     * @param baseCubeY the base Y coordinate (in cubes)
     * @param baseCubeZ the base Z coordinate (in cubes)
     * @param level     the detail level
     */
    public void init(int baseCubeX, int baseCubeY, int baseCubeZ, int level) {
        final int gbCacheStart = this.gbCacheStart;
        final int gbCacheSize = this.gbCacheSize;
        final Biome[] gbCache = this.gbCache;

        int baseGenTileX = (baseCubeX << 2) - gbCacheStart;
        int baseGenTileZ = (baseCubeZ << 2) - gbCacheStart;

        if (level == 0) { //base level, simply use vanilla system
            checkState(this.biomeProvider.getBiomesForGeneration(gbCache, baseGenTileX, baseGenTileZ, gbCacheSize, gbCacheSize) == gbCache);
        } else { //not the base level, scale it all up
            //TODO: optimized method for generating biomes at low resolution?
            //TODO: this does not handle smoothing correctly, the smoothing radius should be unaffected by the detail level
            Biome[] tmpArr = new Biome[1];
            for (int i = 0, x = 0; x < gbCacheSize; x++) {
                for (int z = 0; z < gbCacheSize; z++) {
                    this.biomeProvider.getBiomesForGeneration(tmpArr, baseGenTileX + (x << level), baseGenTileZ + (z << level), 1, 1);
                    gbCache[i++] = tmpArr[0];
                }
            }
        }
    }

    /*public Biome[] getBiomes2d(int baseX, int baseZ, int level) {
        if (level == 0) { //base level, simply use vanilla system
            return this.biomeProvider.getBiomes(this.biomeCache, baseX - 1, baseZ - 1, BIOME_CACHE_SIZE, BIOME_CACHE_SIZE, false);
        } else { //not the base level, scale it all up
            //TODO: optimized method for generating biomes at low resolution?
            //TODO: this is really slow because it uses the vanilla biome cache
            Biome[] biomes = this.biomeCache;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < T_VOXELS; x++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    pos.setPos(baseX + (x << level), 0, baseZ + (z << level));
                    biomes[(z + 1) * BIOME_CACHE_SIZE + (x + 1)] = this.biomeProvider.getBiome(pos, Biomes.PLAINS);
                }
            }
            return biomes;
        }
    }*/

    @Override
    public double get(int x, int y, int z) {
        return this.delegateBuilder.get(x, y, z); //TODO: implement this
    }
}
