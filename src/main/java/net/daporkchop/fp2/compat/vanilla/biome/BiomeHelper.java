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
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerAddIsland;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerAddMushroomIsland;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerAddSnow;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerBiome;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerBiomeEdge;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerDeepOcean;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerFuzzyZoom;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerHills;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerIsland;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRandomValues;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRareBiome;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRemoveTooMuchOcean;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRiver;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRiverInit;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerRiverMix;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerShore;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerSmooth;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerVoronoiZoom;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.FastLayerZoom;
import net.daporkchop.fp2.compat.vanilla.biome.layer.vanilla.GenLayerRandomValues;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerAddMushroomIsland;
import net.minecraft.world.gen.layer.GenLayerAddSnow;
import net.minecraft.world.gen.layer.GenLayerBiome;
import net.minecraft.world.gen.layer.GenLayerBiomeEdge;
import net.minecraft.world.gen.layer.GenLayerDeepOcean;
import net.minecraft.world.gen.layer.GenLayerEdge;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerHills;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerRareBiome;
import net.minecraft.world.gen.layer.GenLayerRemoveTooMuchOcean;
import net.minecraft.world.gen.layer.GenLayerRiver;
import net.minecraft.world.gen.layer.GenLayerRiverInit;
import net.minecraft.world.gen.layer.GenLayerRiverMix;
import net.minecraft.world.gen.layer.GenLayerShore;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper class for working with biome generation layers.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@Mod.EventBusSubscriber(modid = FP2.MODID)
public class BiomeHelper {
    public static final int BIOME_COUNT = 256;

    public static final int ID_OCEAN = Biome.getIdForBiome(Biomes.OCEAN);
    public static final int ID_DEFAULT = Biome.getIdForBiome(Biomes.DEFAULT);
    public static final int ID_PLAINS = Biome.getIdForBiome(Biomes.PLAINS);
    public static final int ID_DESERT = Biome.getIdForBiome(Biomes.DESERT);
    public static final int ID_EXTREME_HILLS = Biome.getIdForBiome(Biomes.EXTREME_HILLS);
    public static final int ID_FOREST = Biome.getIdForBiome(Biomes.FOREST);
    public static final int ID_TAIGA = Biome.getIdForBiome(Biomes.TAIGA);
    public static final int ID_SWAMPLAND = Biome.getIdForBiome(Biomes.SWAMPLAND);
    public static final int ID_RIVER = Biome.getIdForBiome(Biomes.RIVER);
    public static final int ID_HELL = Biome.getIdForBiome(Biomes.HELL);
    public static final int ID_SKY = Biome.getIdForBiome(Biomes.SKY);
    public static final int ID_FROZEN_OCEAN = Biome.getIdForBiome(Biomes.FROZEN_OCEAN);
    public static final int ID_FROZEN_RIVER = Biome.getIdForBiome(Biomes.FROZEN_RIVER);
    public static final int ID_ICE_PLAINS = Biome.getIdForBiome(Biomes.ICE_PLAINS);
    public static final int ID_ICE_MOUNTAINS = Biome.getIdForBiome(Biomes.ICE_MOUNTAINS);
    public static final int ID_MUSHROOM_ISLAND = Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND);
    public static final int ID_MUSHROOM_ISLAND_SHORE = Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND_SHORE);
    public static final int ID_BEACH = Biome.getIdForBiome(Biomes.BEACH);
    public static final int ID_DESERT_HILLS = Biome.getIdForBiome(Biomes.DESERT_HILLS);
    public static final int ID_FOREST_HILLS = Biome.getIdForBiome(Biomes.FOREST_HILLS);
    public static final int ID_TAIGA_HILLS = Biome.getIdForBiome(Biomes.TAIGA_HILLS);
    public static final int ID_EXTREME_HILLS_EDGE = Biome.getIdForBiome(Biomes.EXTREME_HILLS_EDGE);
    public static final int ID_JUNGLE = Biome.getIdForBiome(Biomes.JUNGLE);
    public static final int ID_JUNGLE_HILLS = Biome.getIdForBiome(Biomes.JUNGLE_HILLS);
    public static final int ID_JUNGLE_EDGE = Biome.getIdForBiome(Biomes.JUNGLE_EDGE);
    public static final int ID_DEEP_OCEAN = Biome.getIdForBiome(Biomes.DEEP_OCEAN);
    public static final int ID_STONE_BEACH = Biome.getIdForBiome(Biomes.STONE_BEACH);
    public static final int ID_COLD_BEACH = Biome.getIdForBiome(Biomes.COLD_BEACH);
    public static final int ID_BIRCH_FOREST = Biome.getIdForBiome(Biomes.BIRCH_FOREST);
    public static final int ID_BIRCH_FOREST_HILLS = Biome.getIdForBiome(Biomes.BIRCH_FOREST_HILLS);
    public static final int ID_ROOFED_FOREST = Biome.getIdForBiome(Biomes.ROOFED_FOREST);
    public static final int ID_COLD_TAIGA = Biome.getIdForBiome(Biomes.COLD_TAIGA);
    public static final int ID_COLD_TAIGA_HILLS = Biome.getIdForBiome(Biomes.COLD_TAIGA_HILLS);
    public static final int ID_REDWOOD_TAIGA = Biome.getIdForBiome(Biomes.REDWOOD_TAIGA);
    public static final int ID_REDWOOD_TAIGA_HILLS = Biome.getIdForBiome(Biomes.REDWOOD_TAIGA_HILLS);
    public static final int ID_EXTREME_HILLS_WITH_TREES = Biome.getIdForBiome(Biomes.EXTREME_HILLS_WITH_TREES);
    public static final int ID_SAVANNA = Biome.getIdForBiome(Biomes.SAVANNA);
    public static final int ID_SAVANNA_PLATEAU = Biome.getIdForBiome(Biomes.SAVANNA_PLATEAU);
    public static final int ID_MESA = Biome.getIdForBiome(Biomes.MESA);
    public static final int ID_MESA_ROCK = Biome.getIdForBiome(Biomes.MESA_ROCK);
    public static final int ID_MESA_CLEAR_ROCK = Biome.getIdForBiome(Biomes.MESA_CLEAR_ROCK);
    public static final int ID_VOID = Biome.getIdForBiome(Biomes.VOID);
    public static final int ID_MUTATED_PLAINS = Biome.getIdForBiome(Biomes.MUTATED_PLAINS);
    public static final int ID_MUTATED_DESERT = Biome.getIdForBiome(Biomes.MUTATED_DESERT);
    public static final int ID_MUTATED_EXTREME_HILLS = Biome.getIdForBiome(Biomes.MUTATED_EXTREME_HILLS);
    public static final int ID_MUTATED_FOREST = Biome.getIdForBiome(Biomes.MUTATED_FOREST);
    public static final int ID_MUTATED_TAIGA = Biome.getIdForBiome(Biomes.MUTATED_TAIGA);
    public static final int ID_MUTATED_SWAMPLAND = Biome.getIdForBiome(Biomes.MUTATED_SWAMPLAND);
    public static final int ID_MUTATED_ICE_FLATS = Biome.getIdForBiome(Biomes.MUTATED_ICE_FLATS);
    public static final int ID_MUTATED_JUNGLE = Biome.getIdForBiome(Biomes.MUTATED_JUNGLE);
    public static final int ID_MUTATED_JUNGLE_EDGE = Biome.getIdForBiome(Biomes.MUTATED_JUNGLE_EDGE);
    public static final int ID_MUTATED_BIRCH_FOREST = Biome.getIdForBiome(Biomes.MUTATED_BIRCH_FOREST);
    public static final int ID_MUTATED_BIRCH_FOREST_HILLS = Biome.getIdForBiome(Biomes.MUTATED_BIRCH_FOREST_HILLS);
    public static final int ID_MUTATED_ROOFED_FOREST = Biome.getIdForBiome(Biomes.MUTATED_ROOFED_FOREST);
    public static final int ID_MUTATED_TAIGA_COLD = Biome.getIdForBiome(Biomes.MUTATED_TAIGA_COLD);
    public static final int ID_MUTATED_REDWOOD_TAIGA = Biome.getIdForBiome(Biomes.MUTATED_REDWOOD_TAIGA);
    public static final int ID_MUTATED_REDWOOD_TAIGA_HILLS = Biome.getIdForBiome(Biomes.MUTATED_REDWOOD_TAIGA_HILLS);
    public static final int ID_MUTATED_EXTREME_HILLS_WITH_TREES = Biome.getIdForBiome(Biomes.MUTATED_EXTREME_HILLS_WITH_TREES);
    public static final int ID_MUTATED_SAVANNA = Biome.getIdForBiome(Biomes.MUTATED_SAVANNA);
    public static final int ID_MUTATED_SAVANNA_ROCK = Biome.getIdForBiome(Biomes.MUTATED_SAVANNA_ROCK);
    public static final int ID_MUTATED_MESA = Biome.getIdForBiome(Biomes.MUTATED_MESA);
    public static final int ID_MUTATED_MESA_ROCK = Biome.getIdForBiome(Biomes.MUTATED_MESA_ROCK);
    public static final int ID_MUTATED_MESA_CLEAR_ROCK = Biome.getIdForBiome(Biomes.MUTATED_MESA_CLEAR_ROCK);

    //gets all direct children of a GenLayer
    public static final Map<Class<? extends GenLayer>, Function<GenLayer, GenLayer[]>> GET_PARENTS = new IdentityHashMap<>();
    public static final Map<Class<? extends GenLayer>, Function<GenLayer, IFastLayer>> LAYER_CONVERTERS = new IdentityHashMap<>();
    public static final double[] BIOME_HEIGHTS = new double[BIOME_COUNT];
    public static final double[] BIOME_VARIATIONS = new double[BIOME_COUNT];

    static {
        Function<GenLayer, GenLayer[]> parent = genLayer -> new GenLayer[]{ genLayer.parent };
        Function<GenLayer, GenLayer[]> none = genLayer -> new GenLayer[0];

        GET_PARENTS.put(GenLayerAddIsland.class, parent);
        GET_PARENTS.put(GenLayerAddMushroomIsland.class, parent);
        GET_PARENTS.put(GenLayerAddSnow.class, parent);
        GET_PARENTS.put(GenLayerBiome.class, parent);
        GET_PARENTS.put(GenLayerBiomeEdge.class, parent);
        GET_PARENTS.put(GenLayerDeepOcean.class, parent);
        GET_PARENTS.put(GenLayerEdge.class, parent);
        GET_PARENTS.put(GenLayerFuzzyZoom.class, parent);
        GET_PARENTS.put(GenLayerHills.class, genLayer -> new GenLayer[]{ genLayer.parent, ((GenLayerHills) genLayer).riverLayer });
        GET_PARENTS.put(GenLayerIsland.class, none);
        GET_PARENTS.put(GenLayerRareBiome.class, parent);
        GET_PARENTS.put(GenLayerRemoveTooMuchOcean.class, parent);
        GET_PARENTS.put(GenLayerRiver.class, parent);
        GET_PARENTS.put(GenLayerRiverInit.class, parent);
        GET_PARENTS.put(GenLayerRiverMix.class, genLayer -> {
            GenLayerRiverMix l = (GenLayerRiverMix) genLayer;
            return new GenLayer[]{ l.biomePatternGeneratorChain, l.riverPatternGeneratorChain };
        });
        GET_PARENTS.put(GenLayerShore.class, parent);
        GET_PARENTS.put(GenLayerSmooth.class, parent);
        GET_PARENTS.put(GenLayerVoronoiZoom.class, parent);
        GET_PARENTS.put(GenLayerZoom.class, parent);

        GET_PARENTS.put(GenLayerRandomValues.class, none);
    }

    static {
        LAYER_CONVERTERS.put(GenLayerAddIsland.class, layer -> new FastLayerAddIsland(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerAddMushroomIsland.class, layer -> new FastLayerAddMushroomIsland(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerAddSnow.class, layer -> new FastLayerAddSnow(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerBiome.class, layer -> new FastLayerBiome(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerBiomeEdge.class, layer -> new FastLayerBiomeEdge(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerDeepOcean.class, layer -> new FastLayerDeepOcean(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerEdge.class, layer -> new FastLayerBiomeEdge(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerFuzzyZoom.class, layer -> new FastLayerFuzzyZoom(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerHills.class, layer -> new FastLayerHills(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerIsland.class, layer -> new FastLayerIsland(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerRareBiome.class, layer -> new FastLayerRareBiome(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerRemoveTooMuchOcean.class, layer -> new FastLayerRemoveTooMuchOcean(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerRiver.class, layer -> new FastLayerRiver(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerRiverInit.class, layer -> new FastLayerRiverInit(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerRiverMix.class, layer -> new FastLayerRiverMix(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerShore.class, layer -> new FastLayerShore(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerSmooth.class, layer -> new FastLayerSmooth(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerVoronoiZoom.class, layer -> new FastLayerVoronoiZoom(layer.worldGenSeed));
        LAYER_CONVERTERS.put(GenLayerZoom.class, layer -> new FastLayerZoom(layer.worldGenSeed));

        LAYER_CONVERTERS.put(GenLayerRandomValues.class, layer -> new FastLayerRandomValues(layer.worldGenSeed, ((GenLayerRandomValues) layer).limit()));
    }

    static {
        for (int id = 0; id < BIOME_COUNT; id++) {
            Biome biome = Biome.getBiome(id, Biomes.PLAINS);
            BIOME_HEIGHTS[id] = biome.getBaseHeight();
            BIOME_VARIATIONS[id] = biome.getHeightVariation();
        }
    }

    public static double weightFactor(double baseHeight) {
        return abs(1.0d / (baseHeight + 2.0d));
    }

    public static double biomeHeightVanilla(double height) {
        return height * (17.0d / 64.0d) - (1.0d / 256.0d);
    }

    public static double biomeHeightVariationVanilla(double heightVariation) {
        return heightVariation * 2.4d + (4.0d / 15.0d);
    }

    /**
     * Gets all the parent layers of the given {@link GenLayer}.
     *
     * @param layer the {@link GenLayer}
     * @return the {@link GenLayer}'s parents
     */
    public GenLayer[] getParents(@NonNull GenLayer layer) {
        Function<GenLayer, GenLayer[]> getParents = GET_PARENTS.get(layer.getClass());
        checkArg(getParents != null, "invalid GenLayer class: %s", layer.getClass().getCanonicalName());
        return getParents.apply(layer);
    }

    /**
     * Converts the given {@link GenLayer} to an uninitialized {@link IFastLayer}.
     * <p>
     * This is an internal method, you probably shouldn't touch this.
     *
     * @param layer the {@link GenLayer}
     * @return the {@link IFastLayer}
     * @see FastLayerProvider#makeFast(GenLayer...)
     */
    public IFastLayer convertLayer(@NonNull GenLayer layer) {
        Function<GenLayer, IFastLayer> converter = LAYER_CONVERTERS.get(layer.getClass());
        checkArg(converter != null, "invalid GenLayer class: %s", layer.getClass().getCanonicalName());
        return converter.apply(layer);
    }

    /**
     * Creates a fast {@link IBiomeProvider} from the given vanilla {@link BiomeProvider}.
     *
     * @param provider the {@link BiomeProvider}
     * @return the created {@link IBiomeProvider}
     */
    public IBiomeProvider from(@NonNull BiomeProvider provider) {
        if (provider.isFixedBiome()) {
            return new FixedBiomeProvider(provider.getFixedBiome());
        } else {
            //don't allow custom subclasses
            checkArg(provider.getClass() == BiomeProvider.class, "unsupported BiomeProvider implementation: %s", provider.getClass());
            return new FastThreadSafeBiomeProvider(provider);
        }
    }

    // stateless GenLayer PRNG emulation

    public static long update(long state, long seed) {
        return state * (state * 6364136223846793005L + 1442695040888963407L) + seed;
    }

    public static long start(long seed, long x, long z) {
        long state = seed;
        state = update(state, x);
        state = update(state, z);
        state = update(state, x);
        state = update(state, z);
        return state;
    }

    public static int nextInt(long state, int max) {
        if ((max & (max - 1)) == 0) { //max is a power of two
            return (int) (state >> 24L) & (max - 1);
        } else { //max is NOT a power of two, fall back to slow implementation using modulo
            int i = (int) ((state >> 24L) % max);
            //equivalent to if (i < 0) { i += max; }
            i += (i >> 31) & max;
            return i;
        }
    }
}
