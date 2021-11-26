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

package net.daporkchop.fp2.client.gui.container;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gui.GuiHelper;
import net.daporkchop.fp2.client.gui.IConfigGuiElement;
import net.daporkchop.fp2.client.gui.IGuiContext;
import net.daporkchop.fp2.client.gui.access.GuiObjectAccess;
import net.daporkchop.fp2.client.gui.element.AbstractConfigGuiElement;
import net.daporkchop.fp2.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.client.gui.util.ElementBounds;
import net.daporkchop.fp2.core.config.FP2Config;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class RenderDistanceContainer extends VerticallyStackedContainer<FP2Config> {
    protected static List<IConfigGuiElement> createElements(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull List<IConfigGuiElement> elements) {
        return Arrays.asList(
                new ColumnsContainer<>(context, access, elements),
                new GuiDynamicLabel(context, access,
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
                                    GuiHelper.formatByteCount(estimatedVRAM));
                        },
                        (config, langKey) -> {
                            double factor = 1.5d;

                            int renderDistanceVanilla = MC.gameSettings.renderDistanceChunks;
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

    public RenderDistanceContainer(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull List<IConfigGuiElement> elements) {
        super(context, access, createElements(context, access, elements));
    }

    /**
     * @author DaPorkchop_
     */
    protected static class GuiDynamicLabel extends AbstractConfigGuiElement {
        protected final GuiObjectAccess<FP2Config> access;
        protected final BiFunction<FP2Config, String, LabelContents>[] labels;

        @SafeVarargs
        public GuiDynamicLabel(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull BiFunction<FP2Config, String, LabelContents>... labels) {
            super(context);

            this.access = access;
            this.labels = labels;
        }

        @Override
        protected String langKey() {
            return this.context.localeKeyBase();
        }

        protected Stream<LabelContents> labels() {
            return Stream.of(this.labels)
                    .map(textFormatter -> textFormatter.apply(this.access.getCurrent(), this.langKey()));
        }

        @Override
        public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
            return Stream.of(new ComponentDimensions(totalSizeX, min(this.heightFor(totalSizeX), totalSizeY)));
        }

        @Override
        public ComponentDimensions preferredMinimumDimensions() {
            return new ComponentDimensions(
                    this.labels()
                            .flatMap(label -> Stream.of(label.text(this.langKey(), Integer.MAX_VALUE)))
                            .mapToInt(MC.fontRenderer::getStringWidth)
                            .max().orElse(0),
                    this.heightFor(Integer.MAX_VALUE));
        }

        protected int heightFor(int width) {
            return this.labels().mapToInt(label -> label.height(this.langKey(), width)).sum() + max(this.labels.length - 1, 0) * PADDING;
        }

        @Override
        public void init() {
        }

        @Override
        public void pack() {
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            int x = this.bounds.x() + PADDING;
            int y = this.bounds.y() + PADDING;

            for (LabelContents label : this.labels().filter(LabelContents::visible).collect(Collectors.toList())) {
                for (String line : label.text(this.langKey(), this.bounds.sizeX())) {
                    MC.fontRenderer.drawStringWithShadow(line, x, y, -1);
                    y += MC.fontRenderer.FONT_HEIGHT + 1;
                }
                y += PADDING;
            }
        }

        @Override
        public Optional<String[]> getTooltip(int mouseX, int mouseY) {
            if (!this.bounds.contains(mouseX, mouseY)) {
                return Optional.empty();
            }

            int x = this.bounds.x() + PADDING;
            int y = this.bounds.y() + PADDING;

            for (LabelContents label : this.labels().filter(LabelContents::visible).collect(Collectors.toList())) {
                int height = label.height(this.langKey(), this.bounds.sizeX());
                int width = Stream.of(label.text(this.langKey(), this.bounds.sizeX())).mapToInt(MC.fontRenderer::getStringWidth).max().orElse(0);

                if (new ElementBounds(x, y, width, height).contains(mouseX, mouseY)) {
                    return label.tooltip(this.langKey());
                }

                y += height + PADDING;
            }

            return Optional.empty();
        }

        @Override
        public void mouseDown(int mouseX, int mouseY, int button) {
        }

        @Override
        public void mouseUp(int mouseX, int mouseY, int button) {
        }

        @Override
        public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        }

        @Override
        public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        }

        @Override
        public void keyPressed(char typedChar, int keyCode) {
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

            NumberFormat numberFormat = GuiHelper.numberFormat();
            this.formatArgs = Stream.of(formatArgs).map(arg -> arg instanceof Number ? numberFormat.format(arg) : arg).toArray();
        }

        public String[] text(@NonNull String langKeyBase, int width) {
            return MC.fontRenderer.listFormattedStringToWidth(I18n.format(langKeyBase + this.name, this.formatArgs), width).toArray(new String[0]);
        }

        public int height(@NonNull String langKeyBase, int width) {
            int lines = this.text(langKeyBase, width).length;
            return lines * MC.fontRenderer.FONT_HEIGHT + max(lines - 1, 0);
        }

        public Optional<String[]> tooltip(@NonNull String langKeyBase) {
            String tooltipKey = langKeyBase + this.name + ".tooltip";
            return I18n.hasKey(tooltipKey)
                    ? Optional.of(I18n.format(tooltipKey, this.formatArgs).split("\n"))
                    : Optional.empty();
        }
    }
}
