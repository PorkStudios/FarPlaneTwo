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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.gui;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiRenderer;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class GuiContext1_12_2 extends net.minecraft.client.gui.GuiScreen implements GuiContext {
    @Getter
    protected GuiRenderer renderer;
    protected net.minecraft.client.gui.GuiScreen previousScreen;

    protected boolean initialized = false;
    protected int scaleFactor;

    protected GuiScreen screen;

    public <T extends GuiScreen> T createScreenAndOpen(@NonNull Minecraft mc, @NonNull Function<GuiContext, T> factory) {
        this.renderer = new GuiRenderer1_12_2(mc, this);
        this.previousScreen = mc.currentScreen;

        T screen = factory.apply(this);
        this.screen = screen;

        mc.displayGuiScreen(this);
        return screen;
    }

    @Override
    public void close() {
        this.screen.close();

        checkState(this.mc.currentScreen == this, "context is not active!");
        this.mc.displayGuiScreen(this.previousScreen);
    }

    @Override
    public void initGui() {
        this.scaleFactor = new ScaledResolution(this.mc).getScaleFactor();

        if (!this.initialized) {
            this.initialized = true;
            this.screen.init();
        }

        this.screen.resized(this.width, this.height);
        this.screen.pack();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.screen.render(mouseX, mouseY);
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
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);

        int oldX = this.scaleMouseX(scaledResolution, Mouse.getEventX() - Mouse.getEventDX());
        int oldY = this.scaleMouseY(scaledResolution, Mouse.getEventY() - Mouse.getEventDY());

        this.screen.mouseDragged(oldX, oldY, mouseX, mouseY, clickedMouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            ScaledResolution scaledResolution = new ScaledResolution(this.mc);
            int x = this.scaleMouseX(scaledResolution, Mouse.getX());
            int y = this.scaleMouseY(scaledResolution, Mouse.getY());
            this.screen.mouseScroll(x, y, this.scaleDWheel(scaledResolution, dWheel));
        }
    }

    protected int scaleMouseX(@NonNull ScaledResolution scaledResolution, int mouseX) {
        return mouseX * scaledResolution.getScaledWidth() / this.mc.displayWidth;
    }

    protected int scaleMouseY(@NonNull ScaledResolution scaledResolution, int mouseY) {
        return scaledResolution.getScaledHeight() - mouseY * scaledResolution.getScaledHeight() / this.mc.displayHeight - 1;
    }

    protected int scaleDWheel(@NonNull ScaledResolution scaledResolution, int dWheel) {
        return -dWheel * scaledResolution.getScaledHeight() / this.mc.displayHeight;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.screen.keyPressed(typedChar, keyCode);
    }
}
