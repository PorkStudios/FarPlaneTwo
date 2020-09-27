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
import net.daporkchop.fp2.mode.heightmap.HeightmapPiece;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.stream.Stream;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL41.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderBaker implements IFarRenderBaker<HeightmapPos, HeightmapPiece> {
    public static final int HEIGHTMAP_VERTEX_SIZE = INT_SIZE //state
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
        glVertexAttribIPointer(0, 1, GL_UNSIGNED_INT, HEIGHTMAP_VERTEX_SIZE, offset); //state
        glVertexAttribPointer(1, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += INT_SIZE); //light
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += SHORT_SIZE); //color
        glVertexAttribPointer(3, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += MEDIUM_SIZE); //light_water
        glVertexAttribPointer(4, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += SHORT_SIZE); //color_water
        glVertexAttribLPointer(5, 3, HEIGHTMAP_VERTEX_SIZE, offset += MEDIUM_SIZE); //pos_low
        glVertexAttribLPointer(6, 3, HEIGHTMAP_VERTEX_SIZE, offset += DVEC3_SIZE_TIGHT); //pos_high
        glVertexAttribPointer(7, 1, GL_UNSIGNED_SHORT, false, HEIGHTMAP_VERTEX_SIZE, offset += DVEC3_SIZE_TIGHT); //level_scale
    }

    @Override
    public Stream<HeightmapPos> bakeOutputs(@NonNull HeightmapPos srcPos) {
        int x = srcPos.x();
        int z = srcPos.z();
        int level = srcPos.level();

        return Stream.of(
                //same level
                srcPos,
                new HeightmapPos(x, z - 1, level),
                new HeightmapPos(x - 1, z, level),
                new HeightmapPos(x - 1, z - 1, level),
                //below level
                new HeightmapPos(x << 1, z << 1, level - 1),
                new HeightmapPos(x << 1, (z << 1) - 1, level - 1),
                new HeightmapPos((x << 1) - 1, z << 1, level - 1),
                new HeightmapPos((x << 1) - 1, (z << 1) - 1, level - 1)); //TODO: possibly need to output a larger area below
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
    public void bake(@NonNull HeightmapPos dstPos, @NonNull HeightmapPiece[] srcs, @NonNull ByteBuf vertices, @NonNull ByteBuf indices) {
        if (srcs[0] == null) {
            return;
        }

        final int level = dstPos.level();
        final int baseX = dstPos.blockX();
        final int baseZ = dstPos.blockZ();

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dz = 0; dz < T_VOXELS; dz++) {
                this.writeVertex(baseX, baseZ, level, 0, srcs, dx, dz, vertices, pos, biomeAccess);
            }
        }

        int index = T_VOXELS * T_VOXELS;
        int indexZ = -1;
        if (srcs[1] != null) {
            indexZ = index;
            for (int dx = 0; dx < T_VOXELS; dx++) {
                this.writeVertex(baseX, baseZ + (T_VOXELS << level), level, 1, srcs, dx, 0, vertices, pos, biomeAccess);
            }
            index += T_VOXELS;
        }

        int indexX = -1;
        if (srcs[2] != null) {
            indexX = index;
            for (int dz = 0; dz < T_VOXELS; dz++) {
                this.writeVertex(baseX + (T_VOXELS << level), baseZ, level, 2, srcs, 0, dz, vertices, pos, biomeAccess);
            }
            index += T_VOXELS;
        }

        int indexXZ = -1;
        if (srcs[1] != null && srcs[2] != null && srcs[3] != null) {
            indexXZ = index++;
            this.writeVertex(baseX + (T_VOXELS << level), baseZ + (T_VOXELS << level), level, 3, srcs, 0, 0, vertices, pos, biomeAccess);
        }

        for (int dx = 0; dx < T_VOXELS - 1; dx++) {
            for (int dz = 0; dz < T_VOXELS - 1; dz++) {
                indices.writeShort(dx * T_VOXELS + dz)
                        .writeShort(dx * T_VOXELS + (dz + 1))
                        .writeShort((dx + 1) * T_VOXELS + (dz + 1))
                        .writeShort(dx * T_VOXELS + dz)
                        .writeShort((dx + 1) * T_VOXELS + dz)
                        .writeShort((dx + 1) * T_VOXELS + (dz + 1));
            }
        }

        if (indexZ >= 0) {
            for (int dx = 0; dx < T_VOXELS - 1; dx++) {
                indices.writeShort(dx * T_VOXELS + (T_VOXELS - 1))
                        .writeShort(indexZ + dx)
                        .writeShort(indexZ + dx + 1)
                        .writeShort(dx * T_VOXELS + (T_VOXELS - 1))
                        .writeShort((dx + 1) * T_VOXELS + (T_VOXELS - 1))
                        .writeShort(indexZ + dx + 1);
            }
        }

        if (indexX >= 0) {
            for (int dz = 0; dz < T_VOXELS - 1; dz++) {
                indices.writeShort((T_VOXELS - 1) * T_VOXELS + dz)
                        .writeShort((T_VOXELS - 1) * T_VOXELS + (dz + 1))
                        .writeShort(indexX + dz + 1)
                        .writeShort((T_VOXELS - 1) * T_VOXELS + dz)
                        .writeShort(indexX + dz)
                        .writeShort(indexX + dz + 1);
            }
        }

        if (indexXZ >= 0) {
            indices.writeShort((T_VOXELS - 1) * T_VOXELS + (T_VOXELS - 1))
                    .writeShort(indexZ + (T_VOXELS - 1))
                    .writeShort(indexXZ)
                    .writeShort((T_VOXELS - 1) * T_VOXELS + (T_VOXELS - 1))
                    .writeShort(indexX + (T_VOXELS - 1))
                    .writeShort(indexXZ);
        }
    }

    private void writeVertex(int baseX, int baseZ, int level, int i, HeightmapPiece[] srcs, int x, int z, ByteBuf out, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess) {
        HeightmapPiece piece = srcs[i];
        final int height = piece.height(x, z);
        final int state = piece.block(x, z);

        final int blockX = baseX + (x << level);
        final int blockZ = baseZ + (z << level);

        pos.setPos(blockX, height, blockZ);
        biomeAccess.biome(Biome.getBiome(piece.biome(x, z), Biomes.PLAINS));

        //block
        out.writeInt(TexUVs.STATEID_TO_INDEXID.get(state)); //state
        out.writeShort(Constants.packedLightTo8BitVec2(piece.light(x, z))); //light
        out.writeMedium(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Block.getStateById(state), biomeAccess, pos, 0))); //color

        //water
        biomeAccess.biome(Biome.getBiome(piece.waterBiome(x, z), Biomes.PLAINS));
        out.writeShort(Constants.packedLightTo8BitVec2(piece.waterLight(x, z))); //light_water
        out.writeMedium(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(Blocks.WATER.getDefaultState(), biomeAccess, pos, 0))); //color_water

        //position
        out.writeDouble(blockX).writeDouble(height).writeDouble(blockZ); //pos_low

        int basePieceX = (baseX >> (level + T_SHIFT)) - (i >> 1);
        int basePieceZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        HeightmapPiece highPiece = srcs[4 | (i & (((basePieceX & 1) << 1) | (basePieceZ & 1)))];
        if (highPiece == null) { //pos_high
            out.writeDouble(blockX).writeDouble(height).writeDouble(blockZ);
        } else {
            final int flooredX = blockX & -(1 << (level + 1));
            final int flooredZ = blockZ & -(1 << (level + 1));

            double highHeight = highPiece.height((flooredX >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK);

            //lerp between the higher vertex positions
            if (((x | z) & 1) != 0) {
                highHeight = (highHeight + this.sampleHeight(baseX, baseZ, level, i, srcs, x + (x & 1), z + (z & 1), highHeight)) * 0.5d;
            }

            out.writeDouble(blockX).writeDouble(highHeight).writeDouble(blockZ);
        }

        out.writeShort(1 << level); //level_scale
    }

    private double sampleHeight(int baseX, int baseZ, int level, int i, HeightmapPiece[] srcs, int x, int z, double fallback) {
        baseX += (x & 0x10) << level;
        baseZ += (z & 0x10) << level;

        i |= ((x & 0x10) >> 3) | ((z & 0x10) >> 4);

        if (x == 16) {
            x = 0;
        }
        if (z == 16) {
            z = 0;
        }

        int basePieceX = (baseX >> (level + T_SHIFT)) - (i >> 1);
        int basePieceZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        HeightmapPiece highPiece = srcs[4 | (i & (((basePieceX & 1) << 1) | (basePieceZ & 1)))];
        if (highPiece == null) {
            return fallback;
        } else {
            final int flooredX = (baseX + (x << level)) & -(1 << (level + 1));
            final int flooredZ = (baseZ + (z << level)) & -(1 << (level + 1));

            return highPiece.height((flooredX >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK);
        }
    }
}
