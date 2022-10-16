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

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.util.Direction;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.util.math.BlockPos;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class Util1_16 {
    /**
     * A thread-local {@link Recycler} for {@link BlockPos.Mutable} instances.
     */
    public static final Cached<Recycler<BlockPos.Mutable>> MUTABLEBLOCKPOS_RECYCLER = Cached.threadLocal(
            () -> Recycler.bounded(BlockPos.Mutable::new, 32),
            ReferenceStrength.WEAK);

    private final net.minecraft.util.Direction[] DIRECTION_TO_FACING = {
            net.minecraft.util.Direction.EAST, //POSITIVE_X
            net.minecraft.util.Direction.UP, //POSITIVE_Y
            net.minecraft.util.Direction.SOUTH, //POSITIVE_Z
            net.minecraft.util.Direction.WEST, //NEGATIVE_X
            net.minecraft.util.Direction.DOWN, //NEGATIVE_Y
            net.minecraft.util.Direction.NORTH, //NEGATIVE_Z
    };

    private final Direction[] FACING_TO_DIRECTION = {
            Direction.NEGATIVE_Y, //DOWN
            Direction.POSITIVE_Y, //UP
            Direction.NEGATIVE_Z, //NORTH
            Direction.POSITIVE_Z, //SOUTH
            Direction.NEGATIVE_X, //WEST
            Direction.POSITIVE_X, //EAST
    };

    /**
     * Converts the given {@link Direction} to the equivalent {@link net.minecraft.util.Direction}.
     *
     * @param direction the {@link Direction}
     * @return the {@link net.minecraft.util.Direction}
     */
    public net.minecraft.util.Direction directionToFacing(@NonNull Direction direction) {
        return DIRECTION_TO_FACING[direction.ordinal()];
    }

    /**
     * Converts the given {@link net.minecraft.util.Direction} to the equivalent {@link Direction}.
     *
     * @param facing the {@link net.minecraft.util.Direction}
     * @return the {@link Direction}
     */
    public Direction facingToDirection(@NonNull net.minecraft.util.Direction facing) {
        return FACING_TO_DIRECTION[facing.ordinal()];
    }
}
