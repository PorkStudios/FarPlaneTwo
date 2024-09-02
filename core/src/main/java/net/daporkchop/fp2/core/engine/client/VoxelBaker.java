/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.engine.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TileData;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.engine.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;

/**
 * Shared code for baking voxel geometry.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class VoxelBaker implements IRenderBaker<IndexedBakeOutput<VoxelGlobalAttributes, VoxelLocalAttributes>> {
    protected static int vertexMapIndex(int dx, int dy, int dz, int i, int edge) {
        int j = CONNECTION_INDICES[i];
        int ddx = dx + ((j >> 2) & 1);
        int ddy = dy + ((j >> 1) & 1);
        int ddz = dz + (j & 1);

        return ((ddx * T_VERTS + ddy) * T_VERTS + ddz) * EDGE_COUNT + edge;
    }

    @NonNull
    protected final LevelRenderer levelRenderer;
    @NonNull
    protected final TextureUVs textureUVs;
    protected final boolean forceBlockyMesh;

    @Override
    public Set<TilePos> bakeOutputs(@NonNull TilePos srcPos) {
        int x = srcPos.x();
        int y = srcPos.y();
        int z = srcPos.z();
        int level = srcPos.level();

        val res = DirectTilePosAccess.newPositionHashSet();
        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 0; dz++) {
                    res.add(new TilePos(level, x + dx, y + dy, z + dz));
                }
            }
        }
        return res;
    }

    @Override
    public List<TilePos> bakeInputs(@NonNull TilePos dstPos) {
        int x = dstPos.x();
        int y = dstPos.y();
        int z = dstPos.z();
        int level = dstPos.level();

        val res = DirectTilePosAccess.newPositionArrayList();
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    res.add(new TilePos(level, x + dx, y + dy, z + dz));
                }
            }
        }
        return res;
    }

    @Override
    public void bake(@NonNull TilePos pos, @NonNull Tile[] srcs, @NonNull IndexedBakeOutput<VoxelGlobalAttributes, VoxelLocalAttributes> output) {
        if (srcs[0] == null) {
            return;
        }

        //write globals
        output.globals().append()
                .tilePos(pos.x(), pos.y(), pos.z(), pos.level())
                .close();

        ArrayAllocator<int[]> alloc = GlobalAllocators.ALLOC_INT.get();
        int[] map = alloc.atLeast(cb(T_VERTS) * EDGE_COUNT);
        Arrays.fill(map, 0, cb(T_VERTS) * EDGE_COUNT, -1);

        try {
            //step 1: write vertices for all source tiles, and assign indices
            this.writeVertices(srcs, pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.level(), map, output.verts());

            //step 2: write indices to actually connect the vertices and build the mesh
            this.writeIndices(srcs[0], map, output.indices());
        } finally {
            alloc.release(map);
        }
    }

    protected void writeVertices(Tile[] srcs, int blockX, int blockY, int blockZ, int level, int[] map, AttributeWriter<VoxelLocalAttributes> verts) {
        final TileData data = new TileData();

        for (int i = 0; i < 8; i++) {
            Tile src = srcs[i];
            if (src == null) {
                continue;
            }

            int maxDx = CONNECTION_INTERSECTION_VOLUMES[i * 3 + 0];
            int maxDy = CONNECTION_INTERSECTION_VOLUMES[i * 3 + 1];
            int maxDz = CONNECTION_INTERSECTION_VOLUMES[i * 3 + 2];
            for (int dx = 0; dx < maxDx; dx++) {
                for (int dy = 0; dy < maxDy; dy++) {
                    for (int dz = 0; dz < maxDz; dz++) {
                        if (!src.get(dx, dy, dz, data)) {
                            continue;
                        }

                        this.writeVertex(blockX, blockY, blockZ, level, dx + (((i >> 2) & 1) << T_SHIFT), dy + (((i >> 1) & 1) << T_SHIFT), dz + ((i & 1) << T_SHIFT), data, verts, map);
                    }
                }
            }
        }
    }

    protected void writeVertex(int baseX, int baseY, int baseZ, int level, int x, int y, int z, TileData data, AttributeWriter<VoxelLocalAttributes> vertices, int[] map) {
        baseX += (x & T_VOXELS) << level;
        baseY += (y & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        int baseMapIndex = ((x * T_VERTS + y) * T_VERTS + z) * 3;

        final int blockX = baseX + ((x & ~(x & T_VOXELS)) << level);
        final int blockY = baseY + ((y & ~(y & T_VOXELS)) << level);
        final int blockZ = baseZ + ((z & ~(z & T_VOXELS)) << level);

        final int fractMask = this.forceBlockyMesh ? 0 : -1;

        int blockLight = BlockLevelConstants.unpackBlockLight(data.light);
        int skyLight = BlockLevelConstants.unpackSkyLight(data.light);
        //sky and block light are one unsigned byte each, and are interpreted as normalized floats. since the lightmap texture is 16x16, using
        //  the upper 4 bits for the regular light level (which is in range [0,15]) results in a float range of [0,15/16]. additionally, we set
        //  the bottom 4 bits to 0b1000. this is equivalent to an offset of 0.5 texels, which prevents blending artifacts when sampling right along
        //  the edge of a texture.
        vertices.append()
                .light((blockLight << 4) | 8, (skyLight << 4) | 8)
                .pos((x << POS_FRACT_SHIFT) + (data.x & fractMask), (y << POS_FRACT_SHIFT) + (data.y & fractMask), (z << POS_FRACT_SHIFT) + (data.z & fractMask))
                .close();

        boolean first = true;
        EDGES:
        for (int edge = 0; edge < EDGE_COUNT; edge++) {
            for (int j = 0; j < edge; j++) {
                if (data.states[j] == data.states[edge]) { //states match, don't duplicate vertex data for this edge
                    map[baseMapIndex + edge] = map[baseMapIndex + j];
                    continue EDGES;
                }
            }

            if (!first) {
                vertices.append().close();
                vertices.copy(vertices.position() - 1, vertices.position());
            }

            vertices.current()
                    .state(this.textureUVs.state2index(data.states[edge]))
                    .color(this.levelRenderer.tintFactorForStateInBiomeAtPos(data.states[edge], data.biome, blockX, blockY, blockZ))
                    .close();

            map[baseMapIndex + edge] = vertices.position();
            first = false;
        }
    }

    protected void writeIndices(Tile src, int[] map, IndexWriter[] indices) {
        final TileData data = new TileData();

        for (int j = 0; j < src.count(); j++) {
            int voxelPos = src.get(j, data);
            int dx = (voxelPos >> (T_SHIFT << 1)) & T_MASK;
            int dy = (voxelPos >> T_SHIFT) & T_MASK;
            int dz = voxelPos & T_MASK;

            int edges = data.edges;
            if ((((edges >> 2) ^ (edges >> 3)) & 1) != 0) { //for some reason y is backwards... let's invert it
                edges ^= EDGE_DIR_MASK << 2;
            }
            for (int edge = 0; edge < EDGE_COUNT; edge++) {
                if ((edges & (EDGE_DIR_MASK << (edge << 1))) == EDGE_DIR_NONE) {
                    continue;
                }

                int base = edge * CONNECTION_INDEX_COUNT;
                int oppositeCorner, c0, c1, provoking;
                if ((provoking = map[vertexMapIndex(dx, dy, dz, base, edge)]) < 0
                    || (c0 = map[vertexMapIndex(dx, dy, dz, base + 1, edge)]) < 0
                    || (c1 = map[vertexMapIndex(dx, dy, dz, base + 2, edge)]) < 0
                    || (oppositeCorner = map[vertexMapIndex(dx, dy, dz, base + 3, edge)]) < 0) {
                    continue; //skip if any of the vertices are missing
                }

                IndexWriter buf = indices[this.levelRenderer.renderTypeForState(data.states[edge])];

                boolean water = false; //TODO: isWater(data.states[edge]);
                if (water) {
                    edges |= EDGE_DIR_BOTH << (edge << 1);
                }

                if ((edges & (EDGE_DIR_NEGATIVE << (edge << 1))) != 0) { //the face has the negative bit set
                    if ((edges & (EDGE_DIR_POSITIVE << (edge << 1))) != 0) { //the positive bit is set as well, output the face once before flipping
                        buf.appendQuad(oppositeCorner, c0, c1, provoking);
                    }

                    //flip the face around
                    int i = c0;
                    c0 = c1;
                    c1 = i;
                }

                buf.appendQuad(oppositeCorner, c0, c1, provoking);
            }
        }
    }
}
