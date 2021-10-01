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
import net.daporkchop.fp2.config.Config;
import net.daporkchop.fp2.config.gui.util.ComponentDimensions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.client.config.GuiMessageDialog;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class DefaultGuiContext extends GuiScreen implements IGuiContext {
    protected final DefaultGuiContext topContext;
    protected final GuiScreen parentScreen;

    protected final GuiObjectAccess<?> access;
    protected final Consumer callback;

    @Getter
    protected final String localeKeyBase;

    protected final IConfigGuiScreen screen;

    public DefaultGuiContext(@NonNull String localeKeyBase, @NonNull GuiObjectAccess<?> access, @NonNull Consumer callback) {
        this.topContext = this;
        this.parentScreen = MC.currentScreen;

        this.access = access;
        this.callback = callback;

        this.localeKeyBase = localeKeyBase;
        this.screen = ConfigHelper.createConfigGuiScreen(this, this.access);

        MC.displayGuiScreen(this);
    }

    protected DefaultGuiContext(@NonNull DefaultGuiContext parentContext, @NonNull String name, @NonNull GuiObjectAccess<?> access, @NonNull Function<IGuiContext, IConfigGuiScreen> screenFactory) {
        this.topContext = parentContext.topContext;
        this.parentScreen = parentContext;

        this.access = access;
        this.callback = null;

        this.localeKeyBase = parentContext.localeKeyBase() + name + '.';
        this.screen = screenFactory.apply(this);
    }

    @Override
    public void pushSubmenu(@NonNull String name, @NonNull GuiObjectAccess<?> access, @NonNull Function<IGuiContext, IConfigGuiScreen> screenFactory) {
        MC.displayGuiScreen(new DefaultGuiContext(this, name, access, screenFactory));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void pop() {
        if (this.topContext == this) { //this is the top screen
            Object oldInstance = this.access.oldInstance;
            Object newInstance = this.access.newInstance;

            if (oldInstance.equals(newInstance)) {
                FP2_LOG.info("closed config gui: unchanged");
            } else {
                FP2_LOG.info("closed config gui: {}", newInstance);

                ConfigHelper.validateConfig(newInstance);

                Config.Requirement restartRequirement = ConfigHelper.restartRequirement(oldInstance, newInstance);
                FP2_LOG.info("restart required: {}", restartRequirement);

                this.callback.accept(newInstance);

                switch (restartRequirement) {
                    case WORLD:
                        if (MC.world == null) { //the world isn't set, so we don't need to notify the player that a reload is required
                            break;
                        }
                    case GAME:
                        MC.displayGuiScreen(new GuiMessageDialog(
                                this.parentScreen,
                                MODID + ".config.restartRequired.dialog.title." + restartRequirement,
                                new TextComponentString(I18n.format(MODID + ".config.restartRequired.dialog.message." + restartRequirement)),
                                MODID + ".config.restartRequired.dialog.confirm"));
                        return;
                }
            }
        }

        MC.displayGuiScreen(this.parentScreen);
    }

    @Override
    public void initGui() {
        this.screen.init();
        this.screen.setDimensions(new ComponentDimensions(this.width, this.height));
    }

    @Override
    public void pack() {
        this.screen.init();
        this.screen.pack();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.screen.render(mouseX, mouseY, partialTicks);
        this.screen.getTooltip(mouseX, mouseY).ifPresent(lines -> GuiUtils.drawHoveringText(Arrays.asList(lines), mouseX, mouseY, this.width, this.height, -1, MC.fontRenderer));
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

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.screen.keyPressed(typedChar, keyCode);
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
}
