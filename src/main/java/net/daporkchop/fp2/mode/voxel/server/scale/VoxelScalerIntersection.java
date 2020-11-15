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
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class VoxelScalerIntersection implements IFarScaler<VoxelPos, VoxelPiece, VoxelPieceBuilder> {
    public static final int SRC_MIN = -4;
    public static final int SRC_MAX = (T_VOXELS << 1) + 2;
    //public static final int SRC_MIN = 0;
    //public static final int SRC_MAX = (T_VOXELS << 1);
    public static final int SRC_SIZE = SRC_MAX - SRC_MIN;

    public static final int DST_MIN = -1;
    public static final int DST_MAX = T_VOXELS;
    public static final int DST_SIZE = DST_MAX - DST_MIN;

    protected static int srcPieceIndex(int x, int y, int z) {
        final int fac = (((SRC_MAX - 1) >> T_SHIFT) + 1) - (SRC_MIN >> T_SHIFT);
        final int add = -(SRC_MIN >> T_SHIFT) << T_SHIFT;
        return (((x + add) >> T_SHIFT) * fac + ((y + add) >> T_SHIFT)) * fac + ((z + add) >> T_SHIFT);
    }

    protected static int srcIndex(int x, int y, int z) {
        return ((x - SRC_MIN) * SRC_SIZE + (y - SRC_MIN)) * SRC_SIZE + (z - SRC_MIN);
    }

    protected static int srcIndex(int x, int y, int z, int edge) {
        return srcIndex(x, y, z) * 3 + edge;
    }

    protected static int dstIndex(int x, int y, int z) {
        return ((x - DST_MIN) * DST_SIZE + (y - DST_MIN)) * DST_SIZE + (z - DST_MIN);
    }

    protected static int dstIndex(int x, int y, int z, int edge) {
        return dstIndex(x, y, z) * 3 + edge;
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

        final int min = SRC_MIN >> T_SHIFT;
        final int max = ((SRC_MAX - 1) >> T_SHIFT) + 1;
        VoxelPos[] positions = new VoxelPos[(max - min) * (max - min) * (max - min)];
        for (int i = 0, dx = min; dx < max; dx++) {
            for (int dy = min; dy < max; dy++) {
                for (int dz = min; dz < max; dz++) {
                    positions[i++] = new VoxelPos(x + dx, y + dy, z + dz, level);
                }
            }
        }
        return Arrays.stream(positions);
    }

    @Override
    public void scale(@NonNull VoxelPiece[] srcPieces, @NonNull VoxelPieceBuilder dst) {
        VoxelData sample = new VoxelData();

        BitSet srcVoxels = new BitSet(SRC_SIZE * SRC_SIZE * SRC_SIZE);
        BitSet dstVoxels = new BitSet(DST_SIZE * DST_SIZE * DST_SIZE);

        int[] srcEdges = new int[SRC_SIZE * SRC_SIZE * SRC_SIZE];
        int[] srcStates = new int[SRC_SIZE * SRC_SIZE * SRC_SIZE * 3];
        for (int x = SRC_MIN; x < SRC_MAX; x++) {
            for (int y = SRC_MIN; y < SRC_MAX; y++) {
                for (int z = SRC_MIN; z < SRC_MAX; z++) {
                    VoxelPiece srcPiece = srcPieces[srcPieceIndex(x, y, z)];
                    if (srcPiece != null && srcPiece.get(x & T_MASK, y & T_MASK, z & T_MASK, sample)) {
                        srcVoxels.set(srcIndex(x, y, z));

                        int edges = 0;
                        for (int edge = 0; edge < 3; edge++) {
                            if (((sample.edges >> (edge << 1)) & EDGE_DIR_MASK) != EDGE_DIR_NONE
                                && voxelType(Block.getStateById(sample.states[edge])) == TYPE_OPAQUE) {
                                edges |= (sample.edges & (EDGE_DIR_MASK << (edge << 1)));
                                srcStates[srcIndex(x, y, z, edge)] = sample.states[edge];
                            }
                        }
                        srcEdges[srcIndex(x, y, z)] = edges;
                    }
                }
            }
        }

        //BitSet dstVoxels = new BitSet(DST_SIZE * DST_SIZE * DST_SIZE);
        for (int x = DST_MIN; x < DST_MAX; x++) {
            for (int y = DST_MIN; y < DST_MAX; y++) {
                for (int z = DST_MIN; z < DST_MAX; z++) {
                    VOXEL_SEARCH:
                    for (int dx = 0; dx < 2; dx++) {
                        for (int dy = 0; dy < 2; dy++) {
                            for (int dz = 0; dz < 2; dz++) {
                                if (srcVoxels.get(srcIndex((x << 1) + dx, (y << 1) + dy, (z << 1) + dz))) {
                                    dstVoxels.set(dstIndex(x, y, z));
                                    break VOXEL_SEARCH;
                                }
                            }
                        }
                    }
                }
            }
        }

        int[] dstEdges = new int[DST_SIZE * DST_SIZE * DST_SIZE];
        for (int x = DST_MIN; x < DST_MAX; x++) {
            for (int y = DST_MIN; y < DST_MAX; y++) {
                for (int z = DST_MIN; z < DST_MAX; z++) {
                    if (dstVoxels.get(dstIndex(x, y, z))) {
                        int edges = 0;
                        for (int edge = 0; edge < 3; edge++) {
                            int c = CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + CONNECTION_INDEX_COUNT - 1];
                            int cdx = (c >> 2) & 1;
                            int cdy = (c >> 1) & 1;
                            int cdz = c & 1;

                            int o = c ^ 7;
                            int odx = (o >> 2) & 1;
                            int ody = (o >> 1) & 1;
                            int odz = o & 1;

                            int low0 = srcEdges[srcIndex((x << 1) - cdx, (y << 1) - cdy, (z << 1) - cdz)];
                            int low1 = srcEdges[srcIndex((x << 1) - cdx + odx, (y << 1) - cdy + ody, (z << 1) - cdz + odz)];

                            edges |= (((low0 | low1) >> (edge << 1)) & EDGE_DIR_MASK) << (edge << 1);
                        }

                        for (int edge = 0; edge < 3; edge++) { //TODO: remove this
                            if (((edges >> (edge << 1)) & EDGE_DIR_MASK) != EDGE_DIR_NONE) {
                                edges |= EDGE_DIR_BOTH << (edge << 1);
                            }
                        }

                        dstEdges[dstIndex(x, y, z)] = edges;
                    }
                }
            }
        }

        for (int x = DST_MIN; x < DST_MAX - 1; x++) {
            for (int y = DST_MIN; y < DST_MAX - 1; y++) {
                for (int z = DST_MIN; z < DST_MAX - 1; z++) {
                    int edges = dstEdges[dstIndex(x, y, z)];
                    for (int edge = 0; edge < 3; edge++) {
                        if (((edges >> (edge << 1)) & EDGE_DIR_MASK) != EDGE_DIR_NONE) {
                            for (int i = 0; i < CONNECTION_INDEX_COUNT; i++) {
                                int c = CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + i];
                                dstVoxels.set(dstIndex(x + ((c >> 2) & 1), y + ((c >> 1) & 1), z + (c & 1)));
                            }
                        }
                    }
                }
            }
        }

        sample.light = 0xF0;
        sample.x = sample.y = sample.z = POS_ONE >> 1;
        for (int x = 0; x < T_VOXELS; x++) {
            for (int y = 0; y < T_VOXELS; y++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    if (dstVoxels.get(dstIndex(x, y, z))) {
                        Arrays.fill(sample.states, 0);
                        int edges = sample.edges = dstEdges[dstIndex(x, y, z)];
                        for (int edge = 0; edge < 3; edge++) {
                            if (((edges >> (edge << 1)) & EDGE_DIR_MASK) != EDGE_DIR_NONE) {
                                sample.states[edge] = 1;

                                VOXEL_SEARCH:
                                for (int dx = 0; dx < 2; dx++) {
                                    for (int dy = 0; dy < 2; dy++) {
                                        for (int dz = 0; dz < 2; dz++) {
                                            if ((sample.states[edge] = srcStates[srcIndex((x << 1) + dx, (y << 1) + dy, (z << 1) + dz, edge)]) != 0) {
                                                break VOXEL_SEARCH;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        dst.set(x, y, z, sample);
                    }
                }
            }
        }
    }
}
