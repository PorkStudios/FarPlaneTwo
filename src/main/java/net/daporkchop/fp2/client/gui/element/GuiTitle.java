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

package net.daporkchop.fp2.client.gui.element;

import lombok.NonNull;
import net.daporkchop.fp2.client.gui.IConfigGuiContext;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiElement;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;
import net.minecraft.client.resources.I18n;

import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.client.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class GuiTitle extends AbstractGuiElement {
    protected final String name;

    protected String text;
    protected int textWidth;

    public GuiTitle(@NonNull IConfigGuiContext context, @NonNull String name) {
        super(context);

        this.name = name;
    }

    @Override
    protected String langKey() {
        return this.context.localeKeyBase() + this.name;
    }

    @Override
    public void init() {
        this.text = I18n.format(this.langKey());
        this.textWidth = MC.fontRenderer.getStringWidth(this.text);
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        return Stream.of(new ComponentDimensions(totalSizeX, min((MC.fontRenderer.FONT_HEIGHT << 1) + PADDING, totalSizeY)));
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return new ComponentDimensions(this.textWidth, (MC.fontRenderer.FONT_HEIGHT << 1) + PADDING);
    }

    @Override
    public void pack() {
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        MC.fontRenderer.drawStringWithShadow(this.text, this.bounds.x() + ((this.bounds.sizeX() - this.textWidth) >> 1), this.bounds.y() + MC.fontRenderer.FONT_HEIGHT + PADDING, -1);
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
