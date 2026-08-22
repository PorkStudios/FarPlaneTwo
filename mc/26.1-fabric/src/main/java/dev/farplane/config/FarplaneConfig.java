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

package dev.farplane.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for FarPlane.
 * Stored in {@code config/farplane.json}.
 *
 * @author FarPlane contributors
 */
public class FarplaneConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "farplane.json";

    // --- Terrain generation ---
    /** Maximum number of LoD levels (1 = LoD 0 only). */
    private int maxLevels = 3;

    /** Cutoff distance in tiles at the finest LoD level. */
    private int cutoffDistance = 256;

    // --- Debug / preview ---
    /** Enable the debug height-grid preview (Phase 1 fallback). */
    private boolean debugPreview = true;

    /** Extra chunks beyond view distance to show in debug preview. */
    private int debugPreviewExtraChunks = 24;

    /** Grid step size for debug preview (in blocks). */
    private int debugGridStep = 8;

    // --- Performance ---
    /** Number of terrain generation threads. */
    private int terrainThreads = 2;

    /** Number of bake (meshing) threads. */
    private int bakeThreads = 2;

    // --- Getters ---
    public int maxLevels() { return maxLevels; }
    public int cutoffDistance() { return cutoffDistance; }
    public boolean debugPreview() { return debugPreview; }
    public int debugPreviewExtraChunks() { return debugPreviewExtraChunks; }
    public int debugGridStep() { return debugGridStep; }
    public int terrainThreads() { return terrainThreads; }
    public int bakeThreads() { return bakeThreads; }

    /**
     * Computes the effective render distance in blocks.
     * Formula: cutoffDistance << (maxLevels - 1)
     */
    public int effectiveRenderDistanceBlocks() {
        return cutoffDistance << (maxLevels - 1);
    }

    // --- Persistence ---

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static FarplaneConfig load() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                return GSON.fromJson(json, FarplaneConfig.class);
            } catch (IOException e) {
                System.err.println("[FarPlane] Failed to read config, using defaults: " + e.getMessage());
            }
        }
        return new FarplaneConfig();
    }

    public void save() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[FarPlane] Failed to save config: " + e.getMessage());
        }
    }
}
