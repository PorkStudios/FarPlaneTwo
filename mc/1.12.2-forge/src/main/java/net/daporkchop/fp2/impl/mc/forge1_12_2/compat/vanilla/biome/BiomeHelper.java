/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerBiome1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerEdge1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerHills1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerRiverMix1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat.CompatLayerHelper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat.CompatPaddedLayerWrapper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerAddIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerAddMushroomIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerAddSnow;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerBiome;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerBiomeEdge;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerDeepOcean;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerEdge;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerFixedBiome;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerFuzzyZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerHills;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRandomValues;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRareBiome;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRemoveTooMuchOcean;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRiver;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRiverInit;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRiverMix;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerShore;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerSmooth;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerVoronoiZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.vanilla.GenLayerRandomValues;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.biome.BiomeMesa;
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
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    //gets all direct children of a GenLayer
    public static final Map<Class<? extends GenLayer>, Function<GenLayer, GenLayer[]>> GET_PARENTS = new IdentityHashMap<>();
    public static final Map<Class<? extends GenLayer>, Function<GenLayer, IFastLayer>> LAYER_CONVERTERS = new IdentityHashMap<>();
    public static final Map<Class<? extends GenLayer>, BiFunction<GenLayer, GenLayer[], GenLayer>> LAYER_CLONERS = new IdentityHashMap<>();

    static {
        Function<GenLayer, GenLayer[]> parent = genLayer -> new GenLayer[]{ ((ATGenLayer1_12) genLayer).getParent() };
        Function<GenLayer, GenLayer[]> none = genLayer -> new GenLayer[0];

        GET_PARENTS.put(GenLayerAddIsland.class, parent);
        GET_PARENTS.put(GenLayerAddMushroomIsland.class, parent);
        GET_PARENTS.put(GenLayerAddSnow.class, parent);
        GET_PARENTS.put(GenLayerBiome.class, parent);
        GET_PARENTS.put(GenLayerBiomeEdge.class, parent);
        GET_PARENTS.put(GenLayerDeepOcean.class, parent);
        GET_PARENTS.put(GenLayerEdge.class, parent);
        GET_PARENTS.put(GenLayerFuzzyZoom.class, parent);
        GET_PARENTS.put(GenLayerHills.class, genLayer -> new GenLayer[]{ ((ATGenLayer1_12) genLayer).getParent(), ((ATGenLayerHills1_12) genLayer).getRiverLayer() });
        GET_PARENTS.put(GenLayerIsland.class, none);
        GET_PARENTS.put(GenLayerRareBiome.class, parent);
        GET_PARENTS.put(GenLayerRemoveTooMuchOcean.class, parent);
        GET_PARENTS.put(GenLayerRiver.class, parent);
        GET_PARENTS.put(GenLayerRiverInit.class, parent);
        GET_PARENTS.put(GenLayerRiverMix.class, genLayer -> {
            ATGenLayerRiverMix1_12 l = (ATGenLayerRiverMix1_12) genLayer;
            return new GenLayer[]{ l.getBiomePatternGeneratorChain(), l.getRiverPatternGeneratorChain() };
        });
        GET_PARENTS.put(GenLayerShore.class, parent);
        GET_PARENTS.put(GenLayerSmooth.class, parent);
        GET_PARENTS.put(GenLayerVoronoiZoom.class, parent);
        GET_PARENTS.put(GenLayerZoom.class, parent);

        GET_PARENTS.put(GenLayerRandomValues.class, none);
    }

    static {
        LAYER_CONVERTERS.put(GenLayerAddIsland.class, layer -> new JavaFastLayerAddIsland(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerAddMushroomIsland.class, layer -> new JavaFastLayerAddMushroomIsland(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerAddSnow.class, layer -> new JavaFastLayerAddSnow(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerBiome.class, layer -> JavaFastLayerBiome.isConstant((GenLayerBiome) layer)
                ? new JavaFastLayerFixedBiome(((ATGenLayerBiome1_12) layer).getSettings().fixedBiome)
                : new JavaFastLayerBiome((GenLayerBiome) layer));
        LAYER_CONVERTERS.put(GenLayerBiomeEdge.class, layer -> new JavaFastLayerBiomeEdge(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerDeepOcean.class, layer -> new JavaFastLayerDeepOcean(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerEdge.class, layer -> JavaFastLayerEdge.makeFast((GenLayerEdge) layer));
        LAYER_CONVERTERS.put(GenLayerFuzzyZoom.class, layer -> new JavaFastLayerFuzzyZoom(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerHills.class, layer -> new JavaFastLayerHills(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerIsland.class, layer -> new JavaFastLayerIsland(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerRareBiome.class, layer -> new JavaFastLayerRareBiome(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerRemoveTooMuchOcean.class, layer -> new JavaFastLayerRemoveTooMuchOcean(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerRiver.class, layer -> new JavaFastLayerRiver(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerRiverInit.class, layer -> new JavaFastLayerRiverInit(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerRiverMix.class, layer -> new JavaFastLayerRiverMix(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerShore.class, layer -> new JavaFastLayerShore(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerSmooth.class, layer -> new JavaFastLayerSmooth(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerVoronoiZoom.class, layer -> new JavaFastLayerVoronoiZoom(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CONVERTERS.put(GenLayerZoom.class, layer -> new JavaFastLayerZoom(((ATGenLayer1_12) layer).getWorldGenSeed()));

        LAYER_CONVERTERS.put(GenLayerRandomValues.class, layer -> new JavaFastLayerRandomValues(((ATGenLayer1_12) layer).getWorldGenSeed(), ((GenLayerRandomValues) layer).limit()));
    }

    static {
        LAYER_CLONERS.put(GenLayerAddIsland.class, (layer, parents) -> new GenLayerAddIsland(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerAddMushroomIsland.class, (layer, parents) -> new GenLayerAddMushroomIsland(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerAddSnow.class, (layer, parents) -> new GenLayerAddSnow(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerBiome.class, (layer, parents) -> new GenLayerBiome(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], null, ((ATGenLayerBiome1_12) layer).getSettings()));
        LAYER_CLONERS.put(GenLayerBiomeEdge.class, (layer, parents) -> new GenLayerBiomeEdge(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerDeepOcean.class, (layer, parents) -> new GenLayerDeepOcean(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerEdge.class, (layer, parents) -> new GenLayerEdge(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], ((ATGenLayerEdge1_12) layer).getMode()));
        LAYER_CLONERS.put(GenLayerFuzzyZoom.class, (layer, parents) -> new GenLayerFuzzyZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerHills.class, (layer, parents) -> new GenLayerHills(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], parents[1]));
        LAYER_CLONERS.put(GenLayerIsland.class, (layer, parents) -> new GenLayerIsland(((ATGenLayer1_12) layer).getWorldGenSeed()));
        LAYER_CLONERS.put(GenLayerRareBiome.class, (layer, parents) -> new GenLayerRareBiome(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerRemoveTooMuchOcean.class, (layer, parents) -> new GenLayerRemoveTooMuchOcean(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerRiver.class, (layer, parents) -> new GenLayerRiver(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerRiverInit.class, (layer, parents) -> new GenLayerRiverInit(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerRiverMix.class, (layer, parents) -> new GenLayerRiverMix(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], parents[1]));
        LAYER_CLONERS.put(GenLayerShore.class, (layer, parents) -> new GenLayerShore(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerSmooth.class, (layer, parents) -> new GenLayerSmooth(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerVoronoiZoom.class, (layer, parents) -> new GenLayerVoronoiZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));
        LAYER_CLONERS.put(GenLayerZoom.class, (layer, parents) -> new GenLayerZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0]));

        LAYER_CLONERS.put(GenLayerRandomValues.class, (layer, parents) -> new GenLayerRandomValues(((ATGenLayer1_12) layer).getWorldGenSeed(), ((GenLayerRandomValues) layer).limit()));
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
        if (getParents == null) {
            getParents = CompatLayerHelper.getLayerParents(layer.getClass());
        }
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
        if (converter == null) {
            //TODO: auto-detect which kind of layer it is and use an appropriate wrapper class
            return new CompatPaddedLayerWrapper(layer);
        }
        checkArg(converter != null, "invalid GenLayer class: %s", layer.getClass().getCanonicalName());
        return converter.apply(layer);
    }

    /**
     * Clones the given {@link GenLayer}.
     * <p>
     * This is an internal method, you probably shouldn't touch this.
     *
     * @param layer   the {@link GenLayer}
     * @param parents the layer's new parents
     * @return the {@link GenLayer}
     */
    public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer[] parents) {
        BiFunction<GenLayer, GenLayer[], GenLayer> cloner = LAYER_CLONERS.get(layer.getClass());
        if (cloner == null) {
            cloner = CompatLayerHelper.cloneLayerFunc(layer.getClass());
        }
        checkArg(cloner != null, "invalid GenLayer class: %s", layer.getClass().getCanonicalName());
        GenLayer cloned = cloner.apply(layer, parents);
        ((ATGenLayer1_12) cloned).setWorldGenSeed(((ATGenLayer1_12) layer).getWorldGenSeed());
        return cloned;
    }

    /**
     * Clones the given {@link GenLayer}.
     *
     * @param layer the {@link GenLayer} to clone
     * @return the cloned {@link GenLayer}
     */
    public GenLayer cloneLayer(@NonNull GenLayer layer) {
        return clone0(layer, new IdentityHashMap<>());
    }

    private GenLayer clone0(@NonNull GenLayer layer, @NonNull Map<GenLayer, GenLayer> cache) {
        GenLayer cloned = cache.get(layer);
        if (cloned != null) { //return value from cache if present
            return cloned;
        }

        GenLayer[] origParents = getParents(layer);
        GenLayer[] clonedParents = new GenLayer[origParents.length];
        for (int i = 0; i < origParents.length; i++) {
            clonedParents[i] = clone0(origParents[i], cache);
        }

        cloned = cloneLayer(layer, clonedParents);
        checkArg(cache.putIfAbsent(layer, cloned) == null, "duplicate layer %s", layer);
        return cloned;
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
