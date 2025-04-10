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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerBiome1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerEdge1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerHills1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayerRiverMix1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerAddIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerAddMushroomIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerAddSnow;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerBiome;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerBiomeEdge;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerDeepOcean;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerEdge;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerFuzzyZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerHills;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerRareBiome;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerRemoveTooMuchOcean;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerRiver;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerRiverInit;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerRiverMix;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerShore;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerSmooth;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerVoronoiZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c.NativeFastLayerZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat.AutoIntCacheResettingFastLayer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat.CompatLayerHelper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat.CompatPaddedLayerWrapper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerAddIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerAddMushroomIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerAddSnow;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerBiome;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerBiomeEdge;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerDeepOcean;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerEdge;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerFuzzyZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerHills;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerIsland;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRareBiome;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRemoveTooMuchOcean;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRiver;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRiverInit;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerRiverMix;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerShore;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerSmooth;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerVoronoiZoom;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaFastLayerZoom;
import net.daporkchop.lib.common.util.PorkUtil;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper class for working with biome generation layers.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class GenLayerHelper {
    private static final Map<Class<? extends GenLayer>, GenLayerFunctions> REGISTERED_FUNCTIONS = new ConcurrentHashMap<>();

    private static final ClassValue<GenLayerFunctions> DEFAULT_FUNCTIONS_CACHE = new ClassValue<GenLayerFunctions>() {
        @Override
        protected GenLayerFunctions computeValue(Class<?> type) {
            Class<? extends GenLayer> layerClass = PorkUtil.uncheckedCast(type);

            //if the target class explicitly implements IGenLayerFunctions, use that
            //if (PArrays.linearSearchIdentity(layerClass.getInterfaces(), GenLayerFunctions.IGenLayerFunctions.class) >= 0) {
            if (Arrays.asList(layerClass.getInterfaces()).contains(GenLayerFunctions.IGenLayerFunctions.class)) {
                return GenLayerFunctions.IGenLayerFunctions.proxy(layerClass);
            }

            //fall back to CompatHelper
            return CompatLayerHelper.getDefaultGenLayerFunctions(layerClass);
        }
    };

    /**
     * Registers the given {@link GenLayerFunctions} for the given {@link GenLayer} implementation.
     *
     * @param layerClass the {@link GenLayer} implementation class
     * @param functions  the {@link GenLayerFunctions} instance to register
     */
    public static void registerFunctions(@NonNull Class<? extends GenLayer> layerClass, @NonNull GenLayerFunctions functions) {
        if (REGISTERED_FUNCTIONS.putIfAbsent(layerClass, functions) != null) {
            throw new IllegalArgumentException("already registered: " + layerClass);
        }

        DEFAULT_FUNCTIONS_CACHE.remove(layerClass);
    }

    /**
     * Finds the registered {@link GenLayerFunctions} for the given {@link GenLayer} implementation, or tries to select a default one if none were explicitly registered.
     *
     * @param layerClass the {@link GenLayer} implementation class
     * @return a {@link GenLayerFunctions} suitable for the given class
     */
    public static GenLayerFunctions lookupFunctions(@NonNull Class<? extends GenLayer> layerClass) {
        GenLayerFunctions result = REGISTERED_FUNCTIONS.get(layerClass);
        if (result != null) {
            return result;
        }

        //fall back to the default implementation
        return DEFAULT_FUNCTIONS_CACHE.get(layerClass);
    }

    static {
        registerFunctions(GenLayerAddIsland.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerAddIsland(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerAddIsland(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerAddIsland(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerAddMushroomIsland.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerAddMushroomIsland(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerAddMushroomIsland(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerAddMushroomIsland(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerAddSnow.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerAddSnow(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerAddSnow(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerAddSnow(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerBiome.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerBiome(0L, parent, null, ((ATGenLayerBiome1_12) layer).getSettings());
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerBiome((GenLayerBiome) layer, parent)
                        : new NativeFastLayerBiome((GenLayerBiome) layer, parent);
            }
        });
        registerFunctions(GenLayerBiomeEdge.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerBiomeEdge(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerBiomeEdge(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerBiomeEdge(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerDeepOcean.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerDeepOcean(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerDeepOcean(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerDeepOcean(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerEdge.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerEdge(0L, parent, ((ATGenLayerEdge1_12) layer).getMode());
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? JavaFastLayerEdge.makeFast((GenLayerEdge) layer, parent)
                        : NativeFastLayerEdge.makeFast((GenLayerEdge) layer, parent);
            }
        });
        registerFunctions(GenLayerFuzzyZoom.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerFuzzyZoom(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerFuzzyZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerFuzzyZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerHills.class, new GenLayerFunctions() {
            @Override
            public GenLayer[] getParents(@NonNull GenLayer layer) {
                return new GenLayer[]{ ((ATGenLayer1_12) layer).getParent(), ((ATGenLayerHills1_12) layer).getRiverLayer() };
            }

            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, GenLayer @NonNull [] parents) {
                return new GenLayerHills(0L, parents[0], parents[1]);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, IFastLayer @NonNull [] parents) {
                return provider.isNative()
                        ? new JavaFastLayerHills(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], parents[1])
                        : new NativeFastLayerHills(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], parents[1]);
            }
        });
        registerFunctions(GenLayerIsland.class, new GenLayerFunctions.NoParent() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer) {
                return new GenLayerIsland(0L);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer) {
                return provider.isNative()
                        ? new NativeFastLayerIsland(((ATGenLayer1_12) layer).getWorldGenSeed())
                        : new JavaFastLayerIsland(((ATGenLayer1_12) layer).getWorldGenSeed());
            }
        });
        registerFunctions(GenLayerRareBiome.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerRareBiome(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerRareBiome(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerRareBiome(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerRemoveTooMuchOcean.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerRemoveTooMuchOcean(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerRemoveTooMuchOcean(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerRemoveTooMuchOcean(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerRiver.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerRiver(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerRiver(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerRiver(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerRiverInit.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerRiverInit(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerRiverInit(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerRiverInit(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerRiverMix.class, new GenLayerFunctions() {
            @Override
            public GenLayer[] getParents(@NonNull GenLayer layer) {
                ATGenLayerRiverMix1_12 l = (ATGenLayerRiverMix1_12) layer;
                return new GenLayer[]{ l.getBiomePatternGeneratorChain(), l.getRiverPatternGeneratorChain() };
            }

            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, GenLayer @NonNull [] parents) {
                return new GenLayerRiverMix(0L, parents[0], parents[1]);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, IFastLayer @NonNull [] parents) {
                return provider.isNative()
                        ? new JavaFastLayerRiverMix(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], parents[1])
                        : new NativeFastLayerRiverMix(((ATGenLayer1_12) layer).getWorldGenSeed(), parents[0], parents[1]);
            }
        });
        registerFunctions(GenLayerShore.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerShore(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerShore(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerShore(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerSmooth.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerSmooth(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerSmooth(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerSmooth(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerVoronoiZoom.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerVoronoiZoom(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerVoronoiZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerVoronoiZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
        registerFunctions(GenLayerZoom.class, new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                return new GenLayerZoom(0L, parent);
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                return provider.isNative()
                        ? new JavaFastLayerZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parent)
                        : new NativeFastLayerZoom(((ATGenLayer1_12) layer).getWorldGenSeed(), parent);
            }
        });
    }

    /**
     * Extracts the parent {@link GenLayer}s of the given {@link GenLayer}.
     *
     * @param layer the {@link GenLayer}
     * @return an array containing the given {@link GenLayer}'s parents
     */
    public static GenLayer[] getParents(@NonNull GenLayer layer) {
        return lookupFunctions(layer.getClass()).getParents(layer);
    }

    /**
     * Creates a clone of the given {@link GenLayer} instance, replacing its parents with the given {@link GenLayer}s.
     *
     * @param layer   the {@link GenLayer} to clone
     * @param parents the parent {@link GenLayer}s of the cloned layer, in the same order as they would be returned by {@link #getParents(GenLayer)}
     * @return the cloned {@link GenLayer}
     */
    public static GenLayer cloneSingleLayer(@NonNull GenLayer layer, @NonNull List<GenLayer> parents) {
        GenLayer result = lookupFunctions(layer.getClass()).cloneLayer(layer, parents.toArray(new GenLayer[0]));

        //copy the exact seed values over, as setting them via the constructor/vanilla setters modifies the argument
        ((ATGenLayer1_12) result).setWorldGenSeed(((ATGenLayer1_12) layer).getWorldGenSeed());
        ((ATGenLayer1_12) result).setBaseSeed(((ATGenLayer1_12) layer).getBaseSeed());

        return result;
    }

    /**
     * Converts the given {@link GenLayer} instance to an equivalent {@link IFastLayer}.
     *
     * @param provider the {@link FastLayerProvider} being used
     * @param layer    the {@link GenLayer} to convert
     * @param parents  the parent {@link IFastLayer}s of the cloned layer, in the same order as they would be returned by {@link #getParents(GenLayer)}
     * @return the converted {@link IFastLayer}
     */
    public static IFastLayer makeSingleLayerFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull List<IFastLayer> parents) {
        return lookupFunctions(layer.getClass()).makeFast(provider, layer, parents.toArray(new IFastLayer[0]));
    }

    private static void getParentsDFS(Map<GenLayer, GenLayer[]> parentsMap, List<GenLayer> dependencyOrder, GenLayer layer) {
        if (!parentsMap.containsKey(layer)) {
            GenLayer[] parents = getParents(layer);
            parentsMap.put(layer, parents);

            for (GenLayer parent : parents) {
                getParentsDFS(parentsMap, dependencyOrder, parent);
            }

            dependencyOrder.add(layer);
        }
    }

    private static <T> List<T> remapLayers(BiFunction<GenLayer, List<T>, T> remapper, List<GenLayer> inputs) {
        Map<GenLayer, GenLayer[]> parentsMap = new IdentityHashMap<>();
        List<GenLayer> dependencyOrder = new ArrayList<>();

        //perform a DFS through the GenLayer tree
        for (GenLayer input : inputs) {
            getParentsDFS(parentsMap, dependencyOrder, input);
        }

        //remap map the input layers in order
        Map<GenLayer, T> fastLayers = new IdentityHashMap<>(dependencyOrder.size());
        Function<GenLayer, T> fastLayers_get = fastLayers::get;
        for (GenLayer layer : dependencyOrder) {
            List<T> remappedParents = Arrays.stream(parentsMap.get(layer)).map(fastLayers_get).peek(Objects::requireNonNull).collect(Collectors.toList());
            fastLayers.put(layer, remapper.apply(layer, remappedParents));
        }

        //collect the input GenLayers to their remapped equivalents in order
        return inputs.stream().map(fastLayers_get).collect(Collectors.toList());
    }

    /**
     * Clones an entire {@link GenLayer} hierarchy.
     *
     * @param inputs the input {@link GenLayer}s
     * @return the cloned {@link GenLayer}s in the corresponding order
     */
    public static List<GenLayer> cloneLayerTree(@NonNull List<GenLayer> inputs) {
        return remapLayers(GenLayerHelper::cloneSingleLayer, inputs);
    }

    /**
     * Converts an entire {@link GenLayer} hierarchy to equivalent {@link IFastLayer}s.
     *
     * @param inputs the input {@link GenLayer}s
     * @return the converted {@link IFastLayer}s in the corresponding order
     */
    public static List<IFastLayer> makeFast(@NonNull FastLayerProvider provider, @NonNull List<GenLayer> inputs) {
        List<IFastLayer> result = remapLayers((layer, parents) -> makeSingleLayerFast(provider, layer, parents), inputs);

        //ensure that all GenLayers will automatically reset the IntCache if necessary
        result.replaceAll(AutoIntCacheResettingFastLayer::adapt);

        return result;
    }
}
