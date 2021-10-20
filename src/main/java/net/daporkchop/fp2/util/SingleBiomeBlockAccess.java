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

package net.daporkchop.fp2.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
public class SingleBiomeBlockAccess implements IBlockAccess {
    protected Biome biome;

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.biome;
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null; //there are no tile entities
    }

    //other mods shouldn't be calling any of the following methods, although they can be implemented if it turns out they need to be

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorldType getWorldType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        throw new UnsupportedOperationException();
    }
}
