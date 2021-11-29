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

package net.daporkchop.fp2.util.threading.asyncblockaccess;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Implementation of {@link IAsyncBlockAccess} which delegates all method calls to a parent instance.
 * <p>
 * Intended for use as a base for implementations returned by {@link IAsyncBlockAccess#prefetch(Stream)} and
 * {@link IAsyncBlockAccess#prefetch(Stream, Function)}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractPrefetchedAsyncBlockAccess<P extends IAsyncBlockAccess> implements IBlockHeightAccess {
    @NonNull
    protected final P parent;
    @NonNull
    protected final WorldServer world;
    protected final boolean allowGeneration;

    @Override
    public final WorldType getWorldType() {
        return this.parent.getWorldType();
    }

    @Override
    public int getTopBlockY(int blockX, int blockZ) {
        return this.parent.getTopBlockY(blockX, blockZ, this.allowGeneration);
    }

    @Override
    public int getTopBlockYBelow(int blockX, int blockY, int blockZ) {
        return this.parent.getTopBlockYBelow(blockX, blockY, blockZ, this.allowGeneration);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int defaultBlockLightValue) {
        return this.parent.getCombinedLight(pos, defaultBlockLightValue, this.allowGeneration);
    }

    @Override
    public int getBlockLight(BlockPos pos) {
        return this.parent.getBlockLight(pos, this.allowGeneration);
    }

    @Override
    public int getSkyLight(BlockPos pos) {
        return this.parent.getSkyLight(pos, this.allowGeneration);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return this.parent.getBlockState(pos, this.allowGeneration);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.parent.getBiome(pos, this.allowGeneration);
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return this.getBlockState(pos).getStrongPower(this, pos, direction);
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.parent.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
