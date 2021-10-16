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

package net.daporkchop.fp2.mode.heightmap.server.tracking;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.common.server.tracking.AbstractTracker;
import net.daporkchop.fp2.mode.common.server.tracking.AbstractTrackerManager;
import net.daporkchop.fp2.mode.common.server.tracking.TrackingState;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.util.math.IntAxisAlignedBB;

import java.util.Comparator;
import java.util.function.Consumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapTracker extends AbstractTracker<HeightmapPos, HeightmapTile, TrackingState> {
    protected static boolean overlaps(int x0, int z0, int x1, int z1, int radius) {
        int dx = abs(x0 - x1);
        int dz = abs(z0 - z1);
        return dx <= radius && dz <= radius;
    }

    public HeightmapTracker(@NonNull AbstractTrackerManager<HeightmapPos, HeightmapTile> manager, @NonNull IFarServerContext<HeightmapPos, HeightmapTile> context) {
        super(manager, context);
    }

    @Override
    protected TrackingState currentState(@NonNull IFarServerContext<HeightmapPos, HeightmapTile> context) {
        return TrackingState.createDefault(context);
    }

    @Override
    protected boolean shouldTriggerUpdate(@NonNull TrackingState oldState, @NonNull TrackingState newState) {
        return oldState.cutoff() != newState.cutoff()
               || oldState.minLevel() != newState.minLevel()
               || oldState.maxLevel() != newState.maxLevel()
               || sq(oldState.x() - newState.x()) + sq(oldState.z() - newState.z()) >= UPDATE_TRIGGER_DISTANCE_SQUARED;
    }

    @Override
    protected void allPositions(@NonNull TrackingState state, @NonNull Consumer<HeightmapPos> callback) {
        final int playerX = floorI(state.x());
        final int playerZ = floorI(state.z());

        for (int lvl = state.minLevel(); lvl < state.maxLevel(); lvl++) {
            final int baseX = asrRound(playerX, T_SHIFT + lvl);
            final int baseZ = asrRound(playerZ, T_SHIFT + lvl);

            IntAxisAlignedBB limits = this.coordLimits[lvl];
            int minX = max(baseX - state.cutoff(), limits.minX());
            int minZ = max(baseZ - state.cutoff(), limits.minZ());
            int maxX = min(baseX + state.cutoff(), limits.maxX());
            int maxZ = min(baseZ + state.cutoff(), limits.maxZ());

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    callback.accept(new HeightmapPos(lvl, x, z));
                }
            }
        }
    }

    @Override
    protected void deltaPositions(@NonNull TrackingState oldState, @NonNull TrackingState newState, @NonNull Consumer<HeightmapPos> added, @NonNull Consumer<HeightmapPos> removed) {
        final int oldPlayerX = floorI(oldState.x());
        final int oldPlayerZ = floorI(oldState.z());
        final int newPlayerX = floorI(newState.x());
        final int newPlayerZ = floorI(newState.z());

        for (int lvl = min(oldState.minLevel(), newState.minLevel()); lvl < max(oldState.maxLevel(), newState.maxLevel()); lvl++) {
            final int oldBaseX = asrRound(oldPlayerX, T_SHIFT + lvl);
            final int oldBaseZ = asrRound(oldPlayerZ, T_SHIFT + lvl);
            final int newBaseX = asrRound(newPlayerX, T_SHIFT + lvl);
            final int newBaseZ = asrRound(newPlayerZ, T_SHIFT + lvl);

            if (oldState.hasLevel(lvl) && newState.hasLevel(lvl) && oldState.cutoff() == newState.cutoff()
                && oldBaseX == newBaseX && oldBaseZ == newBaseZ) { //nothing changed, skip this level
                continue;
            }

            IntAxisAlignedBB limits = this.coordLimits[lvl];

            //removed positions
            if (!newState.hasLevel(lvl) || oldState.hasLevel(lvl)) {
                int minX = max(oldBaseX - oldState.cutoff(), limits.minX());
                int minZ = max(oldBaseZ - oldState.cutoff(), limits.minZ());
                int maxX = min(oldBaseX + oldState.cutoff(), limits.maxX());
                int maxZ = min(oldBaseZ + oldState.cutoff(), limits.maxZ());

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (!newState.hasLevel(lvl) || !overlaps(x, z, newBaseX, newBaseZ, newState.cutoff())) {
                            removed.accept(new HeightmapPos(lvl, x, z));
                        }
                    }
                }
            }

            //added positions
            if (!oldState.hasLevel(lvl) || newState.hasLevel(lvl)) {
                int minX = max(newBaseX - newState.cutoff(), limits.minX());
                int minZ = max(newBaseZ - newState.cutoff(), limits.minZ());
                int maxX = min(newBaseX + newState.cutoff(), limits.maxX());
                int maxZ = min(newBaseZ + newState.cutoff(), limits.maxZ());

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (!oldState.hasLevel(lvl) || !overlaps(x, z, oldBaseX, oldBaseZ, oldState.cutoff())) {
                            added.accept(new HeightmapPos(lvl, x, z));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean isVisible(@NonNull TrackingState state, @NonNull HeightmapPos pos) {
        return state.hasLevel(pos.level())
               && this.coordLimits[pos.level()].contains2d(pos.x(), pos.z())
               && abs(pos.x() - asrRound(floorI(state.x()), T_SHIFT + pos.level())) <= state.cutoff()
               && abs(pos.z() - asrRound(floorI(state.z()), T_SHIFT + pos.level())) <= state.cutoff();
    }

    @Override
    protected Comparator<HeightmapPos> comparatorFor(@NonNull TrackingState state) {
        class HeightmapPosAndComparator extends HeightmapPos implements Comparator<HeightmapPos> {
            public HeightmapPosAndComparator(int level, int x, int z) {
                super(level, x, z);
            }

            @Override
            public int compare(HeightmapPos o1, HeightmapPos o2) {
                int d;
                if ((d = o1.level() - o2.level()) != 0) {
                    return d;
                }
                return Integer.compare(this.manhattanDistance(o1), this.manhattanDistance(o2));
            }
        }

        return new HeightmapPosAndComparator(0, asrRound(floorI(state.x()), T_SHIFT), asrRound(floorI(state.z()), T_SHIFT));
    }
}
