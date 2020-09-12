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

import net.daporkchop.fp2.strategy.base.server.AbstractFarGenerator;
import net.daporkchop.fp2.strategy.voxel.VoxelData;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.util.math.qef.QefSolver;

import static net.daporkchop.fp2.strategy.voxel.server.gen.VoxelGeneratorConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractVoxelGenerator<P> extends AbstractFarGenerator {
    public static final int DMAP_SIZE = T_VOXELS + 2;

    protected void buildMesh(int baseX, int baseY, int baseZ, int level, VoxelPiece piece, double[] densityMap, P param) {
        QefSolver qef = new QefSolver();
        VoxelData data = new VoxelData();

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    //check for intersection data for each corner
                    int corners = 0;
                    for (int i = 0; i < 8; i++) {
                        int offsetSubX = dx + ((i >> 2) & 1);
                        int offsetSubY = dy + ((i >> 1) & 1);
                        int offsetSubZ = dz + (i & 1);
                        double density = densityMap[(offsetSubX * DMAP_SIZE + offsetSubY) * DMAP_SIZE + offsetSubZ];
                        if (density < 0.0d) {
                            corners |= 1 << i;
                        }
                    }

                    if (corners == 0 || corners == 0xFF) { //if all corners are either solid or non-solid, this voxel can be safely skipped
                        continue;
                    }

                    //populate the QEF with data
                    int edgeCount = 0;
                    int edgeMask = 0;

                    for (int i = 0; i < 12 && edgeCount < MAX_EDGES; i++) {
                        int c0 = EDGEVMAP[i << 1];
                        int c1 = EDGEVMAP[(i << 1) | 1];

                        if (((corners >> c0) & 1) == ((corners >> c1) & 1)) { //both corners along the current edge are identical, this edge can be skipped
                            continue;
                        }

                        int offsetSubX0 = (dx + ((c0 >> 2) & 1)) << level;
                        int offsetSubY0 = (dy + ((c0 >> 1) & 1)) << level;
                        int offsetSubZ0 = (dz + (c0 & 1)) << level;
                        int offsetSubX1 = (dx + ((c1 >> 2) & 1)) << level;
                        int offsetSubY1 = (dy + ((c1 >> 1) & 1)) << level;
                        int offsetSubZ1 = (dz + (c1 & 1)) << level;

                        double density0 = densityMap[(offsetSubX0 * DMAP_SIZE + offsetSubY0) * DMAP_SIZE + offsetSubZ0];
                        double density1 = densityMap[(offsetSubX1 * DMAP_SIZE + offsetSubY1) * DMAP_SIZE + offsetSubZ1];

                        double t = minimize(density0, density1);
                        double px = lerp((c0 >> 2) & 1, (c1 >> 2) & 1, t);
                        double py = lerp((c0 >> 1) & 1, (c1 >> 1) & 1, t);
                        double pz = lerp(c0 & 1, c1 & 1, t);

                        int offsetSubXP = t < 0.5d ? offsetSubX0 : offsetSubX1;
                        int offsetSubYP = t < 0.5d ? offsetSubY0 : offsetSubY1;
                        int offsetSubZP = t < 0.5d ? offsetSubZ0 : offsetSubZ1;

                        double pDensity = densityMap[((offsetSubXP) * DMAP_SIZE + offsetSubYP) * DMAP_SIZE + offsetSubZP];

                        double nx = densityMap[((offsetSubXP + 1) * DMAP_SIZE + offsetSubYP) * DMAP_SIZE + offsetSubZP] - pDensity;
                        double ny = densityMap[(offsetSubXP * DMAP_SIZE + offsetSubYP + 1) * DMAP_SIZE + offsetSubZP] - pDensity;
                        double nz = densityMap[(offsetSubXP * DMAP_SIZE + offsetSubYP) * DMAP_SIZE + offsetSubZP + 1] - pDensity;

                        qef.add(px, py, pz, nx, ny, nz);
                        edgeCount++;
                        edgeMask |= 1 << i;
                    }

                    //solve QEF and set the piece data
                    qef.solve(data.reset(), 0.0001f, 4, 0.0001f);

                    if (data.x < 0.0d || data.x > 1.0d
                        || data.y < 0.0d || data.y > 1.0d
                        || data.z < 0.0d || data.z > 1.0d) { //ensure that all points are within voxel bounds
                        data.set(qef.massPoint().x, qef.massPoint().y, qef.massPoint().z);
                    }
                    qef.reset();

                    data.edges = edgeMask;
                    this.populateVoxelBlockData(baseX + (dx << level), baseY + (dy << level), baseZ + (dz << level), data, param);

                    piece.set(dx, dy, dz, data);
                }
            }
        }
    }

    /**
     * Sets the block-specific voxel data for a voxel.
     *
     * @param blockX the voxel's X coordinate
     * @param blockY the voxel's Y coordinate
     * @param blockZ the voxel's Z coordinate
     * @param data   the {@link VoxelData} to store data into
     * @param param  an additional user parameter
     */
    protected abstract void populateVoxelBlockData(int blockX, int blockY, int blockZ, VoxelData data, P param);
}
