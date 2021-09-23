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

package net.daporkchop.fp2.mode.common.server;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import net.daporkchop.fp2.config.FP2ConfigOld;
import net.minecraft.entity.player.EntityPlayerMP;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * Immutable container with the information required for an {@link AbstractPlayerTracker} implementation to know which tiles to load for a given player.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class TrackingState {
    public static TrackingState createDefault(@NonNull EntityPlayerMP player) {
        return new TrackingState(player.posX, player.posY, player.posZ,
                asrRound(FP2ConfigOld.levelCutoffDistance, T_SHIFT),
                FP2_DEBUG && FP2ConfigOld.debug.skipLevel0 ? 1 : 0,
                FP2ConfigOld.maxLevels);
    }

    protected final double x;
    protected final double y;
    protected final double z;

    protected final int cutoff;

    protected final int minLevel;
    protected final int maxLevel;

    /**
     * Checks whether or not this state tracks the given level.
     *
     * @param level the level to check
     * @return whether or not this state tracks the given level
     */
    public boolean hasLevel(int level) {
        return level >= this.minLevel && level < this.maxLevel;
    }
}
