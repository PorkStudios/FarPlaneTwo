/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.voxel.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.core.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.core.mode.voxel.VoxelData;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.core.mode.voxel.client.struct.VoxelLocalAttributes;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import java.util.Arrays;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.fp2.core.mode.voxel.VoxelConstants.*;

/**
 * Shared code for baking voxel geometry.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class VoxelBaker implements IRenderBaker<VoxelPos, VoxelTile, IndexedBakeOutput<VoxelGlobalAttributes, VoxelLocalAttributes>> {
    protected static int vertexMapIndex(int dx, int dy, int dz, int i, int edge) {
        int j = CONNECTION_INDICES[i];
        int ddx = dx + ((j >> 2) & 1);
        int ddy = dy + ((j >> 1) & 1);
        int ddz = dz + (j & 1);

        return ((ddx * VT_VERTS + ddy) * VT_VERTS + ddz) * EDGE_COUNT + edge;
    }

    @NonNull
    protected final WorldRenderer worldRenderer;
    @NonNull
    protected final TextureUVs textureUVs;

    @Override
    public Stream<VoxelPos> bakeOutputs(@NonNull VoxelPos srcPos) {
        int x = srcPos.x();
        int y = srcPos.y();
        int z = srcPos.z();
        int level = srcPos.level();

        VoxelPos[] arr = new VoxelPos[8];
        int i = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 0; dz++) {
                    arr[i++] = new VoxelPos(level, x + dx, y + dy, z + dz);
                }
            }
        }
        return Stream.of(arr);
    }

    @Override
    public Stream<VoxelPos> bakeInputs(@NonNull VoxelPos dstPos) {
        int x = dstPos.x();
        int y = dstPos.y();
        int z = dstPos.z();
        int level = dstPos.level();

        VoxelPos[] arr = new VoxelPos[8];
        int i = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    arr[i++] = new VoxelPos(level, x + dx, y + dy, z + dz);
                }
            }
        }
        return Stream.of(arr);
    }

    @Override
    public void bake(@NonNull VoxelPos pos, @NonNull VoxelTile[] srcs, @NonNull IndexedBakeOutput<VoxelGlobalAttributes, VoxelLocalAttributes> output) {
        if (srcs[0] == null) {
            return;
        }

        //write globals
        output.globals().put(new VoxelGlobalAttributes(pos.x(), pos.y(), pos.z(), pos.level()));

        ArrayAllocator<int[]> alloc = GlobalAllocators.ALLOC_INT.get();
        int[] map = alloc.atLeast(cb(VT_VERTS) * EDGE_COUNT);
        Arrays.fill(map, 0, cb(VT_VERTS) * EDGE_COUNT, -1);

        try {
            //step 1: write vertices for all source tiles, and assign indices
            this.writeVertices(srcs, pos.blockX(), pos.blockY(), pos.blockZ(), pos.level(), map, output.verts());

            //step 2: write indices to actually connect the vertices and build the mesh
            this.writeIndices(srcs[0], map, output.indices());
        } finally {
            alloc.release(map);
        }
    }

    protected void writeVertices(VoxelTile[] srcs, int blockX, int blockY, int blockZ, int level, int[] map, AttributeWriter<VoxelLocalAttributes> verts) {
        final VoxelData data = new VoxelData();
        final VoxelLocalAttributes attributes = new VoxelLocalAttributes();

        int indexCounter = 0;
        for (int i = 0; i < 8; i++) {
            VoxelTile src = srcs[i];
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

                        indexCounter = this.writeVertex(blockX, blockY, blockZ, level, dx + (((i >> 2) & 1) << VT_SHIFT), dy + (((i >> 1) & 1) << VT_SHIFT), dz + ((i & 1) << VT_SHIFT), data, verts, attributes, map, indexCounter);
                    }
                }
            }
        }
    }

    protected int writeVertex(int baseX, int baseY, int baseZ, int level, int x, int y, int z, VoxelData data, AttributeWriter<VoxelLocalAttributes> vertices, VoxelLocalAttributes attributes, int[] map, int indexCounter) {
        baseX += (x & VT_VOXELS) << level;
        baseY += (y & VT_VOXELS) << level;
        baseZ += (z & VT_VOXELS) << level;

        int baseMapIndex = ((x * VT_VERTS + y) * VT_VERTS + z) * 3;

        final int blockX = baseX + ((x & ~(x & VT_VOXELS)) << level);
        final int blockY = baseY + ((y & ~(y & VT_VOXELS)) << level);
        final int blockZ = baseZ + ((z & ~(z & VT_VOXELS)) << level);

        int blockLight = BlockWorldConstants.unpackBlockLight(data.light);
        int skyLight = BlockWorldConstants.unpackSkyLight(data.light);
        attributes.lightBlock = (byte) (blockLight << 4);
        attributes.lightSky = (byte) (skyLight << 4);

        attributes.posX = (byte) ((x << POS_FRACT_SHIFT) + data.x);
        attributes.posY = (byte) ((y << POS_FRACT_SHIFT) + data.y);
        attributes.posZ = (byte) ((z << POS_FRACT_SHIFT) + data.z);

        EDGES:
        for (int edge = 0; edge < EDGE_COUNT; edge++) {
            for (int j = 0; j < edge; j++) {
                if (data.states[j] == data.states[edge]) { //states match, don't duplicate vertex data for this edge
                    map[baseMapIndex + edge] = map[baseMapIndex + j];
                    continue EDGES;
                }
            }

            attributes.state = this.textureUVs.state2index(data.states[edge]);
            attributes.color = this.worldRenderer.tintFactorForStateInBiomeAtPos(data.states[edge], data.biome, blockX, blockY, blockZ);

            map[baseMapIndex + edge] = vertices.put(attributes);
        }
        return indexCounter;
    }

    protected void writeIndices(VoxelTile src, int[] map, IndexWriter[] indices) {
        final VoxelData data = new VoxelData();

        for (int j = 0; j < src.count(); j++) {
            int voxelPos = src.get(j, data);
            int dx = (voxelPos >> (VT_SHIFT << 1)) & VT_MASK;
            int dy = (voxelPos >> VT_SHIFT) & VT_MASK;
            int dz = voxelPos & VT_MASK;

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

                IndexWriter buf = indices[this.worldRenderer.renderTypeForState(data.states[edge])];

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
