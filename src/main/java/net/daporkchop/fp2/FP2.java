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

package net.daporkchop.fp2;

import net.daporkchop.fp2.net.client.CPacketDropAllPieces;
import net.daporkchop.fp2.net.client.CPacketRenderingStrategy;
import net.daporkchop.fp2.net.server.SPacketPieceData;
import net.daporkchop.fp2.net.server.SPacketReady;
import net.daporkchop.fp2.net.server.SPacketRenderingStrategy;
import net.daporkchop.fp2.net.server.SPacketUnloadPiece;
import net.daporkchop.fp2.server.ServerProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = MODID,
        useMetadata = true,
        acceptedMinecraftVersions = "1.12.2")
public class FP2 {
    public static final String MODID = "fp2";

    @SidedProxy(clientSide = "net.daporkchop.fp2.client.ClientProxy", serverSide = "net.daporkchop.fp2.server.ServerProxy")
    public static ServerProxy PROXY;

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        MinecraftForge.EVENT_BUS.register(PROXY);

        PROXY.preInit(event);

        this.registerPackets();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PROXY.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        PROXY.postInit(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        PROXY.serverAboutToStart(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        PROXY.serverStopping(event);
    }

    protected void registerPackets() {
        int id = 0;
        NETWORK_WRAPPER.registerMessage(CPacketRenderingStrategy.Handler.class, CPacketRenderingStrategy.class, id++, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(CPacketDropAllPieces.Handler.class, CPacketDropAllPieces.class, id++, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(SPacketReady.Handler.class, SPacketReady.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketRenderingStrategy.Handler.class, SPacketRenderingStrategy.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketPieceData.Handler.class, SPacketPieceData.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketUnloadPiece.Handler.class, SPacketUnloadPiece.class, id++, Side.CLIENT);
    }
}
