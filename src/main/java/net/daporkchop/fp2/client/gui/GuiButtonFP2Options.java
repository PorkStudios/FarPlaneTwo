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

package net.daporkchop.fp2.client.gui;

import lombok.NonNull;
import net.daporkchop.fp2.config.ConfigHelper;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.player.IFarPlayerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import static net.daporkchop.fp2.FP2.*;

/**
 * @author DaPorkchop_
 */
public class GuiButtonFP2Options extends GuiButton {
    protected final GuiScreen parent;

    public GuiButtonFP2Options(int buttonId, int x, int y, @NonNull GuiScreen parent) {
        super(buttonId, x, y, 40, 20, I18n.format(MODID + ".gui.buttonFP2Options"));

        this.parent = parent;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            FP2Config defaultConfig = FP2Config.DEFAULT_CONFIG;
            FP2Config serverConfig = !mc.integratedServerIsRunning && mc.getConnection() != null ? ((IFarPlayerClient) mc.getConnection()).fp2_IFarPlayerClient_serverConfig() : null;
            FP2Config clientConfig = FP2Config.global();

            ConfigHelper.createAndDisplayGuiContext("menu", defaultConfig, serverConfig, clientConfig, FP2Config::set);
            return true;
        } else {
            return false;
        }
    }
}
