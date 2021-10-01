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

package net.daporkchop.fp2.config;

import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import net.daporkchop.fp2.config.gui.container.RenderDistanceContainer;
import net.daporkchop.fp2.config.gui.element.GuiRenderModeButton;
import net.daporkchop.fp2.config.listener.ConfigListenerManager;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Builder(access = AccessLevel.PRIVATE, toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@With
@EqualsAndHashCode
@Setting.GuiCategories({
        @Setting.CategoryMeta(name = "default", title = false),
        @Setting.CategoryMeta(name = FP2Config.CATEGORY_RENDER_DISTANCE, containerClass = RenderDistanceContainer.class),
})
public final class FP2Config {
    @SideOnly(Side.CLIENT)
    protected static final String CATEGORY_RENDER_DISTANCE = "renderDistance";

    private static final Path CONFIG_DIR = Loader.instance().getConfigDir().toPath();
    private static final String CONFIG_FILE_NAME = "fp2.json5";

    public static final FP2Config DEFAULT_CONFIG = new FP2Config();
    private static FP2Config GLOBAL_CONFIG;

    /**
     * Parses an {@link FP2Config} instance from the given JSON string.
     *
     * @param json the JSON string
     * @return the parsed {@link FP2Config}
     */
    public static FP2Config parse(@NonNull String json) {
        return GSON.fromJson(json, FP2Config.class);
    }

    /**
     * Loads the global configuration from disk, falling back to the default configuration if needed.
     * <p>
     * This method may only be called once.
     */
    @SneakyThrows(IOException.class)
    public synchronized static void load() {
        checkState(GLOBAL_CONFIG == null, "global configuration has already been loaded!");

        //delete temporary config file (will only be present if the system crashed while saving config)
        Files.deleteIfExists(CONFIG_DIR.resolve(CONFIG_FILE_NAME + ".tmp"));

        Path configFile = CONFIG_DIR.resolve(CONFIG_FILE_NAME);
        if (Files.exists(configFile)) { //config file already exists, read it
            GLOBAL_CONFIG = parse(new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8));
        } else { //config file doesn't exist, set it to the default config and then save it
            GLOBAL_CONFIG = DEFAULT_CONFIG;
            set(GLOBAL_CONFIG);
        }
    }

    /**
     * Sets the current global configuration and writes it to disk.
     *
     * @param config the new global configuration
     * @throws IllegalStateException if the global configuration hasn't been loaded using {@link #load()}
     */
    @SneakyThrows(IOException.class)
    public synchronized static void set(@NonNull FP2Config config) {
        checkState(GLOBAL_CONFIG != null, "global configuration hasn't been loaded!");

        Files.createDirectories(CONFIG_DIR);
        Path tempConfigFile = CONFIG_DIR.resolve(CONFIG_FILE_NAME + ".tmp");
        Path realConfigFile = CONFIG_DIR.resolve(CONFIG_FILE_NAME);

        //write whole config to temporary file and sync to storage device, then atomically replace the existing one
        Files.write(tempConfigFile, GSON_PRETTY.toJson(config).getBytes(StandardCharsets.UTF_8), WRITE, CREATE, TRUNCATE_EXISTING, SYNC);
        Files.move(tempConfigFile, realConfigFile, REPLACE_EXISTING, ATOMIC_MOVE);

        GLOBAL_CONFIG = config;
        ConfigListenerManager.fire();
    }

    /**
     * @return the current global configuration
     * @throws IllegalStateException if the global configuration hasn't been loaded using {@link #load()}
     */
    public static FP2Config global() {
        FP2Config config = GLOBAL_CONFIG;
        checkState(config != null, "global configuration hasn't been loaded!");
        return config;
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
                .renderModes(Stream.of(serverConfig.renderModes()).filter(ImmutableSet.copyOf(clientConfig.renderModes)::contains).toArray(String[]::new))
                .build();
    }

    @Builder.Default
    @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.fp2.util.Constants#MAX_LODS"))
    @Setting.GuiCategory(CATEGORY_RENDER_DISTANCE)
    private final int maxLevels = preventInline(3);

    @Builder.Default
    @Setting.Range(min = @Setting.Constant(0), max = @Setting.Constant(Integer.MAX_VALUE))
    @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(1024))
    @Setting.GuiCategory(CATEGORY_RENDER_DISTANCE)
    private final int cutoffDistance = preventInline(256);

    @Builder.Default
    @Setting.GuiElementClass(GuiRenderModeButton.class)
    private final String[] renderModes = IFarRenderMode.REGISTRY.stream()
            .map(Map.Entry::getKey)
            .toArray(String[]::new);

    @Builder.Default
    private final Performance performance = new Performance();

    @Builder.Default
    private final Compatibility compatibility = new Compatibility();

    @Builder.Default
    private final Debug debug = new Debug();

    /**
     * @return this configuration encoded as a JSON string
     */
    @Override
    public String toString() {
        return GSON.toJson(this);
    }

    /**
     * @author DaPorkchop_
     */
    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor
    @Getter
    @With
    @EqualsAndHashCode
    @Setting.GuiCategories({
            @Setting.CategoryMeta(name = "default", title = false),
            @Setting.CategoryMeta(name = Performance.CATEGORY_CLIENT),
            @Setting.CategoryMeta(name = Performance.CATEGORY_THREADS),
    })
    public static class Performance {
        @SideOnly(Side.CLIENT)
        protected static final String CATEGORY_CLIENT = "client";
        @SideOnly(Side.CLIENT)
        protected static final String CATEGORY_THREADS = "threads";

        @Builder.Default
        @Setting.RestartRequired(Setting.Requirement.WORLD)
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final boolean gpuFrustumCulling = preventInline(true);

        @Builder.Default
        @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
        @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(1024))
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final int maxBakesProcessedPerFrame = preventInline(256);

        @Builder.Default
        @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
        @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Setting.RestartRequired(Setting.Requirement.GAME)
        @Setting.GuiCategory(CATEGORY_THREADS)
        private final int trackingThreads = max(PorkUtil.CPU_COUNT >> 2, 1);

        @Builder.Default
        @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
        @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Setting.RestartRequired(Setting.Requirement.WORLD)
        @Setting.GuiCategory(CATEGORY_THREADS)
        private final int terrainThreads = max((PorkUtil.CPU_COUNT >> 1) + (PorkUtil.CPU_COUNT >> 2), 1);

        @Builder.Default
        @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
        @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
        @Setting.RestartRequired(Setting.Requirement.WORLD)
        @Setting.GuiCategory(CATEGORY_THREADS)
        private final int bakeThreads = max((PorkUtil.CPU_COUNT >> 1) + (PorkUtil.CPU_COUNT >> 2), 1);
    }

    /**
     * @author DaPorkchop_
     */
    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor
    @Getter
    @With
    @EqualsAndHashCode
    @Setting.GuiCategories({
            @Setting.CategoryMeta(name = "default", title = false),
            @Setting.CategoryMeta(name = Compatibility.CATEGORY_CLIENT),
            @Setting.CategoryMeta(name = Compatibility.CATEGORY_CLIENT_WORKAROUNDS),
    })
    public static class Compatibility {
        @SideOnly(Side.CLIENT)
        protected static final String CATEGORY_CLIENT = "client";
        @SideOnly(Side.CLIENT)
        protected static final String CATEGORY_CLIENT_WORKAROUNDS = "clientWorkarounds";

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final boolean reversedZ = preventInline(true);

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Setting.RestartRequired(Setting.Requirement.GAME)
        private final WorkaroundState workaroundAmdVertexPadding = preventInline(WorkaroundState.AUTO);

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Setting.RestartRequired(Setting.Requirement.GAME)
        private final WorkaroundState workaroundIntelMultidrawNotWorking = preventInline(WorkaroundState.AUTO);

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
    @EqualsAndHashCode
    @Setting.GuiCategories({
            @Setting.CategoryMeta(name = "default", title = false),
            @Setting.CategoryMeta(name = Debug.CATEGORY_CLIENT),
            @Setting.CategoryMeta(name = Debug.CATEGORY_SERVER),
    })
    public static class Debug {
        @SideOnly(Side.CLIENT)
        protected static final String CATEGORY_CLIENT = "client";
        @SideOnly(Side.CLIENT)
        protected static final String CATEGORY_SERVER = "server";

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final boolean backfaceCulling = preventInline(true);

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final boolean vanillaTerrainRendering = preventInline(true);

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final boolean levelZeroRendering = preventInline(true);

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final DebugColorMode debugColors = preventInline(DebugColorMode.DISABLED);

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_SERVER)
        private final boolean exactGeneration = preventInline(true);

        @Builder.Default
        @Setting.GuiCategory(CATEGORY_SERVER)
        private final boolean levelZeroTracking = preventInline(true);

        /**
         * @author DaPorkchop_
         */
        @RequiredArgsConstructor
        @Getter
        public enum DebugColorMode {
            DISABLED(false),
            LEVEL(true),
            POSITION(true),
            FACE_NORMAL(true);

            protected final boolean enable;
        }
    }
}
