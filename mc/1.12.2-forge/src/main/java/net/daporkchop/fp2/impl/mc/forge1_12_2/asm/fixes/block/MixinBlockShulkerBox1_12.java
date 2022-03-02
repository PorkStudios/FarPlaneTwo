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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.fixes.block;

import net.minecraft.block.BlockShulkerBox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author DaPorkchop_
 */
@Mixin(BlockShulkerBox.class)
public abstract class MixinBlockShulkerBox1_12 {
    //workaround for BlockShulkerBox#getBlockFaceShape not checking to see if the tile entity is instanceof TileEntityShulkerBox

    @Redirect(method = "Lnet/minecraft/block/BlockShulkerBox;getBlockFaceShape(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/block/state/BlockFaceShape;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/IBlockAccess;getTileEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/tileentity/TileEntity;"),
            require = 0, allow = 1)
    private TileEntity fp2_getBlockFaceShape_ensureTileEntityValidType(IBlockAccess access, BlockPos pos) {
        TileEntity te = access.getTileEntity(pos);
        return te instanceof TileEntityShulkerBox ? te : null;
    }

    @Redirect(method = "Lnet/minecraft/block/BlockShulkerBox;getBlockFaceShape(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/block/state/BlockFaceShape;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/tileentity/TileEntityShulkerBox;getAnimationStatus()Lnet/minecraft/tileentity/TileEntityShulkerBox$AnimationStatus;"),
            require = 0, allow = 1)
    private TileEntityShulkerBox.AnimationStatus fp2_getBlockFaceShape_ensureTileEntityNonnull(TileEntityShulkerBox tileEntityShulkerBox) {
        return tileEntityShulkerBox != null ? tileEntityShulkerBox.getAnimationStatus() : TileEntityShulkerBox.AnimationStatus.CLOSED;
    }
}
