/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class Util1_12_2 {
    /**
     * A thread-local {@link Recycler} for {@link BlockPos.MutableBlockPos} instances.
     * <p>
     * This exists because {@link BlockPos.PooledMutableBlockPos} uses a single, global pool whose access is synchronized, making it rather poorly suited for
     * parallel workloads.
     */
    public static final Cached<Recycler<BlockPos.MutableBlockPos>> MUTABLEBLOCKPOS_RECYCLER = Cached.threadLocal(
            () -> Recycler.bounded(BlockPos.MutableBlockPos::new, 32),
            ReferenceStrength.WEAK);

    private final EnumFacing[] DIRECTION_TO_FACING = {
            EnumFacing.EAST, //POSITIVE_X
            EnumFacing.UP, //POSITIVE_Y
            EnumFacing.SOUTH, //POSITIVE_Z
            EnumFacing.WEST, //NEGATIVE_X
            EnumFacing.DOWN, //NEGATIVE_Y
            EnumFacing.NORTH, //NEGATIVE_Z
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
     * Converts the given {@link Direction} to the equivalent {@link EnumFacing}.
     *
     * @param direction the {@link Direction}
     * @return the {@link EnumFacing}
     */
    public EnumFacing directionToFacing(@NonNull Direction direction) {
        return DIRECTION_TO_FACING[direction.ordinal()];
    }

    /**
     * Converts the given {@link EnumFacing} to the equivalent {@link Direction}.
     *
     * @param facing the {@link EnumFacing}
     * @return the {@link Direction}
     */
    public Direction facingToDirection(@NonNull EnumFacing facing) {
        return FACING_TO_DIRECTION[facing.ordinal()];
    }
}
