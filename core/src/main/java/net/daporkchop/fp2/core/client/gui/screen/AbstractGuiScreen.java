/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.client.gui.screen;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiEventHandler;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractGuiScreen implements GuiScreen {
    @NonNull
    protected final GuiContext context;

    protected final Set<GuiEventHandler> handlers = Collections.newSetFromMap(new LinkedHashMap<>());

    protected ComponentDimensions dimensions;

    @Override
    public void resized(int width, int height) {
        this.dimensions = new ComponentDimensions(width, height);
    }

    @Override
    public void init() {
        this.handlers.forEach(GuiEventHandler::init);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        this.handlers.forEach(handler -> handler.render(mouseX, mouseY));
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        this.handlers.forEach(handler -> handler.mouseDown(mouseX, mouseY, button));
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        this.handlers.forEach(handler -> handler.mouseUp(mouseX, mouseY, button));
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        this.handlers.forEach(handler -> handler.mouseScroll(mouseX, mouseY, dWheel));
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        this.handlers.forEach(handler -> handler.mouseDragged(oldMouseX, oldMouseY, newMouseX, newMouseY, button));
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        if (keyCode == KEY_ESCAPE) {
            this.context.close();
            return;
        }

        this.handlers.forEach(handler -> handler.keyPressed(typedChar, keyCode));
    }
}
