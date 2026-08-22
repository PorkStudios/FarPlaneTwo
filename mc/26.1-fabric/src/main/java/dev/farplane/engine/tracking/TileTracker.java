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

package dev.farplane.engine.tracking;

import dev.farplane.Farplane;
import dev.farplane.config.FarplaneConfig;
import dev.farplane.engine.TilePos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static dev.farplane.engine.EngineConstants.*;

/**
 * Tracks which tiles are visible to the player and manages loading/unloading.
 * Adapted from FarPlaneTwo {@code Tracker}.
 * <p>
 * The tracker maintains a cube of tiles around the player at each LoD level.
 * When the player moves, it computes which tiles were added/removed and
 * notifies listeners accordingly.
 *
 * @author DaPorkchop_ (original algorithm)
 */
public class TileTracker {
    /**
     * Squared distance the player must move to trigger an update.
     * Default: (T_VOXELS / 2)² = 64
     */
    private static final double UPDATE_TRIGGER_DISTANCE_SQ = (T_VOXELS >> 1) * (T_VOXELS >> 1);

    private final FarplaneConfig config;

    // Current tracking state
    private double lastPlayerX = Double.NaN;
    private double lastPlayerY = Double.NaN;
    private double lastPlayerZ = Double.NaN;
    private int lastMaxLevels = -1;
    private int lastCutoff = -1;

    // Sets of tile positions
    private final Set<TilePos> loadedPositions = ConcurrentHashMap.newKeySet();
    private final Set<TilePos> queuedPositions = ConcurrentHashMap.newKeySet();

    // Listeners
    private final List<Consumer<TilePos>> loadListeners = new ArrayList<>();
    private final List<Consumer<TilePos>> unloadListeners = new ArrayList<>();

    public TileTracker(FarplaneConfig config) {
        this.config = config;
    }

    /**
     * Registers a listener for tile load events.
     */
    public void onTileLoad(Consumer<TilePos> listener) {
        loadListeners.add(listener);
    }

    /**
     * Registers a listener for tile unload events.
     */
    public void onTileUnload(Consumer<TilePos> listener) {
        unloadListeners.add(listener);
    }

    /**
     * Updates the tracker with the current player position.
     * Should be called once per tick or when the player moves significantly.
     *
     * @param playerX player X in block coordinates
     * @param playerY player Y in block coordinates
     * @param playerZ player Z in block coordinates
     */
    public void update(double playerX, double playerY, double playerZ) {
        int maxLevels = config.maxLevels();
        int cutoff = config.cutoffDistance();

        // Check if update is needed
        if (!shouldUpdate(playerX, playerY, playerZ, maxLevels, cutoff)) {
            return;
        }

        // Compute new visible positions
        Set<TilePos> newVisible = computeVisiblePositions(playerX, playerY, playerZ, maxLevels, cutoff);

        // Find added and removed positions
        Set<TilePos> added = new HashSet<>(newVisible);
        added.removeAll(loadedPositions);

        Set<TilePos> removed = new HashSet<>(loadedPositions);
        removed.removeAll(newVisible);

        // Update state
        lastPlayerX = playerX;
        lastPlayerY = playerY;
        lastPlayerZ = playerZ;
        lastMaxLevels = maxLevels;
        lastCutoff = cutoff;

        // Notify listeners for unloads first
        for (TilePos pos : removed) {
            loadedPositions.remove(pos);
            for (Consumer<TilePos> listener : unloadListeners) {
                try {
                    listener.accept(pos);
                } catch (Exception e) {
                    Farplane.LOGGER.error("Error in tile unload listener", e);
                }
            }
        }

        // Sort added positions by priority (level first, then Manhattan distance)
        List<TilePos> sortedAdded = new ArrayList<>(added);
        sortedAdded.sort(createComparator(playerX, playerY, playerZ));

        // Notify listeners for loads
        for (TilePos pos : sortedAdded) {
            loadedPositions.add(pos);
            queuedPositions.add(pos);
            for (Consumer<TilePos> listener : loadListeners) {
                try {
                    listener.accept(pos);
                } catch (Exception e) {
                    Farplane.LOGGER.error("Error in tile load listener", e);
                }
            }
        }
    }

    /**
     * Marks a tile as loaded (no longer queued).
     */
    public void markLoaded(TilePos pos) {
        queuedPositions.remove(pos);
    }

    /**
     * Returns the set of currently loaded positions.
     */
    public Set<TilePos> getLoadedPositions() {
        return Collections.unmodifiableSet(loadedPositions);
    }

    /**
     * Returns the set of queued (loading) positions.
     */
    public Set<TilePos> getQueuedPositions() {
        return Collections.unmodifiableSet(queuedPositions);
    }

    /**
     * Clears all tracking state.
     */
    public void clear() {
        Set<TilePos> allPositions = new HashSet<>(loadedPositions);
        allPositions.addAll(queuedPositions);

        loadedPositions.clear();
        queuedPositions.clear();

        for (TilePos pos : allPositions) {
            for (Consumer<TilePos> listener : unloadListeners) {
                try {
                    listener.accept(pos);
                } catch (Exception e) {
                    Farplane.LOGGER.error("Error in tile unload listener", e);
                }
            }
        }
    }

    private boolean shouldUpdate(double playerX, double playerY, double playerZ, int maxLevels, int cutoff) {
        if (Double.isNaN(lastPlayerX)) {
            return true; // First update
        }

        if (maxLevels != lastMaxLevels || cutoff != lastCutoff) {
            return true; // Config changed
        }

        double dx = playerX - lastPlayerX;
        double dy = playerY - lastPlayerY;
        double dz = playerZ - lastPlayerZ;
        return dx * dx + dy * dy + dz * dz >= UPDATE_TRIGGER_DISTANCE_SQ;
    }

    /**
     * Computes all tile positions visible from the given player position.
     * Adapted from FP2 {@code Tracker.allPositions}.
     */
    private Set<TilePos> computeVisiblePositions(double playerX, double playerY, double playerZ, int maxLevels, int cutoff) {
        Set<TilePos> positions = new HashSet<>();

        int playerTileX = (int) Math.floor(playerX) >> T_SHIFT;
        int playerTileY = (int) Math.floor(playerY) >> T_SHIFT;
        int playerTileZ = (int) Math.floor(playerZ) >> T_SHIFT;

        for (int level = 0; level < maxLevels; level++) {
            int shift = level;
            int baseX = playerTileX >> shift;
            int baseY = playerTileY >> shift;
            int baseZ = playerTileZ >> shift;

            // At higher LoD levels, the cutoff in tile units is smaller
            int levelCutoff = Math.max(1, cutoff >> shift);

            for (int x = baseX - levelCutoff; x <= baseX + levelCutoff; x++) {
                for (int y = baseY - levelCutoff; y <= baseY + levelCutoff; y++) {
                    for (int z = baseZ - levelCutoff; z <= baseZ + levelCutoff; z++) {
                        positions.add(new TilePos(level, x, y, z));
                    }
                }
            }
        }

        return positions;
    }

    /**
     * Creates a comparator for sorting tile positions by load priority.
     * Lower level (more detailed) first, then by Manhattan distance.
     */
    private Comparator<TilePos> createComparator(double playerX, double playerY, double playerZ) {
        int playerTileX = (int) Math.floor(playerX) >> T_SHIFT;
        int playerTileY = (int) Math.floor(playerY) >> T_SHIFT;
        int playerTileZ = (int) Math.floor(playerZ) >> T_SHIFT;

        return Comparator
                .comparingInt(TilePos::level)
                .thenComparingInt(pos -> {
                    int dx = pos.x() - (playerTileX >> pos.level());
                    int dy = pos.y() - (playerTileY >> pos.level());
                    int dz = pos.z() - (playerTileZ >> pos.level());
                    return Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                });
    }
}
