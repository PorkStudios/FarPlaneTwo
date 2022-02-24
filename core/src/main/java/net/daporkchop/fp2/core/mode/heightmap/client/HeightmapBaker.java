/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.heightmap.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.core.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.mode.heightmap.client.struct.HeightmapGlobalAttributes;
import net.daporkchop.fp2.core.mode.heightmap.client.struct.HeightmapLocalAttributes;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapTile.*;

/**
 * Shared code for baking heightmap geometry.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class HeightmapBaker implements IRenderBaker<HeightmapPos, HeightmapTile, IndexedBakeOutput<HeightmapGlobalAttributes, HeightmapLocalAttributes>> {
    protected static int vertexMapIndex(int x, int z, int layer) {
        return (x * HT_VERTS + z) * MAX_LAYERS + layer;
    }

    @NonNull
    protected final WorldRenderer worldRenderer;
    @NonNull
    protected final TextureUVs textureUVs;

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
    public void bake(@NonNull HeightmapPos pos, @NonNull HeightmapTile[] srcs, @NonNull IndexedBakeOutput<HeightmapGlobalAttributes, HeightmapLocalAttributes> output) {
        if (srcs[0] == null) {
            return;
        }

        //write globals
        output.globals().put(new HeightmapGlobalAttributes(pos.x(), pos.z(), pos.level()));

        final int level = pos.level();
        final int blockX = pos.blockX();
        final int blockZ = pos.blockZ();

        final HeightmapData data = new HeightmapData();
        final HeightmapLocalAttributes attributes = new HeightmapLocalAttributes();

        final int[] map = new int[HT_VERTS * HT_VERTS * MAX_LAYERS];
        Arrays.fill(map, -1);

        //write vertices and build index
        for (int i = 0; i < 4; i++) {
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

                        int x = dx + (((i >> 1) & 1) << HT_SHIFT);
                        int z = dz + ((i & 1) << HT_SHIFT);

                        map[vertexMapIndex(x, z, layer)] = this.writeVertex(blockX, blockZ, level, src, x, z, layer, output.verts(), data, attributes);
                    }
                }
            }
        }

        final BitSet rendered = new BitSet(HT_VERTS * HT_VERTS * MAX_LAYERS);

        //write indices
        for (int x = 0; x < HT_VOXELS; x++) {
            for (int z = 0; z < HT_VOXELS; z++) {
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

                    output.indices()[this.worldRenderer.renderTypeForState(data.state)].appendQuad(oppositeCorner, c1, c0, provoking);
                    rendered.set(vertexMapIndex(x, z, layer));
                }
            }
        }

        //re-check behind each vertex to see if we need to render the back-face of a layer transition
        for (int x = 1; x < HT_VERTS; x++) {
            for (int z = 1; z < HT_VERTS; z++) {
                HeightmapTile src = srcs[((x >> HT_SHIFT) << 1) | (z >> HT_SHIFT)];
                if (src == null) {
                    continue;
                }

                for (int layerFlags = src._getLayerFlags(x & HT_MASK, z & HT_MASK), layer = 0; layer < MAX_LAYERS; layer++) {
                    if ((layerFlags & layerFlag(layer)) == 0) {  //layer is unset
                        continue;
                    }

                    src._getLayerUnchecked(x & HT_MASK, z & HT_MASK, layer, data);
                    int provoking = map[vertexMapIndex(x, z, layer)];

                    for (int dx = -1; dx <= 1; dx += 2) {
                        for (int dz = -1; dz <= 1; dz += 2) {
                            int oppositeCorner, c0, c1;
                            if ((dx | dz) >= 0 //at least one offset must be negative - the +,+ quadrant is always handled properly by the first pass
                                || x + dx == HT_VERTS || z + dz == HT_VERTS //avoid out of bounds (will never happen in negative direction)
                                || rendered.get(vertexMapIndex(x + dx, z + dz, layer)) //face behind was rendered correctly
                                || ((c0 = map[vertexMapIndex(x, z + dz, layer)]) < 0 && (c0 = map[vertexMapIndex(x, z + dz, data.secondaryConnection)]) < 0)
                                || ((c1 = map[vertexMapIndex(x + dx, z, layer)]) < 0 && (c1 = map[vertexMapIndex(x + dx, z, data.secondaryConnection)]) < 0)
                                || ((oppositeCorner = map[vertexMapIndex(x + dx, z + dz, layer)]) < 0 && (oppositeCorner = map[vertexMapIndex(x + dx, z + dz, data.secondaryConnection)]) < 0)) {
                                continue; //skip if any of the vertices are missing
                            }

                            output.indices()[this.worldRenderer.renderTypeForState(data.state)].appendQuad(oppositeCorner, c1, c0, provoking);
                        }
                    }
                }
            }
        }
    }

    private int writeVertex(int baseX, int baseZ, int level, HeightmapTile tile, int x, int z, int layer, AttributeWriter<HeightmapLocalAttributes> out, HeightmapData data, HeightmapLocalAttributes attributes) {
        baseX += (x & HT_VOXELS) << level;
        baseZ += (z & HT_VOXELS) << level;

        tile._getLayerUnchecked(x & HT_MASK, z & HT_MASK, layer, data);

        final int blockX = baseX + ((x & HT_MASK) << level);
        final int blockZ = baseZ + ((z & HT_MASK) << level);

        attributes.state = this.textureUVs.state2index(data.state);

        int blockLight = data.light & 0xF;
        int skyLight = data.light >> 4;
        //sky and block light are one unsigned byte each, and are interpreted as normalized floats. since the lightmap texture is 16x16, using
        //  the upper 4 bits for the regular light level (which is in range [0,15]) results in a float range of [0,15/16]. additionally, we set
        //  the bottom 4 bits to 0b1000. this is equivalent to an offset of 0.5 texels, which prevents blending artifacts when sampling right along
        //  the edge of a texture.
        attributes.lightBlock = (byte) ((blockLight << 4) | 8);
        attributes.lightSky = (byte) ((skyLight << 4) | 8);

        attributes.color = this.worldRenderer.tintFactorForStateInBiomeAtPos(data.state, data.biome, blockX, data.height_int, blockZ);

        attributes.posHorizX = (byte) x;
        attributes.posHorizZ = (byte) z;
        attributes.heightInt = data.height_int;
        attributes.heightFrac = (byte) data.height_frac;

        return out.put(attributes);
    }
}
