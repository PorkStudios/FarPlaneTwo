/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.FastRegistry;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.biome.BiomeMesa;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.fml.common.Mod;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.BiomeHelperCached.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper class for working with biome generation layers.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@Mod.EventBusSubscriber(modid = MODID)
public class BiomeHelper {
    public static final int BIOME_COUNT = 256;

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
     * Creates a fast {@link IBiomeProvider} from the given vanilla {@link BiomeProvider}.
     *
     * @param provider the {@link BiomeProvider}
     * @return the created {@link IBiomeProvider}
     */
    public IBiomeProvider from(@NonNull BiomeProvider provider) {
        if (provider.isFixedBiome()) {
            return new FixedBiomeProvider1_12(provider.getFixedBiome());
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

    //various compatibility functions which are required in order to emulate vanilla GenLayer as closely as possible.
    // these functions are actually only called by BiomeHelperCached#reload()

    public static boolean isJungleCompatible0(Biome biome) {
        if (biome != null && biome.getBiomeClass() == BiomeJungle.class) {
            return true;
        } else {
            int id = FastRegistry.getId(biome);
            return id == ID_JUNGLE || id == ID_JUNGLE_EDGE || id == ID_JUNGLE_HILLS || id == ID_FOREST || id == ID_TAIGA || BiomeManager.oceanBiomes.contains(biome);
        }
    }

    public static boolean isBiomeOceanic0(Biome biome) {
        return BiomeManager.oceanBiomes.contains(biome);
    }

    public static boolean isMesa0(Biome biome) {
        return biome instanceof BiomeMesa;
    }

    public static boolean isMutation0(Biome biome) {
        return biome != null && biome.isMutation();
    }

    public static boolean biomesEqualOrMesaPlateau0(int idA, int idB) {
        if (idA == idB) {
            return true;
        }

        Biome a = FastRegistry.getBiome(idA);
        Biome b = FastRegistry.getBiome(idB);
        if (a != null && b != null) {
            return a != Biomes.MESA_ROCK && a != Biomes.MESA_CLEAR_ROCK
                    ? a == b || a.getBiomeClass() == b.getBiomeClass()
                    : b == Biomes.MESA_ROCK || b == Biomes.MESA_CLEAR_ROCK;
        } else {
            return false;
        }
    }

    public static boolean canBiomesBeNeighbors0(int idA, int idB) {
        if (biomesEqualOrMesaPlateau0(idA, idB)) {
            return true;
        }

        Biome a = FastRegistry.getBiome(idA);
        Biome b = FastRegistry.getBiome(idB);
        if (a != null && b != null) {
            Biome.TempCategory ta = a.getTempCategory();
            Biome.TempCategory tb = b.getTempCategory();
            return ta == tb || ta == Biome.TempCategory.MEDIUM || tb == Biome.TempCategory.MEDIUM;
        } else {
            return false;
        }
    }
}
