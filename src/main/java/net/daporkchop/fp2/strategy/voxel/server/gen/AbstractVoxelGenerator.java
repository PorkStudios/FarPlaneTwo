/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.voxel.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.server.AbstractFarGenerator;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.util.math.qef.QefSolver;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractVoxelGenerator extends AbstractFarGenerator {
    public static final int DMAP_MIN = -1;
    public static final int DMAP_MAX = T_VOXELS + 2;
    public static final int DMAP_SIZE = DMAP_MAX - DMAP_MIN;

    protected void buildMesh(int baseX, int baseY, int baseZ, int level, @NonNull VoxelPiece piece, double[] densityMap) {
        QefSolver qef = new QefSolver();

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    //check for intersection data for each corner
                    int corners = 0;
                    for (int i = 0; i < 8; i++) {
                        int offsetSubX = dx + ((i >> 2) & 1);
                        int offsetSubY = dy + ((i >> 1) & 1);
                        int offsetSubZ = dz + (i & 1);
                        double density = densityMap[((offsetSubX - DMAP_MIN) * DMAP_SIZE + offsetSubY - DMAP_MIN) * DMAP_SIZE + offsetSubZ - DMAP_MIN];
                        if (density < 0.0d) {
                            corners |= 1 << i;
                        }
                    }

                    int blockX = baseX + (dx << level);
                    int blockY = baseY + (dy << level);
                    int blockZ = baseZ + (dz << level);
                }
            }
        }
    }
}
