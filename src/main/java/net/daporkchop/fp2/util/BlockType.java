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

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class BlockType {
    public final int BLOCK_TYPE_AIR = 0;
    public final int BLOCK_TYPE_TRANSPARENT = 1;
    public final int BLOCK_TYPE_OPAQUE = 2;

    @SideOnly(Side.CLIENT)
    public final int RENDER_TYPE_OPAQUE = 0;
    @SideOnly(Side.CLIENT)
    public final int RENDER_TYPE_CUTOUT = 1;
    @SideOnly(Side.CLIENT)
    public final int RENDER_TYPE_TRANSLUCENT = 2;
    @SideOnly(Side.CLIENT)
    public final int RENDER_TYPES = 3;

    @SideOnly(Side.CLIENT)
    private final int[] RENDER_TYPE_LOOKUP = {
            RENDER_TYPE_OPAQUE, //SOLID
            RENDER_TYPE_CUTOUT, //CUTOUT_MIPPED,
            RENDER_TYPE_CUTOUT, //CUTOUT,
            RENDER_TYPE_TRANSLUCENT, //TRANSLUCENT
    };

    /**
     * Gets the given {@link IBlockState}'s block type.
     *
     * @param stateId the block state ID
     * @return the block type
     */
    public int blockType(int stateId) {
        return blockType(FastRegistry.getBlockState(stateId));
    }

    /**
     * Gets the given {@link IBlockState}'s block type.
     *
     * @param state the {@link IBlockState}
     * @return the block type
     */
    public int blockType(IBlockState state) {
        if (state.isOpaqueCube()) {
            return BLOCK_TYPE_OPAQUE;
        } else if (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) {
            return BLOCK_TYPE_TRANSPARENT;
        } else {
            return BLOCK_TYPE_AIR;
        }
    }

    /**
     * Gets the given {@link IBlockState}'s render type.
     *
     * @param state the {@link IBlockState}
     * @return the render type
     */
    @SideOnly(Side.CLIENT)
    public int renderType(IBlockState state) {
        //TODO: i need to do something about this: grass is rendered as CUTOUT_MIPPED, which makes it always render both faces
        /*if (state.getBlock() == Blocks.GRASS) {
            return RENDER_TYPE_OPAQUE;
        }*/
        return RENDER_TYPE_LOOKUP[state.getBlock().getRenderLayer().ordinal()];
    }
}
