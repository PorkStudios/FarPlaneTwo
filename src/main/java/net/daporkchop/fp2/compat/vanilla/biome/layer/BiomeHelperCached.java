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

package net.daporkchop.fp2.compat.vanilla.biome.layer;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeJungle;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.IntStream;

import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelper.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Re-implementation of many of the helper methods defined in {@link BiomeHelper}, but with the ability to pre-compute and cache the result for specific biome(s).
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class BiomeHelperCached {
    public static int ID_OCEAN;
    public static int ID_DEFAULT;
    public static int ID_PLAINS;
    public static int ID_DESERT;
    public static int ID_EXTREME_HILLS;
    public static int ID_FOREST;
    public static int ID_TAIGA;
    public static int ID_SWAMPLAND;
    public static int ID_RIVER;
    public static int ID_HELL;
    public static int ID_SKY;
    public static int ID_FROZEN_OCEAN;
    public static int ID_FROZEN_RIVER;
    public static int ID_ICE_PLAINS;
    public static int ID_ICE_MOUNTAINS;
    public static int ID_MUSHROOM_ISLAND;
    public static int ID_MUSHROOM_ISLAND_SHORE;
    public static int ID_BEACH;
    public static int ID_DESERT_HILLS;
    public static int ID_FOREST_HILLS;
    public static int ID_TAIGA_HILLS;
    public static int ID_EXTREME_HILLS_EDGE;
    public static int ID_JUNGLE;
    public static int ID_JUNGLE_HILLS;
    public static int ID_JUNGLE_EDGE;
    public static int ID_DEEP_OCEAN;
    public static int ID_STONE_BEACH;
    public static int ID_COLD_BEACH;
    public static int ID_BIRCH_FOREST;
    public static int ID_BIRCH_FOREST_HILLS;
    public static int ID_ROOFED_FOREST;
    public static int ID_COLD_TAIGA;
    public static int ID_COLD_TAIGA_HILLS;
    public static int ID_REDWOOD_TAIGA;
    public static int ID_REDWOOD_TAIGA_HILLS;
    public static int ID_EXTREME_HILLS_WITH_TREES;
    public static int ID_SAVANNA;
    public static int ID_SAVANNA_PLATEAU;
    public static int ID_MESA;
    public static int ID_MESA_ROCK;
    public static int ID_MESA_CLEAR_ROCK;
    public static int ID_VOID;
    public static int ID_MUTATED_PLAINS;
    public static int ID_MUTATED_DESERT;
    public static int ID_MUTATED_EXTREME_HILLS;
    public static int ID_MUTATED_FOREST;
    public static int ID_MUTATED_TAIGA;
    public static int ID_MUTATED_SWAMPLAND;
    public static int ID_MUTATED_ICE_FLATS;
    public static int ID_MUTATED_JUNGLE;
    public static int ID_MUTATED_JUNGLE_EDGE;
    public static int ID_MUTATED_BIRCH_FOREST;
    public static int ID_MUTATED_BIRCH_FOREST_HILLS;
    public static int ID_MUTATED_ROOFED_FOREST;
    public static int ID_MUTATED_TAIGA_COLD;
    public static int ID_MUTATED_REDWOOD_TAIGA;
    public static int ID_MUTATED_REDWOOD_TAIGA_HILLS;
    public static int ID_MUTATED_EXTREME_HILLS_WITH_TREES;
    public static int ID_MUTATED_SAVANNA;
    public static int ID_MUTATED_SAVANNA_ROCK;
    public static int ID_MUTATED_MESA;
    public static int ID_MUTATED_MESA_ROCK;
    public static int ID_MUTATED_MESA_CLEAR_ROCK;

    public static final int FLAG_VALID = 1 << 0;
    public static final int FLAG_IS_JUNGLE_COMPATIBLE = 1 << 1;
    public static final int FLAG_IS_BIOME_OCEANIC = 1 << 2;
    public static final int FLAG_IS_MESA = 1 << 3;
    public static final int FLAG_IS_MUTATION = 1 << 4;
    public static final int FLAG_IS_JUNGLE = 1 << 5;
    public static final int FLAG_IS_SNOWY_BIOME = 1 << 6;
    public static byte[] FLAGS;

    public static final int EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU = 1 << 0;
    public static final int EQUALS_CAN_BIOMES_BE_NEIGHBORS = 1 << 1;
    public static byte[] EQUALS;

    public static int[] MUTATIONS;

    private static final Set<ReloadListener> RELOAD_LISTENERS = new CopyOnWriteArraySet<>();

    static {
        reload();
    }

    /**
     * Reloads the cached function return values.
     */
    public synchronized static void reload() {
        //set biome IDs
        ID_OCEAN = Biome.getIdForBiome(Biomes.OCEAN);
        ID_DEFAULT = Biome.getIdForBiome(Biomes.DEFAULT);
        ID_PLAINS = Biome.getIdForBiome(Biomes.PLAINS);
        ID_DESERT = Biome.getIdForBiome(Biomes.DESERT);
        ID_EXTREME_HILLS = Biome.getIdForBiome(Biomes.EXTREME_HILLS);
        ID_FOREST = Biome.getIdForBiome(Biomes.FOREST);
        ID_TAIGA = Biome.getIdForBiome(Biomes.TAIGA);
        ID_SWAMPLAND = Biome.getIdForBiome(Biomes.SWAMPLAND);
        ID_RIVER = Biome.getIdForBiome(Biomes.RIVER);
        ID_HELL = Biome.getIdForBiome(Biomes.HELL);
        ID_SKY = Biome.getIdForBiome(Biomes.SKY);
        ID_FROZEN_OCEAN = Biome.getIdForBiome(Biomes.FROZEN_OCEAN);
        ID_FROZEN_RIVER = Biome.getIdForBiome(Biomes.FROZEN_RIVER);
        ID_ICE_PLAINS = Biome.getIdForBiome(Biomes.ICE_PLAINS);
        ID_ICE_MOUNTAINS = Biome.getIdForBiome(Biomes.ICE_MOUNTAINS);
        ID_MUSHROOM_ISLAND = Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND);
        ID_MUSHROOM_ISLAND_SHORE = Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND_SHORE);
        ID_BEACH = Biome.getIdForBiome(Biomes.BEACH);
        ID_DESERT_HILLS = Biome.getIdForBiome(Biomes.DESERT_HILLS);
        ID_FOREST_HILLS = Biome.getIdForBiome(Biomes.FOREST_HILLS);
        ID_TAIGA_HILLS = Biome.getIdForBiome(Biomes.TAIGA_HILLS);
        ID_EXTREME_HILLS_EDGE = Biome.getIdForBiome(Biomes.EXTREME_HILLS_EDGE);
        ID_JUNGLE = Biome.getIdForBiome(Biomes.JUNGLE);
        ID_JUNGLE_HILLS = Biome.getIdForBiome(Biomes.JUNGLE_HILLS);
        ID_JUNGLE_EDGE = Biome.getIdForBiome(Biomes.JUNGLE_EDGE);
        ID_DEEP_OCEAN = Biome.getIdForBiome(Biomes.DEEP_OCEAN);
        ID_STONE_BEACH = Biome.getIdForBiome(Biomes.STONE_BEACH);
        ID_COLD_BEACH = Biome.getIdForBiome(Biomes.COLD_BEACH);
        ID_BIRCH_FOREST = Biome.getIdForBiome(Biomes.BIRCH_FOREST);
        ID_BIRCH_FOREST_HILLS = Biome.getIdForBiome(Biomes.BIRCH_FOREST_HILLS);
        ID_ROOFED_FOREST = Biome.getIdForBiome(Biomes.ROOFED_FOREST);
        ID_COLD_TAIGA = Biome.getIdForBiome(Biomes.COLD_TAIGA);
        ID_COLD_TAIGA_HILLS = Biome.getIdForBiome(Biomes.COLD_TAIGA_HILLS);
        ID_REDWOOD_TAIGA = Biome.getIdForBiome(Biomes.REDWOOD_TAIGA);
        ID_REDWOOD_TAIGA_HILLS = Biome.getIdForBiome(Biomes.REDWOOD_TAIGA_HILLS);
        ID_EXTREME_HILLS_WITH_TREES = Biome.getIdForBiome(Biomes.EXTREME_HILLS_WITH_TREES);
        ID_SAVANNA = Biome.getIdForBiome(Biomes.SAVANNA);
        ID_SAVANNA_PLATEAU = Biome.getIdForBiome(Biomes.SAVANNA_PLATEAU);
        ID_MESA = Biome.getIdForBiome(Biomes.MESA);
        ID_MESA_ROCK = Biome.getIdForBiome(Biomes.MESA_ROCK);
        ID_MESA_CLEAR_ROCK = Biome.getIdForBiome(Biomes.MESA_CLEAR_ROCK);
        ID_VOID = Biome.getIdForBiome(Biomes.VOID);
        ID_MUTATED_PLAINS = Biome.getIdForBiome(Biomes.MUTATED_PLAINS);
        ID_MUTATED_DESERT = Biome.getIdForBiome(Biomes.MUTATED_DESERT);
        ID_MUTATED_EXTREME_HILLS = Biome.getIdForBiome(Biomes.MUTATED_EXTREME_HILLS);
        ID_MUTATED_FOREST = Biome.getIdForBiome(Biomes.MUTATED_FOREST);
        ID_MUTATED_TAIGA = Biome.getIdForBiome(Biomes.MUTATED_TAIGA);
        ID_MUTATED_SWAMPLAND = Biome.getIdForBiome(Biomes.MUTATED_SWAMPLAND);
        ID_MUTATED_ICE_FLATS = Biome.getIdForBiome(Biomes.MUTATED_ICE_FLATS);
        ID_MUTATED_JUNGLE = Biome.getIdForBiome(Biomes.MUTATED_JUNGLE);
        ID_MUTATED_JUNGLE_EDGE = Biome.getIdForBiome(Biomes.MUTATED_JUNGLE_EDGE);
        ID_MUTATED_BIRCH_FOREST = Biome.getIdForBiome(Biomes.MUTATED_BIRCH_FOREST);
        ID_MUTATED_BIRCH_FOREST_HILLS = Biome.getIdForBiome(Biomes.MUTATED_BIRCH_FOREST_HILLS);
        ID_MUTATED_ROOFED_FOREST = Biome.getIdForBiome(Biomes.MUTATED_ROOFED_FOREST);
        ID_MUTATED_TAIGA_COLD = Biome.getIdForBiome(Biomes.MUTATED_TAIGA_COLD);
        ID_MUTATED_REDWOOD_TAIGA = Biome.getIdForBiome(Biomes.MUTATED_REDWOOD_TAIGA);
        ID_MUTATED_REDWOOD_TAIGA_HILLS = Biome.getIdForBiome(Biomes.MUTATED_REDWOOD_TAIGA_HILLS);
        ID_MUTATED_EXTREME_HILLS_WITH_TREES = Biome.getIdForBiome(Biomes.MUTATED_EXTREME_HILLS_WITH_TREES);
        ID_MUTATED_SAVANNA = Biome.getIdForBiome(Biomes.MUTATED_SAVANNA);
        ID_MUTATED_SAVANNA_ROCK = Biome.getIdForBiome(Biomes.MUTATED_SAVANNA_ROCK);
        ID_MUTATED_MESA = Biome.getIdForBiome(Biomes.MUTATED_MESA);
        ID_MUTATED_MESA_ROCK = Biome.getIdForBiome(Biomes.MUTATED_MESA_ROCK);
        ID_MUTATED_MESA_CLEAR_ROCK = Biome.getIdForBiome(Biomes.MUTATED_MESA_CLEAR_ROCK);

        //set flag properties for each biome
        byte[] flags = new byte[BIOME_COUNT];
        for (int id = 0; id < BIOME_COUNT; id++) {
            byte temp = 0;
            Biome biome = Biome.getBiome(id);
            if (biome != null) { //check if biome exists
                temp |= FLAG_VALID;

                if (BiomeHelper.isJungleCompatible0(biome)) {
                    temp |= FLAG_IS_JUNGLE_COMPATIBLE;
                }
                if (BiomeHelper.isBiomeOceanic0(biome)) {
                    temp |= FLAG_IS_BIOME_OCEANIC;
                }
                if (BiomeHelper.isMesa0(biome)) {
                    temp |= FLAG_IS_MESA;
                }
                if (BiomeHelper.isMutation0(biome)) {
                    temp |= FLAG_IS_MUTATION;
                }
                if (biome.getBiomeClass() == BiomeJungle.class) {
                    temp |= FLAG_IS_JUNGLE;
                }
                if (biome.isSnowyBiome()) {
                    temp |= FLAG_IS_SNOWY_BIOME;
                }
            }
            flags[id] = temp;
        }
        FLAGS = flags;

        //compare every biome with every other biome
        byte[] equals = new byte[BIOME_COUNT * BIOME_COUNT];
        for (int i = 0, a = 0; a < BIOME_COUNT; a++) {
            for (int b = 0; b < BIOME_COUNT; b++, i++) {
                byte temp = 0;
                if (BiomeHelper.biomesEqualOrMesaPlateau0(b, a)) {
                    temp |= EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU;
                }
                if (BiomeHelper.canBiomesBeNeighbors0(b, a)) {
                    temp |= EQUALS_CAN_BIOMES_BE_NEIGHBORS;
                }
                equals[i] = temp;
            }
        }
        EQUALS = equals;

        //find mutations of each biome
        MUTATIONS = IntStream.range(0, BIOME_COUNT).map(id -> {
            Biome mutation = Biome.getMutationForBiome(Biome.getBiome(id));
            return mutation == null ? -1 : Biome.getIdForBiome(mutation);
        }).toArray();

        //notify reload listeners
        RELOAD_LISTENERS.forEach(ReloadListener::onBiomeHelperCachedReload);
    }

    /**
     * Adds a new reload listener.
     *
     * @param listener the reload listener to add
     */
    public static void addReloadListener(@NonNull ReloadListener listener) {
        checkState(RELOAD_LISTENERS.add(listener), "reload listener %s already present", listener);
    }

    /**
     * Removes a previously added reload listener.
     *
     * @param listener the reload listener to remove
     */
    public static void removeReloadListener(@NonNull ReloadListener listener) {
        checkState(RELOAD_LISTENERS.remove(listener), "reload listener %s not present", listener);
    }

    /**
     * @return whether or not {@code id} is a valid biome ID
     */
    public static boolean isValid(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_VALID) != 0;
    }

    /**
     * @see BiomeHelper#isJungleCompatible0(Biome)
     */
    public static boolean isJungleCompatible(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_JUNGLE_COMPATIBLE) != 0;
    }

    /**
     * @see BiomeHelper#isBiomeOceanic0(Biome)
     */
    public static boolean isBiomeOceanic(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_BIOME_OCEANIC) != 0;
    }

    /**
     * @see BiomeHelper#isMesa0(Biome)
     */
    public static boolean isMesa(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_MESA) != 0;
    }

    /**
     * @see BiomeHelper#isMutation0(Biome)
     */
    public static boolean isMutation(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_MUTATION) != 0;
    }

    public static boolean isJungle(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_JUNGLE) != 0;
    }

    public static boolean isSnowyBiome(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_SNOWY_BIOME) != 0;
    }

    /**
     * @see BiomeHelper#biomesEqualOrMesaPlateau0(int, int)
     */
    public static boolean biomesEqualOrMesaPlateau(int a, int b) {
        return a >= 0 && b >= 0 && (EQUALS[b * BIOME_COUNT + a] & EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU) != 0;
    }

    /**
     * @see BiomeHelper#canBiomesBeNeighbors0(int, int)
     */
    public static boolean canBiomesBeNeighbors(int a, int b) {
        return a >= 0 && b >= 0 && (EQUALS[b * BIOME_COUNT + a] & EQUALS_CAN_BIOMES_BE_NEIGHBORS) != 0;
    }

    /**
     * @see Biome#getMutationForBiome(Biome)
     */
    public static int getMutationForBiome(int id) {
        return id >= 0 ? MUTATIONS[id] : id;
    }

    /**
     * A function which is run whenever {@link BiomeHelperCached} is reloaded.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface ReloadListener {
        void onBiomeHelperCachedReload();
    }
}
