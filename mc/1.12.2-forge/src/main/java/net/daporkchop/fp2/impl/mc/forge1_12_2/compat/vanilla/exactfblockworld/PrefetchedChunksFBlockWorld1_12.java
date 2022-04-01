/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.exactfblockworld;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractChunksExactFBlockWorldHolder;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractPrefetchedChunksExactFBlockWorld;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.daporkchop.lib.common.math.BinMath;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author DaPorkchop_
 */
@Getter
public class PrefetchedChunksFBlockWorld1_12 extends AbstractPrefetchedChunksExactFBlockWorld<Chunk> implements IBlockAccess {
    public PrefetchedChunksFBlockWorld1_12(@NonNull AbstractChunksExactFBlockWorldHolder<Chunk> holder, boolean generationAllowed, @NonNull List<Chunk> chunks) {
        super(holder, generationAllowed, chunks);
    }

    @Override
    protected long packedChunkPosition(@NonNull Chunk chunk) {
        return BinMath.packXY(chunk.x, chunk.z);
    }

    @Override
    protected int getState(int x, int y, int z, Chunk chunk) throws GenerationNotAllowedException {
        BlockPos pos = new BlockPos(x, y, z);
        return GameRegistry1_12_2.get().state2id(chunk.getBlockState(pos).getActualState(this, pos));
    }

    @Override
    protected int getBiome(int x, int y, int z, Chunk chunk) throws GenerationNotAllowedException {
        return GameRegistry1_12_2.get().biome2id(chunk.getBiome(new BlockPos(x, y, z), null));
    }

    @Override
    protected byte getLight(int x, int y, int z, Chunk chunk) throws GenerationNotAllowedException {
        BlockPos pos = new BlockPos(x, y, z);
        return BlockWorldConstants.packLight(chunk.getLightFor(EnumSkyBlock.SKY, pos), chunk.getLightFor(EnumSkyBlock.BLOCK, pos));
    }

    // IBlockAccess

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null; //there are no tile entities!
    }

    @Override
    @SneakyThrows(GenerationNotAllowedException.class)
    public IBlockState getBlockState(BlockPos pos) {
        if (!this.holder().isValidPosition(pos.getX(), pos.getY(), pos.getZ())) { //position is outside world, return air
            return Blocks.AIR.getDefaultState();
        } else {
            Chunk chunk = this.chunks().get(BinMath.packXY(pos.getX() >> this.chunkShift(), pos.getZ() >> this.chunkShift()));

            //this is gross, i'd rather have it throw an exception. unfortunately, Block#getActualBlockState may have to access the state of a neighboring block, which may
            //  not have been prefetched. however, since we NEED to know the real block state at the position, we're forced to load the chunk...
            if (chunk == null) {
                //this instance doesn't have the chunk prefetched, try to retrieve it from the holder...
                chunk = this.holder().getChunk(pos.getX() >> this.chunkShift(), pos.getZ() >> this.chunkShift(), this.generationAllowed());

                //don't bother saving the loaded chunk into the cache:
                //- we don't want to modify this instance's state, in order to avoid causing future regular block accesses to be succeed when the would otherwise have failed
                //- this is very much an edge case which doesn't necessarily need to be fast
            }

            return chunk.getBlockState(pos);
        }
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return this.getBlockState(pos).getStrongPower(this, pos, direction);
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        if (!this.holder().isValidPosition(pos.getX(), pos.getY(), pos.getZ())) { //position is outside world, return default
            return _default;
        }

        return this.getBlockState(pos).isSideSolid(this, pos, side);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        throw new UnsupportedOperationException(); //this is a client-only method, i don't need to implement it
    }

    @SideOnly(Side.CLIENT)
    @Override
    public WorldType getWorldType() {
        throw new UnsupportedOperationException(); //this is a client-only method, i don't need to implement it
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Biome getBiome(BlockPos pos) {
        throw new UnsupportedOperationException(); //this is a client-only method, i don't need to implement it
    }
}
