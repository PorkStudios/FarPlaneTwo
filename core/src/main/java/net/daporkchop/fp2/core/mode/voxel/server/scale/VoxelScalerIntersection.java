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

package net.daporkchop.fp2.core.mode.voxel.server.scale;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.core.mode.voxel.VoxelData;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.util.math.Vector3d;
import net.daporkchop.fp2.core.util.math.qef.QefSolver;
import net.daporkchop.lib.math.vector.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.fp2.api.world.BlockWorldConstants.*;
import static net.daporkchop.fp2.core.mode.voxel.VoxelConstants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
//TODO: this needs a LOT of work
//TODO: re-implement using something based on https://www.researchgate.net/publication/220792145_Model_Simplification_Using_Vertex-Clustering
public class VoxelScalerIntersection extends AbstractFarGenerator implements IFarScaler<VoxelPos, VoxelTile> {
    public static final int SRC_MIN = -4;
    public static final int SRC_MAX = (VT_VOXELS << 1) + 4;
    //public static final int SRC_MIN = 0;
    //public static final int SRC_MAX = (VT_VOXELS << 1);
    public static final int SRC_SIZE = SRC_MAX - SRC_MIN;

    public static final int DST_MIN = -1;
    public static final int DST_MAX = VT_VOXELS + 1;
    public static final int DST_SIZE = DST_MAX - DST_MIN;

    protected static int srcTileIndex(int x, int y, int z) {
        final int fac = (((SRC_MAX - 1) >> VT_SHIFT) + 1) - (SRC_MIN >> VT_SHIFT);
        final int add = -(SRC_MIN >> VT_SHIFT) << VT_SHIFT;
        return (((x + add) >> VT_SHIFT) * fac + ((y + add) >> VT_SHIFT)) * fac + ((z + add) >> VT_SHIFT);
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

    protected static Vec3d pos(int x, int y, int z, int edge, int i) {
        int c = CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + i];
        return Vec3d.of(x + ((c >> 2) & 1) + 0.5d, y + ((c >> 1) & 1) + 0.5d, z + (c & 1) + 0.5d);
    }

    public VoxelScalerIntersection(@NonNull IFarWorldServer world) {
        super(world);
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

        final int min = SRC_MIN >> VT_SHIFT;
        final int max = ((SRC_MAX - 1) >> VT_SHIFT) + 1;
        VoxelPos[] positions = new VoxelPos[(max - min) * (max - min) * (max - min)];
        for (int i = 0, dx = min; dx < max; dx++) {
            for (int dy = min; dy < max; dy++) {
                for (int dz = min; dz < max; dz++) {
                    positions[i++] = new VoxelPos(level, x + dx, y + dy, z + dz);
                }
            }
        }
        return Arrays.stream(positions);
    }

    @Override
    public long scale(@NonNull VoxelTile[] srcTiles, @NonNull VoxelTile dst) {
        VoxelData data = new VoxelData();
        QefSolver qef = new QefSolver();

        BitSet srcVoxels = new BitSet(SRC_SIZE * SRC_SIZE * SRC_SIZE);
        BitSet dstVoxels = new BitSet(DST_SIZE * DST_SIZE * DST_SIZE);

        int[] srcEdges = new int[SRC_SIZE * SRC_SIZE * SRC_SIZE];
        int[] srcBiomesAndLights = new int[SRC_SIZE * SRC_SIZE * SRC_SIZE];
        int[] srcStates = new int[SRC_SIZE * SRC_SIZE * SRC_SIZE * 3];
        for (int x = SRC_MIN; x < SRC_MAX; x++) {
            for (int y = SRC_MIN; y < SRC_MAX; y++) {
                for (int z = SRC_MIN; z < SRC_MAX; z++) {
                    VoxelTile srcTile = srcTiles[srcTileIndex(x, y, z)];
                    if (srcTile != null && srcTile.get(x & VT_MASK, y & VT_MASK, z & VT_MASK, data)) {
                        srcVoxels.set(srcIndex(x, y, z));

                        int edges = 0;
                        for (int edge = 0; edge < 3; edge++) {
                            if (((data.edges >> (edge << 1)) & EDGE_DIR_MASK) != EDGE_DIR_NONE
                                && (true || this.extendedStateRegistryData.type(data.states[edge]) == BLOCK_TYPE_OPAQUE)) {
                                edges |= (data.edges & (EDGE_DIR_MASK << (edge << 1)));
                                srcStates[srcIndex(x, y, z, edge)] = data.states[edge];
                            }
                        }
                        srcEdges[srcIndex(x, y, z)] = edges;
                        srcBiomesAndLights[srcIndex(x, y, z)] = data.biome << 8 | (data.light & 0xFF);
                    }
                }
            }
        }

        List<Quad>[] quadQuads = uncheckedCast(new List[DST_SIZE * DST_SIZE * DST_SIZE]);
        for (int x = SRC_MIN; x < SRC_MAX; x++) {
            for (int y = SRC_MIN; y < SRC_MAX; y++) {
                for (int z = SRC_MIN; z < SRC_MAX; z++) {
                    int edges = srcEdges[srcIndex(x, y, z)];
                    EDGES:
                    for (int edge = 0; edge < 3; edge++) {
                        if (((edges >> (edge << 1)) & EDGE_DIR_MASK) == EDGE_DIR_NONE) {
                            continue;
                        }

                        if (x >> 1 >= DST_MIN && x >> 1 < DST_MAX && y >> 1 >= DST_MIN && y >> 1 < DST_MAX && z >> 1 >= DST_MIN && z >> 1 < DST_MAX) {
                            int di = dstIndex(x >> 1, y >> 1, z >> 1);
                            if (quadQuads[di] == null) {
                                quadQuads[di] = new ArrayList<>();
                            }

                            Vec3d v0 = pos(x, y, z, edge, 0);
                            Vec3d v1 = pos(x, y, z, edge, 1);
                            Vec3d v2 = pos(x, y, z, edge, 2);
                            Vec3d v3 = pos(x, y, z, edge, 3);
                            quadQuads[di].add(new Quad(v0, v1, v2, v3, edges));
                        }
                    }
                }
            }
        }

        int[] dstEdges = new int[DST_SIZE * DST_SIZE * DST_SIZE];
        for (int x = DST_MIN; x < DST_MAX; x++) {
            for (int y = DST_MIN; y < DST_MAX; y++) {
                for (int z = DST_MIN; z < DST_MAX; z++) {
                    int edges = 0;
                    for (int edge = 0; edge < 3; edge++) {
                        int c0 = EDGE_VERTEX_MAP[edge << 1];
                        int c1 = EDGE_VERTEX_MAP[(edge << 1) | 1];
                        Vec3d p0 = Vec3d.of((x + ((c0 >> 2) & 1)) << 1, (y + ((c0 >> 1) & 1)) << 1, (z + (c0 & 1)) << 1);
                        Vec3d p1 = Vec3d.of((x + ((c1 >> 2) & 1)) << 1, (y + ((c1 >> 1) & 1)) << 1, (z + (c1 & 1)) << 1);

                        int dir = 0;
                        if (quadQuads[dstIndex(x, y, z)] != null) {
                            for (Quad quad : quadQuads[dstIndex(x, y, z)]) {
                                Vec3d i = quad.intersect(p0, p1);
                                if (i != null) {
                                    dir |= quad.dir;
                                }
                            }
                        }
                        edges |= dir & (EDGE_DIR_MASK << (edge << 1));
                    }
                    dstEdges[dstIndex(x, y, z)] = edges;
                }
            }
        }

        for (int x = DST_MIN; x < DST_MAX - 1; x++) { //ensure all possible destination voxels are set
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

        for (int x = 0; x < VT_VOXELS; x++) {
            for (int y = 0; y < VT_VOXELS; y++) {
                for (int z = 0; z < VT_VOXELS; z++) {
                    if (dstVoxels.get(dstIndex(x, y, z))) {
                        qef.reset();
                        for (int edge = 0; edge < QEF_EDGE_COUNT; edge++) {
                            int c0 = QEF_EDGE_VERTEX_MAP[edge << 1];
                            int c1 = QEF_EDGE_VERTEX_MAP[(edge << 1) | 1];
                            Vec3d p0 = Vec3d.of((x + ((c0 >> 2) & 1)) << 1, (y + ((c0 >> 1) & 1)) << 1, (z + (c0 & 1)) << 1);
                            Vec3d p1 = Vec3d.of((x + ((c1 >> 2) & 1)) << 1, (y + ((c1 >> 1) & 1)) << 1, (z + (c1 & 1)) << 1);
                            for (int dx = -1; dx <= 0; dx++) {
                                for (int dy = -1; dy <= 0; dy++) {
                                    for (int dz = -1; dz <= 0; dz++) {
                                        int di = dstIndex(x + dx, y + dy, z + dz);
                                        if (quadQuads[di] != null) {
                                            for (Quad quad : quadQuads[di]) {
                                                Vec3d i = quad.intersect(p0, p1);
                                                if (i != null) {
                                                    qef.add(i.x(), i.y(), i.z(), quad.n.x(), quad.n.y(), quad.n.z());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (qef.numPoints() > 0) {
                            Vector3d vec = new Vector3d();
                            qef.solve(vec, 0.1, 1, 0.5);
                            data.x = clamp(floorI((vec.x - (x << 1)) * POS_ONE), 0, POS_ONE);
                            data.y = clamp(floorI((vec.y - (y << 1)) * POS_ONE), 0, POS_ONE);
                            data.z = clamp(floorI((vec.z - (z << 1)) * POS_ONE), 0, POS_ONE);
                        } else {
                            data.x = data.y = data.z = POS_ONE >> 1;
                        }

                        Arrays.fill(data.states, 0);
                        int edges = data.edges = dstEdges[dstIndex(x, y, z)];
                        for (int edge = 0; edge < 3; edge++) {
                            if (((edges >> (edge << 1)) & EDGE_DIR_MASK) != EDGE_DIR_NONE) {
                                data.states[edge] = 1;

                                VOXEL_SEARCH:
                                for (int dx = 0; dx < 2; dx++) {
                                    for (int dy = 0; dy < 2; dy++) {
                                        for (int dz = 0; dz < 2; dz++) {
                                            if ((data.states[edge] = srcStates[srcIndex((x << 1) + dx, (y << 1) + dy, (z << 1) + dz, edge)]) != 0) {
                                                int si = srcIndex((x << 1) + dx, (y << 1) + dy, (z << 1) + dz);
                                                data.biome = srcBiomesAndLights[si] >> 8;
                                                data.light = (byte) srcBiomesAndLights[si];
                                                break VOXEL_SEARCH;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        dst.set(x, y, z, data);
                    }
                }
            }
        }

        return 0L;
    }

    @AllArgsConstructor
    protected static class Quad {
        protected static double dotProduct(Vec3d a, Vec3d b) {
            return a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
        }

        protected static Vec3d crossProduct(Vec3d a, Vec3d b) {
            return Vec3d.of(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(), a.x() * b.y() - a.y() * b.z());
        }

        protected static Vec3d intersect(Vec3d v0, Vec3d v1, Vec3d v2, Vec3d n, Vec3d p0, Vec3d p1) {
            Vec3d u = v1.sub(v0);
            Vec3d v = v2.sub(v0);

            Vec3d dir = p1.sub(p0);
            Vec3d w0 = p0.sub(v0);
            double a = -dotProduct(n, w0);
            double b = dotProduct(n, dir);
            double r = a / b;
            if (r < 0.0d || r > 1.0d) {
                return null;
            }

            Vec3d i = p0.add(dir.mul(r, r, r));

            double uu = dotProduct(u, u);
            double uv = dotProduct(u, v);
            double vv = dotProduct(v, v);
            Vec3d w = i.sub(v0);
            double wu = dotProduct(w, u);
            double wv = dotProduct(w, v);
            double D = uv * uv - uu * vv;

            double s = (uv * wv - vv * wu) / D;
            if (s < 0.0d || s > 1.0d) {
                return null;
            }
            double t = (uv * wu - uu * wv) / D;
            if (t < 0.0d || (s + t) > 1.0d) {
                return null;
            }
            return i;
        }

        protected final Vec3d v0;
        protected final Vec3d v1;
        protected final Vec3d v2;
        protected final Vec3d v3;
        protected final Vec3d n;
        protected final int dir;

        public Quad(Vec3d v0, Vec3d v1, Vec3d v2, Vec3d v3, int dir) {
            this(v0, v1, v2, v3, crossProduct(v1.sub(v0), v2.sub(v0)), dir);
        }

        public Vec3d intersect(Vec3d p0, Vec3d p1) {
            Vec3d i = intersect(this.v0, this.v1, this.v2, this.n, p0, p1);
            if (i == null) {
                i = intersect(this.v3, this.v1, this.v2, this.n, p0, p1);
            }
            return i;
        }
    }
}
