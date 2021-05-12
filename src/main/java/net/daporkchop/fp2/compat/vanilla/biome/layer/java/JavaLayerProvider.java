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

package net.daporkchop.fp2.compat.vanilla.biome.layer.java;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.minecraft.world.gen.layer.GenLayer;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Implementation of {@link FastLayerProvider} in Java.
 *
 * @author DaPorkchop_
 */
public class JavaLayerProvider implements FastLayerProvider {
    private static void addAllLayers(Map<GenLayer, GenLayer[]> childrenMap, GenLayer layer) {
        if (childrenMap.containsKey(layer)) {
            return; //don't re-add the same layer twice
        }

        GenLayer[] parents = BiomeHelper.getParents(layer);
        childrenMap.put(layer, parents);

        //add parents recursively
        for (GenLayer child : parents) {
            addAllLayers(childrenMap, child);
        }
    }

    protected final Map<Class<? extends GenLayer>, Function<GenLayer, IFastLayer>> fastMapperOverrides = new IdentityHashMap<>();

    /**
     * @see FastLayerProvider#INSTANCE
     * @deprecated internal API, do not touch!
     */
    @Deprecated
    public JavaLayerProvider() {
    }

    protected IFastLayer convertLayer(@NonNull GenLayer layer) {
        return this.fastMapperOverrides.getOrDefault(layer.getClass(), BiomeHelper::convertLayer).apply(layer);
    }

    @Override
    public IFastLayer[] makeFast(@NonNull GenLayer... inputs) {
        //initial add all layers and find their children
        Map<GenLayer, GenLayer[]> children = new IdentityHashMap<>();
        for (GenLayer layer : inputs) {
            addAllLayers(children, layer);
        }

        //map vanilla layers to fast layers
        Map<GenLayer, IFastLayer> fastLayers = new IdentityHashMap<>();
        children.keySet().forEach(layer -> fastLayers.put(layer, this.convertLayer(layer)));

        //init fast layers with their children
        fastLayers.forEach((vanilla, fast) -> {
            IFastLayer[] fastChildren = Arrays.stream(children.get(vanilla)).map(fastLayers::get).toArray(IFastLayer[]::new);
            fast.init(fastChildren);
        });

        return Arrays.stream(inputs).map(fastLayers::get).toArray(IFastLayer[]::new);
    }

    @Override
    public boolean isNative() {
        return false;
    }
}
