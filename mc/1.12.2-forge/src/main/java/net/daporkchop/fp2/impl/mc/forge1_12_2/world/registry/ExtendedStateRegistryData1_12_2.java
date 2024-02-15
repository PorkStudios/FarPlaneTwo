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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.minecraft.block.state.IBlockState;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class ExtendedStateRegistryData1_12_2 implements FExtendedStateRegistryData {
    private static int type(IBlockState state) {
        if (state.isOpaqueCube()) {
            return BLOCK_TYPE_OPAQUE;
        } else if (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) {
            return BLOCK_TYPE_TRANSPARENT;
        } else {
            return BLOCK_TYPE_INVISIBLE;
        }
    }

    private final GameRegistry1_12_2 registry;

    private final byte[] types;
    private final byte[] lightEmissions;
    private final byte[] lightOpacities;

    public ExtendedStateRegistryData1_12_2(@NonNull GameRegistry1_12_2 registry) {
        this.registry = registry;

        int maxStateId = registry.states().max().getAsInt();
        this.types = new byte[maxStateId + 1];
        this.lightEmissions = new byte[maxStateId + 1];
        this.lightOpacities = new byte[maxStateId + 1];
        registry.states().forEach(state -> {
            IBlockState iBlockState = registry.id2state(state);
            this.types[state] = (byte) type(iBlockState);
            this.lightEmissions[state] = (byte) Math.max(Math.min(iBlockState.getLightValue(), 15), 0);
            this.lightOpacities[state] = (byte) Math.max(Math.min(iBlockState.getLightOpacity(), 15), 0);
        });
    }

    @Override
    public int type(int state) throws IndexOutOfBoundsException {
        return this.types[state] & 0xFF;
    }

    @Override
    public int lightEmission(int state) throws IndexOutOfBoundsException {
        return this.lightEmissions[state];
    }

    @Override
    public int lightOpacity(int state) throws IndexOutOfBoundsException {
        return this.lightOpacities[state];
    }
}
