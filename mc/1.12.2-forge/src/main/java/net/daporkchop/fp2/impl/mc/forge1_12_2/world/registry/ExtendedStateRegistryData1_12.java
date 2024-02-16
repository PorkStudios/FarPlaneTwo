/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry;

import lombok.NonNull;
import net.daporkchop.fp2.core.world.registry.AbstractDenseExtendedStateRegistryData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.MathHelper;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;

/**
 * @author DaPorkchop_
 */
public final class ExtendedStateRegistryData1_12 extends AbstractDenseExtendedStateRegistryData<IBlockState> {
    public ExtendedStateRegistryData1_12(@NonNull GameRegistry1_12 registry) {
        super(registry);
    }

    @Override
    protected int type(int id, IBlockState state) {
        if (state.isOpaqueCube()) {
            return BLOCK_TYPE_OPAQUE;
        } else if (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) {
            return BLOCK_TYPE_TRANSPARENT;
        } else {
            return BLOCK_TYPE_INVISIBLE;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected int lightOpacity(int id, IBlockState state) {
        return MathHelper.clamp(state.getLightOpacity(), 0, 15);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected int lightEmission(int id, IBlockState state) {
        return MathHelper.clamp(state.getLightValue(), 0, 15);
    }
}
