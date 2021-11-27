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

import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.client.gui.element.AbstractGuiButton.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractGuiSlider extends AbstractGuiElement {
    public static final int CONTROL_WIDTH = 8;

    protected final double min;
    protected final double max;
    protected final double step;

    protected double value;

    protected boolean dragging = false;

    public AbstractGuiSlider(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull Number initialValue) {
        super(context, properties);

        this.min = properties.min().get().doubleValue();
        this.max = properties.max().get().doubleValue();
        this.step = properties.step().orElse(Double.NaN).doubleValue();

        this.value = initialValue.doubleValue();
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
        this.drawBackground(mouseX, mouseY);

        //draw control
        this.context.renderer().drawButtonBackground(this.bounds.x() + (int) (this.value / (this.max - this.min) * (this.bounds.sizeX() - CONTROL_WIDTH)), this.bounds.y(), CONTROL_WIDTH, this.bounds.sizeY(), this.bounds.contains(mouseX, mouseY), true);

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

    protected void drawBackground(int mouseX, int mouseY) {
        this.context.renderer().drawButtonBackground(this.bounds.x(), this.bounds.y(), this.bounds.sizeX(), this.bounds.sizeY(), false, false);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        super.mouseDown(mouseX, mouseY, button);

        if (button == MOUSE_BUTTON_LEFT && this.bounds.contains(mouseX, mouseY)) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        super.mouseDragged(oldMouseX, oldMouseY, newMouseX, newMouseY, button);

        if (button == MOUSE_BUTTON_LEFT && this.dragging) {
            double value = lerp(this.min, this.max, clamp((newMouseX - (this.bounds.minX() + (CONTROL_WIDTH >> 1))) / (double) (this.bounds.sizeX() - CONTROL_WIDTH), 0.0d, 1.0d));
            if (!Double.isNaN(this.step)) {
                value = round(value / this.step) * this.step;
            }

            if (value != this.value) {
                this.value = value;
                this.valueChanged(value);
            }
        }
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        super.mouseUp(mouseX, mouseY, button);

        if (button == MOUSE_BUTTON_LEFT && this.dragging) {
            this.dragging = false;
        }
    }

    protected abstract void valueChanged(@NonNull Number value);
}
