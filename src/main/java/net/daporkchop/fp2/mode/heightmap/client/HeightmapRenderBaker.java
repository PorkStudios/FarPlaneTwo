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

package net.daporkchop.fp2.mode.heightmap.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.mode.common.client.IFarRenderBaker;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;
import java.util.stream.Stream;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderBaker implements IFarRenderBaker<HeightmapPos, HeightmapPiece> {
    public static final int HEIGHTMAP_VERTEX_SIZE = INT_SIZE //state
                                                    + SHORT_SIZE //light
                                                    + MEDIUM_SIZE //color
                                                    + SHORT_SIZE //light_water
                                                    + MEDIUM_SIZE //color_water
                                                    + SHORT_SIZE //pos_low
                                                    + INT_SIZE //height_low
                                                    + SHORT_SIZE //pos_high
                                                    + INT_SIZE; //height_high

    @Override
    public int indexType() {
        return GL_UNSIGNED_SHORT;
    }

    @Override
    public int passes() {
        return 1; //everything is rendered in a single pass
    }

    @Override
    public int vertexAttributes() {
        return 9;
    }

    @Override
    public void assignVertexAttributes() {
        long offset = 0L;
        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, HEIGHTMAP_VERTEX_SIZE, offset); //state
        glVertexAttribPointer(2, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += INT_SIZE); //light
        glVertexAttribPointer(3, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += SHORT_SIZE); //color
        glVertexAttribPointer(4, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += MEDIUM_SIZE); //light_water
        glVertexAttribPointer(5, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += SHORT_SIZE); //color_water
        glVertexAttribIPointer(6, 2, GL_UNSIGNED_BYTE, HEIGHTMAP_VERTEX_SIZE, offset += MEDIUM_SIZE); //pos_low
        glVertexAttribIPointer(7, 1, GL_UNSIGNED_INT, HEIGHTMAP_VERTEX_SIZE, offset += SHORT_SIZE); //height_low
        glVertexAttribIPointer(8, 2, GL_UNSIGNED_BYTE, HEIGHTMAP_VERTEX_SIZE, offset += INT_SIZE); //pos_high
        glVertexAttribIPointer(9, 1, GL_UNSIGNED_INT, HEIGHTMAP_VERTEX_SIZE, offset += SHORT_SIZE); //height_high
    }

    @Override
    public Stream<HeightmapPos> bakeOutputs(@NonNull HeightmapPos srcPos) {
        int x = srcPos.x();
        int z = srcPos.z();
        int level = srcPos.level();

        HeightmapPos[] arr = new HeightmapPos[4 + 9];
        int i = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                arr[i++] = new HeightmapPos(x + dx, z + dz, level);
            }
        }
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                arr[i++] = new HeightmapPos((x << 1) + dx, (z << 1) + dz, level - 1);
            }
        }
        return Arrays.stream(arr, 0, i);
    }

    @Override
    public Stream<HeightmapPos> bakeInputs(@NonNull HeightmapPos dstPos) {
        int x = dstPos.x();
        int z = dstPos.z();
        int level = dstPos.level();

        return Stream.of(
                //same level
                dstPos,
                new HeightmapPos(x, z + 1, level),
                new HeightmapPos(x + 1, z, level),
                new HeightmapPos(x + 1, z + 1, level),
                //above level
                new HeightmapPos(x >> 1, z >> 1, level + 1),
                new HeightmapPos(x >> 1, (z >> 1) + 1, level + 1),
                new HeightmapPos((x >> 1) + 1, z >> 1, level + 1),
                new HeightmapPos((x >> 1) + 1, (z >> 1) + 1, level + 1));
    }

    @Override
    public int estimatedVerticesBufferCapacity() {
        return T_VERTS * T_VERTS * HEIGHTMAP_VERTEX_SIZE;
    }

    @Override
    public int estimatedIndicesBufferCapacity() {
        return T_VERTS * T_VERTS * INT_SIZE * 6;
    }

    @Override
    public void bake(@NonNull HeightmapPos dstPos, @NonNull HeightmapPiece[] srcs, @NonNull ByteBuf vertices, @NonNull ByteBuf[] indices) {
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
                this.writeVertex(baseX, baseZ, level, 0, srcs, dx, dz, vertices, pos, biomeAccess, data);
            }
        }

        int index = T_VOXELS * T_VOXELS;
        int indexZ = -1;
        if (srcs[1] != null) {
            indexZ = index;
            for (int dx = 0; dx < T_VOXELS; dx++) {
                this.writeVertex(baseX, baseZ, level, 1, srcs, dx, T_VOXELS, vertices, pos, biomeAccess, data);
            }
            index += T_VOXELS;
        }

        int indexX = -1;
        if (srcs[2] != null) {
            indexX = index;
            for (int dz = 0; dz < T_VOXELS; dz++) {
                this.writeVertex(baseX, baseZ, level, 2, srcs, T_VOXELS, dz, vertices, pos, biomeAccess, data);
            }
            index += T_VOXELS;
        }

        int indexXZ = -1;
        if (srcs[1] != null && srcs[2] != null && srcs[3] != null) {
            indexXZ = index++;
            this.writeVertex(baseX, baseZ, level, 3, srcs, T_VOXELS, T_VOXELS, vertices, pos, biomeAccess, data);
        }

        for (int dx = 0; dx < T_VOXELS - 1; dx++) {
            for (int dz = 0; dz < T_VOXELS - 1; dz++) {
                indices[0].writeShort(dx * T_VOXELS + dz)
                        .writeShort(dx * T_VOXELS + (dz + 1))
                        .writeShort((dx + 1) * T_VOXELS + (dz + 1))
                        .writeShort((dx + 1) * T_VOXELS + dz)
                        .writeShort(dx * T_VOXELS + dz)
                        .writeShort((dx + 1) * T_VOXELS + (dz + 1));
            }
        }

        if (indexZ >= 0) {
            for (int dx = 0; dx < T_VOXELS - 1; dx++) {
                indices[0].writeShort(dx * T_VOXELS + (T_VOXELS - 1))
                        .writeShort(indexZ + dx)
                        .writeShort(indexZ + dx + 1)
                        .writeShort((dx + 1) * T_VOXELS + (T_VOXELS - 1))
                        .writeShort(dx * T_VOXELS + (T_VOXELS - 1))
                        .writeShort(indexZ + dx + 1);
            }
        }

        if (indexX >= 0) {
            for (int dz = 0; dz < T_VOXELS - 1; dz++) {
                indices[0].writeShort((T_VOXELS - 1) * T_VOXELS + dz)
                        .writeShort((T_VOXELS - 1) * T_VOXELS + (dz + 1))
                        .writeShort(indexX + dz + 1)
                        .writeShort(indexX + dz)
                        .writeShort((T_VOXELS - 1) * T_VOXELS + dz)
                        .writeShort(indexX + dz + 1);
            }
        }

        if (indexXZ >= 0) {
            indices[0].writeShort((T_VOXELS - 1) * T_VOXELS + (T_VOXELS - 1))
                    .writeShort(indexZ + (T_VOXELS - 1))
                    .writeShort(indexXZ)
                    .writeShort(indexX + (T_VOXELS - 1))
                    .writeShort((T_VOXELS - 1) * T_VOXELS + (T_VOXELS - 1))
                    .writeShort(indexXZ);
        }
    }

    private void writeVertex(int baseX, int baseZ, int level, int i, HeightmapPiece[] srcs, int x, int z, ByteBuf out, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, HeightmapData data) {
        baseX += (x & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        srcs[i].get(x & T_MASK, z & T_MASK, data);

        final int blockX = baseX + ((x & T_MASK) << level);
        final int blockZ = baseZ + ((z & T_MASK) << level);

        pos.setPos(blockX, data.height, blockZ);
        biomeAccess.biome(Biome.getBiome(data.biome, Biomes.PLAINS));

        //block
        out.writeInt(TexUVs.STATEID_TO_INDEXID.get(data.state)); //state
        out.writeShort(Constants.packedLightTo8BitVec2(data.light)); //light
        out.writeMedium(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(data.state), biomeAccess, pos, 0))); //color

        //water
        biomeAccess.biome(Biome.getBiome(data.waterBiome, Biomes.PLAINS));
        out.writeShort(Constants.packedLightTo8BitVec2(data.waterLight)); //light_water
        out.writeMedium(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Blocks.WATER.getDefaultState(), biomeAccess, pos, 0))); //color_water

        //position
        //TODO: redo writing
        out.writeByte(x).writeByte(z).writeInt(data.height); //pos_low

        int basePieceX = (baseX >> (level + T_SHIFT)) - (i >> 1);
        int basePieceZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        HeightmapPiece highPiece = srcs[4 | (i & (((basePieceX & 1) << 1) | (basePieceZ & 1)))];
        if (highPiece == null) { //pos_high
            out.writeByte(x).writeByte(z).writeInt(data.height);
        } else {
            final int flooredX = blockX & -(1 << (level + 1));
            final int flooredZ = blockZ & -(1 << (level + 1));

            int highHeight = highPiece.height((flooredX >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK);

            out.writeByte(x & ~1).writeByte(z & ~1).writeInt(highHeight);
        }
    }
}
