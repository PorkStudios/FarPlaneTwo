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

import static dev.farplane.engine.EngineConstants.*;

/**
 * Scales 8 child tiles into one parent tile at the next LoD level.
 * Simplified version of FarPlaneTwo {@code VoxelScalerIntersection}.
 * <p>
 * For v1, this uses a simple intersection-based approach: for each destination voxel,
 * check if any child voxel's surface intersects the destination cell's edges.
 *
 * @author DaPorkchop_ (original algorithm)
 */
public class VoxelScaler {

    /**
     * Scales 8 child tiles into one parent tile.
     *
     * @param children array of 8 child tiles (indexed as x*4 + y*2 + z, may contain nulls)
     * @param dst      the destination parent tile to populate
     */
    public void scale(Tile[] children, Tile dst) {
        TileData data = new TileData();

        // For each destination voxel, check the corresponding 2×2×2 block of child voxels
        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    // Map destination voxel to child coordinates
                    int childX = dx << 1;
                    int childY = dy << 1;
                    int childZ = dz << 1;

                    int edges = 0;
                    int bestState = 0;
                    int bestBiome = 0;
                    byte bestLight = 0;
                    int bestX = POS_ONE >> 1;
                    int bestY = POS_ONE >> 1;
                    int bestZ = POS_ONE >> 1;

                    // Check the 2×2×2 block of child voxels
                    for (int ox = 0; ox < 2; ox++) {
                        for (int oy = 0; oy < 2; oy++) {
                            for (int oz = 0; oz < 2; oz++) {
                                int childIdx = (ox << 2) | (oy << 1) | oz;
                                Tile child = children[childIdx];
                                if (child == null) continue;

                                int cx = childX + ox;
                                int cy = childY + oy;
                                int cz = childZ + oz;

                                // Clamp to child tile bounds
                                if (cx >= T_VOXELS || cy >= T_VOXELS || cz >= T_VOXELS) continue;

                                if (child.get(cx, cy, cz, data)) {
                                    edges |= data.edges;
                                    if (data.edges != 0) {
                                        bestState = data.states[0];
                                        bestBiome = data.biome;
                                        bestLight = data.light;
                                        bestX = (data.x + (ox * POS_ONE)) >> 1;
                                        bestY = (data.y + (oy * POS_ONE)) >> 1;
                                        bestZ = (data.z + (oz * POS_ONE)) >> 1;
                                    }
                                }
                            }
                        }
                    }

                    if (edges != 0) {
                        data.x = Math.max(0, Math.min(POS_ONE, bestX));
                        data.y = Math.max(0, Math.min(POS_ONE, bestY));
                        data.z = Math.max(0, Math.min(POS_ONE, bestZ));
                        data.edges = edges;
                        data.states[0] = bestState;
                        data.states[1] = bestState;
                        data.states[2] = bestState;
                        data.biome = bestBiome;
                        data.light = bestLight;
                        dst.set(dx, dy, dz, data);
                    }
                }
            }
        }
    }
}
