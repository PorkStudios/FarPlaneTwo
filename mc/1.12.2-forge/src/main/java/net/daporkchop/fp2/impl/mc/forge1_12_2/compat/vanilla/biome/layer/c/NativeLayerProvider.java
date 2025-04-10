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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.c;

import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.BiomeHelperCached;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java.JavaLayerProvider;

import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.BiomeHelper.*;
import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.BiomeHelperCached.*;

/**
 * Extension of {@link JavaLayerProvider} which replaces a number of the layer types with native C++ implementations.
 *
 * @author DaPorkchop_
 */
public class NativeLayerProvider extends JavaLayerProvider implements BiomeHelperCached.ReloadListener {
    @Deprecated
    public NativeLayerProvider() {
        //register self as a BiomeHelperCached reload listener
        BiomeHelperCached.addReloadListener(this);
    }

    @Override
    public boolean isNative() {
        return true;
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public void onBiomeHelperCachedReload() {
        int[] ids = new int[63];
        int i = 0;
        ids[i++] = ID_OCEAN;
        ids[i++] = ID_DEFAULT;
        ids[i++] = ID_PLAINS;
        ids[i++] = ID_DESERT;
        ids[i++] = ID_EXTREME_HILLS;
        ids[i++] = ID_FOREST;
        ids[i++] = ID_TAIGA;
        ids[i++] = ID_SWAMPLAND;
        ids[i++] = ID_RIVER;
        ids[i++] = ID_HELL;
        ids[i++] = ID_SKY;
        ids[i++] = ID_FROZEN_OCEAN;
        ids[i++] = ID_FROZEN_RIVER;
        ids[i++] = ID_ICE_PLAINS;
        ids[i++] = ID_ICE_MOUNTAINS;
        ids[i++] = ID_MUSHROOM_ISLAND;
        ids[i++] = ID_MUSHROOM_ISLAND_SHORE;
        ids[i++] = ID_BEACH;
        ids[i++] = ID_DESERT_HILLS;
        ids[i++] = ID_FOREST_HILLS;
        ids[i++] = ID_TAIGA_HILLS;
        ids[i++] = ID_EXTREME_HILLS_EDGE;
        ids[i++] = ID_JUNGLE;
        ids[i++] = ID_JUNGLE_HILLS;
        ids[i++] = ID_JUNGLE_EDGE;
        ids[i++] = ID_DEEP_OCEAN;
        ids[i++] = ID_STONE_BEACH;
        ids[i++] = ID_COLD_BEACH;
        ids[i++] = ID_BIRCH_FOREST;
        ids[i++] = ID_BIRCH_FOREST_HILLS;
        ids[i++] = ID_ROOFED_FOREST;
        ids[i++] = ID_COLD_TAIGA;
        ids[i++] = ID_COLD_TAIGA_HILLS;
        ids[i++] = ID_REDWOOD_TAIGA;
        ids[i++] = ID_REDWOOD_TAIGA_HILLS;
        ids[i++] = ID_EXTREME_HILLS_WITH_TREES;
        ids[i++] = ID_SAVANNA;
        ids[i++] = ID_SAVANNA_PLATEAU;
        ids[i++] = ID_MESA;
        ids[i++] = ID_MESA_ROCK;
        ids[i++] = ID_MESA_CLEAR_ROCK;
        ids[i++] = ID_VOID;
        ids[i++] = ID_MUTATED_PLAINS;
        ids[i++] = ID_MUTATED_DESERT;
        ids[i++] = ID_MUTATED_EXTREME_HILLS;
        ids[i++] = ID_MUTATED_FOREST;
        ids[i++] = ID_MUTATED_TAIGA;
        ids[i++] = ID_MUTATED_SWAMPLAND;
        ids[i++] = ID_MUTATED_ICE_FLATS;
        ids[i++] = ID_MUTATED_JUNGLE;
        ids[i++] = ID_MUTATED_JUNGLE_EDGE;
        ids[i++] = ID_MUTATED_BIRCH_FOREST;
        ids[i++] = ID_MUTATED_BIRCH_FOREST_HILLS;
        ids[i++] = ID_MUTATED_ROOFED_FOREST;
        ids[i++] = ID_MUTATED_TAIGA_COLD;
        ids[i++] = ID_MUTATED_REDWOOD_TAIGA;
        ids[i++] = ID_MUTATED_REDWOOD_TAIGA_HILLS;
        ids[i++] = ID_MUTATED_EXTREME_HILLS_WITH_TREES;
        ids[i++] = ID_MUTATED_SAVANNA;
        ids[i++] = ID_MUTATED_SAVANNA_ROCK;
        ids[i++] = ID_MUTATED_MESA;
        ids[i++] = ID_MUTATED_MESA_ROCK;
        ids[i++] = ID_MUTATED_MESA_CLEAR_ROCK;

        this.reload0(BIOME_COUNT, ids, FLAGS, EQUALS, MUTATIONS);
    }

    protected native void reload0(int count, @NonNull int[] ids, @NonNull byte[] flags, @NonNull byte[] equals, @NonNull int[] mutations);
}
