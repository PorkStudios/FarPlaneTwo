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
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.BitSet;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.mode.heightmap.HeightmapTile.*;
import static net.daporkchop.fp2.util.BlockType.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static org.lwjgl.opengl.GL11.*;

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

    public static final int HEIGHTMAP_VERTEX_POS_LOW_OFFSET = HEIGHTMAP_VERTEX_COLOR_OFFSET + MEDIUM_SIZE;
    public static final int HEIGHTMAP_VERTEX_HEIGHT_INT_LOW_OFFSET = HEIGHTMAP_VERTEX_POS_LOW_OFFSET + SHORT_SIZE;
    public static final int HEIGHTMAP_VERTEX_HEIGHT_FRAC_LOW_OFFSET = HEIGHTMAP_VERTEX_HEIGHT_INT_LOW_OFFSET + INT_SIZE;

    public static final int HEIGHTMAP_VERTEX_POS_HIGH_OFFSET = HEIGHTMAP_VERTEX_HEIGHT_FRAC_LOW_OFFSET + BYTE_SIZE;
    public static final int HEIGHTMAP_VERTEX_HEIGHT_INT_HIGH_OFFSET = HEIGHTMAP_VERTEX_POS_HIGH_OFFSET + SHORT_SIZE;
    public static final int HEIGHTMAP_VERTEX_HEIGHT_FRAC_HIGH_OFFSET = HEIGHTMAP_VERTEX_HEIGHT_INT_HIGH_OFFSET + INT_SIZE;

    public static final int HEIGHTMAP_VERTEX_SIZE = HEIGHTMAP_VERTEX_HEIGHT_FRAC_HIGH_OFFSET + BYTE_SIZE + 1; // +1 to pad to 24 bytes

    protected static int vertexMapIndex(int x, int z, int layer) {
        return (x * T_VERTS + z) * MAX_LAYERS + layer;
    }

    public void vertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao) {
        FP2_LOG.info("heightmap vertex size: {} bytes", HEIGHTMAP_VERTEX_SIZE);

        vao.attrI(buffer, 1, GL_UNSIGNED_INT, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_STATE_OFFSET, 0); //state
        vao.attrF(buffer, 2, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_LIGHT_OFFSET, 0); //light
        vao.attrF(buffer, 4, GL_UNSIGNED_BYTE, true, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_COLOR_OFFSET, 0); //color

        vao.attrI(buffer, 2, GL_UNSIGNED_BYTE, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_POS_LOW_OFFSET, 0); //pos_low
        vao.attrI(buffer, 1, GL_INT, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_HEIGHT_INT_LOW_OFFSET, 0); //height_int_low
        vao.attrF(buffer, 1, GL_UNSIGNED_BYTE, false, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_HEIGHT_FRAC_LOW_OFFSET, 0); //height_frac_low

        vao.attrI(buffer, 2, GL_UNSIGNED_BYTE, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_POS_HIGH_OFFSET, 0); //pos_high
        vao.attrI(buffer, 1, GL_INT, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_HEIGHT_INT_HIGH_OFFSET, 0); //height_int_high
        vao.attrF(buffer, 1, GL_UNSIGNED_BYTE, false, HEIGHTMAP_VERTEX_SIZE, HEIGHTMAP_VERTEX_HEIGHT_FRAC_HIGH_OFFSET, 0); //height_frac_high
    }

    public void bakeForShaderDraw(@NonNull HeightmapPos dstPos, @NonNull HeightmapTile[] srcs, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        if (srcs[0] == null) {
            return;
        }

        final int level = dstPos.level();
        final int blockX = dstPos.blockX();
        final int blockZ = dstPos.blockZ();

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();
        final HeightmapData data = new HeightmapData();

        final int[] map = new int[T_VERTS * T_VERTS * MAX_LAYERS];
        Arrays.fill(map, -1);

        //write vertices and build index
        for (int indexCounter = 0, i = 0; i < 4; i++) {
            HeightmapTile src = srcs[i];
            if (src == null) {
                continue;
            }

            int maxX = CONNECTION_INTERSECTION_AREAS[i * 2 + 0];
            int maxZ = CONNECTION_INTERSECTION_AREAS[i * 2 + 1];

            for (int dx = 0; dx < maxX; dx++) {
                for (int dz = 0; dz < maxZ; dz++) {
                    for (int layerFlags = src._getLayerFlags(dx, dz), layer = 0; layer < MAX_LAYERS; layer++) {
                        if ((layerFlags & layerFlag(layer)) == 0) { //layer is unset
                            continue;
                        }

                        int x = dx + (((i >> 1) & 1) << T_SHIFT);
                        int z = dz + ((i & 1) << T_SHIFT);

                        writeVertex(blockX, blockZ, level, i, srcs, x, z, layer, verts, pos, biomeAccess, data);
                        map[vertexMapIndex(x, z, layer)] = indexCounter++;
                    }
                }
            }
        }

        final BitSet rendered = new BitSet(T_VERTS * T_VERTS * MAX_LAYERS);

        //write indices
        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                for (int layerFlags = srcs[0]._getLayerFlags(x, z), layer = 0; layer < MAX_LAYERS; layer++) {
                    if ((layerFlags & layerFlag(layer)) == 0) { //layer is unset
                        continue;
                    }
                    srcs[0]._getLayerUnchecked(x, z, layer, data);

                    int oppositeCorner, c0, c1, provoking;
                    if ((provoking = map[vertexMapIndex(x, z, layer)]) < 0
                        || ((c0 = map[vertexMapIndex(x, z + 1, layer)]) < 0 && (c0 = map[vertexMapIndex(x, z + 1, data.secondaryConnection)]) < 0)
                        || ((c1 = map[vertexMapIndex(x + 1, z, layer)]) < 0 && (c1 = map[vertexMapIndex(x + 1, z, data.secondaryConnection)]) < 0)
                        || ((oppositeCorner = map[vertexMapIndex(x + 1, z + 1, layer)]) < 0 && (oppositeCorner = map[vertexMapIndex(x + 1, z + 1, data.secondaryConnection)]) < 0)) {
                        continue; //skip if any of the vertices are missing
                    }

                    emitQuad(indices[renderType(data.state)], oppositeCorner, c1, c0, provoking);
                    rendered.set(vertexMapIndex(x, z, layer));
                }
            }
        }

        //re-check behind each vertex to see if we need to render the back-face of a layer transition
        for (int x = 1; x < T_VERTS; x++) {
            for (int z = 1; z < T_VERTS; z++) {
                HeightmapTile src = srcs[((x >> T_SHIFT) << 1) | (z >> T_SHIFT)];
                if (src == null) {
                    continue;
                }

                for (int layerFlags = src._getLayerFlags(x & T_MASK, z & T_MASK), layer = 0; layer < MAX_LAYERS; layer++) {
                    if ((layerFlags & layerFlag(layer)) == 0) {  //layer is unset
                        continue;
                    }

                    src._getLayerUnchecked(x & T_MASK, z & T_MASK, layer, data);
                    int provoking = map[vertexMapIndex(x, z, layer)];

                    for (int dx = -1; dx <= 1; dx += 2) {
                        for (int dz = -1; dz <= 1; dz += 2) {
                            int oppositeCorner, c0, c1;
                            if ((dx | dz) >= 0 //at least one offset must be negative - the +,+ quadrant is always handled properly by the first pass
                                || x + dx == T_VERTS || z + dz == T_VERTS //avoid out of bounds (will never happen in negative direction)
                                || rendered.get(vertexMapIndex(x + dx, z + dz, layer)) //face behind was rendered correctly
                                || ((c0 = map[vertexMapIndex(x, z + dz, layer)]) < 0 && (c0 = map[vertexMapIndex(x, z + dz, data.secondaryConnection)]) < 0)
                                || ((c1 = map[vertexMapIndex(x + dx, z, layer)]) < 0 && (c1 = map[vertexMapIndex(x + dx, z, data.secondaryConnection)]) < 0)
                                || ((oppositeCorner = map[vertexMapIndex(x + dx, z + dz, layer)]) < 0 && (oppositeCorner = map[vertexMapIndex(x + dx, z + dz, data.secondaryConnection)]) < 0)) {
                                continue; //skip if any of the vertices are missing
                            }

                            emitQuad(indices[renderType(data.state)], oppositeCorner, c1, c0, provoking);
                        }
                    }
                }
            }
        }
    }

    private void writeVertex(int baseX, int baseZ, int level, int i, HeightmapTile[] srcs, int x, int z, int layer, ByteBuf out, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, HeightmapData data) {
        baseX += (x & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        srcs[i]._getLayerUnchecked(x & T_MASK, z & T_MASK, layer, data);

        final int blockX = baseX + ((x & T_MASK) << level);
        final int blockZ = baseZ + ((z & T_MASK) << level);

        pos.setPos(blockX, data.height_int, blockZ);
        biomeAccess.biome(data.biome);

        //block
        out.writeIntLE(TexUVs.STATEID_TO_INDEXID.get(data.state)); //state
        out.writeShortLE(Constants.packedLightTo8BitVec2(data.light)); //light
        out.writeMediumLE(Constants.convertARGB_ABGR(mc.getBlockColors().colorMultiplier(data.state, biomeAccess, pos, 0))); //color

        //pos_low
        out.writeByte(x).writeByte(z).writeIntLE(data.height_int).writeByte(data.height_frac);

        //pos_high
        int baseTileX = (baseX >> (level + T_SHIFT)) - (i >> 1);
        int baseTileZ = (baseZ >> (level + T_SHIFT)) - (i & 1);
        HeightmapTile highTile = srcs[4 | (i & (((baseTileX & 1) << 1) | (baseTileZ & 1)))];
        final int flooredX = blockX & -(1 << (level + 1));
        final int flooredZ = blockZ & -(1 << (level + 1));
        double highHeight;
        if (highTile == null || Double.isNaN(highHeight = highTile.getLayerOnlyHeight((flooredX >> (level + 1)) & T_MASK, (flooredZ >> (level + 1)) & T_MASK, layer))) {
            out.writeByte(x).writeByte(z).writeIntLE(data.height_int).writeByte(data.height_frac);
        } else {
            int heightI = floorI(highHeight);
            int heightF = clamp(floorI((highHeight - heightI) * 256.0d), 0, 255);
            out.writeByte(x & ~1).writeByte(z & ~1).writeIntLE(heightI).writeByte(heightF);
        }

        out.writeByte(0); //pad to 24 bytes
    }
}
