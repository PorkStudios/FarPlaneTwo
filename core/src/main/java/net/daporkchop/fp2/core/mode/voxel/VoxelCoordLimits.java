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

package net.daporkchop.fp2.core.mode.voxel;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.fp2.core.mode.voxel.VoxelConstants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class VoxelCoordLimits implements IFarCoordLimits<VoxelPos> {
    protected final int minX;
    protected final int minY;
    protected final int minZ;
    protected final int maxX;
    protected final int maxY;
    protected final int maxZ;

    @Override
    public boolean contains(@NonNull VoxelPos pos) {
        int shift = VT_SHIFT + pos.level();

        return pos.x() >= asrFloor(this.minX, shift) && pos.x() <= asrCeil(this.maxX, shift)
               && pos.y() >= asrFloor(this.minY, shift) && pos.y() <= asrCeil(this.maxY, shift)
               && pos.z() >= asrFloor(this.minZ, shift) && pos.z() <= asrCeil(this.maxZ, shift);
    }

    @Override
    public VoxelPos min(int level) {
        return new VoxelPos(level,
                asrFloor(this.minX, VT_SHIFT + level),
                asrFloor(this.minY, VT_SHIFT + level),
                asrFloor(this.minZ, VT_SHIFT + level));
    }

    @Override
    public VoxelPos max(int level) {
        return new VoxelPos(level,
                incrementExact(asrCeil(this.maxX, VT_SHIFT + level)),
                incrementExact(asrCeil(this.maxY, VT_SHIFT + level)),
                incrementExact(asrCeil(this.maxZ, VT_SHIFT + level)));
    }
}
