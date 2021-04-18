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
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.daporkchop.fp2.util.datastructure.PointOctree3I;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.BlockType.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Shared code for baking voxel geometry.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class VoxelBake {
    protected static final Ref<SimpleRecycler<int[]>> MAP_RECYCLER = ThreadRef.soft(() -> new SimpleRecycler<int[]>() {
        @Override
        protected int[] allocate0() {
            int[] arr = new int[T_VERTS * T_VERTS * T_VERTS * 3];
            this.reset0(arr);
            return arr;
        }

        @Override
        protected void reset0(@NonNull int[] map) {
            Arrays.fill(map, -1);
        }
    });

    public final int VOXEL_VERTEX_STATE_OFFSET = 0;
    public final int VOXEL_VERTEX_LIGHT_OFFSET = VOXEL_VERTEX_STATE_OFFSET + INT_SIZE;
    public final int VOXEL_VERTEX_COLOR_OFFSET = VOXEL_VERTEX_LIGHT_OFFSET + SHORT_SIZE;
    public final int VOXEL_VERTEX_POS_LOW_OFFSET = VOXEL_VERTEX_COLOR_OFFSET + MEDIUM_SIZE;
    public final int VOXEL_VERTEX_POS_HIGH_OFFSET = VOXEL_VERTEX_POS_LOW_OFFSET + MEDIUM_SIZE;

    public final int VOXEL_VERTEX_SIZE = VOXEL_VERTEX_POS_HIGH_OFFSET + INT_SIZE;

    public void vertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao) {
        vao.attrI(buffer, 1, GL_UNSIGNED_INT, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_STATE_OFFSET, 0); //state
        vao.attrF(buffer, 2, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_LIGHT_OFFSET, 0); //light
        vao.attrF(buffer, 3, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_COLOR_OFFSET, 0); //color
        vao.attrF(buffer, 3, GL_UNSIGNED_BYTE, false, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_POS_LOW_OFFSET, 0); //pos_low
        vao.attrF(buffer, 4, GL_INT_2_10_10_10_REV, false, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_POS_HIGH_OFFSET, 0); //pos_high
    }

    protected static int vertexMapIndex(int dx, int dy, int dz, int i, int edge) {
        int j = CONNECTION_INDICES[i];
        int ddx = dx + ((j >> 2) & 1);
        int ddy = dy + ((j >> 1) & 1);
        int ddz = dz + (j & 1);

        return ((ddx * T_VERTS + ddy) * T_VERTS + ddz) * 3 + edge;
    }

    public void bakeForShaderDraw(@NonNull VoxelPos dstPos, @NonNull VoxelTile[] srcs, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        if (srcs[0] == null) {
            return;
        }

        final int level = dstPos.level();
        final int blockX = dstPos.blockX();
        final int blockY = dstPos.blockY();
        final int blockZ = dstPos.blockZ();

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();
        final VoxelData data = new VoxelData();

        final int[] map = MAP_RECYCLER.get().allocate();

        try {
            //step 1: build octree
            PointOctree3I octree = buildHighPointOctree(srcs, data, dstPos);

            //step 2: write vertices for all source tiles, and assign indices
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

                            indexCounter = vertex(blockX, blockY, blockZ, level, dx + (((i >> 2) & 1) << T_SHIFT), dy + (((i >> 1) & 1) << T_SHIFT), dz + ((i & 1) << T_SHIFT), data, verts, pos, biomeAccess, map, indexCounter, octree);
                        }
                    }
                }
            }

            //step 3: write indices to actually connect the vertices and build the mesh
            for (int j = 0; j < srcs[0].count(); j++) {
                int voxelPos = srcs[0].get(j, data);
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

                    IBlockState state = Block.getStateById(data.states[edge]);
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
        } finally {
            MAP_RECYCLER.get().release(map);
        }
    }

    protected PointOctree3I buildHighPointOctree(VoxelTile[] srcs, VoxelData data, VoxelPos pos) {
        IntList highPoints = new IntArrayList();

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

                        if (px >= Int2_10_10_10_Rev.MIN_AXIS_VALUE && px <= Int2_10_10_10_Rev.MAX_AXIS_VALUE
                            && py >= Int2_10_10_10_Rev.MIN_AXIS_VALUE && py <= Int2_10_10_10_Rev.MAX_AXIS_VALUE
                            && pz >= Int2_10_10_10_Rev.MIN_AXIS_VALUE && pz <= Int2_10_10_10_Rev.MAX_AXIS_VALUE) { //this will only discard a very small minority of vertices
                            highPoints.add(Int2_10_10_10_Rev.packCoords(px, py, pz));
                        }
                    }
                }
            }
        }

        return new PointOctree3I(highPoints.toIntArray());
    }

    protected int vertex(int baseX, int baseY, int baseZ, int level, int x, int y, int z, VoxelData data, ByteBuf vertices, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, int[] map, int indexCounter, PointOctree3I octree) {
        baseX += (x & T_VOXELS) << level;
        baseY += (y & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        int baseMapIndex = ((x * T_VERTS + y) * T_VERTS + z) * 3;

        final int blockX = baseX + ((x & ~(x & T_VOXELS)) << level);
        final int blockY = baseY + ((y & ~(y & T_VOXELS)) << level);
        final int blockZ = baseZ + ((z & ~(z & T_VOXELS)) << level);

        pos.setPos(blockX, blockY, blockZ);
        biomeAccess.biome(Biome.getBiome(data.biome, Biomes.PLAINS));

        vertices.writeIntLE(TexUVs.STATEID_TO_INDEXID.get(Block.getStateById(data.states[0]))); //state
        vertices.writeShortLE(Constants.packedLightTo8BitVec2(data.light)); //light
        vertices.writeMediumLE(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.states[0]), biomeAccess, pos, 0))); //color

        int offset = level == 0 ? POS_ONE >> 1 : 0;

        int lowX = (x << POS_FRACT_SHIFT) + data.x + offset;
        int lowY = (y << POS_FRACT_SHIFT) + data.y + offset;
        int lowZ = (z << POS_FRACT_SHIFT) + data.z + offset;
        int highX = lowX;
        int highY = lowY;
        int highZ = lowZ;

        int closestHighPoint = octree.nearestNeighbor(highX, highY, highZ);
        if (closestHighPoint >= 0) {
            highX = Int2_10_10_10_Rev.unpackX(closestHighPoint);
            highY = Int2_10_10_10_Rev.unpackY(closestHighPoint);
            highZ = Int2_10_10_10_Rev.unpackZ(closestHighPoint);
        }

        vertices.writeByte(lowX).writeByte(lowY).writeByte(lowZ); //pos_low
        vertices.writeIntLE(Int2_10_10_10_Rev.packCoords(highX, highY, highZ)); //pos_high

        EDGES:
        for (int edge = 0; edge < EDGE_COUNT; edge++) {
            int bufIndex;
            if (edge == 0) {
                bufIndex = vertices.writerIndex() - VOXEL_VERTEX_SIZE;
            } else {
                for (int j = 0; j < edge; j++) {
                    if (data.states[j] == data.states[edge]) { //states match, don't duplicate vertex data for this edge
                        map[baseMapIndex + edge] = map[baseMapIndex + j];
                        continue EDGES;
                    }
                }
                bufIndex = vertices.writerIndex();
                vertices.writeBytes(vertices, bufIndex - VOXEL_VERTEX_SIZE, VOXEL_VERTEX_SIZE);
            }

            vertices.setIntLE(bufIndex, TexUVs.STATEID_TO_INDEXID.get(Block.getStateById(data.states[edge])));
            vertices.setMediumLE(bufIndex + 6, Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.states[edge]), biomeAccess, pos, 0))); //color
            map[baseMapIndex + edge] = indexCounter++;
        }
        return indexCounter;
    }
}
