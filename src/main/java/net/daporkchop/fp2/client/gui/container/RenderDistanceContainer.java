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
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.client.gui.IConfigGuiElement;
import net.daporkchop.fp2.client.gui.IGuiContext;
import net.daporkchop.fp2.client.gui.access.GuiObjectAccess;
import net.daporkchop.fp2.client.gui.element.AbstractConfigGuiElement;
import net.daporkchop.fp2.client.gui.util.ComponentDimensions;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class RenderDistanceContainer extends VerticallyStackedContainer<FP2Config> {
    protected static List<IConfigGuiElement> createElements(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull List<IConfigGuiElement> elements) {
        NumberFormat numberFormat = NumberFormat.getInstance(MC.languageManager.getCurrentLanguage().getJavaLocale());

        return Arrays.asList(
                new ColumnsContainer<>(context, access, elements),
                new Label(context, access, "labelEquivalentRenderDistance", (config, langKey) -> {
                    int renderDistanceBlocks = config.cutoffDistance() << (config.maxLevels() - 1);
                    return I18n.format(langKey, numberFormat.format(renderDistanceBlocks), numberFormat.format(renderDistanceBlocks >> 4));
                }));
    }

    public RenderDistanceContainer(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull List<IConfigGuiElement> elements) {
        super(context, access, createElements(context, access, elements));
    }

    /**
     * @author DaPorkchop_
     */
    protected static class Label extends AbstractConfigGuiElement {
        protected final GuiObjectAccess<FP2Config> access;
        protected final String name;
        protected final BiFunction<FP2Config, String, String> textFormatter;

        protected final int height = MC.fontRenderer.FONT_HEIGHT + (PADDING << 1);

        public Label(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull String name, @NonNull BiFunction<FP2Config, String, String> textFormatter) {
            super(context);

            this.access = access;
            this.name = name;
            this.textFormatter = textFormatter;
        }

        @Override
        protected String langKey() {
            return this.context.localeKeyBase() + this.name;
        }

        protected String text() {
            return this.textFormatter.apply(this.access.getCurrent(), this.langKey());
        }

        @Override
        public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
            return Stream.of(new ComponentDimensions(totalSizeX, min(this.height, totalSizeY)));
        }

        @Override
        public ComponentDimensions preferredMinimumDimensions() {
            return new ComponentDimensions(MC.fontRenderer.getStringWidth(this.text()) + (PADDING << 1), this.height);
        }

        @Override
        public void init() {
        }

        @Override
        public void pack() {
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            MC.fontRenderer.drawStringWithShadow(this.text(), this.bounds.x() + PADDING, this.bounds.y() + PADDING, -1);
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
