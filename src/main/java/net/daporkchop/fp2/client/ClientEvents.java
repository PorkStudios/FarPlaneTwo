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

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.api.world.CubeEvent;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.asm.core.client.gui.IGuiScreen;
import net.daporkchop.fp2.client.gui.GuiButtonFP2Options;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.server.ServerEvents;
import net.daporkchop.fp2.server.worldlistener.WorldChangeListenerManager;
import net.daporkchop.fp2.util.Constants;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import scala.actors.threadpool.Arrays;

import static net.daporkchop.fp2.client.ClientConstants.*;
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
            ((IFarWorldClient) event.getWorld()).close();
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
    public void onChunkData(ChunkEvent.Load event) {
        if (event.getWorld().isRemote && !Constants.isCubicWorld(event.getWorld())) {
            mc.renderGlobal.chunksToUpdate.addAll(Arrays.asList(mc.renderGlobal.viewFrustum.renderChunks));
            /*ChunkPos pos = event.getChunk().getPos();
            for (RenderChunk renderChunk : mc.renderGlobal.viewFrustum.renderChunks) {
                if (renderChunk.position.getX() == pos.getXStart() && renderChunk.position.getZ() == pos.getZStart()) {
                    mc.renderGlobal.chunksToUpdate.add(renderChunk);
                }
            }*/
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
        @SubscribeEvent
        public void onCubeData(CubeEvent.Load event) {
            if (event.getWorld().isRemote) {
                mc.renderGlobal.chunksToUpdate.addAll(Arrays.asList(mc.renderGlobal.viewFrustum.renderChunks));
            }
        }
    }
}
