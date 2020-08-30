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

package net.daporkchop.fp2.strategy.heightmap.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.client.IFarRenderBaker;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.stream.Stream;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderBaker implements IFarRenderBaker<HeightmapPos, HeightmapPiece> {
    public static final int HEIGHTMAP_VERTEX_SIZE = VEC3_ELEMENTS * FLOAT_SIZE + INT_SIZE + SHORT_SIZE + INT_SIZE;

    @Override
    public int indexType() {
        return GL_UNSIGNED_SHORT;
    }

    @Override
    public int vertexAttributes() {
        return 3;
    }

    @Override
    public void assignVertexAttributes() {
        long offset = 0L;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, HEIGHTMAP_VERTEX_SIZE, offset); //pos
        glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, HEIGHTMAP_VERTEX_SIZE, offset += VEC3_ELEMENTS * FLOAT_SIZE); //state
        glVertexAttribPointer(2, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += INT_SIZE); //light
        glVertexAttribPointer(3, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, offset += SHORT_SIZE); //color
    }

    @Override
    public Stream<HeightmapPos> bakeOutputs(@NonNull HeightmapPos srcPos) {
        int x = srcPos.x();
        int z = srcPos.z();
        int level = srcPos.level();

        return Stream.of(srcPos, new HeightmapPos(x, z - 1, level), new HeightmapPos(x - 1, z, level), new HeightmapPos(x - 1, z - 1, level));
    }

    @Override
    public Stream<HeightmapPos> bakeInputs(@NonNull HeightmapPos dstPos) {
        int x = dstPos.x();
        int z = dstPos.z();
        int level = dstPos.level();

        return Stream.of(dstPos, new HeightmapPos(x, z + 1, level), new HeightmapPos(x + 1, z, level), new HeightmapPos(x + 1, z + 1, level));
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
                this.writeVertex(baseX, baseZ, level, srcs[0], dx, dz, vertices, pos, biomeAccess);
            }
        }

        int index = T_VOXELS * T_VOXELS;
        int indexZ = -1;
        if (srcs[1] != null) {
            indexZ = index;
            for (int dx = 0; dx < T_VOXELS; dx++) {
                this.writeVertex(baseX, baseZ + (T_VOXELS << level), level, srcs[1], dx, 0, vertices, pos, biomeAccess);
            }
            index += T_VOXELS;
        }

        int indexX = -1;
        if (srcs[2] != null) {
            indexX = index;
            for (int dz = 0; dz < T_VOXELS; dz++) {
                this.writeVertex(baseX + (T_VOXELS << level), baseZ, level, srcs[2], 0, dz, vertices, pos, biomeAccess);
            }
            index += T_VOXELS;
        }

        int indexXZ = -1;
        if (srcs[1] != null && srcs[2] != null && srcs[3] != null) {
            indexXZ = index++;
            this.writeVertex(baseX + (T_VOXELS << level), baseZ + (T_VOXELS << level), level, srcs[3], 0, 0, vertices, pos, biomeAccess);
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

    private void writeVertex(int blockX, int blockZ, int level, HeightmapPiece piece, int x, int z, ByteBuf out, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess) {
        int height = piece.height(x, z);
        int block = piece.block(x, z);

        pos.setPos(x, height, z);
        biomeAccess.biome(Biome.getBiome(piece.biome(x, z), Biomes.PLAINS));

        out.writeFloat(blockX + (x << level)).writeFloat(height).writeFloat(blockZ + (z << level)); //pos
        out.writeInt(block); //state
        out.writeShort(Constants.packedLightTo8BitVec2(piece.light(x, z))); //light
        out.writeInt(Constants.convertARGB_ABGR(0xFF000000 | Minecraft.getMinecraft().getBlockColors().colorMultiplier(Block.getStateById(block), biomeAccess, pos, 0))); //color
    }
}
