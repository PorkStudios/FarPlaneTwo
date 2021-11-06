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
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.vertex.attribute.IVertexAttribute;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexAttributeInterpretation;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexAttributeType;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuilder;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.mode.common.client.RenderConstants;
import net.daporkchop.fp2.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.BlockType.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * Shared code for baking voxel geometry.
 *
 * @author DaPorkchop_
 */
public class VoxelBaker implements IRenderBaker<VoxelPos, VoxelTile, IndexedBakeOutput> {
    protected static final IVertexAttribute.Int1 ATTRIB_STATE = IVertexAttribute.Int1.builder()
            .name("state")
            .type(VertexAttributeType.UNSIGNED_INT)
            .interpretation(VertexAttributeInterpretation.INTEGER)
            .build();

    protected static final IVertexAttribute.Int2 ATTRIB_LIGHT = IVertexAttribute.Int2.builder(ATTRIB_STATE)
            .name("light")
            .type(VertexAttributeType.UNSIGNED_BYTE)
            .interpretation(VertexAttributeInterpretation.NORMALIZED_FLOAT)
            .build();

    protected static final IVertexAttribute.Int3 ATTRIB_COLOR = IVertexAttribute.Int3.builder(ATTRIB_LIGHT)
            .name("color")
            .type(VertexAttributeType.UNSIGNED_BYTE)
            .interpretation(VertexAttributeInterpretation.NORMALIZED_FLOAT)
            .build();

    protected static final IVertexAttribute.Int3 ATTRIB_POS = IVertexAttribute.Int3.builder(ATTRIB_COLOR)
            .name("pos")
            .type(VertexAttributeType.UNSIGNED_BYTE)
            .interpretation(VertexAttributeInterpretation.FLOAT)
            .build();

    protected static final VertexFormat VERTEX_FORMAT = new VertexFormat("voxel", ATTRIB_POS);

    protected static int vertexMapIndex(int dx, int dy, int dz, int i, int edge) {
        int j = CONNECTION_INDICES[i];
        int ddx = dx + ((j >> 2) & 1);
        int ddy = dy + ((j >> 1) & 1);
        int ddz = dz + (j & 1);

        return ((ddx * T_VERTS + ddy) * T_VERTS + ddz) * EDGE_COUNT + edge;
    }

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
    public void bake(@NonNull VoxelPos pos, @NonNull VoxelTile[] srcs, @NonNull IndexedBakeOutput output) {
        if (srcs[0] == null) {
            return;
        }

        ArrayAllocator<int[]> alloc = ALLOC_INT.get();
        int[] map = alloc.atLeast(cb(T_VERTS) * EDGE_COUNT);
        Arrays.fill(map, 0, cb(T_VERTS) * EDGE_COUNT, -1);

        try {
            //step 1: write vertices for all source tiles, and assign indices
            this.writeVertices(srcs, pos.blockX(), pos.blockY(), pos.blockZ(), pos.level(), map, /* TODO: output.verts() */ null);

            //step 2: write indices to actually connect the vertices and build the mesh
            this.writeIndices(srcs[0], map, /* TODO: output.indices() */ null);
        } finally {
            alloc.release(map);
        }
    }

    protected void writeVertices(VoxelTile[] srcs, int blockX, int blockY, int blockZ, int level, int[] map, IVertexBuilder verts) {
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

                        indexCounter = this.writeVertex(blockX, blockY, blockZ, level, dx + (((i >> 2) & 1) << T_SHIFT), dy + (((i >> 1) & 1) << T_SHIFT), dz + ((i & 1) << T_SHIFT), data, verts, pos, biomeAccess, map, indexCounter);
                    }
                }
            }
        }
    }

    protected int writeVertex(int baseX, int baseY, int baseZ, int level, int x, int y, int z, VoxelData data, IVertexBuilder vertices, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, int[] map, int indexCounter) {
        baseX += (x & T_VOXELS) << level;
        baseY += (y & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        int baseMapIndex = ((x * T_VERTS + y) * T_VERTS + z) * 3;

        final int blockX = baseX + ((x & ~(x & T_VOXELS)) << level);
        final int blockY = baseY + ((y & ~(y & T_VOXELS)) << level);
        final int blockZ = baseZ + ((z & ~(z & T_VOXELS)) << level);

        pos.setPos(blockX, blockY, blockZ);
        biomeAccess.biome(FastRegistry.getBiome(data.biome, Biomes.PLAINS));

        int vertexBase = vertices.appendVertex();

        IBlockState state = FastRegistry.getBlockState(data.states[0]);
        ATTRIB_STATE.set(vertices, vertexBase, TexUVs.STATEID_TO_INDEXID.get(state));

        int blockLight = data.light & 0xF;
        int skyLight = data.light >> 4;
        ATTRIB_LIGHT.set(vertices, vertexBase, blockLight | (blockLight << 4), skyLight | (skyLight << 4));
        ATTRIB_COLOR.setRGB(vertices, vertexBase, MC.getBlockColors().colorMultiplier(state, biomeAccess, pos, 0));

        int lowX = (x << POS_FRACT_SHIFT) + data.x;
        int lowY = (y << POS_FRACT_SHIFT) + data.y;
        int lowZ = (z << POS_FRACT_SHIFT) + data.z;

        ATTRIB_POS.set(vertices, vertexBase, lowX, lowY, lowZ);

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
                currVertexBase = vertices.appendDuplicateVertex(vertexBase);
            }

            IBlockState edgeState = FastRegistry.getBlockState(data.states[edge]);
            ATTRIB_STATE.set(vertices, currVertexBase, TexUVs.STATEID_TO_INDEXID.get(edgeState));
            ATTRIB_COLOR.setRGB(vertices, currVertexBase, MC.getBlockColors().colorMultiplier(edgeState, biomeAccess, pos, 0));
            map[baseMapIndex + edge] = indexCounter++;
        }
        return indexCounter;
    }

    protected void writeIndices(VoxelTile src, int[] map, ByteBuf[] indices) {
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
                        RenderConstants.emitQuad(buf, oppositeCorner, c0, c1, provoking);
                    }

                    //flip the face around
                    int i = c0;
                    c0 = c1;
                    c1 = i;
                }

                RenderConstants.emitQuad(buf, oppositeCorner, c0, c1, provoking);
            }
        }
    }
}
