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

import dev.farplane.Farplane;
import dev.farplane.config.FarplaneConfig;
import dev.farplane.engine.storage.TileStorage;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Handles asynchronous tile generation and caching.
 * <p>
 * This replaces the synchronous generation in VoxelTerrainRenderer with a proper
 * async pipeline that uses background threads and disk caching.
 *
 * @author FarPlane contributors
 */
public class AsyncTileGenerator {
    private final FarplaneConfig config;
    private final TileStorage storage;
    private final Level level;
    private final ExecutorService generationExecutor;
    private final Map<TilePos, CompletableFuture<Tile>> pendingGenerations = new ConcurrentHashMap<>();
    private final Map<TilePos, Tile> tileCache = new ConcurrentHashMap<>();
    private final VoxelScaler scaler = new VoxelScaler();
    private RoughVoxelGenerator roughGenerator;

    // Callback when a tile is ready
    private Consumer<TilePos> onTileReady;

    public AsyncTileGenerator(FarplaneConfig config, TileStorage storage, Level level) {
        this.config = config;
        this.storage = storage;
        this.level = level;

        int threads = config.terrainThreads();
        this.generationExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "FarPlane-Gen");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        Farplane.LOGGER.info("[FarPlane] Async generator initialized with {} threads", threads);

        // Initialize rough generator if we have a server level
        if (level.getServer() != null) {
            try {
                this.roughGenerator = new RoughVoxelGenerator(level);
                Farplane.LOGGER.info("[FarPlane] Rough noise generator initialized");
            } catch (Exception e) {
                Farplane.LOGGER.warn("[FarPlane] Failed to initialize rough generator: {}", e.getMessage());
            }
        }
    }

    /**
     * Sets the callback for when a tile becomes ready.
     */
    public void setOnTileReady(Consumer<TilePos> callback) {
        this.onTileReady = callback;
    }

    /**
     * Requests generation of a tile. Returns immediately; the tile will be
     * delivered via the onTileReady callback when complete.
     *
     * @param pos    the tile position to generate
     * @param source the block sample source (for exact generation)
     */
    public void requestGeneration(TilePos pos, BlockSampleSource source) {
        if (pendingGenerations.containsKey(pos) || tileCache.containsKey(pos)) {
            return; // Already pending or cached
        }

        CompletableFuture<Tile> future = CompletableFuture.supplyAsync(() -> {
            return generateTile(pos, source);
        }, generationExecutor);

        pendingGenerations.put(pos, future);

        future.thenAccept(tile -> {
            pendingGenerations.remove(pos);
            if (tile != null && !tile.isEmpty()) {
                tileCache.put(pos, tile);

                // Save to disk asynchronously
                storage.save(pos, tile);

                // Notify callback
                if (onTileReady != null) {
                    onTileReady.accept(pos);
                }
            }
        }).exceptionally(e -> {
            pendingGenerations.remove(pos);
            Farplane.LOGGER.debug("Failed to generate tile at {}: {}", pos, e.getMessage());
            return null;
        });
    }

    /**
     * Gets a cached tile, or null if not available.
     */
    public Tile getCachedTile(TilePos pos) {
        return tileCache.get(pos);
    }

    /**
     * Checks if a tile is currently being generated.
     */
    public boolean isGenerating(TilePos pos) {
        return pendingGenerations.containsKey(pos);
    }

    /**
     * Checks if a tile is cached and ready.
     */
    public boolean isCached(TilePos pos) {
        return tileCache.containsKey(pos);
    }

    /**
     * Unloads tiles that are too far from the player.
     */
    public void unloadDistantTiles(double playerX, double playerZ, int maxDistance) {
        int playerTileX = ((int) playerX) >> T_SHIFT;
        int playerTileZ = ((int) playerZ) >> T_SHIFT;

        tileCache.keySet().removeIf(pos -> {
            int dx = pos.x() - playerTileX;
            int dz = pos.z() - playerTileZ;
            boolean distant = dx * dx + dz * dz > maxDistance * maxDistance;
            if (distant) {
                // Save to disk before unloading
                Tile tile = tileCache.get(pos);
                if (tile != null) {
                    storage.save(pos, tile);
                }
            }
            return distant;
        });
    }

    /**
     * Clears all cached tiles and cancels pending generations.
     */
    public void clear() {
        pendingGenerations.clear();
        tileCache.clear();
    }

    /**
     * Shuts down the generator.
     */
    public void shutdown() {
        generationExecutor.shutdown();
        try {
            generationExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            generationExecutor.shutdownNow();
        }
    }

    private Tile generateTile(TilePos pos, BlockSampleSource source) {
        // Try to load from disk first
        try {
            Tile cached = storage.load(pos).get(100, TimeUnit.MILLISECONDS);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            // Ignore timeout or missing tiles
        }

        // Generate new tile
        if (pos.level() == 0) {
            // Try exact generation first (from loaded chunks)
            if (source != null) {
                ExactVoxelGenerator exactGen = new ExactVoxelGenerator(source);
                Tile tile = new Tile();
                if (exactGen.generate(pos, tile)) {
                    return tile;
                }
            }

            // Fall back to rough generation if exact fails
            if (roughGenerator != null) {
                Tile tile = new Tile();
                if (roughGenerator.generate(pos, tile)) {
                    return tile;
                }
            }
        } else {
            // For higher levels, try rough generation first
            if (roughGenerator != null) {
                Tile tile = new Tile();
                if (roughGenerator.generate(pos, tile)) {
                    return tile;
                }
            }

            // Fall back to scaling from children
            return scaleFromChildren(pos);
        }

        return null;
    }

    private Tile scaleFromChildren(TilePos pos) {
        // Get the 8 child tiles
        Tile[] children = new Tile[8];
        boolean allPresent = true;

        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    int childIdx = (dx << 2) | (dy << 1) | dz;
                    TilePos childPos = new TilePos(pos.level() - 1,
                            pos.x() * 2 + dx,
                            pos.y() * 2 + dy,
                            pos.z() * 2 + dz);

                    Tile child = tileCache.get(childPos);
                    if (child == null) {
                        allPresent = false;
                        break;
                    }
                    children[childIdx] = child;
                }
                if (!allPresent) break;
            }
            if (!allPresent) break;
        }

        if (!allPresent) {
            return null; // Can't scale without all children
        }

        Tile result = new Tile();
        scaler.scale(children, result);
        return result;
    }
}
