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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@Getter
@EqualsAndHashCode
@Setting.GuiCategories({
        @Setting.CategoryMeta(name = "default", title = false),
        @Setting.CategoryMeta(name = FP2Config.CATEGORY_RENDER_DISTANCE),
        @Setting.CategoryMeta(name = FP2Config.CATEGORY_THREADS),
})
public final class FP2Config {
    @SideOnly(Side.CLIENT)
    protected static final String CATEGORY_RENDER_DISTANCE = "renderDistance";
    @SideOnly(Side.CLIENT)
    protected static final String CATEGORY_THREADS = "threads";

    @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.fp2.util.Constants#MAX_LODS"))
    @Setting.RestartRequired(Setting.Requirement.WORLD)
    @Setting.GuiCategory(CATEGORY_RENDER_DISTANCE)
    private int maxLevels = 3;

    @Setting.Range(min = @Setting.Constant(0), max = @Setting.Constant(Integer.MAX_VALUE))
    @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(1024))
    @Setting.GuiCategory(CATEGORY_RENDER_DISTANCE)
    private int cutoffDistance = 256;

    @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
    @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
    @Setting.RestartRequired(Setting.Requirement.GAME)
    @Setting.GuiCategory(CATEGORY_THREADS)
    private int trackingThreads = max(PorkUtil.CPU_COUNT >> 2, 1);

    @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
    @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
    @Setting.RestartRequired(Setting.Requirement.WORLD)
    @Setting.GuiCategory(CATEGORY_THREADS)
    private int terrainThreads = max((PorkUtil.CPU_COUNT >> 1) + (PorkUtil.CPU_COUNT >> 2), 1);

    @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
    @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(field = "net.daporkchop.lib.common.util.PorkUtil#CPU_COUNT"))
    @Setting.RestartRequired(Setting.Requirement.WORLD)
    @Setting.GuiCategory(CATEGORY_THREADS)
    private int bakeThreads = max((PorkUtil.CPU_COUNT >> 1) + (PorkUtil.CPU_COUNT >> 2), 1);

    private final Performance performance = new Performance();
    private final Compatibility compatibility = new Compatibility();
    private final Debug debug = new Debug();

    /**
     * @author DaPorkchop_
     */
    @Getter
    @EqualsAndHashCode
    @Setting.GuiCategories({
            @Setting.CategoryMeta(name = "default", title = false),
            @Setting.CategoryMeta(name = Performance.CATEGORY_CLIENT),
    })
    public static class Performance {
        @SideOnly(Side.CLIENT)
        protected static final String CATEGORY_CLIENT = "client";

        @Setting.RestartRequired(Setting.Requirement.WORLD)
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private boolean gpuFrustumCulling = true;

        @Setting.Range(min = @Setting.Constant(1), max = @Setting.Constant(Integer.MAX_VALUE))
        @Setting.GuiRange(min = @Setting.Constant(1), max = @Setting.Constant(1024))
        @Setting.GuiCategory(CATEGORY_CLIENT)
        private int maxBakesProcessedPerFrame = 256;
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
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

        @Setting.GuiCategory(CATEGORY_CLIENT)
        private boolean reversedZ = true;

        @Setting.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Setting.RestartRequired(Setting.Requirement.GAME)
        private final WorkaroundState workaroundAmdVertexPadding = WorkaroundState.AUTO;

        @Setting.GuiCategory(CATEGORY_CLIENT_WORKAROUNDS)
        @Setting.RestartRequired(Setting.Requirement.GAME)
        private final WorkaroundState workaroundIntelMultidrawNotWorking = WorkaroundState.AUTO;

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
    @Getter
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

        @Setting.GuiCategory(CATEGORY_CLIENT)
        private boolean backfaceCulling = true;

        @Setting.GuiCategory(CATEGORY_CLIENT)
        private boolean vanillaTerrainRendering = true;

        @Setting.GuiCategory(CATEGORY_CLIENT)
        private boolean levelZeroRendering = true;

        @Setting.GuiCategory(CATEGORY_CLIENT)
        private final DebugColorMode debugColors = DebugColorMode.DISABLED;

        @Setting.GuiCategory(CATEGORY_SERVER)
        private boolean exactGeneration = true;

        @Setting.GuiCategory(CATEGORY_SERVER)
        private boolean levelZeroTracking = true;

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
