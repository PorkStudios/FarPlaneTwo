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
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.common.server.AbstractPlayerTracker;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Arrays;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class VoxelPlayerTracker extends AbstractPlayerTracker<VoxelPos> {
    public VoxelPlayerTracker(@NonNull VoxelWorld world) {
        super(world);
    }

    @Override
    protected Stream<VoxelPos> getPositions(@NonNull EntityPlayerMP player) {
        final int dist = FP2Config.renderDistance >> T_SHIFT; //TODO: make it based on render distance
        final int baseX = floorI(player.posX) >> T_SHIFT;
        final int baseY = floorI(player.posY) >> T_SHIFT;
        final int baseZ = floorI(player.posZ) >> T_SHIFT;

        final int levels = FP2Config.maxLevels;
        final int d = (FP2Config.levelCutoffDistance >> T_SHIFT) + 1;

        VoxelPos[] positions = new VoxelPos[pow(d * 2 + 1, 3) * levels];
        int i = 0;

        for (int lvl = 0; lvl < levels; lvl++) {
            int xMin = ((baseX >> lvl) - d);
            int xMax = ((baseX >> lvl) + d);
            int yMin = ((baseY >> lvl) - d);
            int yMax = ((baseY >> lvl) + d);
            int zMin = ((baseZ >> lvl) - d);
            int zMax = ((baseZ >> lvl) + d);

            for (int x = xMin; x <= xMax; x++) {
                for (int y = yMin; y <= yMax; y++)  {
                    for (int z = zMin; z <= zMax; z++) {
                        positions[i++] = new VoxelPos(x, y, z, lvl);
                    }
                }
            }
        }

        return Arrays.stream(positions, 0, i);
    }
}
