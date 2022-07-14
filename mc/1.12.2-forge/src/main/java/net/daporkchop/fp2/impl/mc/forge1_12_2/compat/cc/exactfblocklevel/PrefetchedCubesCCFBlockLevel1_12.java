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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.exactfblocklevel;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.core.minecraft.world.cubes.AbstractCubesExactFBlockLevelHolder;
import net.daporkchop.fp2.core.minecraft.world.cubes.AbstractPrefetchedCubesExactFBlockLevel;
import net.daporkchop.lib.math.vector.Vec3i;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author DaPorkchop_
 */
@Getter
public class PrefetchedCubesCCFBlockLevel1_12 extends AbstractPrefetchedCubesExactFBlockLevel<ICube> implements IBlockAccess {
    public PrefetchedCubesCCFBlockLevel1_12(@NonNull AbstractCubesExactFBlockLevelHolder<ICube> holder, boolean generationAllowed, @NonNull List<ICube> cubes) {
        super(holder, generationAllowed, cubes);
    }

    @Override
    protected Vec3i cubePosition(@NonNull ICube cube) {
        return Vec3i.of(cube.getX(), cube.getY(), cube.getZ());
    }

    @Override
    protected int getState(int x, int y, int z, ICube cube) throws GenerationNotAllowedException {
        return this.registry().state2id(cube.getBlockState(x, y, z).getActualState(this, new BlockPos(x, y, z)));
    }

    @Override
    protected int getBiome(int x, int y, int z, ICube cube) throws GenerationNotAllowedException {
        return this.registry().biome2id(cube.getBiome(new BlockPos(x, y, z)));
    }

    @Override
    protected byte getLight(int x, int y, int z, ICube cube) throws GenerationNotAllowedException {
        BlockPos pos = new BlockPos(x, y, z);
        return BlockLevelConstants.packLight(cube.getLightFor(EnumSkyBlock.SKY, pos), cube.getLightFor(EnumSkyBlock.BLOCK, pos));
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
            //this is gross, i'd rather have it throw an exception than having to load the cube. unfortunately, Block#getActualBlockState may have to access the state of
            //  a neighboring block, which may not have been prefetched. however, since we NEED to know the real block state at the position, we're forced to load the cube...
            return this.getOrLoadCube(pos.getX(), pos.getY(), pos.getZ()).getBlockState(pos);
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
