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

package net.daporkchop.fp2.strategy.voxel.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.strategy.base.client.IFarRenderBaker;
import net.daporkchop.fp2.strategy.voxel.VoxelData;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.primitive.map.IntIntMap;
import net.daporkchop.lib.primitive.map.open.IntIntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.stream.Stream;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.strategy.voxel.server.gen.VoxelGeneratorConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL41.*;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderBaker implements IFarRenderBaker<VoxelPos, VoxelPiece> {
    public static final Ref<SimpleRecycler<IntIntMap>> MAP_RECYCLER = ThreadRef.soft(() -> new SimpleRecycler<IntIntMap>() {
        @Override
        protected IntIntMap allocate0() {
            return new IntIntOpenHashMap();
        }

        @Override
        protected void reset0(@NonNull IntIntMap map) {
            map.clear();
        }
    });

    public static final int VOXEL_VERTEX_SIZE = INT_SIZE //state
                                                + SHORT_SIZE //light
                                                + MEDIUM_SIZE //color
                                                + DVEC3_SIZE_TIGHT //pos_low
                                                + DVEC3_SIZE_TIGHT //pos_high
                                                + SHORT_SIZE; //level_scale

    @Override
    public int indexType() {
        return GL_UNSIGNED_SHORT;
    }

    @Override
    public int vertexAttributes() {
        return 6;
    }

    @Override
    public void assignVertexAttributes() {
        long offset = 0L;
        glVertexAttribIPointer(0, 1, GL_UNSIGNED_INT, VOXEL_VERTEX_SIZE, offset); //state
        glVertexAttribPointer(1, 2, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += INT_SIZE); //light
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += SHORT_SIZE); //color
        glVertexAttribLPointer(3, 3, VOXEL_VERTEX_SIZE, offset += MEDIUM_SIZE); //pos_low
        glVertexAttribLPointer(4, 3, VOXEL_VERTEX_SIZE, offset += DVEC3_SIZE_TIGHT); //pos_high
        glVertexAttribPointer(5, 1, GL_UNSIGNED_SHORT, false, VOXEL_VERTEX_SIZE, offset += DVEC3_SIZE_TIGHT); //level_scale
    }

    @Override
    public Stream<VoxelPos> bakeOutputs(@NonNull VoxelPos srcPos) {
        int x = srcPos.x();
        int y = srcPos.y();
        int z = srcPos.z();
        int level = srcPos.level();

        /*return Stream.of(
                //same level
                srcPos, new VoxelPos(x, y, z - 1, level),
                new VoxelPos(x, y - 1, z, level), new VoxelPos(x, y - 1, z - 1, level),
                new VoxelPos(x - 1, y, z, level), new VoxelPos(x - 1, y, z - 1, level),
                new VoxelPos(x - 1, y - 1, z, level), new VoxelPos(x - 1, y - 1, z - 1, level),
                //below level
                new VoxelPos(x << 1, y << 1, z << 1, level - 1), new VoxelPos(x << 1, y << 1, (z << 1) - 1, level - 1),
                new VoxelPos(x << 1, (y << 1) - 1, z << 1, level - 1), new VoxelPos(x << 1, (y << 1) - 1, (z << 1) - 1, level - 1),
                new VoxelPos((x << 1) - 1, y << 1, z << 1, level - 1), new VoxelPos((x << 1) - 1, y << 1, (z << 1) - 1, level - 1),
                new VoxelPos((x << 1) - 1, (y << 1) - 1, z << 1, level - 1), new VoxelPos((x << 1) - 1, (y << 1) - 1, (z << 1) - 1, level - 1));*/

        VoxelPos[] arr = new VoxelPos[8 + 27];
        int i = 0;
        for (int dx = -1; dx <= 0; dx++)    {
            for (int dy = -1; dy <= 0; dy++)    {
                for (int dz = -1; dz <= 0; dz++)    {
                    arr[i++] = new VoxelPos(x + dx, y + dy, z + dz, level);
                }
            }
        }
        for (int dx = -2; dx <= 0; dx++)    {
            for (int dy = -2; dy <= 0; dy++)    {
                for (int dz = -2; dz <= 0; dz++)    {
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
    public void bake(@NonNull VoxelPos dstPos, @NonNull VoxelPiece[] srcs, @NonNull ByteBuf vertices, @NonNull ByteBuf indices) {
        if (srcs[0] == null || srcs[0].isBlank()) {
            return;
        }

        final int level = dstPos.level();
        final int baseX = dstPos.blockX();
        final int baseY = dstPos.blockY();
        final int baseZ = dstPos.blockZ();

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();
        final VoxelData data = new VoxelData();

        final IntIntMap map = MAP_RECYCLER.get().allocate();

        try {
            //step 1: simply write vertices for all source pieces, and assign indices
            int indexCounter = 0;
            for (int dx = 0; dx < T_VOXELS; dx++) { //center
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[0].get(dx, dy, dz, data)) {
                            continue;
                        }

                        map.put((dx * T_VERTS + dy) * T_VERTS + dz, indexCounter++);
                        this.vertex(baseX, baseY, baseZ, level, 0, srcs, dx, dy, dz, data, vertices, pos, biomeAccess);
                    }
                }
            }

            if (srcs[1] != null) { //+z
                for (int dx = 0; dx < T_VOXELS; dx++) { //center
                    for (int dy = 0; dy < T_VOXELS; dy++) {
                        if (!srcs[1].get(dx, dy, 0, data)) {
                            continue;
                        }

                        map.put((dx * T_VERTS + dy) * T_VERTS + T_VOXELS, indexCounter++);
                        this.vertex(baseX, baseY, baseZ, level, 1, srcs, dx, dy, T_VOXELS, data, vertices, pos, biomeAccess);
                    }
                }
            }

            if (srcs[2] != null) { //+y
                for (int dx = 0; dx < T_VOXELS; dx++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[2].get(dx, 0, dz, data)) {
                            continue;
                        }

                        map.put((dx * T_VERTS + T_VOXELS) * T_VERTS + dz, indexCounter++);
                        this.vertex(baseX, baseY, baseZ, level, 2, srcs, dx, T_VOXELS, dz, data, vertices, pos, biomeAccess);
                    }
                }
            }

            if (srcs[3] != null) { //+y,+z
                for (int dx = 0; dx < T_VOXELS; dx++) {
                    if (!srcs[3].get(dx, 0, 0, data)) {
                        continue;
                    }

                    map.put((dx * T_VERTS + T_VOXELS) * T_VERTS + T_VOXELS, indexCounter++);
                    this.vertex(baseX, baseY, baseZ, level, 3, srcs, dx, T_VOXELS, T_VOXELS, data, vertices, pos, biomeAccess);
                }
            }

            if (srcs[4] != null) { //+x
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[4].get(0, dy, dz, data)) {
                            continue;
                        }

                        map.put((T_VOXELS * T_VERTS + dy) * T_VERTS + dz, indexCounter++);
                        this.vertex(baseX, baseY, baseZ, level, 4, srcs, T_VOXELS, dy, dz, data, vertices, pos, biomeAccess);
                    }
                }
            }

            if (srcs[5] != null) { //+x,+z
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    if (!srcs[5].get(0, dy, 0, data)) {
                        continue;
                    }

                    map.put((T_VOXELS * T_VERTS + dy) * T_VERTS + T_VOXELS, indexCounter++);
                    this.vertex(baseX, baseY, baseZ, level, 5, srcs, T_VOXELS, dy, T_VOXELS, data, vertices, pos, biomeAccess);
                }
            }

            if (srcs[6] != null) { //+x,+y
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    if (!srcs[6].get(0, 0, dz, data)) {
                        continue;
                    }

                    map.put((T_VOXELS * T_VERTS + T_VOXELS) * T_VERTS + dz, indexCounter++);
                    this.vertex(baseX, baseY, baseZ, level, 6, srcs, T_VOXELS, T_VOXELS, dz, data, vertices, pos, biomeAccess);
                }
            }

            if (srcs[7] != null && srcs[7].get(0, 0, 0, data)) { //+x,+y,+z
                map.put((T_VOXELS * T_VERTS + T_VOXELS) * T_VERTS + T_VOXELS, indexCounter++);
                this.vertex(baseX, baseY, baseZ, level, 7, srcs, T_VOXELS, T_VOXELS, T_VOXELS, data, vertices, pos, biomeAccess);
            }

            //step 2: write indices to actually connect the vertices and build the mesh
            for (int dx = 0; dx < T_VOXELS; dx++) {
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        if (!srcs[0].get(dx, dy, dz, data)) {
                            continue;
                        }

                        for (int edge = 0; edge < 3; edge++) {
                            if ((data.edges & (1 << edge)) == 0) {
                                continue;
                            }

                            int writerIndex = indices.writerIndex();
                            for (int base = edge * CONNECTION_INDEX_COUNT, i = 0; i < CONNECTION_INDEX_COUNT; i++) {
                                int j = CONNECTION_INDICES[base + i];
                                int ddx = dx + ((j >> 2) & 1);
                                int ddy = dy + ((j >> 1) & 1);
                                int ddz = dz + (j & 1);

                                int index = map.getOrDefault((ddx * T_VERTS + ddy) * T_VERTS + ddz, -1);
                                if (index < 0) { //skip if any of the vertices are missing
                                    indices.writerIndex(writerIndex);
                                    break;
                                }
                                indices.writeShort(index);
                            }
                        }
                    }
                }
            }
        } finally {
            MAP_RECYCLER.get().release(map);
        }
    }

    protected void vertex(int baseX, int baseY, int baseZ, int level, int i, VoxelPiece[] srcs, int x, int y, int z, VoxelData data, ByteBuf vertices, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess) {
        baseX += (x & T_VOXELS) << level;
        baseY += (y & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        x &= ~(x & T_VOXELS);
        y &= ~(y & T_VOXELS);
        z &= ~(z & T_VOXELS);

        final double scale = 1 << level;

        //these lose precision, so i don't use them for the actual vertex position
        /*final int blockX = baseX + floorI((x + data.x) * scale);
        final int blockY = baseY + floorI((y + data.y) * scale);
        final int blockZ = baseZ + floorI((z + data.z) * scale);*/
        final int blockX = baseX + (x << level);
        final int blockY = baseY + (y << level);
        final int blockZ = baseZ + (z << level);

        pos.setPos(blockX, blockY, blockZ);
        biomeAccess.biome(Biome.getBiome(data.biome, Biomes.PLAINS));

        vertices.writeInt(TexUVs.STATEID_TO_INDEXID.get(data.state)); //state
        vertices.writeShort(Constants.packedLightTo8BitVec2(data.light)); //light
        vertices.writeMedium(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.state), biomeAccess, pos, 0))); //color

        vertices.writeDouble(blockX + data.x * scale + 0.5d)
                .writeDouble(blockY + data.y * scale + 0.5d)
                .writeDouble(blockZ + data.z * scale + 0.5d); //pos_low

        int basePieceX = (baseX >> (level + T_SHIFT)) - ((i >> 2) & 1);
        int basePieceY = (baseY >> (level + T_SHIFT)) - ((i >> 1) & 1);
        int basePieceZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        VoxelPiece highPiece = srcs[8 | (i & (((basePieceX & 1) << 2) | ((basePieceY & 1) << 1) | (basePieceZ & 1)))];
        if (level == 1 || highPiece == null) { //pos_high
            vertices.writeDouble(blockX + data.x * scale + 0.5d)
                    .writeDouble(blockY + data.y * scale + 0.5d)
                    .writeDouble(blockZ + data.z * scale + 0.5d);
        } else {
            final int flooredX = blockX & -(1 << (level + 1));
            final int flooredY = blockY & -(1 << (level + 1));
            final int flooredZ = blockZ & -(1 << (level + 1));

            double highX = 0.0d;
            double highY = 0.0d;
            double highZ = 0.0d;
            if (highPiece.get((flooredX >> (level + 1)) & T_MASK, (flooredY >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK, data)) {
                highX = data.x;
                highY = data.y;
                highZ = data.z;
            }

            vertices.writeDouble(flooredX + highX * 2 * scale + 0.5d)
                    .writeDouble(flooredY + highY * 2 * scale + 0.5d)
                    .writeDouble(flooredZ + highZ * 2 * scale + 0.5d);
        }

        vertices.writeShort(1 << level);
    }

    private boolean samplePos(int baseX, int baseY, int baseZ, int level, int i, VoxelPiece[] srcs, int x, int y, int z, VoxelData data) {
        baseX += (x & 0x10) << level;
        baseY += (y & 0x10) << level;
        baseZ += (z & 0x10) << level;

        i |= ((x & 0x10) >> 2) | ((y & 0x10) >> 3) | ((z & 0x10) >> 4);

        if (x == 16) {
            x = 0;
        }
        if (y == 16) {
            y = 0;
        }
        if (z == 16) {
            z = 0;
        }

        int basePieceX = (baseX >> (level + T_SHIFT)) - ((i >> 2) & 1);
        int basePieceY = (baseY >> (level + T_SHIFT)) - ((i >> 1) & 1);
        int basePieceZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        VoxelPiece highPiece = srcs[8 | (i & (((basePieceX & 1) << 2) | ((basePieceY & 1) << 1) | (basePieceZ & 1)))];
        if (highPiece == null) {
            return false;
        } else {
            final int flooredX = (baseX + (x << level)) & -(1 << (level + 1));
            final int flooredY = (baseY + (y << level)) & -(1 << (level + 1));
            final int flooredZ = (baseZ + (z << level)) & -(1 << (level + 1));

            return highPiece.get((flooredX >> (level + 1)) & T_MASK, (flooredY >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK, data);
        }
    }
}
