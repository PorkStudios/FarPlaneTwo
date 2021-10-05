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

import lombok.NonNull;
import net.daporkchop.fp2.client.gui.IConfigGuiElement;
import net.daporkchop.fp2.client.gui.IGuiContext;
import net.daporkchop.fp2.client.gui.access.GuiObjectAccess;
import net.daporkchop.fp2.client.gui.element.AbstractConfigGuiElement;
import net.daporkchop.fp2.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.config.FP2Config;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class RenderDistanceContainer extends VerticallyStackedContainer<FP2Config> {
    protected static List<IConfigGuiElement> createElements(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull List<IConfigGuiElement> elements) {
        NumberFormat numberFormat = NumberFormat.getInstance(MC.languageManager.getCurrentLanguage().getJavaLocale());

        return Arrays.asList(
                new ColumnsContainer<>(context, access, elements),
                new DynamicLabel(context, access,
                        (config, langKey) -> {
                            int renderDistanceBlocks = config.cutoffDistance() << (config.maxLevels() - 1);
                            return I18n.format(langKey + "labelEquivalentRenderDistance", numberFormat.format(renderDistanceBlocks), numberFormat.format(renderDistanceBlocks >> 4)).split("\n");
                        },
                        (config, langKey) -> {
                            int renderDistanceVanilla = MC.gameSettings.renderDistanceChunks;

                            String[] lines = I18n.format(langKey + "labelCutoffDistanceLessThanVanilla", numberFormat.format(renderDistanceVanilla), numberFormat.format(renderDistanceVanilla << 4), numberFormat.format(renderDistanceVanilla << 5)).split("\n");
                            if ((config.cutoffDistance() >> 4) >= (renderDistanceVanilla << 1)) {
                                Arrays.fill(lines, null);
                            }
                            return lines;
                        },
                        (config, langKey) -> {
                            final int minCutoff = 64;

                            String[] lines = I18n.format(langKey + "labelCutoffDistanceTooSmall", numberFormat.format(minCutoff)).split("\n");
                            if (config.cutoffDistance() >= minCutoff) {
                                Arrays.fill(lines, null);
                            }
                            return lines;
                        }));
    }

    public RenderDistanceContainer(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull List<IConfigGuiElement> elements) {
        super(context, access, createElements(context, access, elements));
    }

    /**
     * @author DaPorkchop_
     */
    protected static class DynamicLabel extends AbstractConfigGuiElement {
        protected final GuiObjectAccess<FP2Config> access;
        protected final BiFunction<FP2Config, String, String[]>[] textFormatters;

        protected final int height;

        @SafeVarargs
        public DynamicLabel(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull BiFunction<FP2Config, String, String[]>... textFormatters) {
            super(context);

            this.access = access;
            this.textFormatters = textFormatters;

            int theoreticalMaxLines = toInt(this.text().count());
            this.height = theoreticalMaxLines * (MC.fontRenderer.FONT_HEIGHT + PADDING) + PADDING;
        }

        @Override
        protected String langKey() {
            return this.context.localeKeyBase();
        }

        protected Stream<String> text() {
            return Stream.of(this.textFormatters)
                    .map(textFormatter -> textFormatter.apply(this.access.getCurrent(), this.langKey()))
                    .flatMap(Stream::of);
        }

        @Override
        public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
            return Stream.of(new ComponentDimensions(totalSizeX, min(this.height, totalSizeY)));
        }

        @Override
        public ComponentDimensions preferredMinimumDimensions() {
            int maxWidth = this.text()
                    .filter(Objects::nonNull)
                    .mapToInt(MC.fontRenderer::getStringWidth)
                    .max().orElse(0);

            return new ComponentDimensions(maxWidth + (PADDING << 1), this.height);
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

            for (String line : this.text().filter(Objects::nonNull).toArray(String[]::new)) {
                MC.fontRenderer.drawStringWithShadow(line, x, y, -1);
                y += MC.fontRenderer.FONT_HEIGHT + PADDING;
            }
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
}
