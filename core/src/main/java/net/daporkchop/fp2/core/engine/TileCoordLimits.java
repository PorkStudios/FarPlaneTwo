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
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class TileCoordLimits implements IFarCoordLimits {
    protected final int minX;
    protected final int minY;
    protected final int minZ;
    protected final int maxX;
    protected final int maxY;
    protected final int maxZ;

    @Override
    public boolean contains(@NonNull TilePos pos) {
        int shift = T_SHIFT + pos.level();

        return pos.x() >= asrFloor(this.minX, shift) && pos.x() < asrCeil(this.maxX, shift)
               && pos.y() >= asrFloor(this.minY, shift) && pos.y() <= asrCeil(this.maxY, shift)
               && pos.z() >= asrFloor(this.minZ, shift) && pos.z() < asrCeil(this.maxZ, shift);
    }

    @Override
    public TilePos min(int level) {
        return new TilePos(level,
                asrFloor(this.minX, T_SHIFT + level),
                asrFloor(this.minY, T_SHIFT + level),
                asrFloor(this.minZ, T_SHIFT + level));
    }

    @Override
    public TilePos max(int level) {
        return new TilePos(level,
                asrCeil(this.maxX, T_SHIFT + level),
                asrCeil(this.maxY, T_SHIFT + level) + 1, //TODO: remove + 1 on Y once voxel mode actually renders voxels
                asrCeil(this.maxZ, T_SHIFT + level));
    }
}
