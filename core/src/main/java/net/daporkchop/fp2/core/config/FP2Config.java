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

package net.daporkchop.fp2.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.With;
import net.daporkchop.fp2.core.config.gui.container.ConfigGuiRenderDistanceContainer;
import net.daporkchop.lib.common.misc.Cloneable;
import net.daporkchop.lib.common.util.PorkUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@With
@Config.GuiCategories({
        @Config.CategoryMeta(name = "default", title = false),
        @Config.CategoryMeta(name = FP2Config.CATEGORY_RENDER_DISTANCE, containerClass = ConfigGuiRenderDistanceContainer.class),
})
public final class FP2Config implements Cloneable<FP2Config> {
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    static final String CATEGORY_RENDER_DISTANCE = "renderDistance";

    private static final String CONFIG_FILE_NAME = "fp2.json5";

    public static final FP2Config DEFAULT_CONFIG = new FP2Config();

    /**
     * Parses an {@link FP2Config} instance from the given JSON string.
     *
     * @param json the JSON string
     * @return the parsed {@link FP2Config}
     */
    public static FP2Config fromJson(@NonNull String json) {
        FP2Config config = GSON.fromJson(json, FP2Config.class);
        return config != null ? ConfigHelper.validateConfig(config.clean()) : null;
    }

    /**
     * Encodes an {@link FP2Config} instance to a JSON string.
     *
     * @param config the {@link FP2Config} instance
     * @return the JSON-encoded config
     */
    public static String toJson(FP2Config config) {
        return GSON.toJson(config);
    }

    /**
     * Loads the config from the given directory, falling back to the default configuration if needed.
     */
    @SneakyThrows(IOException.class)
    public synchronized static FP2Config load(@NonNull Path configDir) {
        //delete temporary config file (will only be present if the system crashed while saving config)
        Files.deleteIfExists(configDir.resolve(CONFIG_FILE_NAME + ".tmp"));

        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        if (Files.exists(configFile)) { //config file already exists, read it
            return fromJson(new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8));
        } else { //config file doesn't exist, set it to the default config and then save it
            save(configDir, DEFAULT_CONFIG);
            return DEFAULT_CONFIG;
        }
    }

    /**
     * Saves the given config instance into the given config directory.
     *
     * @param config the new global configuration
     */
    @SneakyThrows(IOException.class)
    public synchronized static void save(@NonNull Path configDir, @NonNull FP2Config config) {
        Files.createDirectories(configDir);
        Path tempConfigFile = configDir.resolve(CONFIG_FILE_NAME + ".tmp");
        Path realConfigFile = configDir.resolve(CONFIG_FILE_NAME);

        byte[] configBytes = GSON_PRETTY.toJson(config).getBytes(StandardCharsets.UTF_8);

        OpenOption[] openOptions = FP2_DEBUG
                ? new OpenOption[0] //don't sync the config to disk in debug mode
                : new OpenOption[]{ CREATE, TRUNCATE_EXISTING, SYNC };

        //write whole config to temporary file, then atomically replace the existing one
        Files.write(tempConfigFile, configBytes, openOptions);
        Files.move(tempConfigFile, realConfigFile, REPLACE_EXISTING, ATOMIC_MOVE);
    }

    /**
     * Merges the given server and client configurations.
     *
     * @param serverConfig the server's configuration
     * @param clientConfig the client's configuration
     * @return the merged configuration, or {@code null}
     */
    public static FP2Config merge(FP2Config serverConfig, FP2Config clientConfig) {
        if (serverConfig == null || clientConfig == null) { //client config is already null, do nothing lol
            return null;
        }

        return new FP2Config(
                Math.min(serverConfig.maxLevels, clientConfig.maxLevels),
                Math.min(serverConfig.cutoffDistance, clientConfig.cutoffDistance),
                serverConfig.performance,
                serverConfig.compatibility,
                serverConfig.quality,
                serverConfig.debug);
    }

    @Config.Range(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.fp2.core.engine.EngineConstants#MAX_LODS"))
    @Config.GuiCategory(CATEGORY_RENDER_DISTANCE)
    @Config.GuiShowServerValue
    private final int maxLevels;

    @Config.Range(min = @Config.Constant(0), max = @Config.Constant(Integer.MAX_VALUE))
    @Config.GuiRange(min = @Config.Constant(field = "net.daporkchop.fp2.core.engine.EngineConstants#T_VOXELS"), max = @Config.Constant(1024), snapTo = @Config.Constant(field = "net.daporkchop.fp2.core.engine.EngineConstants#T_VOXELS"))
    @Config.GuiCategory(CATEGORY_RENDER_DISTANCE)
    @Config.GuiShowServerValue
    private final int cutoffDistance;

    private final @NonNull Performance performance;
    private final @NonNull Compatibility compatibility;
    private final @NonNull Quality quality;
    private final @NonNull Debug debug;

    private FP2Config() {
        this.maxLevels = 3;
        this.cutoffDistance = 256;

        this.performance = new Performance();
        this.compatibility = new Compatibility();
        this.quality = new Quality();
        this.debug = new Debug();
    }

    /**
     * Cleans up this config, eliminating any impossible values.
     *
     * @return the cleaned config
     */
    //TODO: do i want/need to keep this?
    public FP2Config clean() {
        return this;
    }

    /**
     * @return the effective render distance, in blocks
     */
    public long effectiveRenderDistanceBlocks() {
        return (long) this.cutoffDistance << (this.maxLevels - 1);
    }

    @Override
    public FP2Config clone() {
        return new FP2Config(
                this.maxLevels,
                this.cutoffDistance,
                this.performance.clone(),
                this.compatibility.clone(),
                this.quality.clone(),
                this.debug.clone());
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    @With
    @Config.GuiCategories({
            @Config.CategoryMeta(name = "default", title = false),
            @Config.CategoryMeta(name = Performance.CATEGORY_CLIENT),
    })
    public static final class Quality implements Cloneable<Quality> {
        static final String CATEGORY_CLIENT = "client";

        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean forceBlockyMesh;

        Quality() {
            this.forceBlockyMesh = false;
        }

        @Override
        public Quality clone() {
            return (Quality) super.clone();
        }
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    @With
    @Config.GuiCategories({
            @Config.CategoryMeta(name = "default", title = false),
            @Config.CategoryMeta(name = Performance.CATEGORY_CLIENT),
            @Config.CategoryMeta(name = Performance.CATEGORY_THREADS),
    })
    public static final class Performance implements Cloneable<Performance> {
        static final String CATEGORY_CLIENT = "client";
        static final String CATEGORY_THREADS = "threads";

        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean gpuFrustumCulling;

        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(1024))
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final int maxBakesProcessedPerFrame;

        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean renderQuads;

        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Config.RestartRequired(Config.Requirement.GAME)
        @Config.GuiCategory(CATEGORY_THREADS)
        private final int trackingThreads;

        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_THREADS)
        private final int terrainThreads;

        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_THREADS)
        private final int bakeThreads;

        Performance() {
            this.gpuFrustumCulling = true;
            this.maxBakesProcessedPerFrame = 256;
            this.renderQuads = true;

            this.trackingThreads = Math.max(PorkUtil.CPU_COUNT >> 2, 1);
            this.terrainThreads =
            this.bakeThreads = Math.max((PorkUtil.CPU_COUNT >> 1) + (PorkUtil.CPU_COUNT >> 2), 1);
        }

        @Override
        public Performance clone() {
            return (Performance) super.clone();
        }
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    @With
    @Config.GuiCategories({
            @Config.CategoryMeta(name = "default", title = false),
            @Config.CategoryMeta(name = Compatibility.CATEGORY_CLIENT),
            @Config.CategoryMeta(name = Compatibility.CATEGORY_CLIENT_WORKAROUNDS),
    })
    public static final class Compatibility implements Cloneable<Compatibility> {
        static final String CATEGORY_CLIENT = "client";
        static final String CATEGORY_CLIENT_WORKAROUNDS = "clientWorkarounds";

        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean reversedZ;

        @Config.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Config.RestartRequired(Config.Requirement.GAME)
        @NonNull
        private final WorkaroundState workaroundAmdVertexPadding;

        @Config.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Config.RestartRequired(Config.Requirement.GAME)
        @NonNull
        private final WorkaroundState workaroundIntelMultidrawNotWorking;

        Compatibility() {
            this.reversedZ = true;

            this.workaroundAmdVertexPadding = WorkaroundState.AUTO;
            this.workaroundIntelMultidrawNotWorking = WorkaroundState.AUTO;
        }

        @Override
        public Compatibility clone() {
            return (Compatibility) super.clone();
        }

        /**
         * @author DaPorkchop_
         */
        public enum WorkaroundState {
            AUTO,
            ENABLED,
            DISABLED,
        }
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    @With
    @Config.GuiCategories({
            @Config.CategoryMeta(name = "default", title = false),
            @Config.CategoryMeta(name = Debug.CATEGORY_CLIENT),
            @Config.CategoryMeta(name = Debug.CATEGORY_SERVER),
            @Config.CategoryMeta(name = Debug.CATEGORY_STORAGE),
    })
    public static final class Debug implements Cloneable<Debug> {
        static final String CATEGORY_CLIENT = "client";
        static final String CATEGORY_SERVER = "server";
        static final String CATEGORY_STORAGE = "storage";

        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean backfaceCulling;

        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean vanillaTerrainRendering;

        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean levelZeroRendering;

        @Config.GuiCategory(CATEGORY_CLIENT)
        @NonNull
        private final DebugColorMode debugColors;

        @Config.GuiCategory(CATEGORY_SERVER)
        @Config.GuiShowServerValue
        private final boolean exactGeneration;

        @Config.GuiCategory(CATEGORY_SERVER)
        @Config.GuiShowServerValue
        private final boolean levelZeroTracking;

        @Config.GuiCategory(CATEGORY_STORAGE)
        @Config.GuiShowServerValue
        @Config.RestartRequired(Config.Requirement.WORLD)
        private final boolean memoryStorage;

        @Config.GuiCategory(CATEGORY_STORAGE)
        @Config.GuiShowServerValue
        @Config.RestartRequired(Config.Requirement.WORLD)
        private final boolean uncompressedStorage;

        Debug() {
            this.backfaceCulling = true;
            this.vanillaTerrainRendering = true;
            this.levelZeroRendering = true;
            this.debugColors = DebugColorMode.DISABLED;
            this.exactGeneration = true;
            this.levelZeroTracking = true;
            this.memoryStorage = false;
            this.uncompressedStorage = false;
        }

        @Override
        public Debug clone() {
            return (Debug) super.clone();
        }

        /**
         * @author DaPorkchop_
         */
        public enum DebugColorMode { //synced with resources/assets/fp2/shaders/util/debug_color_mode.glsl
            DISABLED,
            LEVEL,
            POSITION,
            NORMAL,
        }
    }
}
