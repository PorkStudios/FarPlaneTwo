/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.core.config;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.With;
import net.daporkchop.fp2.core.config.gui.container.ConfigGuiRenderDistanceContainer;
import net.daporkchop.fp2.core.config.gui.element.ConfigGuiRenderModeButton;
import net.daporkchop.lib.common.misc.Cloneable;
import net.daporkchop.lib.common.util.PorkUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Builder(access = AccessLevel.PRIVATE, toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@With
@ToString
@EqualsAndHashCode
@Config.GuiCategories({
        @Config.CategoryMeta(name = "default", title = false),
        @Config.CategoryMeta(name = FP2Config.CATEGORY_RENDER_DISTANCE, containerClass = ConfigGuiRenderDistanceContainer.class),
})
public final class FP2Config implements Cloneable<FP2Config> {
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    protected static final String CATEGORY_RENDER_DISTANCE = "renderDistance";

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
    public static FP2Config load(@NonNull Path configDir) {
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
    public static void save(@NonNull Path configDir, @NonNull FP2Config config) {
        Files.createDirectories(configDir);
        Path tempConfigFile = configDir.resolve(CONFIG_FILE_NAME + ".tmp");
        Path realConfigFile = configDir.resolve(CONFIG_FILE_NAME);

        //write whole config to temporary file and sync to storage device, then atomically replace the existing one
        Files.write(tempConfigFile, GSON_PRETTY.toJson(config).getBytes(StandardCharsets.UTF_8), WRITE, CREATE, TRUNCATE_EXISTING, SYNC);
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

        return clientConfig.toBuilder()
                .maxLevels(min(serverConfig.maxLevels(), clientConfig.maxLevels()))
                .cutoffDistance(min(serverConfig.cutoffDistance(), clientConfig.cutoffDistance()))
                .renderModes(Stream.of(clientConfig.renderModes()).filter(ImmutableSet.copyOf(serverConfig.renderModes())::contains).toArray(String[]::new))
                .build();
    }

    @Builder.Default
    @Config.Range(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.fp2.mode.voxel.VoxelConstants#V_MAX_LODS"))
    @Config.GuiCategory(CATEGORY_RENDER_DISTANCE)
    @Config.GuiShowServerValue
    private final int maxLevels = preventInline(3);

    @Builder.Default
    @Config.Range(min = @Config.Constant(0), max = @Config.Constant(Integer.MAX_VALUE))
    @Config.GuiRange(min = @Config.Constant(field = "net.daporkchop.fp2.util.Constants#T_VOXELS"), max = @Config.Constant(1024), snapTo = @Config.Constant(field = "net.daporkchop.fp2.util.Constants#T_VOXELS"))
    @Config.GuiCategory(CATEGORY_RENDER_DISTANCE)
    @Config.GuiShowServerValue
    private final int cutoffDistance = preventInline(256);

    @Builder.Default
    @Config.GuiElementClass(ConfigGuiRenderModeButton.class)
    @NonNull
    private final String[] renderModes = fp2().renderModeNames();

    @Builder.Default
    @NonNull
    private final Performance performance = new Performance();

    @Builder.Default
    @NonNull
    private final Compatibility compatibility = new Compatibility();

    @Builder.Default
    @NonNull
    private final Debug debug = new Debug();

    /**
     * Cleans up this config, eliminating any impossible values.
     *
     * @return the cleaned config
     */
    public FP2Config clean() {
        return this.withRenderModes(Stream.of(this.renderModes)
                .filter(ImmutableSet.copyOf(fp2().renderModeNames())::contains)
                .toArray(String[]::new));
    }

    /**
     * @return the effective render distance, in blocks
     */
    public long effectiveRenderDistanceBlocks() {
        return (long) this.cutoffDistance << (this.maxLevels - 1L);
    }

    @Override
    public FP2Config clone() {
        return this.toBuilder()
                .renderModes(this.renderModes.clone())
                .performance(this.performance.clone())
                .compatibility(this.compatibility.clone())
                .debug(this.debug.clone())
                .build();
    }

    /**
     * @author DaPorkchop_
     */
    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor
    @Getter
    @With
    @ToString
    @EqualsAndHashCode
    @Config.GuiCategories({
            @Config.CategoryMeta(name = "default", title = false),
            @Config.CategoryMeta(name = Performance.CATEGORY_CLIENT),
            @Config.CategoryMeta(name = Performance.CATEGORY_THREADS),
    })
    public static class Performance implements Cloneable<Performance> {
        protected static final String CATEGORY_CLIENT = "client";
        protected static final String CATEGORY_THREADS = "threads";

        @Builder.Default
        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean gpuFrustumCulling = preventInline(true);

        @Builder.Default
        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(1024))
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final int maxBakesProcessedPerFrame = preventInline(256);

        @Builder.Default
        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Config.RestartRequired(Config.Requirement.GAME)
        @Config.GuiCategory(CATEGORY_THREADS)
        private final int trackingThreads = max(PorkUtil.CPU_COUNT >> 2, 1);

        @Builder.Default
        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_THREADS)
        private final int terrainThreads = max((PorkUtil.CPU_COUNT >> 1) + (PorkUtil.CPU_COUNT >> 2), 1);

        @Builder.Default
        @Config.Range(min = @Config.Constant(1), max = @Config.Constant(Integer.MAX_VALUE))
        @Config.GuiRange(min = @Config.Constant(1), max = @Config.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Config.RestartRequired(Config.Requirement.WORLD)
        @Config.GuiCategory(CATEGORY_THREADS)
        private final int bakeThreads = max((PorkUtil.CPU_COUNT >> 1) + (PorkUtil.CPU_COUNT >> 2), 1);

        @Override
        public Performance clone() {
            return this.toBuilder().build();
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor
    @Getter
    @With
    @ToString
    @EqualsAndHashCode
    @Config.GuiCategories({
            @Config.CategoryMeta(name = "default", title = false),
            @Config.CategoryMeta(name = Compatibility.CATEGORY_CLIENT),
            @Config.CategoryMeta(name = Compatibility.CATEGORY_CLIENT_WORKAROUNDS),
    })
    public static class Compatibility implements Cloneable<Compatibility> {
        protected static final String CATEGORY_CLIENT = "client";
        protected static final String CATEGORY_CLIENT_WORKAROUNDS = "clientWorkarounds";

        @Builder.Default
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean reversedZ = preventInline(true);

        @Builder.Default
        @Config.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Config.RestartRequired(Config.Requirement.GAME)
        @NonNull
        private final WorkaroundState workaroundAmdVertexPadding = preventInline(WorkaroundState.AUTO);

        @Builder.Default
        @Config.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Config.RestartRequired(Config.Requirement.GAME)
        @NonNull
        private final WorkaroundState workaroundIntelMultidrawNotWorking = preventInline(WorkaroundState.AUTO);

        @Override
        public Compatibility clone() {
            return this.toBuilder().build();
        }

        /**
         * @author DaPorkchop_
         */
        public enum WorkaroundState {
            AUTO {
                @Override
                public boolean shouldEnable(boolean flag) {
                    return flag;
                }
            },
            ENABLED {
                @Override
                public boolean shouldEnable(boolean flag) {
                    return true;
                }
            },
            DISABLED {
                @Override
                public boolean shouldEnable(boolean flag) {
                    return false;
                }
            };

            public abstract boolean shouldEnable(boolean flag);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor
    @Getter
    @With
    @ToString
    @EqualsAndHashCode
    @Config.GuiCategories({
            @Config.CategoryMeta(name = "default", title = false),
            @Config.CategoryMeta(name = Debug.CATEGORY_CLIENT),
            @Config.CategoryMeta(name = Debug.CATEGORY_SERVER),
    })
    public static class Debug implements Cloneable<Debug> {
        protected static final String CATEGORY_CLIENT = "client";
        protected static final String CATEGORY_SERVER = "server";

        @Builder.Default
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean backfaceCulling = preventInline(true);

        @Builder.Default
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean vanillaTerrainRendering = preventInline(true);

        @Builder.Default
        @Config.GuiCategory(CATEGORY_CLIENT)
        private final boolean levelZeroRendering = preventInline(true);

        @Builder.Default
        @Config.GuiCategory(CATEGORY_CLIENT)
        @NonNull
        private final DebugColorMode debugColors = preventInline(DebugColorMode.DISABLED);

        @Builder.Default
        @Config.GuiCategory(CATEGORY_SERVER)
        @Config.GuiShowServerValue
        private final boolean exactGeneration = preventInline(true);

        @Builder.Default
        @Config.GuiCategory(CATEGORY_SERVER)
        @Config.GuiShowServerValue
        private final boolean levelZeroTracking = preventInline(true);

        @Override
        public Debug clone() {
            return this.toBuilder().build();
        }

        /**
         * @author DaPorkchop_
         */
        @RequiredArgsConstructor
        @Getter
        public enum DebugColorMode {
            DISABLED(false),
            LEVEL(true),
            POSITION(true),
            NORMAL(true);

            protected final boolean enable;
        }
    }
}
