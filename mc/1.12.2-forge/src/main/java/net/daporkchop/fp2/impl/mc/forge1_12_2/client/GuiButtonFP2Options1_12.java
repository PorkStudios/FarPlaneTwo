/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.client;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.config.gui.ConfigGuiHelper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.ATMinecraft1_12;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Vanilla {@link GuiButton} which opens fp2's config GUI.
 *
 * @author DaPorkchop_
 */
public class GuiButtonFP2Options1_12 extends GuiButton {
    protected final GuiScreen parent;

    public GuiButtonFP2Options1_12(int buttonId, int x, int y, @NonNull GuiScreen parent) {
        super(buttonId, x, y, 40, 20, fp2().i18n().format(MODID + ".gui.buttonFP2Options"));

        this.parent = parent;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            FP2Config defaultConfig = FP2Config.DEFAULT_CONFIG;
            FP2Config serverConfig = ((ATMinecraft1_12) mc).getIntegratedServerIsRunning() ? null : fp2().client().currentPlayer().map(IFarPlayerClient::serverConfig).orElse(null);
            FP2Config clientConfig = fp2().globalConfig();

            ConfigGuiHelper.createAndDisplayGuiContext("menu", defaultConfig, serverConfig, clientConfig, fp2()::globalConfig);
            return true;
        } else {
            return false;
        }
    }
}
