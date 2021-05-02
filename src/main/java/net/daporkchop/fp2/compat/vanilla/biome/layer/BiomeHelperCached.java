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

import lombok.experimental.UtilityClass;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeJungle;

import java.util.stream.IntStream;

import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelper.*;

/**
 * Re-implementation of many of the helper methods defined in {@link BiomeHelper}, but with the ability to pre-compute and cache the result for specific biome(s).
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class BiomeHelperCached {
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

    static {
        reload();
    }

    /**
     * Reloads the cached function return values.
     */
    public synchronized static void reload() {
        //set flag properties for each biome
        byte[] flags = new byte[BIOME_COUNT];
        for (int id = 0; id < BIOME_COUNT; id++) {
            byte temp = 0;
            Biome biome = Biome.getBiome(id);
            if (biome != null) { //check if biome exists
                temp |= FLAG_VALID;

                if (BiomeHelper.isJungleCompatible(biome)) {
                    temp |= FLAG_IS_JUNGLE_COMPATIBLE;
                }
                if (BiomeHelper.isBiomeOceanic(biome)) {
                    temp |= FLAG_IS_BIOME_OCEANIC;
                }
                if (BiomeHelper.isMesa(biome)) {
                    temp |= FLAG_IS_MESA;
                }
                if (BiomeHelper.isMutation(biome)) {
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
                if (BiomeHelper.biomesEqualOrMesaPlateau0(a, b)) {
                    temp |= EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU;
                }
                if (BiomeHelper.canBiomesBeNeighbors0(a, b)) {
                    temp |= EQUALS_CAN_BIOMES_BE_NEIGHBORS;
                }
                equals[i] = temp;
            }
        }
        EQUALS = equals;

        MUTATIONS = IntStream.range(0, BIOME_COUNT).map(id -> {
            Biome mutation = Biome.getMutationForBiome(Biome.getBiome(id));
            return mutation == null ? -1 : Biome.getIdForBiome(mutation);
        }).toArray();
    }

    /**
     * @return whether or not {@code id} is a valid biome ID
     */
    public static boolean isValid(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_VALID) != 0;
    }

    /**
     * @see BiomeHelper#isJungleCompatible(Biome)
     */
    public static boolean isJungleCompatible(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_JUNGLE_COMPATIBLE) != 0;
    }

    /**
     * @see BiomeHelper#isBiomeOceanic(Biome)
     */
    public static boolean isBiomeOceanic(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_BIOME_OCEANIC) != 0;
    }

    /**
     * @see BiomeHelper#isMesa(Biome)
     */
    public static boolean isMesa(int id) {
        return id >= 0 && (FLAGS[id] & FLAG_IS_MESA) != 0;
    }

    /**
     * @see BiomeHelper#isMutation(Biome)
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
    public static boolean biomesEqualOrMesaPlateau(int idA, int idB) {
        return idA >= 0 && idB >= 0 && (EQUALS[idA * BIOME_COUNT + idB] & EQUALS_BIOMES_EQUAL_OR_MESA_PLATEAU) != 0;
    }

    /**
     * @see BiomeHelper#canBiomesBeNeighbors0(int, int)
     */
    public static boolean canBiomesBeNeighbors(int idA, int idB) {
        return idA >= 0 && idB >= 0 && (EQUALS[idA * BIOME_COUNT + idB] & EQUALS_CAN_BIOMES_BE_NEIGHBORS) != 0;
    }

    /**
     * @see Biome#getMutationForBiome(Biome)
     */
    public static int getMutationForBiome(int id) {
        return id >= 0 ? MUTATIONS[id] : id;
    }
}
