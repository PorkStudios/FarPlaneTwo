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

package net.daporkchop.fp2.compat.vanilla.biome.layer.c;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.JavaLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.vanilla.GenLayerRandomValues;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerAddMushroomIsland;
import net.minecraft.world.gen.layer.GenLayerAddSnow;
import net.minecraft.world.gen.layer.GenLayerDeepOcean;
import net.minecraft.world.gen.layer.GenLayerEdge;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerRareBiome;
import net.minecraft.world.gen.layer.GenLayerRemoveTooMuchOcean;
import net.minecraft.world.gen.layer.GenLayerRiver;
import net.minecraft.world.gen.layer.GenLayerRiverInit;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;

/**
 * Extension of {@link JavaLayerProvider} which replaces a number of the layer types with native C++ implementations.
 *
 * @author DaPorkchop_
 */
public class NativeLayerProvider extends JavaLayerProvider {
    protected synchronized static void initBiomeIds() {
        int[] ids = new int[63];
        int i = 0;
        ids[i++] = Biome.getIdForBiome(Biomes.OCEAN);
        ids[i++] = Biome.getIdForBiome(Biomes.DEFAULT);
        ids[i++] = Biome.getIdForBiome(Biomes.PLAINS);
        ids[i++] = Biome.getIdForBiome(Biomes.DESERT);
        ids[i++] = Biome.getIdForBiome(Biomes.EXTREME_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.FOREST);
        ids[i++] = Biome.getIdForBiome(Biomes.TAIGA);
        ids[i++] = Biome.getIdForBiome(Biomes.SWAMPLAND);
        ids[i++] = Biome.getIdForBiome(Biomes.RIVER);
        ids[i++] = Biome.getIdForBiome(Biomes.HELL);
        ids[i++] = Biome.getIdForBiome(Biomes.SKY);
        ids[i++] = Biome.getIdForBiome(Biomes.FROZEN_OCEAN);
        ids[i++] = Biome.getIdForBiome(Biomes.FROZEN_RIVER);
        ids[i++] = Biome.getIdForBiome(Biomes.ICE_PLAINS);
        ids[i++] = Biome.getIdForBiome(Biomes.ICE_MOUNTAINS);
        ids[i++] = Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND);
        ids[i++] = Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND_SHORE);
        ids[i++] = Biome.getIdForBiome(Biomes.BEACH);
        ids[i++] = Biome.getIdForBiome(Biomes.DESERT_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.FOREST_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.TAIGA_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.EXTREME_HILLS_EDGE);
        ids[i++] = Biome.getIdForBiome(Biomes.JUNGLE);
        ids[i++] = Biome.getIdForBiome(Biomes.JUNGLE_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.JUNGLE_EDGE);
        ids[i++] = Biome.getIdForBiome(Biomes.DEEP_OCEAN);
        ids[i++] = Biome.getIdForBiome(Biomes.STONE_BEACH);
        ids[i++] = Biome.getIdForBiome(Biomes.COLD_BEACH);
        ids[i++] = Biome.getIdForBiome(Biomes.BIRCH_FOREST);
        ids[i++] = Biome.getIdForBiome(Biomes.BIRCH_FOREST_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.ROOFED_FOREST);
        ids[i++] = Biome.getIdForBiome(Biomes.COLD_TAIGA);
        ids[i++] = Biome.getIdForBiome(Biomes.COLD_TAIGA_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.REDWOOD_TAIGA);
        ids[i++] = Biome.getIdForBiome(Biomes.REDWOOD_TAIGA_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.EXTREME_HILLS_WITH_TREES);
        ids[i++] = Biome.getIdForBiome(Biomes.SAVANNA);
        ids[i++] = Biome.getIdForBiome(Biomes.SAVANNA_PLATEAU);
        ids[i++] = Biome.getIdForBiome(Biomes.MESA);
        ids[i++] = Biome.getIdForBiome(Biomes.MESA_ROCK);
        ids[i++] = Biome.getIdForBiome(Biomes.MESA_CLEAR_ROCK);
        ids[i++] = Biome.getIdForBiome(Biomes.VOID);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_PLAINS);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_DESERT);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_EXTREME_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_FOREST);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_TAIGA);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_SWAMPLAND);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_ICE_FLATS);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_JUNGLE);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_JUNGLE_EDGE);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_BIRCH_FOREST);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_BIRCH_FOREST_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_ROOFED_FOREST);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_TAIGA_COLD);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_REDWOOD_TAIGA);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_REDWOOD_TAIGA_HILLS);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_EXTREME_HILLS_WITH_TREES);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_SAVANNA);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_SAVANNA_ROCK);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_MESA);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_MESA_ROCK);
        ids[i++] = Biome.getIdForBiome(Biomes.MUTATED_MESA_CLEAR_ROCK);
        initBiomeIds0(ids);
    }

    protected static native void initBiomeIds0(@NonNull int[] ids);

    protected NativeLayerProvider() {
        this.fastMapperOverrides.put(GenLayerAddIsland.class, layer -> new NativeFastLayerAddIsland(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerAddMushroomIsland.class, layer -> new NativeFastLayerAddMushroomIsland(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerAddSnow.class, layer -> new NativeFastLayerAddSnow(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerDeepOcean.class, layer -> new NativeFastLayerDeepOcean(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerEdge.class, layer -> NativeFastLayerEdge.makeFast((GenLayerEdge) layer));
        this.fastMapperOverrides.put(GenLayerFuzzyZoom.class, layer -> new NativeFastLayerFuzzyZoom(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerIsland.class, layer -> new NativeFastLayerIsland(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerRareBiome.class, layer -> new NativeFastLayerRareBiome(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerRemoveTooMuchOcean.class, layer -> new NativeFastLayerRemoveTooMuchOcean(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerRiver.class, layer -> new NativeFastLayerRiver(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerRiverInit.class, layer -> new NativeFastLayerRiverInit(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerSmooth.class, layer -> new NativeFastLayerSmooth(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerVoronoiZoom.class, layer -> new NativeFastLayerVoronoiZoom(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerZoom.class, layer -> new NativeFastLayerZoom(layer.worldGenSeed));

        this.fastMapperOverrides.put(GenLayerRandomValues.class, layer -> new NativeFastLayerRandomValues(layer.worldGenSeed, ((GenLayerRandomValues) layer).limit()));
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public IFastLayer[] makeFast(@NonNull GenLayer... inputs) {
        initBiomeIds();
        return super.makeFast(inputs);
    }
}
