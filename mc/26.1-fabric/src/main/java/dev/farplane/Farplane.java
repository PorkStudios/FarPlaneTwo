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

package dev.farplane;

import dev.farplane.config.FarplaneConfig;
import dev.farplane.engine.network.FarplaneNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for FarPlane (server/common side).
 *
 * @author FarPlane contributors
 */
public class Farplane implements ModInitializer {
    public static final String MOD_ID = "farplane";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static FarplaneConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("[FarPlane] Initializing FarPlane v0.1.0");

        config = FarplaneConfig.load();
        config.save(); // ensure file exists

        LOGGER.info("[FarPlane] Config loaded: maxLevels={}, cutoffDistance={}, effectiveRenderDistance={} blocks",
                config.maxLevels(), config.cutoffDistance(), config.effectiveRenderDistanceBlocks());

        // Register network payloads
        FarplaneNetworking.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[FarPlane] Server started — FarPlane terrain engine ready");
            // Phase 3: initialize TileProvider, Tracker, networking here
        });

        LOGGER.info("[FarPlane] Initialization complete");
    }

    public static FarplaneConfig config() {
        return config;
    }
}
