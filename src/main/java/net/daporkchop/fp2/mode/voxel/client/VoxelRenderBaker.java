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

package net.daporkchop.fp2.mode.voxel.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.mode.common.client.IFarRenderBaker;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
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
import java.util.stream.Stream;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderBaker implements IFarRenderBaker<VoxelPos, VoxelPiece> {
    public static final Ref<SimpleRecycler<int[]>> MAP_RECYCLER = ThreadRef.soft(() -> new SimpleRecycler<int[]>() {
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

    public static final int VOXEL_VERTEX_SIZE = INT_SIZE //state
                                                + SHORT_SIZE //light
                                                + MEDIUM_SIZE //color
                                                + MEDIUM_SIZE //pos_low
                                                + MEDIUM_SIZE //pos_high
                                                + 1; //pad to 16 bytes

    protected static int vertexMapIndex(int dx, int dy, int dz, int i, int edge) {
        int j = CONNECTION_INDICES[i];
        int ddx = dx + ((j >> 2) & 1);
        int ddy = dy + ((j >> 1) & 1);
        int ddz = dz + (j & 1);

        return ((ddx * T_VERTS + ddy) * T_VERTS + ddz) * 3 + edge;
    }

    @Override
    public int indexType() {
        return GL_UNSIGNED_SHORT;
    }

    @Override
    public int passes() {
        return RENDER_TYPES;
    }

    @Override
    public int vertexAttributes() {
        return 5;
    }

    @Override
    public void assignVertexAttributes() {
        long offset = 0L;
        glVertexAttribIPointer(0, 1, GL_UNSIGNED_INT, VOXEL_VERTEX_SIZE, offset); //state
        glVertexAttribPointer(1, 2, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += INT_SIZE); //light
        glVertexAttribPointer(2, 3, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += SHORT_SIZE); //color
        glVertexAttribPointer(3, 3, GL_UNSIGNED_BYTE, false, VOXEL_VERTEX_SIZE, offset += MEDIUM_SIZE); //pos_low
        glVertexAttribPointer(4, 3, GL_UNSIGNED_BYTE, false, VOXEL_VERTEX_SIZE, offset += MEDIUM_SIZE); //pos_high
    }

    @Override
    public Stream<VoxelPos> bakeOutputs(@NonNull VoxelPos srcPos) {
        int x = srcPos.x();
        int y = srcPos.y();
        int z = srcPos.z();
        int level = srcPos.level();

        VoxelPos[] arr = new VoxelPos[8 + 27];
        int i = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 0; dz++) {
                    arr[i++] = new VoxelPos(x + dx, y + dy, z + dz, level);
                }
            }
        }
        for (int dx = -2; dx <= 0; dx++) {
            for (int dy = -2; dy <= 0; dy++) {
                for (int dz = -2; dz <= 0; dz++) {
                    arr[i++] = new VoxelPos((x << 1) + dx, (y << 1) + dy, (z << 1) + dz, level - 1);
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

        return Stream.of(
                //same level
                dstPos, new VoxelPos(x, y, z + 1, level),
                new VoxelPos(x, y + 1, z, level), new VoxelPos(x, y + 1, z + 1, level),
                new VoxelPos(x + 1, y, z, level), new VoxelPos(x + 1, y, z + 1, level),
                new VoxelPos(x + 1, y + 1, z, level), new VoxelPos(x + 1, y + 1, z + 1, level),
                //below level
                new VoxelPos(x >> 1, y >> 1, z >> 1, level + 1), new VoxelPos(x >> 1, y >> 1, (z >> 1) + 1, level + 1),
                new VoxelPos(x >> 1, (y >> 1) + 1, z >> 1, level + 1), new VoxelPos(x >> 1, (y >> 1) + 1, (z >> 1) + 1, level + 1),
                new VoxelPos((x >> 1) + 1, y >> 1, z >> 1, level + 1), new VoxelPos((x >> 1) + 1, y >> 1, (z >> 1) + 1, level + 1),
                new VoxelPos((x >> 1) + 1, (y >> 1) + 1, z >> 1, level + 1), new VoxelPos((x >> 1) + 1, (y >> 1) + 1, (z >> 1) + 1, level + 1));
    }

    @Override
    public int estimatedVerticesBufferCapacity() {
        return (T_VERTS * T_VERTS * T_VERTS) * VOXEL_VERTEX_SIZE;
    }

    @Override
    public int estimatedIndicesBufferCapacity() {
        return (T_VERTS * T_VERTS * T_VERTS) * INT_SIZE * CONNECTION_INDEX_COUNT * 3;
    }

    @Override
    public void bake(@NonNull VoxelPos dstPos, @NonNull VoxelPiece[] srcs, @NonNull ByteBuf vertices, @NonNull ByteBuf[] indices) {
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
            for (int dx = 0; dx < T_VOXELS; dx++) { //center
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[0].get(dx, dy, dz, data)) {
                            continue;
                        }

                        indexCounter = this.vertex(baseX, baseY, baseZ, level, 0, srcs, dx, dy, dz, data, vertices, pos, biomeAccess, map, indexCounter);
                    }
                }
            }

            if (srcs[1] != null) { //+z
                for (int dx = 0; dx < T_VOXELS; dx++) { //center
                    for (int dy = 0; dy < T_VOXELS; dy++) {
                        if (!srcs[1].get(dx, dy, 0, data)) {
                            continue;
                        }

                        indexCounter = this.vertex(baseX, baseY, baseZ, level, 1, srcs, dx, dy, T_VOXELS, data, vertices, pos, biomeAccess, map, indexCounter);
                    }
                }
            }

            if (srcs[2] != null) { //+y
                for (int dx = 0; dx < T_VOXELS; dx++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[2].get(dx, 0, dz, data)) {
                            continue;
                        }

                        indexCounter = this.vertex(baseX, baseY, baseZ, level, 2, srcs, dx, T_VOXELS, dz, data, vertices, pos, biomeAccess, map, indexCounter);
                    }
                }
            }

            if (srcs[3] != null) { //+y,+z
                for (int dx = 0; dx < T_VOXELS; dx++) {
                    if (!srcs[3].get(dx, 0, 0, data)) {
                        continue;
                    }

                    indexCounter = this.vertex(baseX, baseY, baseZ, level, 3, srcs, dx, T_VOXELS, T_VOXELS, data, vertices, pos, biomeAccess, map, indexCounter);
                }
            }

            if (srcs[4] != null) { //+x
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[4].get(0, dy, dz, data)) {
                            continue;
                        }

                        indexCounter = this.vertex(baseX, baseY, baseZ, level, 4, srcs, T_VOXELS, dy, dz, data, vertices, pos, biomeAccess, map, indexCounter);
                    }
                }
            }

            if (srcs[5] != null) { //+x,+z
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    if (!srcs[5].get(0, dy, 0, data)) {
                        continue;
                    }

                    indexCounter = this.vertex(baseX, baseY, baseZ, level, 5, srcs, T_VOXELS, dy, T_VOXELS, data, vertices, pos, biomeAccess, map, indexCounter);
                }
            }

            if (srcs[6] != null) { //+x,+y
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    if (!srcs[6].get(0, 0, dz, data)) {
                        continue;
                    }

                    indexCounter = this.vertex(baseX, baseY, baseZ, level, 6, srcs, T_VOXELS, T_VOXELS, dz, data, vertices, pos, biomeAccess, map, indexCounter);
                }
            }

            if (srcs[7] != null && srcs[7].get(0, 0, 0, data)) { //+x,+y,+z
                indexCounter = this.vertex(baseX, baseY, baseZ, level, 7, srcs, T_VOXELS, T_VOXELS, T_VOXELS, data, vertices, pos, biomeAccess, map, indexCounter);
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

        vertices.writeInt(TexUVs.STATEID_TO_INDEXID.get(data.states[0])); //state
        vertices.writeShort(Constants.packedLightTo8BitVec2(data.light)); //light
        vertices.writeMedium(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.states[0]), biomeAccess, pos, 0))); //color

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

            int highX = 0;
            int highY = 0;
            int highZ = 0;
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

            vertices.setInt(bufIndex, TexUVs.STATEID_TO_INDEXID.get(data.states[edge]));
            vertices.setMedium(bufIndex + 6, Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.states[edge]), biomeAccess, pos, 0))); //color
            map[baseMapIndex + edge] = indexCounter++;
        }
        return indexCounter;
    }
}
