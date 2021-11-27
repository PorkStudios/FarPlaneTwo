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

package net.daporkchop.fp2.core.client.gui.container;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.gui.GuiContainer;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.util.ElementBounds;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractConfigGuiContainer implements GuiContainer {
    @NonNull
    protected final GuiContext context;
    @NonNull
    protected final List<GuiElement> elements;

    protected ElementBounds bounds = ElementBounds.ZERO;

    @Getter(AccessLevel.NONE)
    protected final BitSet mouseButtonsDown = new BitSet();

    @Override
    public Iterator<GuiElement> iterator() {
        return this.elements.iterator();
    }

    @Override
    public void bounds(@NonNull ElementBounds bounds) {
        this.bounds = bounds;

        this.pack();
    }

    @Override
    public void init() {
        this.elements.forEach(GuiElement::init);
    }

    @Override
    public void close() {
        this.elements.forEach(GuiElement::close);
    }

    protected int offsetX(int x) {
        return x - this.bounds.x();
    }

    protected int offsetY(int y) {
        return y - this.bounds.y();
    }

    @Override
    public void render(int mouseX, int mouseY) {
        int offsetMouseX;
        int offsetMouseY;
        if (!this.mouseButtonsDown.isEmpty() || this.bounds.contains(mouseX, mouseY)) {
            offsetMouseX = this.offsetX(mouseX);
            offsetMouseY = this.offsetY(mouseY);
        } else {
            offsetMouseX = offsetMouseY = Integer.MIN_VALUE;
        }

        this.context.renderer().translate(-this.offsetX(0), -this.offsetY(0), () -> {
            for (GuiElement element : this.elements) {
                element.render(offsetMouseX, offsetMouseY);
            }
        });
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (this.bounds.contains(mouseX, mouseY)) {
            this.mouseButtonsDown.set(button);

            for (GuiElement element : this.elements) {
                element.mouseDown(this.offsetX(mouseX), this.offsetY(mouseY), button);
            }
        }
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        if (this.mouseButtonsDown.get(button)) {
            this.mouseButtonsDown.clear(button);

            for (GuiElement element : this.elements) {
                element.mouseUp(this.offsetX(mouseX), this.offsetY(mouseY), button);
            }
        }
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        if (this.mouseButtonsDown.get(button)) {
            for (GuiElement element : this.elements) {
                element.mouseDragged(this.offsetX(oldMouseX), this.offsetY(oldMouseY), this.offsetX(newMouseX), this.offsetY(newMouseY), button);
            }
        }
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        if (this.bounds.contains(mouseX, mouseY)) {
            for (GuiElement element : this.elements) {
                element.mouseScroll(this.offsetX(mouseX), this.offsetY(mouseY), dWheel);
            }
        }
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        for (GuiElement element : this.elements) {
            element.keyPressed(typedChar, keyCode);
        }
    }
}
