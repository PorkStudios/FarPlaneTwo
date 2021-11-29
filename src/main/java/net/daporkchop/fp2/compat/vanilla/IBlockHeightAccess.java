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

package net.daporkchop.fp2.compat.vanilla;

import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.FGameRegistry;
import net.daporkchop.fp2.core.util.IHeightMap;
import net.daporkchop.fp2.impl.mc.forge1_12_2.GameRegistry1_12_2;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;

/**
 * Combination of {@link IBlockAccess} and {@link IHeightMap}.
 *
 * @author DaPorkchop_
 */
@Deprecated
public interface IBlockHeightAccess extends IBlockAccess, IHeightMap, FBlockWorld {
    /**
     * Re-definition of {@link IBlockAccess#getBiome(BlockPos)}, as that method's marked as client-only.
     */
    @Override
    Biome getBiome(BlockPos pos);

    int getSkyLight(BlockPos pos);

    int getBlockLight(BlockPos pos);

    @Override
    default int getCombinedLight(BlockPos pos, int defaultBlockLightValue) {
        return (this.getSkyLight(pos) << 20)
               | (Math.max(this.getBlockLight(pos), defaultBlockLightValue) << 4);
    }

    @Override
    default void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    default FGameRegistry registry() {
        return GameRegistry1_12_2.get();
    }

    @Override
    default int getState(int x, int y, int z) {
        return GameRegistry1_12_2.get().state2id(this.getBlockState(new BlockPos(x, y, z)));
    }

    @Override
    default int getBiome(int x, int y, int z) {
        return GameRegistry1_12_2.get().biome2id(this.getBiome(new BlockPos(x, y, z)));
    }

    @Override
    default byte getLight(int x, int y, int z) {
        int combinedLight = this.getCombinedLight(new BlockPos(x, y, z), 0);
        return FBlockWorld.packLight(combinedLight >> 20, combinedLight >> 4);
    }
}
