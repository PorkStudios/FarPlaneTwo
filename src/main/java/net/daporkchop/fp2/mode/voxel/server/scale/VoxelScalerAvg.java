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
    protected static void cross(int[] a, int aOff, int[] b, int bOff, int[] dst, int dstOff) {
        dst[dstOff + 0] = (a[aOff + 1] * b[bOff + 2] - a[aOff + 2] * b[bOff + 1]) >> POS_FRACT_SHIFT;
        dst[dstOff + 1] = (a[aOff + 2] * b[bOff + 0] - a[aOff + 0] * b[bOff + 2]) >> POS_FRACT_SHIFT;
        dst[dstOff + 2] = (a[aOff + 0] * b[bOff + 1] - a[aOff + 1] * b[bOff + 0]) >> POS_FRACT_SHIFT;
    }

    protected static void cross3(int[] a, int aOff, int[] b, int bOff, int[] c, int cOff, int[] dst, int dstOff) {
        int ax = a[aOff + 0] - b[bOff + 0];
        int ay = a[aOff + 1] - b[bOff + 1];
        int az = a[aOff + 2] - b[bOff + 2];
        int bx = a[aOff + 0] - c[cOff + 0];
        int by = a[aOff + 1] - c[cOff + 1];
        int bz = a[aOff + 2] - c[cOff + 2];
        dst[dstOff + 0] = (ay * bz - az * by) >> POS_FRACT_SHIFT;
        dst[dstOff + 1] = (az * bx - ax * bz) >> POS_FRACT_SHIFT;
        dst[dstOff + 2] = (ax * by - ay * bx) >> POS_FRACT_SHIFT;
    }

    protected static void crossAdd(int[] a, int aOff, int[] b, int bOff, int[] dst, int dstOff) {
        dst[dstOff + 0] += (a[aOff + 1] * b[bOff + 2] - a[aOff + 2] * b[bOff + 1]) >> POS_FRACT_SHIFT;
        dst[dstOff + 1] += (a[aOff + 2] * b[bOff + 0] - a[aOff + 0] * b[bOff + 2]) >> POS_FRACT_SHIFT;
        dst[dstOff + 2] += (a[aOff + 0] * b[bOff + 1] - a[aOff + 1] * b[bOff + 0]) >> POS_FRACT_SHIFT;
    }

    protected static void cross3Add(int[] a, int aOff, int[] b, int bOff, int[] c, int cOff, int[] dst, int dstOff) {
        int ax = a[aOff + 0] - b[bOff + 0];
        int ay = a[aOff + 1] - b[bOff + 1];
        int az = a[aOff + 2] - b[bOff + 2];
        int bx = a[aOff + 0] - c[cOff + 0];
        int by = a[aOff + 1] - c[cOff + 1];
        int bz = a[aOff + 2] - c[cOff + 2];
        dst[dstOff + 0] += (ay * bz - az * by) >> 0;
        dst[dstOff + 1] += (az * bx - ax * bz) >> 0;
        dst[dstOff + 2] += (ax * by - ay * bx) >> 0;
    }

    protected static void cross3Sub(int[] a, int aOff, int[] b, int bOff, int[] c, int cOff, int[] dst, int dstOff) {
        int ax = a[aOff + 0] - b[bOff + 0];
        int ay = a[aOff + 1] - b[bOff + 1];
        int az = a[aOff + 2] - b[bOff + 2];
        int bx = a[aOff + 0] - c[cOff + 0];
        int by = a[aOff + 1] - c[cOff + 1];
        int bz = a[aOff + 2] - c[cOff + 2];
        dst[dstOff + 0] -= (ay * bz - az * by) >> POS_FRACT_SHIFT;
        dst[dstOff + 1] -= (az * bx - ax * bz) >> POS_FRACT_SHIFT;
        dst[dstOff + 2] -= (ax * by - ay * bx) >> POS_FRACT_SHIFT;
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
        int[] positions = new int[T_VERTS * T_VERTS * T_VERTS * 3];
        int[] normals = new int[T_VOXELS * T_VOXELS * T_VOXELS * 3];
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
                                    this.computeAverageNormal(srcs, (subX << T_SHIFT) + x, (subY << T_SHIFT) + y, (subZ << T_SHIFT) + z, normals, (((baseX + (x >> 1)) * T_VOXELS + (baseY + (y >> 1))) * T_VOXELS + (baseZ + (z >> 1))) * 3);
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

        datas[8].light = 0xFF;
        Arrays.fill(datas[8].states, 1);
        for (int x = 0; x < T_VOXELS; x++) {
            for (int y = 0; y < T_VOXELS; y++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    int i = (x * T_VERTS + y) * T_VERTS + z;
                    if (voxels.get(i)) {
                        //this.computeAverageNormal(srcs, x,y, z, normals, 0);

                        int edges = 0;
                        for (int edge = 0; edge < EDGE_COUNT; edge++) {
                            if (existentEdges.get(((x * T_VOXELS + y) * T_VOXELS + z) * EDGE_COUNT + edge)) {
                                int normalAxisValue = normals[((x * T_VOXELS + y) * T_VOXELS + z) * EDGE_COUNT + edge];
                                //int normalAxisValue = normals[edge];
                                //edges |= (normalAxisValue != 0 ? EDGE_DIR_BOTH : 0) << (edge << 1);
                                if (normalAxisValue != 0) {
                                    edges |= (normalAxisValue < 0 ? EDGE_DIR_NEGATIVE : EDGE_DIR_POSITIVE) << (edge << 1);
                                }
                                edges |= EDGE_DIR_BOTH << (edge << 1);
                            }
                        }
                        datas[8].edges = edges;

                        datas[8].x = positions[i * 3 + 0];
                        datas[8].y = positions[i * 3 + 1];
                        datas[8].z = positions[i * 3 + 2];
                        dst.set(x, y, z, datas[8]);
                    }
                }
            }
        }
    }

    protected boolean computePosition(VoxelPiece src, int srcX, int srcY, int srcZ, int[] dst, int dstOff, VoxelData data) {
        int validCount = 0;
        for (int i = 0; i < 8; i++) { //fetch data from all 8 contributing voxels
            if (src.getOnlyPos(srcX + ((i >> 2) & 1), srcY + ((i >> 1) & 1), srcZ + (i & 1), data)) {
                dst[dstOff + 0] += data.x + ((i << (POS_FRACT_SHIFT - 2)) & POS_ONE);
                dst[dstOff + 1] += data.y + ((i << (POS_FRACT_SHIFT - 1)) & POS_ONE);
                dst[dstOff + 2] += data.z + ((i << (POS_FRACT_SHIFT - 0)) & POS_ONE);
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

    protected void computeAverageNormal(VoxelPiece[] srcs, int x, int y, int z, int[] dst, int dstOff) {
        int[] p = new int[CONNECTION_INDEX_COUNT * 3];

        for (int i = 0; i < 8; i++) {
            int X = x + ((i >> 2) & 1);
            int Y = y + ((i >> 1) & 1);
            int Z = z + (i & 1);
            EDGE:
            for (int edge = 0; edge < EDGE_COUNT; edge++) {
                int edges = this.getPos(srcs, X, Y, Z, CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT], p, 0);
                if (edges >= 0) {
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

    protected int getPos(VoxelPiece[] srcs, int x, int y, int z, int connectionIndex, int[] dst, int dstOff) {
        int edges = this.getPos(srcs, x + ((connectionIndex >> 2) & 1), y + ((connectionIndex >> 1) & 1), z + (connectionIndex & 1), dst, dstOff);
        if (edges >= 0) {
            for (int i = 0; i < 3; i++) {
                dst[dstOff + i] += ((connectionIndex >> (2 - i)) & 1) << T_SHIFT;
            }
        }
        return edges;
    }

    protected int getPos(VoxelPiece[] srcs, int x, int y, int z, int[] dst, int dstOff) {
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

    protected void computeNormal(int edge, int x, int y, int z, int[] positions, int[] dst, int dstOff) {
        int[] base = this.getPosition(x, y, z, CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + 0], positions);
        int[] a = this.getPosition(x, y, z, CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + 1], positions);
        int[] b = this.getPosition(x, y, z, CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + 2], positions);
        for (int i = 0; i < 3; i++) {
            a[i] -= base[i];
            b[i] -= base[i];
        }
        cross(a, 0, b, 0, dst, dstOff);
    }

    protected int[] getPosition(int x, int y, int z, int connectionIndexIndex, int[] positions) {
        int connectionIndex = CONNECTION_INDICES[connectionIndexIndex];
        int i = (((x + ((connectionIndex >> 2) & 1)) * T_VERTS + (y + ((connectionIndex >> 1) & 1))) * T_VERTS + (z + (connectionIndex & 1))) * 3;
        return new int[]{
                (((connectionIndex >> 2) & 1) << POS_FRACT_SHIFT) + positions[i + 0],
                (((connectionIndex >> 1) & 1) << POS_FRACT_SHIFT) + positions[i + 0],
                ((connectionIndex & 1) << POS_FRACT_SHIFT) + positions[i + 0]
        };
    }

    protected void scaleSample(VoxelPiece src, int srcX, int srcY, int srcZ, VoxelPieceBuilder dst, int dstX, int dstY, int dstZ, VoxelData[] datas) {
        int validFlags = 0;
        int validCount = 0;
        for (int i = 0; i < 8; i++) { //fetch data from all 8 contributing voxels
            if (src.get(srcX + ((i >> 2) & 1), srcY + ((i >> 1) & 1), srcZ + (i & 1), datas[i])) {
                validFlags |= 1 << i;
                validCount++;
            }
        }

        if (validCount == 0) { //none of the voxels contain any data at all
            return;
        }

        VoxelData data = datas[8].reset();

        int x = 0;
        int y = 0;
        int z = 0;
        for (int i = 0; i < 8; i++) { //compute average voxel position
            if ((validFlags & (1 << i)) != 0) {
                x += datas[i].x + ((i << (POS_FRACT_SHIFT - 2)) & POS_ONE);
                y += datas[i].y + ((i << (POS_FRACT_SHIFT - 1)) & POS_ONE);
                z += datas[i].z + ((i << (POS_FRACT_SHIFT - 0)) & POS_ONE);
            }
        }
        x /= validCount;
        y /= validCount;
        z /= validCount;
        data.x = x >> 1;
        data.y = y >> 1;
        data.z = z >> 1;

        int edges = 0;
        for (int edge = 0; edge < 3; edge++) { //compute connection edges
            for (int base = CONNECTION_SUB_NEIGHBOR_COUNT * edge, i = 0; i < CONNECTION_SUB_NEIGHBOR_COUNT; i++) {
                int j = CONNECTION_SUB_NEIGHBORS[base + i];
                if ((validFlags & (1 << j)) != 0) {
                    edges |= datas[j].edges & (EDGE_DIR_MASK << (edge << 1));
                }
            }
        }
        data.edges = edges;

        //compute appearance data
        //TODO: i need a better algorithm for selecting which voxel to use
        int j = (floorI(x) << 2) | (floorI(y) << 1) | floorI(z);
        if ((validFlags & (1 << j)) != 0) {
            System.arraycopy(datas[j].states, 0, data.states, 0, EDGE_COUNT);
            data.biome = datas[j].biome;
            data.light = datas[j].light;
        } else {
            //fall back to using anything
            for (int i = 0; i < 8; i++) {
                if ((validFlags & (1 << i)) != 0) {
                    System.arraycopy(datas[i].states, 0, data.states, 0, EDGE_COUNT);
                    data.biome = datas[i].biome;
                    data.light = datas[i].light;
                    break;
                }
            }
        }

        dst.set(dstX, dstY, dstZ, data);
    }
}
