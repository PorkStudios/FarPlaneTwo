/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.engine.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.engine.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.engine.TileData;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.server.gen.AbstractVoxelGenerator;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import java.util.Optional;
import java.util.Set;

import static java.lang.Math.*;
import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;
import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactVoxelGenerator extends AbstractVoxelGenerator implements IFarGeneratorExact {
    public AbstractExactVoxelGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        super(world, provider);
    }

    @Override
    public Optional<Set<TilePos>> batchGenerationGroup(@NonNull FBlockLevel world, @NonNull TilePos pos) {
        checkArg(pos.level() == 0, "can only exact generate at level 0, not %d!", pos.level());

        IntAxisAlignedBB initialBB = new IntAxisAlignedBB(
                pos.minBlockX() + CACHE_MIN, pos.minBlockY() + CACHE_MIN, pos.minBlockZ() + CACHE_MIN,
                pos.minBlockX() + CACHE_MAX, pos.minBlockY() + CACHE_MAX, pos.minBlockZ() + CACHE_MAX);
        IntAxisAlignedBB dataAvailableBB = world.guaranteedDataAvailableVolume(initialBB);

        //the bounding boxes are identical, so there's no point in batching
        if (initialBB.equals(dataAvailableBB)) {
            return Optional.empty();
        }

        TilePos min = this.provider().coordLimits().min(0);
        TilePos max = this.provider().coordLimits().max(0);

        //TODO: figure out whether or not this is actually correct? i don't think this properly accounts for every possible edge case, rather it just happens to work for the current
        // values of CACHE_MIN/CACHE_MAX...
        Set<TilePos> out = DirectTilePosAccess.newPositionHashSet();
        for (int tileX = max(asrCeil(dataAvailableBB.minX() - CACHE_MIN, T_SHIFT), min.x()); tileX < min(asrCeil(dataAvailableBB.maxX() - CACHE_MAX, T_SHIFT), max.x()); tileX++) {
            for (int tileY = max(asrCeil(dataAvailableBB.minY() - CACHE_MIN, T_SHIFT), min.y()); tileY < min(asrCeil(dataAvailableBB.maxY() - CACHE_MAX, T_SHIFT), max.y()); tileY++) {
                for (int tileZ = max(asrCeil(dataAvailableBB.minZ() - CACHE_MIN, T_SHIFT), min.z()); tileZ < min(asrCeil(dataAvailableBB.maxZ() - CACHE_MAX, T_SHIFT), max.z()); tileZ++) {
                    out.add(new TilePos(0, tileX, tileY, tileZ));
                }
            }
        }
        return Optional.of(out);
    }

    protected void populateTypeMapFromStateMap(@NonNull int[] stateMap, @NonNull byte[] typeMap) {
        //range check here to allow JIT to avoid range checking inside the loop
        checkArg(typeMap.length >= cb(CACHE_SIZE) && stateMap.length >= cb(CACHE_SIZE));

        for (int i = 0; i < cb(CACHE_SIZE); i++) { //set each type flag depending on the block state at the corresponding position
            typeMap[i] = (byte) this.extendedStateRegistryData().type(stateMap[i]);
        }
    }

    @Override
    public void generate(@NonNull FBlockLevel world, @NonNull TilePos posIn, @NonNull Tile tile) throws GenerationNotAllowedException {
        checkArg(posIn.level() == 0, "can only exact generate at level 0, not %d!", posIn.level());

        ArrayAllocator<int[]> intAlloc = GlobalAllocators.ALLOC_INT.get();
        ArrayAllocator<byte[]> byteAlloc = GlobalAllocators.ALLOC_BYTE.get();

        //allocate temporary arrays
        int[] stateCache = intAlloc.atLeast(cb(CACHE_SIZE));
        int[] biomeCache = intAlloc.atLeast(cb(CACHE_SIZE));
        byte[] lightCache = byteAlloc.atLeast(cb(CACHE_SIZE));
        byte[] typeCache = byteAlloc.atLeast(cb(CACHE_SIZE));

        try {
            //query all world data at once
            world.multiGetDense(
                    posIn.minBlockX() + CACHE_MIN - 1, posIn.minBlockY() + CACHE_MIN - 1, posIn.minBlockZ() + CACHE_MIN - 1,
                    CACHE_SIZE, CACHE_SIZE, CACHE_SIZE,
                    stateCache, 0, biomeCache, 0, lightCache, 0);

            //use bit flags to identify voxel types rather than reading from the world each time to keep innermost loop head tight and cache-friendly
            this.populateTypeMapFromStateMap(stateCache, typeCache);

            TileData data = new TileData();

            data.x = data.y = data.z = 0;

            for (int dx = 0; dx < T_VOXELS; dx++) {
                for (int dy = 0; dy < T_VOXELS; dy++) {
                    for (int dz = 0; dz < T_VOXELS; dz++) {
                        int corners = 0;
                        for (int ciCache = cacheIndex(dx, dy, dz), i = 0; i < 8; i++) {
                            corners |= (typeCache[ciCache + CACHE_INDEX_ADD[i]] & 0xFF) << (i << 1);
                        }

                        if (corners == 0 || corners == 0x5555 || corners == 0xAAAA) { //if all corners are the same type, this voxel can be safely skipped
                            continue;
                        }

                        int edges = 0;
                        for (int edge = 0; edge < EDGE_COUNT; edge++) {
                            int c0 = EDGE_VERTEX_MAP[edge << 1] << 1;
                            int c1 = EDGE_VERTEX_MAP[(edge << 1) | 1] << 1;

                            if (((corners >> c0) & 3) == ((corners >> c1) & 3)) { //both corners along the current edge are identical, this edge can be skipped
                                continue;
                            }

                            if (((corners >> c0) & 3) < ((corners >> c1) & 3)) { //the face is facing towards negative coordinates
                                edges |= EDGE_DIR_NEGATIVE << (edge << 1);
                            } else {
                                edges |= EDGE_DIR_POSITIVE << (edge << 1);
                            }
                        }

                        data.edges = edges;

                        for (int edge = 0; edge < EDGE_COUNT; edge++) {
                            if ((edges & (EDGE_DIR_MASK << (edge << 1))) != EDGE_DIR_NONE) {
                                //((edges >> (edge << 1) >> 1) & 1) is 1 if the face is negative, 0 otherwise
                                int i = EDGE_VERTEX_MAP[(edge << 1) | ((edges >> (edge << 1) >> 1) & 1)];
                                data.states[edge] = stateCache[cacheIndex(dx + ((i >> 2) & 1), dy + ((i >> 1) & 1), dz + (i & 1))];
                            }
                        }

                        data.biome = biomeCache[cacheIndex(dx, dy, dz)];

                        int skyLight = 0;
                        int blockLight = 0;
                        int samples = 0;
                        if (edges == 0) { //this voxel is only present as a dummy placeholder for other voxels to connect to
                            //compute average light levels for the least opaque block type intersecting this voxel

                            int type = BLOCK_TYPE_OPAQUE;
                            for (int i = 0; i < 8; i++) {
                                type = min(type, (corners >> (i << 1)) & 3);
                            }

                            for (int i = 0; i < 8; i++) {
                                if (((corners >> (i << 1)) & 3) == type) {
                                    byte packedLight = lightCache[cacheIndex(dx + ((i >> 2) & 1), dy + ((i >> 1) & 1), dz + (i & 1))];
                                    skyLight += unpackSkyLight(packedLight);
                                    blockLight += unpackBlockLight(packedLight);
                                    samples++;
                                }
                            }
                        } else {
                            //compute average light levels for the "less opaque" side of all non-transparent faces
                            for (int edge = 0; edge < EDGE_COUNT; edge++) {
                                if ((edges & (EDGE_DIR_MASK << (edge << 1))) != EDGE_DIR_NONE) {
                                    int i = EDGE_VERTEX_MAP[(edge << 1) | (~(edges >> (edge << 1) >> 1) & 1)];
                                    byte packedLight = lightCache[cacheIndex(dx + ((i >> 2) & 1), dy + ((i >> 1) & 1), dz + (i & 1))];
                                    skyLight += unpackSkyLight(packedLight);
                                    blockLight += unpackBlockLight(packedLight);
                                    samples++;
                                }
                            }
                        }
                        if (samples > 1) {
                            skyLight /= samples;
                            blockLight /= samples;
                        }
                        data.light = packLight(skyLight, blockLight);

                        tile.set(dx, dy, dz, data);
                    }
                }
            }

            //TODO: compute neighbor connections
            tile.extra(0L);
        } finally {
            //release all buffers
            byteAlloc.release(typeCache);
            byteAlloc.release(lightCache);
            intAlloc.release(biomeCache);
            intAlloc.release(stateCache);
        }
    }
}
