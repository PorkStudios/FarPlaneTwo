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

import lombok.NonNull;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuilder;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeWriter;
import net.daporkchop.fp2.mode.common.client.RenderConstants;
import net.daporkchop.fp2.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Stream;

import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.mode.heightmap.HeightmapTile.*;
import static net.daporkchop.fp2.util.BlockType.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * Shared code for baking heightmap geometry.
 *
 * @author DaPorkchop_
 */
public class HeightmapBaker implements IRenderBaker<HeightmapPos, HeightmapTile, IndexedBakeOutput> {
    protected static int vertexMapIndex(int x, int z, int layer) {
        return (x * T_VERTS + z) * MAX_LAYERS + layer;
    }

    protected final Attribute.Int1 state;
    protected final Attribute.Int2 light;
    protected final Attribute.Int3 color;
    protected final Attribute.Int2 posHoriz;
    protected final Attribute.Int1 heightInt;
    protected final Attribute.Int1 heightFrac;

    public HeightmapBaker(@NonNull ShaderBasedHeightmapRenderStrategy strategy) {
        this.state = strategy.attrLocalState;
        this.light = strategy.attrLocalLight;
        this.color = strategy.attrLocalColor;
        this.posHoriz = strategy.attrLocalPosHoriz;
        this.heightInt = strategy.attrLocalHeightInt;
        this.heightFrac = strategy.attrLocalHeightFrac;
    }

    @Override
    public Stream<HeightmapPos> bakeOutputs(@NonNull HeightmapPos srcPos) {
        int x = srcPos.x();
        int z = srcPos.z();
        int level = srcPos.level();

        HeightmapPos[] arr = new HeightmapPos[4];
        int i = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                arr[i++] = new HeightmapPos(level, x + dx, z + dz);
            }
        }
        return Arrays.stream(arr, 0, i);
    }

    @Override
    public Stream<HeightmapPos> bakeInputs(@NonNull HeightmapPos dstPos) {
        int x = dstPos.x();
        int z = dstPos.z();
        int level = dstPos.level();

        HeightmapPos[] arr = new HeightmapPos[4];
        int i = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                arr[i++] = new HeightmapPos(level, x + dx, z + dz);
            }
        }
        return Arrays.stream(arr, 0, i);
    }

    @Override
    public void bake(@NonNull HeightmapPos pos, @NonNull HeightmapTile[] srcs, @NonNull IndexedBakeOutput output) {
        if (srcs[0] == null) {
            return;
        }

        final int level = pos.level();
        final int blockX = pos.blockX();
        final int blockZ = pos.blockZ();

        final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
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

                        this.writeVertex(blockX, blockZ, level, src, x, z, layer, output.verts(), blockPos, biomeAccess, data);
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

                    output.indices()[renderType(data.state)].appendQuad(oppositeCorner, c1, c0, provoking);
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

                            output.indices()[renderType(data.state)].appendQuad(oppositeCorner, c1, c0, provoking);
                        }
                    }
                }
            }
        }
    }

    private void writeVertex(int baseX, int baseZ, int level, HeightmapTile tile, int x, int z, int layer, LocalAttributeWriter out, BlockPos.MutableBlockPos pos, SingleBiomeBlockAccess biomeAccess, HeightmapData data) {
        baseX += (x & T_VOXELS) << level;
        baseZ += (z & T_VOXELS) << level;

        tile._getLayerUnchecked(x & T_MASK, z & T_MASK, layer, data);

        final int blockX = baseX + ((x & T_MASK) << level);
        final int blockZ = baseZ + ((z & T_MASK) << level);

        pos.setPos(blockX, data.height_int, blockZ);
        biomeAccess.biome(data.biome);

        out.set(this.state, TexUVs.STATEID_TO_INDEXID.get(data.state));

        int blockLight = data.light & 0xF;
        int skyLight = data.light >> 4;
        out.set(this.light, blockLight | (blockLight << 4), skyLight | (skyLight << 4));
        out.setARGB(this.color, MC.getBlockColors().colorMultiplier(data.state, biomeAccess, pos, 0));

        out.set(this.posHoriz, x, z);
        out.set(this.heightInt, data.height_int);
        out.set(this.heightFrac, data.height_frac);
        out.endVertex();
    }
}
