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

package net.daporkchop.fp2.core.world.level.block;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevelDataAvailability;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.server.world.FBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractFBlockLevelBase implements FBlockLevelDataAvailability {
    @NonNull
    private final FGameRegistry registry;
    @NonNull
    private final IntAxisAlignedBB dataLimits;

    //
    // Generic coordinate utilities
    //

    /**
     * Checks whether the given Y coordinate is within the world's vertical limits.
     *
     * @param y the Y coordinate to check
     * @return whether the Y coordinate is valid
     */
    public boolean isValidY(int y) {
        return this.dataLimits.containsY(y);
    }

    /**
     * Checks whether the given X,Z coordinates are within the world's horizontal limits.
     *
     * @param x the X coordinate of the position to check
     * @param z the Z coordinate of the position to check
     * @return whether the X,Z coordinates are valid
     */
    public boolean isValidXZ(int x, int z) {
        return this.dataLimits.containsX(x) && this.dataLimits.containsZ(z);
    }

    /**
     * Checks whether the given point is within the world's limits.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @param z the point's Z coordinate
     * @return whether the given point is valid
     */
    public boolean isValidPosition(int x, int y, int z) {
        return this.dataLimits.contains(x, y, z);
    }
}
