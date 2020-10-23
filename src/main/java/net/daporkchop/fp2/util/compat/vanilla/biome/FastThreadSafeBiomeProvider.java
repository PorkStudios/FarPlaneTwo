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

package net.daporkchop.fp2.util.compat.vanilla.biome;

import lombok.NonNull;
import net.daporkchop.fp2.util.compat.vanilla.biome.layer.FastLayer;
import net.daporkchop.fp2.util.compat.vanilla.biome.layer.FastLayerAddIsland;
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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class FastThreadSafeBiomeProvider implements IBiomeProvider {
    //gets all direct children of a GenLayer
    public static final Map<Class<? extends GenLayer>, Function<GenLayer, GenLayer[]>> GET_CHILDREN = new IdentityHashMap<>();

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
        GET_CHILDREN.put(GenLayerIsland.class, parent);
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

    public static final Map<Class<? extends GenLayer>, Function<GenLayer, FastLayer>> FAST_MAPPERS = new IdentityHashMap<>();

    static {
        FAST_MAPPERS.put(GenLayerAddIsland.class, layer -> new FastLayerAddIsland(layer.worldGenSeed));
    }

    protected static void addAllLayers(Map<GenLayer, GenLayer[]> childrenMap, GenLayer layer) {
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

    public FastThreadSafeBiomeProvider(@NonNull BiomeProvider provider) {
        Map<GenLayer, GenLayer[]> children = new IdentityHashMap<>();
        addAllLayers(children, provider.genBiomes);
        addAllLayers(children, provider.biomeIndexLayer);
        Map<GenLayer, FastLayer> fastLayers = children.entrySet();
    }

    //TODO: implement everything

    @Override
    public Biome biome(int blockX, int blockZ) {
        return null;
    }

    @Override
    public int biomeId(int blockX, int blockZ) {
        return 0;
    }

    @Override
    public void biomes(@NonNull Biome[] arr, int blockX, int blockZ, int sizeX, int sizeZ) {
    }

    @Override
    public void biomeIds(@NonNull byte[] arr, int blockX, int blockZ, int sizeX, int sizeZ) {
    }
}
