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
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.client.IFarRenderBaker;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelData;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.util.math.BlockPos;

import java.util.stream.Stream;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.strategy.voxel.server.gen.VoxelGeneratorConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL41.*;

/**
 * @author DaPorkchop_
 */
public class VoxelRenderBaker implements IFarRenderBaker<VoxelPos, VoxelPiece> {
    public static final int VOXEL_VERTEX_SIZE = INT_SIZE //state
                                                + SHORT_SIZE //light
                                                + MEDIUM_SIZE //color
                                                + SHORT_SIZE //light_water
                                                + MEDIUM_SIZE //color_water
                                                + DVEC3_SIZE_TIGHT //pos_low
                                                + DVEC3_SIZE_TIGHT //pos_high
                                                + SHORT_SIZE; //level_scale

    @Override
    public int indexType() {
        return GL_UNSIGNED_SHORT;
    }

    @Override
    public int vertexAttributes() {
        return 8;
    }

    @Override
    public void assignVertexAttributes() {
        long offset = 0L;
        glVertexAttribIPointer(0, 1, GL_UNSIGNED_INT, VOXEL_VERTEX_SIZE, offset); //state
        glVertexAttribPointer(1, 2, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += INT_SIZE); //light
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += SHORT_SIZE); //color
        glVertexAttribPointer(3, 2, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += MEDIUM_SIZE); //light_water
        glVertexAttribPointer(4, 4, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, offset += SHORT_SIZE); //color_water
        glVertexAttribLPointer(5, 3, VOXEL_VERTEX_SIZE, offset += MEDIUM_SIZE); //pos_low
        glVertexAttribLPointer(6, 3, VOXEL_VERTEX_SIZE, offset += DVEC3_SIZE_TIGHT); //pos_high
        glVertexAttribPointer(7, 1, GL_UNSIGNED_SHORT, false, VOXEL_VERTEX_SIZE, offset += DVEC3_SIZE_TIGHT); //level_scale
    }

    @Override
    public Stream<VoxelPos> bakeOutputs(@NonNull VoxelPos srcPos) {
        int x = srcPos.x();
        int y = srcPos.y();
        int z = srcPos.z();
        int level = srcPos.level();

        return Stream.of(
                //same level
                srcPos, new VoxelPos(x, y, z - 1, level),
                new VoxelPos(x, y - 1, z, level), new VoxelPos(x, y - 1, z - 1, level),
                new VoxelPos(x - 1, y, z, level), new VoxelPos(x - 1, y, z - 1, level),
                new VoxelPos(x - 1, y - 1, z, level), new VoxelPos(x - 1, y - 1, z - 1, level)//,
                //below level
        );
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
                new VoxelPos(x + 1, y + 1, z, level), new VoxelPos(x + 1, y + 1, z + 1, level)//,
                //below level
        );
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

        Int2IntMap map = new Int2IntOpenHashMap();
        map.defaultReturnValue(0);

        {
            vertices.writeInt(0); //state
            vertices.writeShort(Constants.packedLightTo8BitVec2(0xFF)); //light
            vertices.writeMedium(Constants.convertARGB_ABGR(0xFFFFFFFF)); //color

            vertices.writeShort(Constants.packedLightTo8BitVec2(0xFF)); //light_water
            vertices.writeMedium(Constants.convertARGB_ABGR(0xFFFFFFFF)); //color_water

            vertices.writeDouble(baseX).writeDouble(baseY).writeDouble(baseZ); //pos_low
            vertices.writeDouble(baseX).writeDouble(baseY).writeDouble(baseZ); //pos_high

            vertices.writeShort(1 << level); //level_scale
        }

        int indexCounter = 1;

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    if (!srcs[0].get(dx, dy, dz, data)) {
                        continue;
                    }

                    map.put((dx * (T_VOXELS + 1) + dy) * (T_VOXELS + 1) + dz, indexCounter++);

                    vertices.writeInt(0); //state
                    vertices.writeShort(Constants.packedLightTo8BitVec2(0xFF)); //light
                    vertices.writeMedium(Constants.convertARGB_ABGR(0xFFFFFFFF)); //color

                    vertices.writeShort(Constants.packedLightTo8BitVec2(0xFF)); //light_water
                    vertices.writeMedium(Constants.convertARGB_ABGR(0xFFFFFFFF)); //color_water

                    vertices.writeDouble(baseX + dx + data.dx).writeDouble(baseY + dy + data.dy).writeDouble(baseZ + dz + data.dz); //pos_low
                    vertices.writeDouble(baseX + dx + data.dx).writeDouble(baseY + dy + data.dy).writeDouble(baseZ + dz + data.dz); //pos_high

                    vertices.writeShort(1 << level); //level_scale
                }
            }
        }

        for (int dx = 0; dx < T_VOXELS - 1; dx++) {
            for (int dy = 0; dy < T_VOXELS - 1; dy++) {
                for (int dz = 0; dz < T_VOXELS - 1; dz++) {
                    if (!srcs[0].get(dx, dy, dz, data)) {
                        continue;
                    }

                    for (int edge = 0; edge < 3; edge++) {
                        if ((data.edges & (1 << edge)) == 0) {
                            continue;
                        }

                        for (int base = edge * CONNECTION_INDEX_COUNT, i = 0; i < CONNECTION_INDEX_COUNT; i++) {
                            int j = CONNECTION_INDICES[base + i];
                            int index = map.get(((dx + ((j >> 2) & 1)) * (T_VOXELS + 1) + (dy + ((j >> 1) & 1))) * (T_VOXELS + 1) + (dz + (j & 1)));
                            indices.writeShort(index);
                        }
                    }
                }
            }
        }
    }

    protected void putIndex(int dx, int dy, int dz, int i, VoxelPiece piece, ByteBuf indices) {
        int index = piece.getIndex(dx + ((i >> 2) & 1), dy + ((i >> 1) & 1), dz + (i & 1));
        checkState(index >= 0);
        indices.writeShort(index);
    }
}
