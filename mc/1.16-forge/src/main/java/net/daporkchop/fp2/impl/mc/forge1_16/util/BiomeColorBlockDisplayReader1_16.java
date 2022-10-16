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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraftforge.client.model.pipeline.LightUtil;

import javax.annotation.Nullable;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
public class BiomeColorBlockDisplayReader1_16 implements IBlockDisplayReader {
    @NonNull
    protected Biome biome;

    @Override
    public float getShade(Direction dir, boolean shade) {
        return shade ? LightUtil.diffuseLight(dir) : 1.0f;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return resolver.getColor(this.biome, pos.getX(), pos.getZ());
    }

    @Nullable
    @Override
    public TileEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    //other mods shouldn't be calling any of the following methods, although they can be implemented if it turns out they need to be

    @Override
    public WorldLightManager getLightEngine() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        throw new UnsupportedOperationException();
    }
}
