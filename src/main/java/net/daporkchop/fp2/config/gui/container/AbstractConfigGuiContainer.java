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

package net.daporkchop.fp2.config.gui.container;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.config.gui.IConfigGuiComponent;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.util.ElementBounds;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractConfigGuiContainer implements IConfigGuiElement {
    protected final List<IConfigGuiElement> elements;

    @Getter
    protected ElementBounds bounds = ElementBounds.ZERO;

    protected final BitSet mouseButtonsDown = new BitSet();

    public AbstractConfigGuiContainer(@NonNull List<IConfigGuiElement> elements) {
        checkArg(!elements.isEmpty(), "cannot create %s with 0 elements!", PorkUtil.className(this));

        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    public void bounds(@NonNull ElementBounds bounds) {
        this.bounds = bounds;

        this.pack();
    }

    @Override
    public void init() {
        this.elements.forEach(IConfigGuiComponent::init);
    }

    protected int offsetX(int x) {
        return x - this.bounds.x();
    }

    protected int offsetY(int y) {
        return y - this.bounds.y();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        glPushMatrix();
        glTranslatef(-this.offsetX(0), -this.offsetY(0), 0.0f);

        try {
            if (!this.mouseButtonsDown.isEmpty() || this.bounds.contains(mouseX, mouseY)) {
                mouseX = this.offsetX(mouseX);
                mouseY = this.offsetY(mouseY);
            } else {
                mouseX = mouseY = Integer.MIN_VALUE;
            }

            for (IConfigGuiElement element : this.elements) {
                element.render(mouseX, mouseY, partialTicks);
            }
        } finally {
            glPopMatrix();
        }
    }

    @Override
    public Optional<String[]> getTooltip(int mouseX, int mouseY) {
        if (!this.bounds.contains(mouseX, mouseY)) {
            return Optional.empty();
        }

        return this.elements.stream()
                .map(element -> element.getTooltip(this.offsetX(mouseX), this.offsetY(mouseY)).orElse(null))
                .filter(Objects::nonNull)
                .reduce((a, b) -> {
                    String[] merged = Arrays.copyOf(a, a.length + b.length);
                    System.arraycopy(b, 0, merged, a.length, b.length);
                    return merged;
                });
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (this.bounds.contains(mouseX, mouseY)) {
            this.mouseButtonsDown.set(button);

            for (IConfigGuiElement element : this.elements) {
                element.mouseDown(this.offsetX(mouseX), this.offsetY(mouseY), button);
            }
        }
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        if (this.mouseButtonsDown.get(button)) {
            this.mouseButtonsDown.clear(button);

            for (IConfigGuiElement element : this.elements) {
                element.mouseUp(this.offsetX(mouseX), this.offsetY(mouseY), button);
            }
        }
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        if (this.mouseButtonsDown.get(button)) {
            for (IConfigGuiElement element : this.elements) {
                element.mouseDragged(this.offsetX(oldMouseX), this.offsetY(oldMouseY), this.offsetX(newMouseX), this.offsetY(newMouseY), button);
            }
        }
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        if (this.bounds.contains(mouseX, mouseY)) {
            for (IConfigGuiElement element : this.elements) {
                element.mouseScroll(this.offsetX(mouseX), this.offsetY(mouseY), dWheel);
            }
        }
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        for (IConfigGuiElement element : this.elements) {
            element.keyPressed(typedChar, keyCode);
        }
    }
}
