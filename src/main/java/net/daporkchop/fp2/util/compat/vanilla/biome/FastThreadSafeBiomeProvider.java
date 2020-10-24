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
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import static net.daporkchop.fp2.util.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 */
public class FastThreadSafeBiomeProvider implements IBiomeProvider {
    public FastThreadSafeBiomeProvider(@NonNull BiomeProvider provider) {
        FastLayer[] fastLayers = makeFast(provider.genBiomes, provider.biomeIndexLayer);
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
