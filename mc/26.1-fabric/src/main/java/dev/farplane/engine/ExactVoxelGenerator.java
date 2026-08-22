/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2026 DaPorkchop_
 * Portions Copyright (c) 2026 FarPlane contributors
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

package dev.farplane.engine;

import net.minecraft.world.level.Level;

import static dev.farplane.engine.EngineConstants.*;

/**
 * Exact voxel generator — generates LoD-0 tiles from already-loaded world data.
 * Adapted from FarPlaneTwo {@code AbstractExactVoxelGenerator}.
 * <p>
 * This reads a padded cube of block data around the tile, classifies block types,
 * and performs dual-contouring edge detection to produce sparse voxel tiles.
 *
 * @author DaPorkchop_ (original algorithm)
 */
public class ExactVoxelGenerator {
    // Cache dimensions: we need one extra block on each side for corner sampling
    public static final int CACHE_MIN = -1;
    public static final int CACHE_MAX = T_VOXELS + 1; // 17
    public static final int CACHE_SIZE = CACHE_MAX - CACHE_MIN; // 18

    // Pre-computed offsets for the 8 corners of each voxel cell
    private static final int[] CACHE_INDEX_ADD = new int[8];

    static {
        for (int i = 0; i < 8; i++) {
            CACHE_INDEX_ADD[i] = cacheIndex(
                    CACHE_MIN + ((i >> 2) & 1),
                    CACHE_MIN + ((i >> 1) & 1),
                    CACHE_MIN + (i & 1));
        }
    }

    private static int cacheIndex(int x, int y, int z) {
        return ((x - CACHE_MIN) * CACHE_SIZE + (y - CACHE_MIN)) * CACHE_SIZE + (z - CACHE_MIN);
    }

    private final BlockSampleSource sampleSource;

    public ExactVoxelGenerator(Level level) {
        this.sampleSource = new BlockSampleSource(level);
    }

    public ExactVoxelGenerator(BlockSampleSource sampleSource) {
        this.sampleSource = sampleSource;
    }

    /**
     * Generates a tile at the given position. Only works at level 0.
     *
     * @param pos  the tile position (must be level 0)
     * @param tile the tile to populate
     * @return true if any data was generated, false if the tile is empty
     */
    public boolean generate(TilePos pos, Tile tile) {
        if (pos.level() != 0) {
            throw new IllegalArgumentException("Exact generation only works at level 0, got level " + pos.level());
        }

        int minBX = pos.minBlockX();
        int minBY = pos.minBlockY();
        int minBZ = pos.minBlockZ();

        // Allocate caches
        int cacheVolume = CACHE_SIZE * CACHE_SIZE * CACHE_SIZE;
        int[] stateCache = new int[cacheVolume];
        int[] biomeCache = new int[cacheVolume];
        byte[] lightCache = new byte[cacheVolume];
        byte[] typeCache = new byte[cacheVolume];

        // Query world data (padded cube around the tile)
        sampleSource.multiGetDense(
                minBX + CACHE_MIN, minBY + CACHE_MIN, minBZ + CACHE_MIN,
                CACHE_SIZE, CACHE_SIZE, CACHE_SIZE,
                stateCache, biomeCache, lightCache);

        // Classify block types
        for (int i = 0; i < cacheVolume; i++) {
            typeCache[i] = (byte) BlockSampleSource.typeOf(stateCache[i]);
        }

        TileData data = new TileData();

        // Iterate over all 16³ cells in the tile
        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    // Sample the 8 corners of this cell
                    int corners = 0;
                    int ciBase = cacheIndex(dx, dy, dz);
                    for (int i = 0; i < 8; i++) {
                        corners |= (typeCache[ciBase + CACHE_INDEX_ADD[i]] & 0xFF) << (i << 1);
                    }

                    // Skip if all corners are the same type
                    if (corners == 0 || corners == 0x5555 || corners == 0xAAAA) {
                        continue;
                    }

                    // Detect edge crossings on each of the 3 axes
                    int edges = 0;
                    for (int edge = 0; edge < EDGE_COUNT; edge++) {
                        int c0 = EDGE_VERTEX_MAP[edge << 1] << 1;
                        int c1 = EDGE_VERTEX_MAP[(edge << 1) | 1] << 1;

                        if (((corners >> c0) & 3) == ((corners >> c1) & 3)) {
                            continue; // no crossing on this edge
                        }

                        if (((corners >> c0) & 3) < ((corners >> c1) & 3)) {
                            edges |= EDGE_DIR_NEGATIVE << (edge << 1);
                        } else {
                            edges |= EDGE_DIR_POSITIVE << (edge << 1);
                        }
                    }

                    data.edges = edges;

                    // Determine block states for each crossing edge
                    for (int edge = 0; edge < EDGE_COUNT; edge++) {
                        if ((edges & (EDGE_DIR_MASK << (edge << 1))) != EDGE_DIR_NONE) {
                            // Pick the "more solid" side's state
                            int i = EDGE_VERTEX_MAP[(edge << 1) | ((edges >> (edge << 1) >> 1) & 1)];
                            data.states[edge] = stateCache[cacheIndex(
                                    dx + ((i >> 2) & 1),
                                    dy + ((i >> 1) & 1),
                                    dz + (i & 1))];
                        }
                    }

                    // Biome from cell center
                    data.biome = biomeCache[cacheIndex(dx, dy, dz)];

                    // Compute average light on the less-opaque side
                    int skyLight = 0;
                    int blockLight = 0;
                    int samples = 0;

                    if (edges == 0) {
                        // Dummy placeholder voxel — average light for the least opaque type
                        int type = BLOCK_TYPE_OPAQUE;
                        for (int i = 0; i < 8; i++) {
                            type = Math.min(type, (corners >> (i << 1)) & 3);
                        }
                        for (int i = 0; i < 8; i++) {
                            if (((corners >> (i << 1)) & 3) == type) {
                                byte packedLight = lightCache[cacheIndex(
                                        dx + ((i >> 2) & 1),
                                        dy + ((i >> 1) & 1),
                                        dz + (i & 1))];
                                skyLight += unpackSkyLight(packedLight);
                                blockLight += unpackBlockLight(packedLight);
                                samples++;
                            }
                        }
                    } else {
                        // Average light on the "less opaque" side of each crossing
                        for (int edge = 0; edge < EDGE_COUNT; edge++) {
                            if ((edges & (EDGE_DIR_MASK << (edge << 1))) != EDGE_DIR_NONE) {
                                int i = EDGE_VERTEX_MAP[(edge << 1) | (~(edges >> (edge << 1) >> 1) & 1)];
                                byte packedLight = lightCache[cacheIndex(
                                        dx + ((i >> 2) & 1),
                                        dy + ((i >> 1) & 1),
                                        dz + (i & 1))];
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
                    data.light = (byte) packLight(skyLight, blockLight);

                    tile.set(dx, dy, dz, data);
                }
            }
        }

        return !tile.isEmpty();
    }

    private static int unpackSkyLight(byte packedLight) {
        return (packedLight >> 4) & 0xF;
    }

    private static int unpackBlockLight(byte packedLight) {
        return packedLight & 0xF;
    }
}
