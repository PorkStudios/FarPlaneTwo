/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.biome;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.IBiomeAccess;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * Wraps an {@link ICube}'s biome data into an {@link IBiomeAccess}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class CubeBiomeAccessWrapper implements IBiomeAccess {
    @NonNull
    protected final byte[] biomeArray;

    @Override
    public Biome getBiome(@NonNull BlockPos pos) {
        int biomeLocalX = Coords.blockToLocalBiome3d(pos.getX());
        int biomeLocalY = Coords.blockToLocalBiome3d(pos.getY());
        int biomeLocalZ = Coords.blockToLocalBiome3d(pos.getZ());
        return FastRegistry.getBiome(this.biomeArray[AddressTools.getBiomeAddress3d(biomeLocalX, biomeLocalY, biomeLocalZ)] & 0xFF, Biomes.PLAINS);
    }
}
