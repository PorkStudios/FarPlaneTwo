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

package net.daporkchop.fp2.core.config.gui.container;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.GuiRenderer;
import net.daporkchop.fp2.core.client.gui.container.ColumnsContainer;
import net.daporkchop.fp2.core.client.gui.container.VerticallyStackedContainer;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiElement;
import net.daporkchop.fp2.core.client.gui.element.properties.SimpleGuiElementProperties;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.core.client.gui.util.ElementBounds;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class ConfigGuiRenderDistanceContainer extends VerticallyStackedContainer {
    protected static final int T_SHIFT = 4; //TODO: remove this
    protected static final int T_VOXELS = 1 << T_SHIFT;

    protected static List<GuiElement> createElements(@NonNull GuiContext context, @NonNull ConfigGuiObjectAccess<FP2Config> access, @NonNull List<GuiElement> elements) {
        return Arrays.asList(
                new ColumnsContainer(context, elements),
                new GuiDynamicLabel(context, MODID + ".config.menu.", access,
                        (config, langKey) -> {
                            long renderDistanceBlocks = config.effectiveRenderDistanceBlocks();

                            return new LabelContents(
                                    "labelEquivalentRenderDistance",
                                    true,
                                    renderDistanceBlocks, renderDistanceBlocks >> 4L);
                        },
                        (config, langKey) -> {
                            //approximation: tested in a vanilla world, 1024 cutoff with 1 level had 65247 renderable tiles and went down
                            //  to almost exactly 1/4 when cutoff was reduced to 512
                            //  -> for vanilla-style worlds, we can imagine the world as being conceptually flat

                            long renderDistanceBlocks = config.effectiveRenderDistanceBlocks();
                            long renderDistanceChunks = renderDistanceBlocks >> 4L;

                            //test case with 1024 cutoff had 65247 tiles loaded, use that to compute average tile count
                            int testCaseCutoff = 1024;
                            int testCaseLoadedTiles = 65247;

                            double avgVoxelTilesPerColumn = testCaseLoadedTiles / (double) sq(asrRound(testCaseCutoff, T_SHIFT) * 2L + 1L);

                            long estimatedTileColumns = sq(asrRound(config.cutoffDistance(), T_SHIFT) * 2L + 1L) * config.maxLevels();
                            long estimatedChunkColumns = sq(renderDistanceChunks * 2L + 1L) * config.maxLevels();

                            double estimatedVoxelTiles = estimatedTileColumns * avgVoxelTilesPerColumn;
                            double estimatedChunkTiles = estimatedChunkColumns * avgVoxelTilesPerColumn;

                            double performanceFactor = estimatedChunkTiles / estimatedVoxelTiles;

                            return new LabelContents(
                                    "labelApproximatePerformance",
                                    true,
                                    performanceFactor);
                        },
                        (config, langKey) -> {
                            //approximation: tested in a vanilla world, 1024 cutoff with 1 level used about 688MiB and went down
                            //  to almost exactly 1/4 when cutoff was reduced to 512
                            //  -> for vanilla-style worlds, we can imagine the world as being conceptually flat

                            //test case with 1024 cutoff had 65247 tiles loaded and used 689MiB, use that to compute average VRAM usage per tile
                            int testCaseCutoff = 1024;
                            int testCaseLoadedTiles = 65247;
                            long testCaseVRAM = 689L * 1024L * 1024L;

                            double avgVoxelTilesPerColumn = testCaseLoadedTiles / (double) sq(asrRound(testCaseCutoff, T_SHIFT) * 2L + 1L);
                            double avgVRAMPerVoxelTile = testCaseVRAM / (double) testCaseLoadedTiles;

                            long estimatedTileColumns = sq(asrRound(config.cutoffDistance(), T_SHIFT) * 2L + 1L) * config.maxLevels();
                            double estimatedVoxelTiles = estimatedTileColumns * avgVoxelTilesPerColumn;
                            long estimatedVRAM = roundL(estimatedVoxelTiles * avgVRAMPerVoxelTile);

                            return new LabelContents(
                                    "labelApproximateVRAM",
                                    true,
                                    fp2().i18n().formatByteCount(estimatedVRAM));
                        },
                        (config, langKey) -> {
                            double factor = 1.5d;

                            int renderDistanceVanilla = fp2().vanillaRenderDistanceChunks();
                            int renderDistanceBlocks = renderDistanceVanilla << 4;
                            int recommendedCutoff = roundUp(floorI(renderDistanceBlocks * factor), T_VOXELS);

                            return new LabelContents(
                                    "labelCutoffDistanceLessThanVanilla",
                                    config.cutoffDistance() < recommendedCutoff,
                                    factor, renderDistanceVanilla, renderDistanceBlocks, recommendedCutoff);
                        },
                        (config, langKey) -> {
                            final int maxCutoff = 400;

                            return new LabelContents(
                                    "labelCutoffDistanceTooHigh",
                                    config.cutoffDistance() >= maxCutoff);
                        },
                        (config, langKey) -> {
                            final int minCutoff = 64;

                            return new LabelContents(
                                    "labelCutoffDistanceTooSmall",
                                    config.cutoffDistance() < minCutoff,
                                    minCutoff);
                        }));
    }

    public ConfigGuiRenderDistanceContainer(@NonNull GuiContext context, @NonNull ConfigGuiObjectAccess<FP2Config> access, @NonNull List<GuiElement> elements) {
        super(context, createElements(context, access, elements));
    }

    /**
     * @author DaPorkchop_
     */
    protected static class GuiDynamicLabel extends AbstractGuiElement {
        protected final String localeKey;
        protected final ConfigGuiObjectAccess<FP2Config> access;
        protected final BiFunction<FP2Config, String, LabelContents>[] labels;

        public GuiDynamicLabel(@NonNull GuiContext context, @NonNull String localeKey, @NonNull ConfigGuiObjectAccess<FP2Config> access, @NonNull BiFunction<FP2Config, String, LabelContents>... labels) {
            super(context, new SimpleGuiElementProperties(localeKey));

            this.localeKey = localeKey;
            this.access = access;
            this.labels = labels;
        }

        protected Stream<LabelContents> labels() {
            return Stream.of(this.labels)
                    .map(textFormatter -> textFormatter.apply(this.access.getCurrent(), this.localeKey));
        }

        @Override
        public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
            return Stream.of(new ComponentDimensions(totalSizeX, min(this.heightFor(totalSizeX), totalSizeY)));
        }

        @Override
        public ComponentDimensions preferredMinimumDimensions() {
            return new ComponentDimensions(
                    this.labels()
                            .flatMap(label -> Stream.of(label.text(this.context.renderer(), this.localeKey, Integer.MAX_VALUE)))
                            .mapToInt(this.context.renderer()::getStringWidth)
                            .max().orElse(0),
                    this.heightFor(Integer.MAX_VALUE));
        }

        protected int heightFor(int width) {
            return this.labels().mapToInt(label -> label.height(this.context.renderer(), this.localeKey, width)).sum() + max(this.labels.length - 1, 0) * PADDING;
        }

        @Override
        public void render(int mouseX, int mouseY) {
            int x = this.bounds.x() + PADDING;
            int y = this.bounds.y() + PADDING;

            for (LabelContents label : this.labels().filter(LabelContents::visible).collect(Collectors.toList())) {
                int startY = y;
                int maxWidth = 0;

                for (CharSequence line : label.text(this.context.renderer(), this.localeKey, this.bounds.sizeX())) {
                    maxWidth = max(maxWidth, this.context.renderer().getStringWidth(line));

                    this.context.renderer().drawString(line, x, y, -1, true);
                    y += this.context.renderer().getStringHeight() + 1;
                }
                y += PADDING;

                if (new ElementBounds(x, startY, maxWidth, y - startY - PADDING).contains(mouseX, mouseY)) {
                    label.tooltip(this.localeKey).ifPresent(lines -> this.context.renderer().drawTooltip(mouseX, mouseY, -1, lines));
                }
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
    protected static class LabelContents {
        protected final String name;
        protected final boolean visible;
        protected final Object[] formatArgs;

        public LabelContents(@NonNull String name, boolean visible, @NonNull Object... formatArgs) {
            this.name = name;
            this.visible = visible;

            NumberFormat numberFormat = fp2().i18n().numberFormat();
            this.formatArgs = Stream.of(formatArgs).map(arg -> arg instanceof Number ? numberFormat.format(arg) : arg).toArray();
        }

        public CharSequence[] text(@NonNull GuiRenderer renderer, @NonNull String langKeyBase, int width) {
            return renderer.wrapStringToWidth(fp2().i18n().format(langKeyBase + this.name, this.formatArgs), width).toArray(new CharSequence[0]);
        }

        public int height(@NonNull GuiRenderer renderer, @NonNull String langKeyBase, int width) {
            int lines = this.text(renderer, langKeyBase, width).length;
            return lines * renderer.getStringHeight() + max(lines - 1, 0);
        }

        public Optional<String[]> tooltip(@NonNull String langKeyBase) {
            String tooltipKey = langKeyBase + this.name + ".tooltip";
            return fp2().i18n().hasKey(tooltipKey)
                    ? Optional.of(fp2().i18n().format(tooltipKey, this.formatArgs).split("\n"))
                    : Optional.empty();
        }
    }
}
