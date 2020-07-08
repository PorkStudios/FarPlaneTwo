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
     * @param range a bounding box containing the region to prefetch
     */
    void prefetch(@NonNull AxisAlignedBB range);

    @Override
    boolean isOccluded(int localX, int blockY, int localZ);

    @Override
    int getTopBlockY(int localX, int localZ);

    @Override
    int getTopBlockYBelow(int localX, int localZ, int blockY);

    @Override
    int getLowestTopBlockY();

    @Override
    int getCombinedLight(BlockPos pos, int lightValue);

    @Override
    IBlockState getBlockState(BlockPos pos);

    @Override
    boolean isAirBlock(BlockPos pos);

    @Override
    Biome getBiome(BlockPos pos);

    @Override
    int getStrongPower(BlockPos pos, EnumFacing direction);

    @Override
    WorldType getWorldType();

    @Override
    boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default);

    /**
     * @deprecated access to tile entities is not allowed
     */
    @Override
    @Deprecated
    default TileEntity getTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException();
    }
}
