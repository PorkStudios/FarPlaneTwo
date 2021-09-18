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

package net.daporkchop.fp2.config.gui;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.config.ConfigHelper;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class DefaultGuiContext extends GuiScreen implements IGuiContext {
    protected final DefaultGuiContext topContext;
    protected final GuiScreen parentScreen;

    protected final Object instance;

    @Getter
    protected final String localeKeyBase;

    protected final IConfigGuiScreen screen;

    public DefaultGuiContext(@NonNull String localeKeyBase, @NonNull Object instance) {
        this.topContext = this;
        this.parentScreen = MC.currentScreen;
        this.instance = instance;

        this.localeKeyBase = localeKeyBase;
        this.screen = ConfigHelper.createConfigGuiScreen(this, instance);

        MC.displayGuiScreen(this);
    }

    protected DefaultGuiContext(@NonNull DefaultGuiContext parentContext, @NonNull String name, @NonNull Object instance) {
        this.topContext = parentContext.topContext;
        this.parentScreen = parentContext;
        this.instance = instance;

        this.localeKeyBase = parentContext.localeKeyBase() + name + '.';
        this.screen = ConfigHelper.createConfigGuiScreen(this, instance);
    }

    @Override
    public void pushSubmenu(@NonNull String name, @NonNull Object instance) {
        MC.displayGuiScreen(new DefaultGuiContext(this, name, instance));
    }

    @Override
    public void pop() {
        if (this.topContext == this) { //this is the top screen
            FP2_LOG.info("closed config gui: {}", this.instance);
        }

        MC.displayGuiScreen(this.parentScreen);
    }

    @Override
    public void initGui() {
        this.screen.setDimensions(this.width, this.height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.screen.render(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.screen.mouseDown(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.screen.mouseUp(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        this.screen.mouseDragged(mouseX - Mouse.getEventDX(), mouseY - Mouse.getEventDY(), mouseX, mouseY, clickedMouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.screen.keyPressed(typedChar, keyCode);
    }
}
