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

package net.daporkchop.fp2.client;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.asm.core.client.gui.IGuiScreen;
import net.daporkchop.fp2.client.gui.GuiButtonFP2Options;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class ClientEvents {
    private boolean REGISTERED = false;

    public synchronized void register() {
        checkState(!REGISTERED, "already registered!");
        REGISTERED = true;

        MinecraftForge.EVENT_BUS.register(ClientEvents.class);

        if (CC) {
            MinecraftForge.EVENT_BUS.register(_CC.class);
        }
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            ((IFarWorldClient) event.getWorld()).fp2_IFarWorld_close();
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

    @SubscribeEvent
    public void renderWorldLast(RenderWorldLastEvent event) {
        ReversedZ.disable();
    }

    /**
     * Forge event listeners for Cubic Chunks' events used on the client.
     *
     * @author DaPorkchop_
     */
    @UtilityClass
    public static class _CC {
    }
}
