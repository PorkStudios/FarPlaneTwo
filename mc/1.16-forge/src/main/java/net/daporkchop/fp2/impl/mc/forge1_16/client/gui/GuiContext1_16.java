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

package net.daporkchop.fp2.impl.mc.forge1_16.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class GuiContext1_16 extends Screen implements GuiContext {
    @Getter
    protected GuiRenderer1_16 renderer;
    protected Screen previousScreen;

    protected boolean initialized;

    protected GuiScreen screen;

    public GuiContext1_16() {
        super(new StringTextComponent(GuiContext1_16.class.getTypeName()));
    }

    public <T extends GuiScreen> T createScreenAndOpen(@NonNull Minecraft mc, @NonNull Function<GuiContext, T> factory) {
        this.renderer = new GuiRenderer1_16(mc, this);
        this.previousScreen = mc.screen;

        T screen = factory.apply(this);
        this.screen = screen;

        mc.setScreen(this);
        return screen;
    }

    @Override
    public void close() {
        checkState(this.minecraft.screen == this, "context is not active!");
        this.minecraft.setScreen(this.previousScreen);

        this.screen.close();
    }

    @Override
    protected void init() {
        if (!this.initialized) {
            this.initialized = true;
            this.screen.init();
        }

        this.screen.resized(this.width, this.height);
        this.screen.pack();
    }

    @Override
    public void reinit() {
        this.screen.init();
        this.screen.resized(this.width, this.height);
        this.screen.pack();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        this.renderer.render(matrixStack, () -> this.screen.render(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.screen.mouseDown((int) mouseX, (int) mouseY, mouseButton);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.screen.mouseUp((int) mouseX, (int) mouseY, mouseButton);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double oldX, double oldY) {
        this.screen.mouseDragged((int) oldX, (int) oldY, (int) mouseX, (int) mouseY, mouseButton);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dWheel) {
        if (dWheel != 0.0d) {
            this.screen.mouseScroll((int) mouseX, (int) mouseY, (int) dWheel);
        }
        return true;
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        this.screen.keyPressed(typedChar, keyCode);
        return true;
    }
}
