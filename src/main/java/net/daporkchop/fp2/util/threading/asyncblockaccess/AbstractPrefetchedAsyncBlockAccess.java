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
import net.daporkchop.lib.concurrent.PFuture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Implementation of {@link AsyncBlockAccess} which delegates all method calls to a parent instance.
 * <p>
 * Intended for use as a base for implementations returned by {@link AsyncBlockAccess#prefetchAsync(Stream)} and
 * {@link AsyncBlockAccess#prefetchAsync(Stream, Function)}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractPrefetchedAsyncBlockAccess<P extends AsyncBlockAccess> implements AsyncBlockAccess {
    @NonNull
    protected final P parent;
    @NonNull
    protected final WorldServer world;

    @Override
    public final PFuture<IBlockHeightAccess> prefetchAsync(@NonNull Stream<ChunkPos> columns) {
        return this.parent.prefetchAsync(columns);
    }

    @Override
    public final PFuture<IBlockHeightAccess> prefetchAsync(@NonNull Stream<ChunkPos> columns, @NonNull Function<IBlockHeightAccess, Stream<Vec3i>> cubesMappingFunction) {
        return this.parent.prefetchAsync(columns, cubesMappingFunction);
    }

    @Override
    public final WorldType getWorldType() {
        return this.parent.getWorldType();
    }

    @Override
    public int getTopBlockY(int blockX, int blockZ) {
        return this.parent.getTopBlockY(blockX, blockZ);
    }

    @Override
    public int getTopBlockYBelow(int blockX, int blockY, int blockZ) {
        return this.parent.getTopBlockYBelow(blockX, blockY, blockZ);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int defaultBlockLightValue) {
        return this.parent.getCombinedLight(pos, defaultBlockLightValue);
    }

    @Override
    public int getBlockLight(BlockPos pos) {
        return this.parent.getBlockLight(pos);
    }

    @Override
    public int getSkyLight(BlockPos pos) {
        return this.parent.getSkyLight(pos);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return this.parent.getBlockState(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.parent.getBiome(pos);
    }
}
