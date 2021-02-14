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
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelData;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
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
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;

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

    public final int VOXEL_VERTEX_SIZE = VOXEL_VERTEX_POS_HIGH_OFFSET + MEDIUM_SIZE + 1; // +1 in order to pad to 16 bytes

    public void vertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao) {
        vao.attrI(buffer, 1, GL_UNSIGNED_INT, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_STATE_OFFSET, 0); //state
        vao.attrF(buffer, 2, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_LIGHT_OFFSET, 0); //light
        vao.attrF(buffer, 3, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_COLOR_OFFSET, 0); //color
        vao.attrF(buffer, 3, GL_UNSIGNED_BYTE, false, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_POS_LOW_OFFSET, 0); //pos_low
        vao.attrF(buffer, 3, GL_UNSIGNED_BYTE, false, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_POS_HIGH_OFFSET, 0); //pos_high
    }

    protected static int vertexMapIndex(int dx, int dy, int dz, int i, int edge) {
        int j = CONNECTION_INDICES[i];
        int ddx = dx + ((j >> 2) & 1);
        int ddy = dy + ((j >> 1) & 1);
        int ddz = dz + (j & 1);

        return ((ddx * T_VERTS + ddy) * T_VERTS + ddz) * 3 + edge;
    }

    public void bakeForShaderDraw(@NonNull VoxelPos dstPos, @NonNull VoxelPiece[] srcs, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        if (srcs[0] == null) {
            return;
        }

        final int level = dstPos.level();
        final int baseX = dstPos.blockX();
        final int baseY = dstPos.blockY();
        final int baseZ = dstPos.blockZ();

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();
        final VoxelData data = new VoxelData();

        final int[] map = MAP_RECYCLER.get().allocate();

        try {
            //step 1: simply write vertices for all source pieces, and assign indices
            int indexCounter = 0;
            for (int i = 0; i < 8; i++) {
                VoxelPiece src = srcs[i];
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

                            indexCounter = vertex(baseX, baseY, baseZ, level, i, srcs, dx + (((i >> 2) & 1) << T_SHIFT), dy + (((i >> 1) & 1) << T_SHIFT), dz + ((i & 1) << T_SHIFT), data, verts, pos, biomeAccess, map, indexCounter);
                        }
                    }
                }
            }

            //step 2: write indices to actually connect the vertices and build the mesh
            for (int dx = 0; dx < T_VOXELS; dx++) {
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[0].get(dx, dy, dz, data)) {
                            continue;
                        }

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
                }
            }
        } finally {
            MAP_RECYCLER.get().release(map);
        }
    }

    protected int vertex(int baseX, int baseY, int baseZ, int level, int i, VoxelPiece[] srcs, int x, int y, int z, VoxelData data, ByteBuf vertices, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, int[] map, int indexCounter) {
        baseX += (x & T_VOXELS) << level;
        baseY += (y & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        int baseMapIndex = ((x * T_VERTS + y) * T_VERTS + z) * 3;

        final int blockX = baseX + ((x & ~(x & T_VOXELS)) << level);
        final int blockY = baseY + ((y & ~(y & T_VOXELS)) << level);
        final int blockZ = baseZ + ((z & ~(z & T_VOXELS)) << level);

        pos.setPos(blockX, blockY, blockZ);
        biomeAccess.biome(Biome.getBiome(data.biome, Biomes.PLAINS));

        vertices.writeIntLE(TexUVs.STATEID_TO_INDEXID.get(data.states[0])); //state
        vertices.writeShortLE(Constants.packedLightTo8BitVec2(data.light)); //light
        vertices.writeMediumLE(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.states[0]), biomeAccess, pos, 0))); //color

        final int offset = level == 0 ? POS_ONE >> 1 : 0;

        vertices.writeByte((x << POS_FRACT_SHIFT) + data.x + offset)
                .writeByte((y << POS_FRACT_SHIFT) + data.y + offset)
                .writeByte((z << POS_FRACT_SHIFT) + data.z + offset); //pos_low

        int basePieceX = (baseX >> (level + T_SHIFT)) - ((i >> 2) & 1);
        int basePieceY = (baseY >> (level + T_SHIFT)) - ((i >> 1) & 1);
        int basePieceZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        VoxelPiece highPiece = srcs[8 | (i & (((basePieceX & 1) << 2) | ((basePieceY & 1) << 1) | (basePieceZ & 1)))];
        if (highPiece == null) { //pos_high
            vertices.writeByte((x << POS_FRACT_SHIFT) + data.x + offset)
                    .writeByte((y << POS_FRACT_SHIFT) + data.y + offset)
                    .writeByte((z << POS_FRACT_SHIFT) + data.z + offset);
        } else {
            final int flooredX = blockX & -(1 << (level + 1));
            final int flooredY = blockY & -(1 << (level + 1));
            final int flooredZ = blockZ & -(1 << (level + 1));

            int highX = POS_ONE;
            int highY = POS_ONE;
            int highZ = POS_ONE;
            if (highPiece.getOnlyPos((flooredX >> (level + 1)) & T_MASK, (flooredY >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK, data)) {
                highX = data.x << 1;
                highY = data.y << 1;
                highZ = data.z << 1;
            }

            vertices.writeByte(((x & ~1) << POS_FRACT_SHIFT) + highX)
                    .writeByte(((y & ~1) << POS_FRACT_SHIFT) + highY)
                    .writeByte(((z & ~1) << POS_FRACT_SHIFT) + highZ);
        }
        vertices.writeByte(0); //pad to 16 bytes

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

            vertices.setIntLE(bufIndex, TexUVs.STATEID_TO_INDEXID.get(data.states[edge]));
            vertices.setMediumLE(bufIndex + 6, Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.states[edge]), biomeAccess, pos, 0))); //color
            map[baseMapIndex + edge] = indexCounter++;
        }
        return indexCounter;
    }
}
