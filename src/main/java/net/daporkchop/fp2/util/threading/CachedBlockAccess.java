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

package net.daporkchop.fp2.util.threading;

import lombok.NonNull;
import net.daporkchop.fp2.util.IHeightMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

/**
 * Thread-safe cache for block data in a world.
 *
 * @author DaPorkchop_
 */
public interface CachedBlockAccess extends IBlockAccess, IHeightMap {
    /**
     * Requests that all blocks in the given range be fetched into the cache.
     * <p>
     * This can help prevent multiple consecutive cache misses when a large area is going to be accessed.
     *
     * @param range   a bounding box containing the region to prefetch
     * @param ignoreY whether or not the Y value should be ignored
     */
    void prefetch(@NonNull AxisAlignedBB range, boolean ignoreY);

    /**
     * Drops some data from the cache.
     */
    void gc();

    @Override
    int getTopBlockY(int blockX, int blockZ);

    @Override
    int getTopBlockYBelow(int blockX, int blockY, int blockZ);

    @Override
    int getCombinedLight(BlockPos pos, int lightValue);

    int getBlockLight(BlockPos pos);

    int getSkyLight(BlockPos pos);

    @Override
    IBlockState getBlockState(BlockPos pos);

    @Override
    default boolean isAirBlock(BlockPos pos) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    Biome getBiome(BlockPos pos);

    @Override
    default int getStrongPower(BlockPos pos, EnumFacing direction) {
        return this.getBlockState(pos).getStrongPower(this, pos, direction);
    }

    @Override
    WorldType getWorldType();

    @Override
    default boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return this.getBlockState(pos).isSideSolid(this, pos, side);
    }

    /**
     * @deprecated access to tile entities is not allowed
     */
    @Override
    @Deprecated
    default TileEntity getTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    /**
     * Allows access to the {@link CachedBlockAccess} belonging to a {@link net.minecraft.world.WorldServer}.
     *
     * @author DaPorkchop_
     */
    interface Holder {
        CachedBlockAccess fp2_cachedBlockAccess();
    }
}
