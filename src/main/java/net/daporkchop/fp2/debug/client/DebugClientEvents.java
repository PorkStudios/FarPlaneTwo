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

import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.debug.util.DebugUtils;
import net.daporkchop.fp2.net.client.CPacketDropAllTiles;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class DebugClientEvents {
    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        if (DebugKeyBindings.RELOAD_SHADERS.isPressed()) {
            ShaderManager.reload();
        }
        if (DebugKeyBindings.DROP_TILES.isPressed()) {
            NETWORK_WRAPPER.sendToServer(new CPacketDropAllTiles());
            DebugUtils.clientMsg("§aReloading all tiles.");
        }
        if (DebugKeyBindings.TOGGLE_REVERSED_Z.isPressed()) {
            FP2Config.compatibility.reversedZ ^= true;
            DebugUtils.clientMsg((FP2Config.compatibility.reversedZ ? "§aEnabled" : "§cDisabled") + " reversed-Z projection.");
        }
        if (DebugKeyBindings.TOGGLE_VANILLA_RENDER.isPressed()) {
            FP2Config.debug.skipRenderWorld ^= true;
            DebugUtils.clientMsg((FP2Config.debug.skipRenderWorld ? "§cDisabled" : "§aEnabled") + " vanilla terrain.");
        }
        if (DebugKeyBindings.REBUILD_UVS.isPressed()) {
            TexUVs.reloadUVs();
            DebugUtils.clientMsg("§aRebuilt texture UVs.");
        }
    }
}
