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

package net.daporkchop.fp2.mode.heightmap.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Shared code for baking heightmap geometry.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class HeightmapBake {
    public static final int HEIGHTMAP_VERTEX_STATE_OFFSET = 0;
    public static final int HEIGHTMAP_VERTEX_LIGHT_OFFSET = HEIGHTMAP_VERTEX_STATE_OFFSET + INT_SIZE;
    public static final int HEIGHTMAP_VERTEX_COLOR_OFFSET = HEIGHTMAP_VERTEX_LIGHT_OFFSET + SHORT_SIZE;
    public static final int HEIGHTMAP_VERTEX_LIGHT_WATER_OFFSET = HEIGHTMAP_VERTEX_COLOR_OFFSET + MEDIUM_SIZE;
    public static final int HEIGHTMAP_VERTEX_COLOR_WATER_OFFSET = HEIGHTMAP_VERTEX_LIGHT_WATER_OFFSET + SHORT_SIZE;
    public static final int HEIGHTMAP_VERTEX_POS_LOW_OFFSET = HEIGHTMAP_VERTEX_COLOR_WATER_OFFSET + MEDIUM_SIZE;
    public static final int HEIGHTMAP_VERTEX_HEIGHT_LOW_OFFSET = HEIGHTMAP_VERTEX_POS_LOW_OFFSET + SHORT_SIZE;
    public static final int HEIGHTMAP_VERTEX_POS_HIGH_OFFSET = HEIGHTMAP_VERTEX_HEIGHT_LOW_OFFSET + INT_SIZE;
    public static final int HEIGHTMAP_VERTEX_HEIGHT_HIGH_OFFSET = HEIGHTMAP_VERTEX_POS_HIGH_OFFSET + SHORT_SIZE;
    public static final int HEIGHTMAP_VERTEX_HEIGHT_WATER_OFFSET = HEIGHTMAP_VERTEX_HEIGHT_HIGH_OFFSET + INT_SIZE;

    public static final int HEIGHTMAP_VERTEX_SIZE = HEIGHTMAP_VERTEX_HEIGHT_WATER_OFFSET + INT_SIZE;

    public void vertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao) {
        vao.attrI(buffer, 1, GL_UNSIGNED_INT, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_STATE_OFFSET, 0); //state
        vao.attrF(buffer, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_LIGHT_OFFSET, 0); //light
        vao.attrF(buffer, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_COLOR_OFFSET, 0); //color
        vao.attrF(buffer, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_LIGHT_WATER_OFFSET, 0); //light_water
        vao.attrF(buffer, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_COLOR_WATER_OFFSET, 0); //color_water
        vao.attrI(buffer, 2, GL_UNSIGNED_BYTE, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_POS_LOW_OFFSET, 0); //pos_low
        vao.attrI(buffer, 1, GL_INT, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_HEIGHT_LOW_OFFSET, 0); //height_low
        vao.attrI(buffer, 2, GL_UNSIGNED_BYTE, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_POS_HIGH_OFFSET, 0); //pos_high
        vao.attrI(buffer, 1, GL_INT, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_HEIGHT_HIGH_OFFSET, 0); //height_high
        vao.attrI(buffer, 1, GL_INT, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_HEIGHT_WATER_OFFSET, 0); //height_water
    }

    public void bakeForShaderDraw(@NonNull HeightmapPos dstPos, @NonNull HeightmapTile[] srcs, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        if (srcs[0] == null) {
            return;
        }

        final int level = dstPos.level();
        final int baseX = dstPos.blockX();
        final int baseZ = dstPos.blockZ();

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();
        final HeightmapData data = new HeightmapData();

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dz = 0; dz < T_VOXELS; dz++) {
                writeVertex(baseX, baseZ, level, 0, srcs, dx, dz, verts, pos, biomeAccess, data);
            }
        }

        int index = T_VOXELS * T_VOXELS;
        int indexZ = -1;
        if (srcs[1] != null) {
            indexZ = index;
            for (int dx = 0; dx < T_VOXELS; dx++) {
                writeVertex(baseX, baseZ, level, 1, srcs, dx, T_VOXELS, verts, pos, biomeAccess, data);
            }
            index += T_VOXELS;
        }

        int indexX = -1;
        if (srcs[2] != null) {
            indexX = index;
            for (int dz = 0; dz < T_VOXELS; dz++) {
                writeVertex(baseX, baseZ, level, 2, srcs, T_VOXELS, dz, verts, pos, biomeAccess, data);
            }
            index += T_VOXELS;
        }

        int indexXZ = -1;
        if (srcs[1] != null && srcs[2] != null && srcs[3] != null) {
            indexXZ = index++;
            writeVertex(baseX, baseZ, level, 3, srcs, T_VOXELS, T_VOXELS, verts, pos, biomeAccess, data);
        }

        for (int dx = 0; dx < T_VOXELS - 1; dx++) {
            for (int dz = 0; dz < T_VOXELS - 1; dz++) {
                indices[0].writeShortLE(dx * T_VOXELS + dz)
                        .writeShortLE(dx * T_VOXELS + (dz + 1))
                        .writeShortLE((dx + 1) * T_VOXELS + (dz + 1))
                        .writeShortLE((dx + 1) * T_VOXELS + dz)
                        .writeShortLE(dx * T_VOXELS + dz)
                        .writeShortLE((dx + 1) * T_VOXELS + (dz + 1));
            }
        }

        if (indexZ >= 0) {
            for (int dx = 0; dx < T_VOXELS - 1; dx++) {
                indices[0].writeShortLE(dx * T_VOXELS + (T_VOXELS - 1))
                        .writeShortLE(indexZ + dx)
                        .writeShortLE(indexZ + dx + 1)
                        .writeShortLE((dx + 1) * T_VOXELS + (T_VOXELS - 1))
                        .writeShortLE(dx * T_VOXELS + (T_VOXELS - 1))
                        .writeShortLE(indexZ + dx + 1);
            }
        }

        if (indexX >= 0) {
            for (int dz = 0; dz < T_VOXELS - 1; dz++) {
                indices[0].writeShortLE((T_VOXELS - 1) * T_VOXELS + dz)
                        .writeShortLE((T_VOXELS - 1) * T_VOXELS + (dz + 1))
                        .writeShortLE(indexX + dz + 1)
                        .writeShortLE(indexX + dz)
                        .writeShortLE((T_VOXELS - 1) * T_VOXELS + dz)
                        .writeShortLE(indexX + dz + 1);
            }
        }

        if (indexXZ >= 0) {
            indices[0].writeShortLE((T_VOXELS - 1) * T_VOXELS + (T_VOXELS - 1))
                    .writeShortLE(indexZ + (T_VOXELS - 1))
                    .writeShortLE(indexXZ)
                    .writeShortLE(indexX + (T_VOXELS - 1))
                    .writeShortLE((T_VOXELS - 1) * T_VOXELS + (T_VOXELS - 1))
                    .writeShortLE(indexXZ);
        }
    }

    private void writeVertex(int baseX, int baseZ, int level, int i, HeightmapTile[] srcs, int x, int z, ByteBuf out, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, HeightmapData data) {
        baseX += (x & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        srcs[i].get(x & T_MASK, z & T_MASK, data);

        final int blockX = baseX + ((x & T_MASK) << level);
        final int blockZ = baseZ + ((z & T_MASK) << level);

        pos.setPos(blockX, data.height, blockZ);
        biomeAccess.biome(Biome.getBiome(data.biome, Biomes.PLAINS));

        //block
        out.writeIntLE(TexUVs.STATEID_TO_INDEXID.get(data.state)); //state
        out.writeShortLE(Constants.packedLightTo8BitVec2(data.light)); //light
        out.writeMediumLE(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.state), biomeAccess, pos, 0))); //color

        //water
        biomeAccess.biome(Biome.getBiome(data.waterBiome, Biomes.PLAINS));
        out.writeShortLE(Constants.packedLightTo8BitVec2(data.waterLight)); //light_water
        out.writeMediumLE(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Blocks.WATER.getDefaultState(), biomeAccess, pos, 0))); //color_water

        //position
        //TODO: redo writing
        out.writeByte(x).writeByte(z).writeIntLE(data.height); //pos_low

        int baseTileX = (baseX >> (level + T_SHIFT)) - (i >> 1);
        int baseTileZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        HeightmapTile highTile = srcs[4 | (i & (((baseTileX & 1) << 1) | (baseTileZ & 1)))];
        if (highTile == null) { //pos_high
            out.writeByte(x).writeByte(z).writeIntLE(data.height);
        } else {
            final int flooredX = blockX & -(1 << (level + 1));
            final int flooredZ = blockZ & -(1 << (level + 1));

            int highHeight = highTile.height((flooredX >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK);

            out.writeByte(x & ~1).writeByte(z & ~1).writeIntLE(highHeight);
        }

        out.writeIntLE(data.waterHeight); //height_water
    }
}
