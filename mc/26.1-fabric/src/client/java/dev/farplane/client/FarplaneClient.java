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

package dev.farplane.client;

import dev.farplane.Farplane;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client entry point for FarPlane.
 * Registers Fabric rendering events for the voxel terrain engine.
 *
 * @author FarPlane contributors
 */
public class FarplaneClient implements ClientModInitializer {

    private static VoxelTerrainRenderer voxelRenderer;

    @Override
    public void onInitializeClient() {
        Farplane.LOGGER.info("[FarPlane] Client initializing");

        voxelRenderer = new VoxelTerrainRenderer();

        // Phase 1: debug height-grid preview (extraction)
        // Phase 2: voxel tile rendering (after translucent terrain)
        LevelRenderEvents.END_EXTRACTION.register(context -> {
            if (voxelRenderer != null) {
                voxelRenderer.extract(context);
            }
        });

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
            if (voxelRenderer != null) {
                voxelRenderer.render(context);
            }
        });

        // Clear state when returning to menu
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null && voxelRenderer != null) {
                voxelRenderer.clear();
            }
        });

        Farplane.LOGGER.info("[FarPlane] Client initialization complete");
    }

    public static VoxelTerrainRenderer getVoxelRenderer() {
        return voxelRenderer;
    }
}
