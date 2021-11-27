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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.client.gui.util.ElementBounds;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractGuiElement implements GuiElement {
    @NonNull
    protected final GuiContext context;
    @NonNull
    protected final GuiElementProperties properties;

    @Getter
    protected ElementBounds bounds = ElementBounds.ZERO;

    @Override
    public void bounds(@NonNull ElementBounds bounds) {
        this.bounds = bounds;

        this.pack();
    }

    @Override
    public void init() {
        //no-op
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public void pack() {
        //no-op
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (this.bounds.contains(mouseX, mouseY)) {
            this.properties.tooltip().ifPresent(lines -> this.context.renderer().drawTooltip(mouseX, mouseY, -1, lines));
        }
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        //no-op
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        //no-op
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        //no-op
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        //no-op
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        //no-op
    }
}
