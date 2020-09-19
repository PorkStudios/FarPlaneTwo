/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.client;

import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.asm.client.gui.IGuiScreen;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gui.GuiButtonFP2Options;
import net.daporkchop.fp2.net.client.CPacketDropAllPieces;
import net.daporkchop.fp2.strategy.common.IFarContext;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (event.getWorld().isRemote) {
            TexUVs.reloadUVs();
        }
    }

    @SubscribeEvent
    public void connectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ClientConstants.init();
    }

    @SubscribeEvent
    public void disconnectedFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientConstants.shutdown();
    }

    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        if (FP2Config.debug.debug) {
            if (KeyBindings.RELOAD_SHADERS.isPressed()) {
                ShaderManager.reload();
            }
            if (KeyBindings.DROP_PIECES.isPressed()) {
                NETWORK_WRAPPER.sendToServer(new CPacketDropAllPieces());
            }
            if (KeyBindings.RENDER_PIECES.isPressed()) {
                ((IFarContext) mc.world).renderer().debug_renderPieces();
            }
            if (KeyBindings.TOGGLE_VANILLA_RENDER.isPressed()) {
                FP2Config.debug.skipRenderWorld ^= true;
                mc.player.sendMessage(new TextComponentString((FP2Config.debug.skipRenderWorld ? "§cDisabled" : "§aEnabled") + " vanilla terrain."));
            }
            if (KeyBindings.REBUILD_UVS.isPressed()) {
                TexUVs.reloadUVs();
                mc.player.sendMessage(new TextComponentString("§aRebuilt texture UVs."));
            }
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(FP2.MODID)) {
            ConfigManager.sync(FP2.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (gui instanceof GuiVideoSettings) {
            ((IGuiScreen) gui).getButtonList().add(new GuiButtonFP2Options(0xBEEF, gui.width / 2 + 165, gui.height / 6 - 12, gui));
        }
    }
}
