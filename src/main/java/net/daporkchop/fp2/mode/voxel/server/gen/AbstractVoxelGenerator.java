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

package net.daporkchop.fp2.mode.voxel.server.gen;

import net.daporkchop.fp2.mode.common.server.AbstractFarGenerator;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPieceBuilder;
import net.daporkchop.fp2.util.math.qef.QefSolver;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;

import static java.lang.Math.*;
import static net.daporkchop.fp2.mode.voxel.server.gen.VoxelGeneratorConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractVoxelGenerator<P> extends AbstractFarGenerator {
    public static final int DMAP_MIN = -1;
    public static final int DMAP_SIZE = T_VOXELS + 2 - DMAP_MIN;

    protected static final Ref<double[]> DMAP_CACHE = ThreadRef.soft(() -> new double[DMAP_SIZE * DMAP_SIZE * DMAP_SIZE]);

    protected static int densityIndex(int x, int y, int z) {
        return ((x - DMAP_MIN) * DMAP_SIZE + y - DMAP_MIN) * DMAP_SIZE + z - DMAP_MIN;
    }

    protected static double sampleDensity(double x, double y, double z, double[] densityMap) {
        int xI = floorI(x);
        int yI = floorI(y);
        int zI = floorI(z);

        x -= xI;
        y -= yI;
        z -= zI;

        double xyz = densityMap[densityIndex(xI, yI, zI)];
        double xyZ = densityMap[densityIndex(xI, yI, zI + 1)];
        double xYz = densityMap[densityIndex(xI, yI + 1, zI)];
        double xYZ = densityMap[densityIndex(xI, yI + 1, zI + 1)];
        double Xyz = densityMap[densityIndex(xI + 1, yI, zI)];
        double XyZ = densityMap[densityIndex(xI + 1, yI, zI + 1)];
        double XYz = densityMap[densityIndex(xI + 1, yI + 1, zI)];
        double XYZ = densityMap[densityIndex(xI + 1, yI + 1, zI + 1)];

        return lerp(
                lerp(lerp(xyz, xyZ, z), lerp(xYz, xYZ, z), y),
                lerp(lerp(Xyz, XyZ, z), lerp(XYz, XYZ, z), y), x);
    }

    protected void buildMesh(int baseX, int baseY, int baseZ, int level, VoxelPieceBuilder builder, double[] densityMap, P param) {
        QefSolver qef = new QefSolver();
        VoxelData data = new VoxelData();

        double dn = 0.001d * (1 << level);

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    //check for intersection data for each corner
                    int corners = 0;
                    for (int i = 0; i < 8; i++) {
                        double offsetSubX = dx + ((i >> 2) & 1);
                        double offsetSubY = dy + ((i >> 1) & 1);
                        double offsetSubZ = dz + (i & 1);
                        double density = sampleDensity(offsetSubX, offsetSubY, offsetSubZ, densityMap);
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

                        double offsetSubX0 = dx + ((c0 >> 2) & 1);
                        double offsetSubY0 = dy + ((c0 >> 1) & 1);
                        double offsetSubZ0 = dz + (c0 & 1);
                        double offsetSubX1 = dx + ((c1 >> 2) & 1);
                        double offsetSubY1 = dy + ((c1 >> 1) & 1);
                        double offsetSubZ1 = dz + (c1 & 1);

                        double density0 = sampleDensity(offsetSubX0, offsetSubY0, offsetSubZ0, densityMap);
                        double density1 = sampleDensity(offsetSubX1, offsetSubY1, offsetSubZ1, densityMap);

                        double t = minimize(density0, density1);
                        double px = lerp((c0 >> 2) & 1, (c1 >> 2) & 1, t);
                        double py = lerp((c0 >> 1) & 1, (c1 >> 1) & 1, t);
                        double pz = lerp(c0 & 1, c1 & 1, t);

                        double nx = sampleDensity(dx + px + dn, dy + py, dz + pz, densityMap) - sampleDensity(dx + px - dn, dy + py, dz + pz, densityMap);
                        double ny = sampleDensity(dx + px, dy + py + dn, dz + pz, densityMap) - sampleDensity(dx + px, dy + py - dn, dz + pz, densityMap);
                        double nz = sampleDensity(dx + px, dy + py, dz + pz + dn, densityMap) - sampleDensity(dx + px, dy + py, dz + pz - dn, densityMap);

                        qef.add(px, py, pz, nx, ny, nz);
                        edgeCount++;
                        edgeMask |= 1 << i;
                    }

                    //solve QEF and set the piece data
                    qef.solve(data.reset(), 0.1, 1, 0.5);

                    if (data.x < 0.0d || data.x > 1.0d
                        || data.y < 0.0d || data.y > 1.0d
                        || data.z < 0.0d || data.z > 1.0d) { //ensure that all points are within voxel bounds
                        data.set(qef.massPoint().x, qef.massPoint().y, qef.massPoint().z);
                    }
                    qef.reset();

                    data.edges = edgeMask;

                    double nx = sampleDensity(dx + data.x + dn, dy + data.y, dz + data.z, densityMap) - sampleDensity(dx + data.x - dn, dy + data.y, dz + data.z, densityMap);
                    double ny = sampleDensity(dx + data.x, dy + data.y + dn, dz + data.z, densityMap) - sampleDensity(dx + data.x, dy + data.y - dn, dz + data.z, densityMap);
                    double nz = sampleDensity(dx + data.x, dy + data.y, dz + data.z + dn, densityMap) - sampleDensity(dx + data.x, dy + data.y, dz + data.z - dn, densityMap);
                    double nLen = sqrt(nx * nx + ny * ny + nz * nz);
                    nx /= nLen;
                    ny /= nLen;
                    nz /= nLen;

                    this.populateVoxelBlockData(baseX + (dx << level), baseY + (dy << level), baseZ + (dz << level), data, param, nx, ny, nz);

                    builder.set(dx, dy, dz, data);
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
     * @param nx
     * @param ny
     * @param nz
     */
    protected abstract void populateVoxelBlockData(int blockX, int blockY, int blockZ, VoxelData data, P param, double nx, double ny, double nz);
}
