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

import net.daporkchop.fp2.compat.vanilla.biome.layer.java.JavaLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.vanilla.GenLayerRandomValues;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerAddSnow;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerRemoveTooMuchOcean;
import net.minecraft.world.gen.layer.GenLayerRiverInit;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerZoom;

/**
 * Extension of {@link JavaLayerProvider} which replaces a number of the layer types with native C++ implementations.
 *
 * @author DaPorkchop_
 */
public class NativeLayerProvider extends JavaLayerProvider {
    protected NativeLayerProvider() {
        this.fastMapperOverrides.put(GenLayerAddIsland.class, layer -> new NativeFastLayerAddIsland(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerAddSnow.class, layer -> new NativeFastLayerAddSnow(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerFuzzyZoom.class, layer -> new NativeFastLayerFuzzyZoom(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerIsland.class, layer -> new NativeFastLayerIsland(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerRemoveTooMuchOcean.class, layer -> new NativeFastLayerRemoveTooMuchOcean(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerRiverInit.class, layer -> new NativeFastLayerRiverInit(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerSmooth.class, layer -> new NativeFastLayerSmooth(layer.worldGenSeed));
        this.fastMapperOverrides.put(GenLayerZoom.class, layer -> new NativeFastLayerZoom(layer.worldGenSeed));

        this.fastMapperOverrides.put(GenLayerRandomValues.class, layer -> new NativeFastLayerRandomValues(layer.worldGenSeed, ((GenLayerRandomValues) layer).limit()));
    }

    @Override
    public boolean isNative() {
        return true;
    }
}
