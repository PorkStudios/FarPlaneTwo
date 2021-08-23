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

package net.daporkchop.fp2.debug.client;

import net.daporkchop.fp2.asm.core.client.gui.IGuiScreen;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gui.GuiButtonFP2Options;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.debug.util.DebugUtils;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.net.client.CPacketDropAllTiles;
import net.daporkchop.fp2.net.client.CPacketRenderMode;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.stream.IntStream;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class DebugClientEvents {
    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        if (DebugKeyBindings.RELOAD_SHADERS.isPressed()) {
            ShaderManager.reload(true);
        }
        if (DebugKeyBindings.DROP_TILES.isPressed()) {
            NETWORK_WRAPPER.sendToServer(new CPacketDropAllTiles());
            DebugUtils.clientMsg("§aReloading all tiles");
        }
        if (DebugKeyBindings.TOGGLE_REVERSED_Z.isPressed()) {
            FP2Config.compatibility.reversedZ ^= true;
            DebugUtils.clientMsg((FP2Config.compatibility.reversedZ ? "§aEnabled" : "§cDisabled") + " reversed-Z projection");
        }
        if (DebugKeyBindings.TOGGLE_VANILLA_RENDER.isPressed()) {
            FP2Config.debug.skipRenderWorld ^= true;
            DebugUtils.clientMsg((FP2Config.debug.skipRenderWorld ? "§cDisabled" : "§aEnabled") + " vanilla terrain");
        }
        if (DebugKeyBindings.REBUILD_UVS.isPressed()) {
            TexUVs.reloadUVs();
            DebugUtils.clientMsg("§aRebuilt texture UVs");
        }
        if (DebugKeyBindings.TOGGLE_RENDER_MODE.isPressed()) {
            String[] opts = IFarRenderMode.REGISTRY.stream().map(Map.Entry::getKey).toArray(String[]::new);
            int i = IntStream.range(0, opts.length).filter(j -> FP2Config.renderMode.equals(opts[j])).findFirst().getAsInt();
            FP2Config.renderMode = opts[(i + 1) % opts.length];
            NETWORK_WRAPPER.sendToServer(new CPacketRenderMode().mode(IFarRenderMode.REGISTRY.get(FP2Config.renderMode)));
            DebugUtils.clientMsg("§aSwitched render mode to §7" + FP2Config.renderMode);
        }
        if (DebugKeyBindings.TOGGLE_LEVEL_0.isPressed()) {
            FP2Config.debug.skipLevel0 ^= true;
            DebugUtils.clientMsg((FP2Config.debug.skipLevel0 ? "§cDisabled" : "§aEnabled") + " level 0 rendering.");
        }
        if (DebugKeyBindings.TOGGLE_DEBUG_COLORS.isPressed()) {
            FP2Config.Debug.DebugColorMode[] modes = FP2Config.Debug.DebugColorMode.values();
            FP2Config.Debug.DebugColorMode oldMode = FP2Config.debug.debugColorMode;
            FP2Config.Debug.DebugColorMode newMode = FP2Config.debug.debugColorMode = modes[(oldMode.ordinal() + 1) % modes.length];
            ShaderManager.changeDefines()
                    .undefine("USE_DEBUG_COLORS_" + oldMode)
                    .define("USE_DEBUG_COLORS_" + newMode)
                    .apply();
            DebugUtils.clientMsg("§aSwitched debug color mode to §7" + FP2Config.debug.debugColorMode);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (gui instanceof GuiMainMenu) {
            ((IGuiScreen) gui).getButtonList().add(new GuiButtonFP2Options(0xBEEF, gui.width / 2 + 104, gui.height / 4 + 48, gui));
        } else if (gui instanceof GuiIngameMenu) {
            ((IGuiScreen) gui).getButtonList().add(new GuiButtonFP2Options(0xBEEF, gui.width / 2 + 104, gui.height / 4 + 8, gui));
        }
    }
}
