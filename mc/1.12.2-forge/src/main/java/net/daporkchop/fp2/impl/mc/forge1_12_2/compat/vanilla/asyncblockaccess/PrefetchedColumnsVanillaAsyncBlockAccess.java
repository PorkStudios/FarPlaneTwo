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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.asyncblockaccess;

import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.asyncblockaccess.AbstractPrefetchedAsyncBlockAccess;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.asyncblockaccess.IAsyncBlockAccess;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link IAsyncBlockAccess} returned by {@link IAsyncBlockAccess#prefetch(Stream)}.
 *
 * @author DaPorkchop_
 */
public class PrefetchedColumnsVanillaAsyncBlockAccess extends AbstractPrefetchedAsyncBlockAccess<VanillaAsyncBlockAccessImpl> {
    protected final LongObjMap<Chunk> chunks = new LongObjOpenHashMap<>();

    public PrefetchedColumnsVanillaAsyncBlockAccess(VanillaAsyncBlockAccessImpl parent, WorldServer world, boolean allowGeneration, @NonNull Stream<Chunk> chunks) {
        super(parent, world, allowGeneration);

        chunks.forEach(chunk -> {
            long key = ChunkPos.asLong(chunk.x, chunk.z);
            checkArg(this.chunks.putIfAbsent(key, chunk) == null, "duplicate chunk at (%d, %d)", chunk.x, chunk.z);
        });
    }

    @Override
    public int getTopBlockY(int blockX, int blockZ) {
        Chunk chunk = this.chunks.get(ChunkPos.asLong(blockX >> 4, blockZ >> 4));
        if (chunk != null) {
            return chunk.getHeightValue(blockX & 0xF, blockZ & 0xF) - 1;
        }
        return super.getTopBlockY(blockX, blockZ);
    }

    @Override
    public int getTopBlockYBelow(int blockX, int blockY, int blockZ) {
        throw new UnsupportedOperationException("Not implemented"); //TODO: i could actually write an implementation for this
    }

    @Override
    public int getCombinedLight(BlockPos pos, int defaultBlockLightValue) {
        if (!this.world.isValid(pos))    {
            return this.world.provider.hasSkyLight() ? 0xF << 20 : 0;
        } else {
            Chunk chunk = this.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            if (chunk != null) {
                int skyLight = this.world.provider.hasSkyLight() ? chunk.getLightFor(EnumSkyBlock.SKY, pos) : 0;
                int blockLight = chunk.getLightFor(EnumSkyBlock.BLOCK, pos);
                return (skyLight << 20) | (blockLight << 4);
            }
            return super.getCombinedLight(pos, defaultBlockLightValue);
        }
    }

    @Override
    public int getBlockLight(BlockPos pos) {
        if (!this.world.isValid(pos))    {
            return 0;
        } else {
            Chunk chunk = this.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            if (chunk != null) {
                return chunk.getLightFor(EnumSkyBlock.BLOCK, pos);
            }
            return super.getBlockLight(pos);
        }
    }

    @Override
    public int getSkyLight(BlockPos pos) {
        if (!this.world.provider.hasSkyLight()) {
            return 0;
        } else if (!this.world.isValid(pos))    {
            return 15;
        } else {
            Chunk chunk = this.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            if (chunk != null) {
                return chunk.getLightFor(EnumSkyBlock.SKY, pos);
            }
            return super.getSkyLight(pos);
        }
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        Chunk chunk = this.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (chunk != null) {
            return chunk.getBlockState(pos);
        }
        return super.getBlockState(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        Chunk chunk = this.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (chunk != null) {
            return chunk.getBiome(pos, this.world.getBiomeProvider());
        }
        return super.getBiome(pos);
    }
}
