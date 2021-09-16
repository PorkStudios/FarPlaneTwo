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

package net.daporkchop.fp2.mode.voxel.server;

import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.common.server.AbstractPlayerTracker;
import net.daporkchop.fp2.mode.common.server.TrackingState;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.daporkchop.fp2.util.math.IntAxisAlignedBB;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Comparator;
import java.util.function.Consumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class VoxelPlayerTracker extends AbstractPlayerTracker<VoxelPos, VoxelTile, TrackingState> {
    protected static boolean overlaps(int x0, int y0, int z0, int x1, int y1, int z1, int radius) {
        int dx = abs(x0 - x1);
        int dy = abs(y0 - y1);
        int dz = abs(z0 - z1);
        return dx <= radius && dy <= radius && dz <= radius;
    }

    public VoxelPlayerTracker(@NonNull VoxelWorld world) {
        super(world);
    }

    @Override
    protected TrackingState currentStateFor(@NonNull EntityPlayerMP player) {
        return TrackingState.createDefault(player);
    }

    @Override
    protected void allPositions(@NonNull EntityPlayerMP player, @NonNull TrackingState state, @NonNull Consumer<VoxelPos> callback) {
        final int playerX = floorI(state.x());
        final int playerY = floorI(state.y());
        final int playerZ = floorI(state.z());

        for (int lvl = state.minLevel(); lvl < state.maxLevel(); lvl++) {
            final int baseX = asrRound(playerX, T_SHIFT + lvl);
            final int baseY = asrRound(playerY, T_SHIFT + lvl);
            final int baseZ = asrRound(playerZ, T_SHIFT + lvl);

            IntAxisAlignedBB limits = this.coordLimits[lvl];
            int minX = limits.clampX(baseX - state.cutoff());
            int minY = limits.clampY(baseY - state.cutoff());
            int minZ = limits.clampZ(baseZ - state.cutoff());
            int maxX = limits.clampX(baseX + state.cutoff());
            int maxY = limits.clampY(baseY + state.cutoff());
            int maxZ = limits.clampZ(baseZ + state.cutoff());

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        callback.accept(new VoxelPos(lvl, x, y, z));
                    }
                }
            }
        }
    }

    @Override
    protected void deltaPositions(@NonNull EntityPlayerMP player, @NonNull TrackingState oldState, @NonNull TrackingState newState, @NonNull Consumer<VoxelPos> added, @NonNull Consumer<VoxelPos> removed) {
        final int oldPlayerX = floorI(oldState.x());
        final int oldPlayerY = floorI(oldState.y());
        final int oldPlayerZ = floorI(oldState.z());
        final int newPlayerX = floorI(newState.x());
        final int newPlayerY = floorI(newState.y());
        final int newPlayerZ = floorI(newState.z());

        for (int lvl = min(oldState.minLevel(), newState.minLevel()); lvl < max(oldState.maxLevel(), newState.maxLevel()); lvl++) {
            final int oldBaseX = asrRound(oldPlayerX, T_SHIFT + lvl);
            final int oldBaseY = asrRound(oldPlayerY, T_SHIFT + lvl);
            final int oldBaseZ = asrRound(oldPlayerZ, T_SHIFT + lvl);
            final int newBaseX = asrRound(newPlayerX, T_SHIFT + lvl);
            final int newBaseY = asrRound(newPlayerY, T_SHIFT + lvl);
            final int newBaseZ = asrRound(newPlayerZ, T_SHIFT + lvl);

            if (oldState.hasLevel(lvl) && newState.hasLevel(lvl) && oldState.cutoff() == newState.cutoff()
                && oldBaseX == newBaseX && oldBaseY == newBaseY && oldBaseZ == newBaseZ) { //nothing changed, skip this level
                continue;
            }

            IntAxisAlignedBB limits = this.coordLimits[lvl];

            //removed positions
            if (!newState.hasLevel(lvl) || oldState.hasLevel(lvl)) {
                int minX = limits.clampX(oldBaseX - oldState.cutoff());
                int minY = limits.clampY(oldBaseY - oldState.cutoff());
                int minZ = limits.clampZ(oldBaseZ - oldState.cutoff());
                int maxX = limits.clampX(oldBaseX + oldState.cutoff());
                int maxY = limits.clampY(oldBaseY + oldState.cutoff());
                int maxZ = limits.clampZ(oldBaseZ + oldState.cutoff());

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (!newState.hasLevel(lvl) || !overlaps(x, y, z, newBaseX, newBaseY, newBaseZ, newState.cutoff())) {
                                removed.accept(new VoxelPos(lvl, x, y, z));
                            }
                        }
                    }
                }
            }

            //added positions
            if (!oldState.hasLevel(lvl) || newState.hasLevel(lvl)) {
                int minX = limits.clampX(newBaseX - newState.cutoff());
                int minY = limits.clampY(newBaseY - newState.cutoff());
                int minZ = limits.clampZ(newBaseZ - newState.cutoff());
                int maxX = limits.clampX(newBaseX + newState.cutoff());
                int maxY = limits.clampY(newBaseY + newState.cutoff());
                int maxZ = limits.clampZ(newBaseZ + newState.cutoff());

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (!oldState.hasLevel(lvl) || !overlaps(x, y, z, oldBaseX, oldBaseY, oldBaseZ, oldState.cutoff())) {
                                added.accept(new VoxelPos(lvl, x, y, z));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean isVisible(@NonNull EntityPlayerMP player, @NonNull TrackingState state, @NonNull VoxelPos pos) {
        return state.hasLevel(pos.level())
               && this.coordLimits[pos.level()].contains(pos.x(), pos.y(), pos.z())
               && abs(pos.x() - asrRound(floorI(state.x()), T_SHIFT + pos.level())) <= state.cutoff()
               && abs(pos.y() - asrRound(floorI(state.y()), T_SHIFT + pos.level())) <= state.cutoff()
               && abs(pos.z() - asrRound(floorI(state.z()), T_SHIFT + pos.level())) <= state.cutoff();
    }

    @Override
    protected Comparator<VoxelPos> comparatorFor(@NonNull EntityPlayerMP player, @NonNull TrackingState state) {
        class VoxelPosAndComparator extends VoxelPos implements Comparator<VoxelPos> {
            public VoxelPosAndComparator(int level, int x, int y, int z) {
                super(level, x, y, z);
            }

            @Override
            public int compare(VoxelPos o1, VoxelPos o2) {
                int d;
                if ((d = Integer.compare(o1.level(), o2.level())) != 0) {
                    return d;
                }
                return Integer.compare(this.manhattanDistance(o1), this.manhattanDistance(o2));
            }
        }

        return new VoxelPosAndComparator(0, asrRound(floorI(state.x()), T_SHIFT), asrRound(floorI(state.y()), T_SHIFT), asrRound(floorI(state.z()), T_SHIFT));
    }

    @Override
    protected boolean shouldTriggerUpdate(@NonNull EntityPlayerMP player, @NonNull TrackingState oldState, @NonNull TrackingState newState) {
        return oldState.cutoff() != newState.cutoff()
               || oldState.minLevel() != newState.minLevel()
               || oldState.maxLevel() != newState.maxLevel()
               || sq(oldState.x() - newState.x()) + sq(oldState.y() - newState.y()) + sq(oldState.z() - newState.z()) >= UPDATE_TRIGGER_DISTANCE_SQUARED;
    }
}
