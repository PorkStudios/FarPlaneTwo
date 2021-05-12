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

package net.daporkchop.fp2;

import net.daporkchop.fp2.client.FP2Client;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.debug.FP2Debug;
import net.daporkchop.fp2.net.client.CPacketDropAllTiles;
import net.daporkchop.fp2.net.client.CPacketRenderMode;
import net.daporkchop.fp2.net.server.SPacketReady;
import net.daporkchop.fp2.net.server.SPacketRenderingStrategy;
import net.daporkchop.fp2.net.server.SPacketTileData;
import net.daporkchop.fp2.net.server.SPacketUnloadTile;
import net.daporkchop.fp2.server.FP2Server;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLModIdMappingEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.relauncher.Side;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = MODID,
        useMetadata = true,
        dependencies = "required-after:forgerocks@[6.13.3-1.12.2,);after:cubicchunks@[1.12.2-0.0.1188.0,)",
        acceptedMinecraftVersions = "1.12.2")
public class FP2 {
    public static final String MODID = "fp2";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FP2_LOG = event.getModLog();

        this.registerPackets();

        FP2Server.preInit();

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            FP2Client.preInit();
        }

        FP2Debug.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FP2Server.init();

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            FP2Client.init();
        }

        FP2Debug.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        FP2Server.postInit();

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            FP2Client.postInit();
        }
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        ServerThreadExecutor.INSTANCE.startup();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        ServerThreadExecutor.INSTANCE.workOffQueue();
        ServerThreadExecutor.INSTANCE.shutdown();
    }

    @Mod.EventHandler
    public void onIdsChanged(FMLModIdMappingEvent event) {
        FastRegistry.reload();
    }

    protected void registerPackets() {
        int id = 0;
        NETWORK_WRAPPER.registerMessage(CPacketRenderMode.Handler.class, CPacketRenderMode.class, id++, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(CPacketDropAllTiles.Handler.class, CPacketDropAllTiles.class, id++, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(SPacketReady.Handler.class, SPacketReady.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketRenderingStrategy.Handler.class, SPacketRenderingStrategy.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketTileData.Handler.class, SPacketTileData.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketUnloadTile.Handler.class, SPacketUnloadTile.class, id++, Side.CLIENT);
    }
}
