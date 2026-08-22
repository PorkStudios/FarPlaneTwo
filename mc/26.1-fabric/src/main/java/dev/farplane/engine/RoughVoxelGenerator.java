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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction;

import static dev.farplane.engine.EngineConstants.*;

/**
 * Rough voxel generator — generates tiles from noise without loading chunks.
 * <p>
 * This is the core FP2 feature that enables terrain at extreme distances.
 * It samples the NoiseRouter/DensityFunctions directly to determine terrain shape.
 *
 * @author DaPorkchop_ (original algorithm)
 * @author FarPlane contributors (26.1 adaptation)
 */
public class RoughVoxelGenerator {
    // Cache dimensions (same as ExactVoxelGenerator)
    public static final int CACHE_MIN = -1;
    public static final int CACHE_MAX = T_VOXELS + 1;
    public static final int CACHE_SIZE = CACHE_MAX - CACHE_MIN;

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

    private final Level level;
    private final RandomState randomState;
    private final BiomeManager biomeManager;

    public RoughVoxelGenerator(Level level) {
        this.level = level;
        // Get the RandomState from the level's chunk generator
        // In 26.1, we need to access this through the server level
        this.randomState = level.getServer().overworld().getChunkSource().randomState();
        this.biomeManager = level.getBiomeManager();
    }

    /**
     * Generates a rough tile at the given position using noise sampling.
     *
     * @param pos  the tile position (any level)
     * @param tile the tile to populate
     * @return true if any data was generated
     */
    public boolean generate(TilePos pos, Tile tile) {
        int level = pos.level();
        int scale = 1 << level;

        int minBX = pos.minBlockX();
        int minBY = pos.minBlockY();
        int minBZ = pos.minBlockZ();

        // Sample density at each voxel corner
        double[][] densityMap = new double[2][CACHE_SIZE * CACHE_SIZE * CACHE_SIZE];
        sampleDensityMap(minBX, minBY, minBZ, scale, densityMap);

        // Classify types based on density
        byte[] typeMap = new byte[CACHE_SIZE * CACHE_SIZE * CACHE_SIZE];
        for (int i = 0; i < typeMap.length; i++) {
            byte type = 0;
            if (densityMap[0][i] > 0.0) {
                type |= BLOCK_TYPE_TRANSPARENT;
            }
            if (densityMap[1][i] > 0.0) {
                type |= BLOCK_TYPE_OPAQUE;
            }
            typeMap[i] = type;
        }

        // Dual contour to find surface
        TileData data = new TileData();

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    int diBase = cacheIndex(dx, dy, dz);

                    // Check corners
                    int corners = 0;
                    for (int i = 0; i < 8; i++) {
                        corners |= (typeMap[diBase + CACHE_INDEX_ADD[i]] & 0xFF) << (i << 1);
                    }

                    // Skip if all corners are the same type
                    if (corners == 0 || corners == 0x5555 || corners == 0xAAAA || corners == 0xFFFF) {
                        continue;
                    }

                    // Find edge crossings
                    int edges = 0;
                    for (int edge = 0; edge < EDGE_COUNT; edge++) {
                        int c0 = EDGE_VERTEX_MAP[edge << 1];
                        int c1 = EDGE_VERTEX_MAP[(edge << 1) | 1];

                        int layer0 = (corners >> (c0 << 1)) & 3;
                        int layer1 = (corners >> (c1 << 1)) & 3;

                        if (layer0 == layer1) continue;

                        if (layer0 < layer1) {
                            edges |= EDGE_DIR_NEGATIVE << (edge << 1);
                        } else {
                            edges |= EDGE_DIR_POSITIVE << (edge << 1);
                        }

                        // Determine block state based on position
                        int worldX = minBX + (dx << level);
                        int worldY = minBY + (dy << level);
                        int worldZ = minBZ + (dz << level);
                        data.states[edge] = getBlockStateForPosition(worldX, worldY, worldZ);
                    }

                    if (edges == 0) continue;

                    data.edges = edges;

                    // Simple vertex position (center of cell for v1)
                    data.x = POS_ONE >> 1;
                    data.y = POS_ONE >> 1;
                    data.z = POS_ONE >> 1;

                    // Get biome
                    int worldX = minBX + (dx << level);
                    int worldY = minBY + (dy << level);
                    int worldZ = minBZ + (dz << level);
                    data.biome = getBiomeHash(worldX, worldY, worldZ);

                    // Simple lighting (sky=15, block=0 for rough gen)
                    data.light = (byte) packLight(15, 0);

                    tile.set(dx, dy, dz, data);
                }
            }
        }

        return !tile.isEmpty();
    }

    /**
     * Samples the density map for a padded cube around the tile.
     */
    private void sampleDensityMap(int minBX, int minBY, int minBZ, int scale, double[][] densityMap) {
        NoiseRouter router = randomState.router();

        int idx = 0;
        for (int dx = 0; dx < CACHE_SIZE; dx++) {
            int worldX = minBX + (dx + CACHE_MIN) * scale;
            for (int dy = 0; dy < CACHE_SIZE; dy++) {
                int worldY = minBY + (dy + CACHE_MIN) * scale;
                for (int dz = 0; dz < CACHE_SIZE; dz++) {
                    int worldZ = minBZ + (dz + CACHE_MIN) * scale;

                    // Sample density at this position
                    // Layer 0: terrain density (determines solid/air)
                    // Layer 1: fluid density (determines water/lava)
                    densityMap[0][idx] = sampleFinalDensity(router, worldX, worldY, worldZ);
                    densityMap[1][idx] = sampleFluidDensity(router, worldX, worldY, worldZ);
                    idx++;
                }
            }
        }
    }

    /**
     * Samples the final density at a world position.
     * Positive = solid, negative = air.
     */
    private double sampleFinalDensity(NoiseRouter router, int x, int y, int z) {
        // Create a simple NoisePos for sampling
        DensityFunction.NoisePos pos = new DensityFunction.NoisePos() {
            @Override
            public int blockX() { return x; }

            @Override
            public int blockY() { return y; }

            @Override
            public int blockZ() { return z; }
        };

        return router.finalDensity().compute(pos);
    }

    /**
     * Samples the fluid density at a world position.
     * Positive = fluid, negative = no fluid.
     */
    private double sampleFluidDensity(NoiseRouter router, int x, int y, int z) {
        DensityFunction.NoisePos pos = new DensityFunction.NoisePos() {
            @Override
            public int blockX() { return x; }

            @Override
            public int blockY() { return y; }

            @Override
            public int blockZ() { return z; }
        };

        // Use a simple threshold based on Y level for v1
        // Full implementation would use the actual fluid level density
        if (y < level.getSeaLevel()) {
            return 1.0; // Below sea level = fluid
        }
        return -1.0;
    }

    /**
     * Gets a block state for a position based on terrain type.
     */
    private int getBlockStateForPosition(int x, int y, int z) {
        // For v1, use simple rules:
        // - Below sea level: stone
        // - At surface: grass
        // - Near surface: dirt
        // - Deep: stone

        int seaLevel = level.getSeaLevel();

        if (y < seaLevel - 10) {
            return Block.getId(Blocks.STONE.defaultBlockState());
        } else if (y < seaLevel) {
            return Block.getId(Blocks.GRAVEL.defaultBlockState());
        } else if (y == seaLevel) {
            return Block.getId(Blocks.GRASS_BLOCK.defaultBlockState());
        } else if (y < seaLevel + 4) {
            return Block.getId(Blocks.DIRT.defaultBlockState());
        } else {
            return Block.getId(Blocks.STONE.defaultBlockState());
        }
    }

    /**
     * Gets a biome hash for a position.
     */
    private int getBiomeHash(int x, int y, int z) {
        // For v1, return a simple hash based on position
        // Full implementation would sample actual biome data
        return ((x * 1317194159 + y * 1964379643 + z * 1656858407) & 0xFF);
    }

    private static int packLight(int skyLight, int blockLight) {
        return ((skyLight & 0xF) << 4) | (blockLight & 0xF);
    }
}
