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

package net.daporkchop.fp2.mode.voxel.server.scale;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.server.scale.IFarScaler;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPieceBuilder;
import net.minecraft.block.Block;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Stream;

import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
//TODO: figure out whether or not this is actually correct
public class VoxelScalerAvg implements IFarScaler<VoxelPos, VoxelPiece, VoxelPieceBuilder> {
    protected static void cross(double[] a, int aOff, double[] b, int bOff, double[] dst, int dstOff) {
        dst[dstOff + 0] = (a[aOff + 1] * b[bOff + 2] - a[aOff + 2] * b[bOff + 1]);
        dst[dstOff + 1] = (a[aOff + 2] * b[bOff + 0] - a[aOff + 0] * b[bOff + 2]);
        dst[dstOff + 2] = (a[aOff + 0] * b[bOff + 1] - a[aOff + 1] * b[bOff + 0]);
    }

    protected static void cross3(double[] a, int aOff, double[] b, int bOff, double[] c, int cOff, double[] dst, int dstOff) {
        double ax = a[aOff + 0] - b[bOff + 0];
        double ay = a[aOff + 1] - b[bOff + 1];
        double az = a[aOff + 2] - b[bOff + 2];
        double bx = a[aOff + 0] - c[cOff + 0];
        double by = a[aOff + 1] - c[cOff + 1];
        double bz = a[aOff + 2] - c[cOff + 2];
        dst[dstOff + 0] = (ay * bz - az * by);
        dst[dstOff + 1] = (az * bx - ax * bz);
        dst[dstOff + 2] = (ax * by - ay * bx);
    }

    protected static void crossAdd(double[] a, int aOff, double[] b, int bOff, double[] dst, int dstOff) {
        dst[dstOff + 0] += (a[aOff + 1] * b[bOff + 2] - a[aOff + 2] * b[bOff + 1]);
        dst[dstOff + 1] += (a[aOff + 2] * b[bOff + 0] - a[aOff + 0] * b[bOff + 2]);
        dst[dstOff + 2] += (a[aOff + 0] * b[bOff + 1] - a[aOff + 1] * b[bOff + 0]);
    }

    protected static void cross3Add(double[] a, int aOff, double[] b, int bOff, double[] c, int cOff, double[] dst, int dstOff) {
        double ax = a[aOff + 0] - b[bOff + 0];
        double ay = a[aOff + 1] - b[bOff + 1];
        double az = a[aOff + 2] - b[bOff + 2];
        double bx = a[aOff + 0] - c[cOff + 0];
        double by = a[aOff + 1] - c[cOff + 1];
        double bz = a[aOff + 2] - c[cOff + 2];
        dst[dstOff + 0] += (ay * bz - az * by);
        dst[dstOff + 1] += (az * bx - ax * bz);
        dst[dstOff + 2] += (ax * by - ay * bx);
    }

    protected static void cross3Sub(double[] a, int aOff, double[] b, int bOff, double[] c, int cOff, double[] dst, int dstOff) {
        double ax = a[aOff + 0] - b[bOff + 0];
        double ay = a[aOff + 1] - b[bOff + 1];
        double az = a[aOff + 2] - b[bOff + 2];
        double bx = a[aOff + 0] - c[cOff + 0];
        double by = a[aOff + 1] - c[cOff + 1];
        double bz = a[aOff + 2] - c[cOff + 2];
        dst[dstOff + 0] -= (ay * bz - az * by);
        dst[dstOff + 1] -= (az * bx - ax * bz);
        dst[dstOff + 2] -= (ax * by - ay * bx);
    }

    protected static int findMostCommon(int[] arr, int off, int len) {
        int maxCount = -1;
        int maxIndex = -1;
        for (int i = 0; i < len; i++) {
            int count = 0;
            int val = arr[off + i];
            if (val == 0) {
                continue;
            }
            for (int j = 0; j < len; j++) {
                if (arr[off + j] == val) {
                    count++;
                }
            }
            if (count > maxCount) {
                maxCount = count;
                maxIndex = i;
            }
        }
        return maxIndex < 0 ? 0 : arr[off + maxIndex];
    }

    @Override
    public Stream<VoxelPos> outputs(@NonNull VoxelPos srcPos) {
        return Stream.of(srcPos.up()); //TODO: fix this
    }

    @Override
    public Stream<VoxelPos> inputs(@NonNull VoxelPos dstPos) {
        checkArg(dstPos.level() > 0, "cannot generate inputs for level 0!");

        int x = dstPos.x() << 1;
        int y = dstPos.y() << 1;
        int z = dstPos.z() << 1;
        int level = dstPos.level() - 1;

        final int size = 3;
        VoxelPos[] positions = new VoxelPos[size * size * size];
        int i = 0;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    positions[i++] = new VoxelPos(x + dx, y + dy, z + dz, level);
                }
            }
        }
        return Arrays.stream(positions);
    }

    @Override
    public void scale(@NonNull VoxelPiece[] srcs, @NonNull VoxelPieceBuilder dst) {
        VoxelData[] datas = new VoxelData[27];
        for (int i = 0; i < datas.length; i++) {
            datas[i] = new VoxelData();
        }

        BitSet voxels = new BitSet(T_VERTS * T_VERTS * T_VERTS);
        double[] positions = new double[T_VERTS * T_VERTS * T_VERTS * 3];
        //double[] normals = new double[T_VOXELS * T_VOXELS * T_VOXELS * 3];
        for (int subX = 0; subX < 2; subX++) {
            for (int subY = 0; subY < 2; subY++) {
                for (int subZ = 0; subZ < 2; subZ++) {
                    VoxelPiece src = srcs[(subX * 3 + subY) * 3 + subZ];
                    if (src == null) {
                        continue;
                    }

                    int baseX = subX * (T_VOXELS >> 1);
                    int baseY = subY * (T_VOXELS >> 1);
                    int baseZ = subZ * (T_VOXELS >> 1);

                    for (int x = 0; x < T_VOXELS; x += 2) {
                        for (int y = 0; y < T_VOXELS; y += 2) {
                            for (int z = 0; z < T_VOXELS; z += 2) {
                                int i = ((baseX + (x >> 1)) * T_VERTS + (baseY + (y >> 1))) * T_VERTS + (baseZ + (z >> 1));
                                if (this.computePosition(src, x, y, z, positions, i * 3, datas[8])) {
                                    voxels.set(i);
                                    //this.computeAverageNormal(srcs, (subX << T_SHIFT) + x, (subY << T_SHIFT) + y, (subZ << T_SHIFT) + z, normals, (((baseX + (x >> 1)) * T_VOXELS + (baseY + (y >> 1))) * T_VOXELS + (baseZ + (z >> 1))) * 3);
                                }
                            }
                        }
                    }
                }
            }
        }

        BitSet existentEdges = new BitSet(T_VOXELS * T_VOXELS * T_VOXELS * 3);
        for (int subX = 0; subX < 2; subX++) {
            for (int subY = 0; subY < 2; subY++) {
                for (int subZ = 0; subZ < 2; subZ++) {
                    VoxelPiece src = srcs[(subX * 3 + subY) * 3 + subZ];
                    if (src == null) {
                        continue;
                    }

                    int baseX = subX * (T_VOXELS >> 1);
                    int baseY = subY * (T_VOXELS >> 1);
                    int baseZ = subZ * (T_VOXELS >> 1);

                    for (int x = 0; x < T_VOXELS; x += 2) {
                        for (int y = 0; y < T_VOXELS; y += 2) {
                            for (int z = 0; z < T_VOXELS; z += 2) {
                                int edgeMask = this.findExistentEdges(src, x, y, z, datas);
                                if (edgeMask != 0) {
                                    int i = ((baseX + (x >> 1)) * T_VOXELS + (baseY + (y >> 1))) * T_VOXELS + (baseZ + (z >> 1));
                                    for (int edge = 0; edge < EDGE_COUNT; edge++) {
                                        if ((edgeMask & (1 << edge)) != 0) {
                                            existentEdges.set(i * EDGE_COUNT + edge);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /*int[][] normals = new int[3][T_VOXELS * T_VOXELS * T_VOXELS * 3];
        for (int x = 0; x < T_VOXELS; x++) {
            for (int y = 0; y < T_VOXELS; y++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    int i = (x * T_VOXELS + y) * T_VOXELS + z;
                    for (int edge = 0; edge < EDGE_COUNT; edge++) {
                        this.computeNormal(edge, x, y, z, positions, normals[edge], i);
                    }
                }
            }
        }*/

        int[] states = new int[8 * 3];
        int[] stateCounts = new int[3];

        for (int x = 0; x < T_VOXELS; x++) {
            for (int y = 0; y < T_VOXELS; y++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    int i = (x * T_VERTS + y) * T_VERTS + z;
                    if (voxels.get(i)) {
                        Arrays.fill(datas[8].states, 0);
                        //this.computeAverageNormal(srcs, x,y, z, normals, 0);

                        int skyLight = 0;
                        int blockLight = 0;
                        int count = 0;

                        Arrays.fill(stateCounts, 0);
                        for (int dx = 0; dx < 2; dx++) {
                            for (int dy = 0; dy < 2; dy++) {
                                for (int dz = 0; dz < 2; dz++) {
                                    VoxelPiece piece = srcs[((x >> (T_SHIFT - 1)) * 3 + (y >> (T_SHIFT - 1))) * 3 + (z >> (T_SHIFT - 1))];
                                    if (piece.get(((x & 7) << 1) + dx, ((y & 7) << 1) + dy, ((z & 7) << 1) + dz, datas[0])) {
                                        skyLight += datas[0].light >> 4;
                                        blockLight += datas[0].light & 0xF;
                                        for (int edge = 0; edge < 3; edge++) {
                                            if (datas[0].states[edge] != 0) {
                                                states[edge * 8 + stateCounts[edge]++] = datas[0].states[edge];
                                            }
                                        }
                                        count++;
                                    }
                                }
                            }
                        }
                        if (count > 1) {
                            skyLight /= count;
                            blockLight /= count;
                        }

                        datas[8].light = (skyLight << 4) | blockLight;

                        int edges = 0;
                        for (int edge = 0; edge < EDGE_COUNT; edge++) {
                            if (existentEdges.get(((x * T_VOXELS + y) * T_VOXELS + z) * EDGE_COUNT + edge)) {
                                /*double normalAxisValue = normals[((x * T_VOXELS + y) * T_VOXELS + z) * EDGE_COUNT + (2 - edge)];
                                if (normalAxisValue != 0.0d) {
                                    edges |= (normalAxisValue < 0 ? EDGE_DIR_NEGATIVE : EDGE_DIR_POSITIVE) << (edge << 1);
                                }*/

                                //TODO: actually set the face direction correctly...
                                //this causes twice the number of faces to be generated
                                edges |= EDGE_DIR_BOTH << (edge << 1);

                                int firstState = findMostCommon(states, edge * 8, stateCounts[edge]);
                                int state = firstState;
                                while (voxelType(Block.getStateById(state)) != TYPE_OPAQUE){
                                    for (int k = 0; k < stateCounts[edge]; k++) {
                                        if (states[edge * 8 + k] == state) {
                                            states[edge * 8 + k] = 0;
                                        }
                                    }
                                    if ((state = findMostCommon(states, edge * 8, stateCounts[edge])) == 0) {
                                        break;
                                    }
                                }
                                datas[8].states[edge] = state == 0 ? firstState : state;
                            }
                        }
                        datas[8].edges = edges;

                        datas[8].x = floorI(positions[i * 3 + 0] * POS_ONE);
                        datas[8].y = floorI(positions[i * 3 + 1] * POS_ONE);
                        datas[8].z = floorI(positions[i * 3 + 2] * POS_ONE);
                        dst.set(x, y, z, datas[8]);
                    }
                }
            }
        }
    }

    protected boolean computePosition(VoxelPiece src, int srcX, int srcY, int srcZ, double[] dst, int dstOff, VoxelData data) {
        int validCount = 0;
        for (int i = 0; i < 8; i++) { //fetch data from all 8 contributing voxels
            if (src.getOnlyPos(srcX + ((i >> 2) & 1), srcY + ((i >> 1) & 1), srcZ + (i & 1), data)) {
                dst[dstOff + 0] += (data.x + ((i << (POS_FRACT_SHIFT - 2)) & POS_ONE)) / (double) POS_ONE;
                dst[dstOff + 1] += (data.y + ((i << (POS_FRACT_SHIFT - 1)) & POS_ONE)) / (double) POS_ONE;
                dst[dstOff + 2] += (data.z + ((i << (POS_FRACT_SHIFT - 0)) & POS_ONE)) / (double) POS_ONE;
                validCount++;
            }
        }

        if (validCount == 0) {
            return false;
        } else if (validCount > 1) {
            for (int i = 0; i < 3; i++) {
                dst[dstOff + i] /= validCount;
            }
        }
        return true;
    }

    protected void computeAverageNormal(VoxelPiece[] srcs, int x, int y, int z, double[] dst, int dstOff) {
        double[] p = new double[CONNECTION_INDEX_COUNT * 3];

        for (int i = 0; i < 8; i++) {
            int X = x + ((i >> 2) & 1);
            int Y = y + ((i >> 1) & 1);
            int Z = z + (i & 1);
            int edges = this.getPos(srcs, X, Y, Z, 0, p, 0);
            if (edges >= 0) {
                EDGE:
                for (int edge = 0; edge < EDGE_COUNT; edge++) {
                    for (int j = 0; j < CONNECTION_INDEX_COUNT; j++) {
                        if (this.getPos(srcs, X, Y, Z, CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + j], p, j * 3) < 0) {
                            continue EDGE;
                        }
                    }
                    switch (edges & (EDGE_DIR_MASK << (edge << 1))) {
                        case EDGE_DIR_POSITIVE:
                            cross3Add(p, 0, p, 1 * 3, p, 2 * 3, dst, dstOff);
                            cross3Add(p, 3 * 3, p, 2 * 3, p, 1 * 3, dst, dstOff);
                            break;
                        case EDGE_DIR_NEGATIVE:
                            cross3Sub(p, 0, p, 1 * 3, p, 2 * 3, dst, dstOff);
                            cross3Sub(p, 3 * 3, p, 2 * 3, p, 1 * 3, dst, dstOff);
                            break;
                    }
                }
            }
        }
    }

    protected int getPos(VoxelPiece[] srcs, int x, int y, int z, int connectionIndex, double[] dst, int dstOff) {
        int edges = this.getPos(srcs, x + ((connectionIndex >> 2) & 1), y + ((connectionIndex >> 1) & 1), z + (connectionIndex & 1), dst, dstOff);
        if (edges >= 0) {
            for (int i = 0; i < 3; i++) {
                dst[dstOff + i] += ((connectionIndex >> (2 - i)) & 1) << T_SHIFT;
            }
        }
        return edges;
    }

    protected int getPos(VoxelPiece[] srcs, int x, int y, int z, double[] dst, int dstOff) {
        VoxelPiece src = srcs[((x >> T_SHIFT) * 3 + (y >> T_SHIFT)) * 3 + (z >> T_SHIFT)];
        return src != null ? src.getOnlyPosAndReturnEdges(x & T_MASK, y & T_MASK, z & T_MASK, dst, dstOff) : -1;
    }

    protected int findExistentEdges(VoxelPiece src, int srcX, int srcY, int srcZ, VoxelData[] datas) {
        int validFlags = 0;
        int validCount = 0;
        for (int i = 0; i < 8; i++) { //fetch data from all 8 contributing voxels
            if (src.get(srcX + ((i >> 2) & 1), srcY + ((i >> 1) & 1), srcZ + (i & 1), datas[i])) {
                validFlags |= 1 << i;
                validCount++;
            }
        }

        if (validCount == 0) {
            return 0;
        }

        int edgeMask = 0;
        for (int edge = 0; edge < 3; edge++) { //compute connection edges
            for (int base = CONNECTION_SUB_NEIGHBOR_COUNT * edge, i = 0; i < CONNECTION_SUB_NEIGHBOR_COUNT; i++) {
                int j = CONNECTION_SUB_NEIGHBORS[base + i];
                if ((validFlags & (1 << j)) != 0 && (datas[j].edges & (EDGE_DIR_MASK << (edge << 1))) != EDGE_DIR_NONE) {
                    edgeMask |= 1 << edge;
                    break;
                }
            }
        }
        return edgeMask;
    }
}
