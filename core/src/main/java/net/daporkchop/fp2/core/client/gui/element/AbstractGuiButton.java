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

package net.daporkchop.fp2.core.client.gui.element;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractGuiButton extends AbstractGuiElement {
    public static final int BUTTON_WIDTH = 200;
    public static final int BUTTON_HEIGHT = 20;

    protected static final int BUTTON_INTERNAL_PADDING_HORIZONTAL = 6;

    @Getter
    @Setter
    protected boolean enabled = true;

    public AbstractGuiButton(@NonNull GuiContext context, @NonNull GuiElementProperties properties) {
        super(context, properties);
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        int textWidth = this.context.renderer().getStringWidth(this.properties.text()) + BUTTON_INTERNAL_PADDING_HORIZONTAL;
        return textWidth >= totalSizeX
                ? Stream.of(new ComponentDimensions(totalSizeX, min(totalSizeY, BUTTON_HEIGHT))) //not enough space for the full text width
                : IntStream.rangeClosed(textWidth, totalSizeX).mapToObj(i -> new ComponentDimensions(i, min(totalSizeY, BUTTON_HEIGHT)));
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return new ComponentDimensions(BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        super.render(mouseX, mouseY);

        //draw background
        this.context.renderer().drawButtonBackground(this.bounds.x(), this.bounds.y(), this.bounds.sizeX(), this.bounds.sizeY(), this.bounds.contains(mouseX, mouseY), this.enabled);

        //get text
        String text = this.properties.text();
        String ellipsis = fp2().i18n().format(MODID + ".gui.ellipsis");

        //truncate text if necessary
        int textWidth = this.context.renderer().getStringWidth(text);
        int ellipsisWidth = this.context.renderer().getStringWidth(ellipsis);
        if (textWidth > this.bounds.sizeX() && textWidth > ellipsisWidth) {
            text = this.context.renderer().trimStringToWidth(text, this.bounds.sizeX() - ellipsisWidth - BUTTON_INTERNAL_PADDING_HORIZONTAL, false) + ellipsis;
        }

        //draw text
        this.context.renderer().drawCenteredString(text, this.bounds.centerX(), this.bounds.centerY(), this.bounds.contains(mouseX, mouseY) ? -1 : 0xFFE0E0E0, true, true, true);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        super.mouseDown(mouseX, mouseY, button);

        if (this.enabled && this.bounds.contains(mouseX, mouseY)) {
            this.handleClick(button);
        }
    }

    protected abstract void handleClick(int button);
}
