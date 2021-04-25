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
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerAddIsland;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerAddMushroomIsland;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerAddSnow;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerBiome;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerBiomeEdge;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerDeepOcean;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerFuzzyZoom;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerHills;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerIsland;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerRareBiome;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerRemoveTooMuchOcean;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerRiver;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerRiverInit;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerRiverMix;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerShore;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerSmooth;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerVoronoiZoom;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerZoom;
import net.daporkchop.fp2.compat.vanilla.biome.nativelayer.NativeFastLayerIsland;
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

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper class for working with biome generation layers.
 *
 * @author DaPorkchop_
 */
@UtilityClass
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
    public static final Map<Class<? extends GenLayer>, Function<GenLayer, GenLayer[]>> GET_CHILDREN = new IdentityHashMap<>();
    public static final Map<Class<? extends GenLayer>, Function<GenLayer, FastLayer>> FAST_MAPPERS = new IdentityHashMap<>();
    public static final double[] BIOME_HEIGHTS = new double[BIOME_COUNT];
    public static final double[] BIOME_VARIATIONS = new double[BIOME_COUNT];

    static {
        Function<GenLayer, GenLayer[]> parent = genLayer -> new GenLayer[]{ genLayer.parent };

        GET_CHILDREN.put(GenLayerAddIsland.class, parent);
        GET_CHILDREN.put(GenLayerAddMushroomIsland.class, parent);
        GET_CHILDREN.put(GenLayerAddSnow.class, parent);
        GET_CHILDREN.put(GenLayerBiome.class, parent);
        GET_CHILDREN.put(GenLayerBiomeEdge.class, parent);
        GET_CHILDREN.put(GenLayerDeepOcean.class, parent);
        GET_CHILDREN.put(GenLayerEdge.class, parent);
        GET_CHILDREN.put(GenLayerFuzzyZoom.class, parent);
        GET_CHILDREN.put(GenLayerHills.class, genLayer -> new GenLayer[]{ genLayer.parent, ((GenLayerHills) genLayer).riverLayer });
        GET_CHILDREN.put(GenLayerIsland.class, genLayer -> new GenLayer[0]);
        GET_CHILDREN.put(GenLayerRareBiome.class, parent);
        GET_CHILDREN.put(GenLayerRemoveTooMuchOcean.class, parent);
        GET_CHILDREN.put(GenLayerRiver.class, parent);
        GET_CHILDREN.put(GenLayerRiverInit.class, parent);
        GET_CHILDREN.put(GenLayerRiverMix.class, genLayer -> {
            GenLayerRiverMix l = (GenLayerRiverMix) genLayer;
            return new GenLayer[]{ l.biomePatternGeneratorChain, l.riverPatternGeneratorChain };
        });
        GET_CHILDREN.put(GenLayerShore.class, parent);
        GET_CHILDREN.put(GenLayerSmooth.class, parent);
        GET_CHILDREN.put(GenLayerVoronoiZoom.class, parent);
        GET_CHILDREN.put(GenLayerZoom.class, parent);
    }

    static {
        FAST_MAPPERS.put(GenLayerAddIsland.class, layer -> new FastLayerAddIsland(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerAddMushroomIsland.class, layer -> new FastLayerAddMushroomIsland(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerAddSnow.class, layer -> new FastLayerAddSnow(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerBiome.class, layer -> new FastLayerBiome(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerBiomeEdge.class, layer -> new FastLayerBiomeEdge(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerDeepOcean.class, layer -> new FastLayerDeepOcean(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerEdge.class, layer -> new FastLayerBiomeEdge(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerFuzzyZoom.class, layer -> new FastLayerFuzzyZoom(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerHills.class, layer -> new FastLayerHills(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerIsland.class, layer -> new NativeFastLayerIsland(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerRareBiome.class, layer -> new FastLayerRareBiome(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerRemoveTooMuchOcean.class, layer -> new FastLayerRemoveTooMuchOcean(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerRiver.class, layer -> new FastLayerRiver(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerRiverInit.class, layer -> new FastLayerRiverInit(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerRiverMix.class, layer -> new FastLayerRiverMix(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerShore.class, layer -> new FastLayerShore(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerSmooth.class, layer -> new FastLayerSmooth(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerVoronoiZoom.class, layer -> new FastLayerVoronoiZoom(layer.worldGenSeed));
        FAST_MAPPERS.put(GenLayerZoom.class, layer -> new FastLayerZoom(layer.worldGenSeed));
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

    private static void addAllLayers(Map<GenLayer, GenLayer[]> childrenMap, GenLayer layer) {
        if (childrenMap.containsKey(layer)) {
            return; //don't re-add the same layer twice
        }

        Function<GenLayer, GenLayer[]> getChildren = GET_CHILDREN.get(layer.getClass());
        checkArg(getChildren != null, "invalid GenLayer class: %s", layer.getClass().getCanonicalName());
        GenLayer[] children = getChildren.apply(layer);
        childrenMap.put(layer, children);

        //add children recursively
        for (GenLayer child : children) {
            addAllLayers(childrenMap, child);
        }
    }

    public FastLayer[] makeFast(@NonNull GenLayer... inputs) {
        //initial add all layers and find their children
        Map<GenLayer, GenLayer[]> children = new IdentityHashMap<>();
        for (GenLayer layer : inputs) {
            addAllLayers(children, layer);
        }

        //map vanilla layers to fast layers
        Map<GenLayer, FastLayer> fastLayers = new IdentityHashMap<>();
        children.keySet().forEach(layer -> {
            Function<GenLayer, FastLayer> fastMapper = FAST_MAPPERS.get(layer.getClass());
            checkArg(fastMapper != null, "invalid GenLayer class: %s", layer.getClass().getCanonicalName());
            fastLayers.put(layer, fastMapper.apply(layer));
        });

        //init fast layers with their children
        fastLayers.forEach((vanilla, fast) -> {
            FastLayer[] fastChildren = Arrays.stream(children.get(vanilla)).map(fastLayers::get).toArray(FastLayer[]::new);
            fast.init(fastChildren);
        });

        return Arrays.stream(inputs).map(fastLayers::get).toArray(FastLayer[]::new);
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
        int i = (int) ((state >> 24) % max);
        //equivalent to if (i < 0) { i += max; }
        i += (i >> 31) & max;
        return i;
    }
}
