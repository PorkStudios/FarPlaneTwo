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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.fixes.world.biome;

import net.daporkchop.fp2.impl.mc.forge1_12_2.util.SingleBiomeBlockAccess;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.Util1_12_2;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Some optional optimizations for biome color calculation.
 *
 * @author DaPorkchop_
 */
@SuppressWarnings("JavadocReference")
@Mixin(BiomeColorHelper.class)
public abstract class MixinBiomeColorHelper1_12 {
    /**
     * This prevents allocation of a few pointless objects in {@link BlockPos#getAllInBoxMutable}, and also eliminates a likely megamorphic call site in
     * {@link BiomeColorHelper#getColorAtPos(IBlockAccess, BlockPos, BiomeColorHelper.ColorResolver)}.
     *
     * @author DaPorkchop_
     * @reason optimization
     */
    @Overwrite
    public static int getGrassColorAtPos(IBlockAccess blockAccess, BlockPos pos) {
        int r = 0;
        int g = 0;
        int b = 0;

        Recycler<BlockPos.MutableBlockPos> recycler = Util1_12_2.MUTABLEBLOCKPOS_RECYCLER.get();
        BlockPos.MutableBlockPos mutableBlockPos = recycler.allocate();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                mutableBlockPos.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                int color = blockAccess.getBiome(mutableBlockPos).getGrassColorAtPos(mutableBlockPos);

                r += (color & 0xFF0000) >> 16;
                g += (color & 0x00FF00) >> 8;
                b += (color & 0x0000FF);
            }
        }
        recycler.release(mutableBlockPos);

        return (((r / 9) & 0xFF) << 16) | (((g / 9) & 0xFF) << 8) | ((b / 9) & 0xFF);
    }

    /**
     * This prevents allocation of a few pointless objects in {@link BlockPos#getAllInBoxMutable}, and also eliminates a likely megamorphic call site in
     * {@link BiomeColorHelper#getColorAtPos(IBlockAccess, BlockPos, BiomeColorHelper.ColorResolver)}.
     *
     * @author DaPorkchop_
     * @reason optimization
     */
    @Overwrite
    public static int getFoliageColorAtPos(IBlockAccess blockAccess, BlockPos pos) {
        int r = 0;
        int g = 0;
        int b = 0;

        Recycler<BlockPos.MutableBlockPos> recycler = Util1_12_2.MUTABLEBLOCKPOS_RECYCLER.get();
        BlockPos.MutableBlockPos mutableBlockPos = recycler.allocate();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                mutableBlockPos.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                int color = blockAccess.getBiome(mutableBlockPos).getFoliageColorAtPos(mutableBlockPos);

                r += (color & 0xFF0000) >> 16;
                g += (color & 0x00FF00) >> 8;
                b += (color & 0x0000FF);
            }
        }
        recycler.release(mutableBlockPos);

        return (((r / 9) & 0xFF) << 16) | (((g / 9) & 0xFF) << 8) | ((b / 9) & 0xFF);
    }

    /**
     * This prevents allocation of a few pointless objects in {@link BlockPos#getAllInBoxMutable}, and also eliminates a likely megamorphic call site in
     * {@link BiomeColorHelper#getColorAtPos(IBlockAccess, BlockPos, BiomeColorHelper.ColorResolver)}.
     *
     * @author DaPorkchop_
     * @reason optimization
     */
    @Overwrite
    public static int getWaterColorAtPos(IBlockAccess blockAccess, BlockPos pos) {
        if (blockAccess instanceof SingleBiomeBlockAccess) { //fast-track for SingleBiomeBlockAccess
            //the biome is the same at every position, and water color is position-independent, so we don't need to compute an average color
            return ((SingleBiomeBlockAccess) blockAccess).biome().getWaterColor();
        }

        int r = 0;
        int g = 0;
        int b = 0;

        Recycler<BlockPos.MutableBlockPos> recycler = Util1_12_2.MUTABLEBLOCKPOS_RECYCLER.get();
        BlockPos.MutableBlockPos mutableBlockPos = recycler.allocate();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                mutableBlockPos.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                int color = blockAccess.getBiome(mutableBlockPos).getWaterColor();

                r += (color & 0xFF0000) >> 16;
                g += (color & 0x00FF00) >> 8;
                b += (color & 0x0000FF);
            }
        }
        recycler.release(mutableBlockPos);

        return (((r / 9) & 0xFF) << 16) | (((g / 9) & 0xFF) << 8) | ((b / 9) & 0xFF);
    }
}
