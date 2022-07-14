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

package net.daporkchop.fp2.impl.mc.forge1_16.world.registry;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockReader;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class ExtendedStateRegistryData1_16 implements FExtendedStateRegistryData {
    private static int type(BlockState state) {
        if (state.isSolidRender(EmptyBlockReader.INSTANCE, BlockPos.ZERO)) { //should be the same as 1.12.2's isOpaqueCube()
            return BLOCK_TYPE_OPAQUE;
        } else if (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) {
            return BLOCK_TYPE_TRANSPARENT;
        } else {
            return BLOCK_TYPE_INVISIBLE;
        }
    }

    private final GameRegistry1_16 registry;

    private final byte[] types;

    public ExtendedStateRegistryData1_16(@NonNull GameRegistry1_16 registry) {
        this.registry = registry;

        this.types = new byte[registry.statesCount()];
        registry.states().forEach(state -> this.types[state] = (byte) type(registry.id2state(state)));
    }

    @Override
    public int type(int state) throws IndexOutOfBoundsException {
        return this.types[state] & 0xFF;
    }
}
