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

package net.daporkchop.fp2.core.engine;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class TileCoordLimits {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public TileCoordLimits(@NonNull IntAxisAlignedBB blockCoordLimits) {
        this(blockCoordLimits.minX(), blockCoordLimits.minY(), blockCoordLimits.minZ(), blockCoordLimits.maxX(), blockCoordLimits.maxY(), blockCoordLimits.maxZ());
    }

    /**
     * Checks whether or not the given position is within these limits.
     *
     * @param pos the position to check
     * @return whether or not the given position is within these limits
     */
    public boolean contains(@NonNull TilePos pos) {
        int shift = T_SHIFT + pos.level();

        return pos.x() >= asrFloor(this.minX, shift) && pos.x() < asrCeil(this.maxX, shift)
               && pos.y() >= asrFloor(this.minY, shift) && pos.y() <= asrCeil(this.maxY, shift)
               && pos.z() >= asrFloor(this.minZ, shift) && pos.z() < asrCeil(this.maxZ, shift);
    }

    /**
     * Gets the position at the minimum corner of the limit's AABB at the given level.
     * <p>
     * The position's coordinates are inclusive.
     *
     * @param level the level
     * @return the position at the minimum corner of the limit's AABB at the given level
     */
    public TilePos min(int level) {
        return new TilePos(level,
                asrFloor(this.minX, T_SHIFT + level),
                asrFloor(this.minY, T_SHIFT + level),
                asrFloor(this.minZ, T_SHIFT + level));
    }

    /**
     * Gets the position at the minimum corner of the limit's AABB at the given level.
     * <p>
     * The position's coordinates are exclusive.
     *
     * @param level the level
     * @return the position at the minimum corner of the limit's AABB at the given level
     */
    public TilePos max(int level) {
        return new TilePos(level,
                asrCeil(this.maxX, T_SHIFT + level),
                asrCeil(this.maxY, T_SHIFT + level) + 1, //TODO: remove + 1 on Y once voxel mode actually renders voxels
                asrCeil(this.maxZ, T_SHIFT + level));
    }
}
