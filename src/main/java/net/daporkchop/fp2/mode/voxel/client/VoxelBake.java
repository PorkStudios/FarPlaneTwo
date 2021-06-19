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

package net.daporkchop.fp2.mode.voxel.client;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.type.Int2_10_10_10_Rev;
import net.daporkchop.fp2.client.gl.vertex.IVertexAttribute;
import net.daporkchop.fp2.client.gl.vertex.VertexAttributeInterpretation;
import net.daporkchop.fp2.client.gl.vertex.VertexAttributeType;
import net.daporkchop.fp2.client.gl.vertex.VertexFormat;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.daporkchop.fp2.util.datastructure.PointOctree3I;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.GLCompatibilityHelper.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.BlockType.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * Shared code for baking voxel geometry.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class VoxelBake {
    protected static final IVertexAttribute.Int1 ATTRIB_STATE = IVertexAttribute.Int1.builder()
            .alignAndPadTo(EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT)
            .type(VertexAttributeType.UNSIGNED_INT)
            .interpretation(VertexAttributeInterpretation.INTEGER)
            .build();

    protected static final IVertexAttribute.Int2 ATTRIB_LIGHT = IVertexAttribute.Int2.builder(ATTRIB_STATE)
            .alignAndPadTo(EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT)
            .type(VertexAttributeType.UNSIGNED_BYTE)
            .interpretation(VertexAttributeInterpretation.NORMALIZED_FLOAT)
            .build();

    protected static final IVertexAttribute.Int3 ATTRIB_COLOR = IVertexAttribute.Int3.builder(ATTRIB_LIGHT)
            .alignAndPadTo(EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT)
            .reportedComponents(4)
            .type(VertexAttributeType.UNSIGNED_BYTE)
            .interpretation(VertexAttributeInterpretation.NORMALIZED_FLOAT)
            .build();

    protected static final IVertexAttribute.Int3 ATTRIB_POS_LOW = IVertexAttribute.Int3.builder(ATTRIB_COLOR)
            .alignAndPadTo(EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT)
            .reportedComponents(4)
            .type(VertexAttributeType.UNSIGNED_BYTE)
            .interpretation(VertexAttributeInterpretation.FLOAT)
            .build();

    protected static final IVertexAttribute.Int4 ATTRIB_POS_HIGH = IVertexAttribute.Int4.builder(ATTRIB_POS_LOW)
            .alignAndPadTo(EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT)
            .type(WORKAROUND_AMD_INT_2_10_10_10_REV ? VertexAttributeType.SHORT : VertexAttributeType.INT_2_10_10_10_REV)
            .interpretation(VertexAttributeInterpretation.FLOAT)
            .build();

    protected static final VertexFormat VERTEX_FORMAT = new VertexFormat(ATTRIB_POS_HIGH, max(EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT, INT_SIZE));

    public void vertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao) {
        FP2_LOG.info("voxel vertex size: {} bytes", VERTEX_FORMAT.size());
        VERTEX_FORMAT.configureVAO(vao, buffer);
    }

    protected static int vertexMapIndex(int dx, int dy, int dz, int i, int edge) {
        int j = CONNECTION_INDICES[i];
        int ddx = dx + ((j >> 2) & 1);
        int ddy = dy + ((j >> 1) & 1);
        int ddz = dz + (j & 1);

        return ((ddx * T_VERTS + ddy) * T_VERTS + ddz) * EDGE_COUNT + edge;
    }

    public void bakeForShaderDraw(@NonNull VoxelPos dstPos, @NonNull VoxelTile[] srcs, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        if (srcs[0] == null) {
            return;
        }

        final int level = dstPos.level();
        final int blockX = dstPos.blockX();
        final int blockY = dstPos.blockY();
        final int blockZ = dstPos.blockZ();

        ArrayAllocator<int[]> alloc = ALLOC_INT.get();
        int[] map = alloc.atLeast(cb(T_VERTS) * EDGE_COUNT);
        Arrays.fill(map, 0, cb(T_VERTS) * EDGE_COUNT, -1);

        try {
            //step 1: build octrees
            PointOctree3I lowOctree = buildLowPointOctree(srcs);
            PointOctree3I highOctree = buildHighPointOctree(srcs, dstPos);

            //step 2: write vertices for all source tiles, and assign indices
            writeVertices(srcs, blockX, blockY, blockZ, level, lowOctree, highOctree, map, verts);

            //step 3: write indices to actually connect the vertices and build the mesh
            writeIndices(srcs[0], map, indices, lowOctree);
        } finally {
            alloc.release(map);
        }
    }

    protected PointOctree3I buildLowPointOctree(VoxelTile[] srcs) {
        if (true) { //TODO: low octree isn't actually used atm
            return null;
        }

        final VoxelData data = new VoxelData();
        final IntList highPoints = new IntArrayList();

        for (int i = 0, tx = 0; tx <= 1; tx++) {
            for (int ty = 0; ty <= 1; ty++) {
                for (int tz = 0; tz <= 1; tz++) {
                    VoxelTile tile = srcs[i++];
                    if (tile == null) {
                        continue;
                    }

                    for (int j = 0; j < tile.count(); j++) {
                        int voxelPos = tile.getOnlyPos(j, data);
                        int dx = (voxelPos >> (T_SHIFT << 1)) & T_MASK;
                        int dy = (voxelPos >> T_SHIFT) & T_MASK;
                        int dz = voxelPos & T_MASK;

                        int px = (tx << (T_SHIFT + POS_FRACT_SHIFT)) + (dx << POS_FRACT_SHIFT) + data.x;
                        int py = (ty << (T_SHIFT + POS_FRACT_SHIFT)) + (dy << POS_FRACT_SHIFT) + data.y;
                        int pz = (tz << (T_SHIFT + POS_FRACT_SHIFT)) + (dz << POS_FRACT_SHIFT) + data.z;

                        if (px >= Int2_10_10_10_Rev.MIN_XYZ_VALUE && px <= Int2_10_10_10_Rev.MAX_XYZ_VALUE
                            && py >= Int2_10_10_10_Rev.MIN_XYZ_VALUE && py <= Int2_10_10_10_Rev.MAX_XYZ_VALUE
                            && pz >= Int2_10_10_10_Rev.MIN_XYZ_VALUE && pz <= Int2_10_10_10_Rev.MAX_XYZ_VALUE) { //this will only discard a very small minority of vertices
                            highPoints.add(Int2_10_10_10_Rev.packXYZ(px, py, pz));
                        }
                    }
                }
            }
        }

        return new PointOctree3I(highPoints.toIntArray());
    }

    protected PointOctree3I buildHighPointOctree(VoxelTile[] srcs, VoxelPos pos) {
        final VoxelData data = new VoxelData();
        final IntList highPoints = new IntArrayList();

        int offX = -(pos.x() & 1) << (T_SHIFT + POS_FRACT_SHIFT);
        int offY = -(pos.y() & 1) << (T_SHIFT + POS_FRACT_SHIFT);
        int offZ = -(pos.z() & 1) << (T_SHIFT + POS_FRACT_SHIFT);

        for (int i = 8, tx = BAKE_HIGH_RADIUS_MIN; tx <= BAKE_HIGH_RADIUS_MAX; tx++) {
            for (int ty = BAKE_HIGH_RADIUS_MIN; ty <= BAKE_HIGH_RADIUS_MAX; ty++) {
                for (int tz = BAKE_HIGH_RADIUS_MIN; tz <= BAKE_HIGH_RADIUS_MAX; tz++) {
                    VoxelTile tile = srcs[i++];
                    if (tile == null) {
                        continue;
                    }

                    for (int j = 0; j < tile.count(); j++) {
                        int voxelPos = tile.getOnlyPos(j, data);
                        int dx = (voxelPos >> (T_SHIFT << 1)) & T_MASK;
                        int dy = (voxelPos >> T_SHIFT) & T_MASK;
                        int dz = voxelPos & T_MASK;

                        int px = (tx << (T_SHIFT + POS_FRACT_SHIFT + 1)) + (dx << (POS_FRACT_SHIFT + 1)) + (data.x << 1) + offX;
                        int py = (ty << (T_SHIFT + POS_FRACT_SHIFT + 1)) + (dy << (POS_FRACT_SHIFT + 1)) + (data.y << 1) + offY;
                        int pz = (tz << (T_SHIFT + POS_FRACT_SHIFT + 1)) + (dz << (POS_FRACT_SHIFT + 1)) + (data.z << 1) + offZ;

                        if (px >= Int2_10_10_10_Rev.MIN_XYZ_VALUE && px <= Int2_10_10_10_Rev.MAX_XYZ_VALUE
                            && py >= Int2_10_10_10_Rev.MIN_XYZ_VALUE && py <= Int2_10_10_10_Rev.MAX_XYZ_VALUE
                            && pz >= Int2_10_10_10_Rev.MIN_XYZ_VALUE && pz <= Int2_10_10_10_Rev.MAX_XYZ_VALUE) { //this will only discard a very small minority of vertices
                            highPoints.add(Int2_10_10_10_Rev.packXYZ(px, py, pz));
                        }
                    }
                }
            }
        }

        return new PointOctree3I(highPoints.toIntArray());
    }

    protected void writeVertices(VoxelTile[] srcs, int blockX, int blockY, int blockZ, int level, PointOctree3I lowOctree, PointOctree3I highOctree, int[] map, ByteBuf verts) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();
        final VoxelData data = new VoxelData();

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

                        indexCounter = writeVertex(blockX, blockY, blockZ, level, dx + (((i >> 2) & 1) << T_SHIFT), dy + (((i >> 1) & 1) << T_SHIFT), dz + ((i & 1) << T_SHIFT), data, verts, pos, biomeAccess, map, indexCounter, highOctree);
                    }
                }
            }
        }
    }

    protected int writeVertex(int baseX, int baseY, int baseZ, int level, int x, int y, int z, VoxelData data, ByteBuf vertices, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, int[] map, int indexCounter, PointOctree3I octree) {
        baseX += (x & T_VOXELS) << level;
        baseY += (y & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        int baseMapIndex = ((x * T_VERTS + y) * T_VERTS + z) * 3;

        final int blockX = baseX + ((x & ~(x & T_VOXELS)) << level);
        final int blockY = baseY + ((y & ~(y & T_VOXELS)) << level);
        final int blockZ = baseZ + ((z & ~(z & T_VOXELS)) << level);

        pos.setPos(blockX, blockY, blockZ);
        biomeAccess.biome(FastRegistry.getBiome(data.biome, Biomes.PLAINS));

        int vertexBase = VERTEX_FORMAT.appendVertex(vertices);

        IBlockState state = FastRegistry.getBlockState(data.states[0]);
        ATTRIB_STATE.set(vertices, vertexBase, TexUVs.STATEID_TO_INDEXID.get(state));

        int blockLight = data.light & 0xF;
        int skyLight = data.light >> 4;
        ATTRIB_LIGHT.set(vertices, vertexBase, blockLight | (blockLight << 4), skyLight | (skyLight << 4));
        ATTRIB_COLOR.setRGB(vertices, vertexBase, mc.getBlockColors().colorMultiplier(state, biomeAccess, pos, 0));

        int lowX = (x << POS_FRACT_SHIFT) + data.x;
        int lowY = (y << POS_FRACT_SHIFT) + data.y;
        int lowZ = (z << POS_FRACT_SHIFT) + data.z;

        int posHigh = octree.nearestNeighbor(lowX, lowY, lowZ);
        if (posHigh < 0) {
            posHigh = Int2_10_10_10_Rev.packXYZ(lowX, lowY, lowZ);
        }

        ATTRIB_POS_LOW.set(vertices, vertexBase, lowX, lowY, lowZ);
        ATTRIB_POS_HIGH.setInt2_10_10_10_rev(vertices, vertexBase, posHigh);

        EDGES:
        for (int edge = 0; edge < EDGE_COUNT; edge++) {
            int currVertexBase;
            if (edge == 0) {
                currVertexBase = vertexBase;
            } else {
                for (int j = 0; j < edge; j++) {
                    if (data.states[j] == data.states[edge]) { //states match, don't duplicate vertex data for this edge
                        map[baseMapIndex + edge] = map[baseMapIndex + j];
                        continue EDGES;
                    }
                }
                currVertexBase = VERTEX_FORMAT.duplicateVertex(vertices, vertexBase);
            }

            IBlockState edgeState = FastRegistry.getBlockState(data.states[edge]);
            ATTRIB_STATE.set(vertices, currVertexBase, TexUVs.STATEID_TO_INDEXID.get(edgeState));
            ATTRIB_COLOR.setRGB(vertices, currVertexBase, mc.getBlockColors().colorMultiplier(edgeState, biomeAccess, pos, 0));
            map[baseMapIndex + edge] = indexCounter++;
        }
        return indexCounter;
    }

    protected void writeIndices(VoxelTile src, int[] map, ByteBuf[] indices, PointOctree3I lowOctree) {
        final VoxelData data = new VoxelData();

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

                IBlockState state = FastRegistry.getBlockState(data.states[edge]);
                ByteBuf buf = indices[renderType(state)];

                boolean water = state.getBlock() == Blocks.WATER;
                if (water) {
                    edges |= EDGE_DIR_BOTH << (edge << 1);
                }

                if ((edges & (EDGE_DIR_NEGATIVE << (edge << 1))) != 0) { //the face has the negative bit set
                    if ((edges & (EDGE_DIR_POSITIVE << (edge << 1))) != 0) { //the positive bit is set as well, output the face once before flipping
                        emitQuad(buf, oppositeCorner, c0, c1, provoking);
                    }

                    //flip the face around
                    int i = c0;
                    c0 = c1;
                    c1 = i;
                }

                emitQuad(buf, oppositeCorner, c0, c1, provoking);
            }
        }
    }
}
