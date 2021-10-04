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
import net.daporkchop.fp2.compat.x86.x86FeatureDetector;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.config.listener.ConfigListenerManager;
import net.daporkchop.fp2.debug.FP2Debug;
import net.daporkchop.fp2.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.mode.api.player.IFarPlayerServer;
import net.daporkchop.fp2.net.packet.client.CPacketClientConfig;
import net.daporkchop.fp2.net.packet.client.CPacketDropAllTiles;
import net.daporkchop.fp2.net.packet.server.SPacketHandshake;
import net.daporkchop.fp2.net.packet.server.SPacketSessionBegin;
import net.daporkchop.fp2.net.packet.server.SPacketSessionEnd;
import net.daporkchop.fp2.net.packet.server.SPacketTileData;
import net.daporkchop.fp2.net.packet.server.SPacketUnloadTile;
import net.daporkchop.fp2.net.packet.server.SPacketUnloadTiles;
import net.daporkchop.fp2.net.packet.server.SPacketUpdateConfig;
import net.daporkchop.fp2.server.FP2Server;
import net.daporkchop.fp2.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLModIdMappingEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = MODID,
        useMetadata = true,
        dependencies = "required-after:forgerocks@[6.20.3-1.12.2,);after:cubicchunks@[1.12.2-0.0.1188.0,)",
        acceptedMinecraftVersions = "1.12.2")
public class FP2 {
    public static final String MODID = "fp2";

    private String version = "";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FP2_LOG = event.getModLog();
        this.version = event.getModMetadata().version;

        FP2Config.load();

        FP2_LOG.info("Detected x86 SIMD extension: {}", x86FeatureDetector.INSTANCE.maxSupportedVectorExtension());
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
        ConfigListenerManager.fire();
        FP2Server.postInit();

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            FP2Client.postInit();
        }
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        try {
            ServerThreadMarkedFutureExecutor.getFor(FMLCommonHandler.instance().getMinecraftServerInstance()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Mod.EventHandler
    public void onIdsChanged(FMLModIdMappingEvent event) {
        FastRegistry.reload();
    }

    @NetworkCheckHandler
    public boolean checkCanConnectWithMods(Map<String, String> modVersions, Side remoteSide) {
        String remoteVersion = modVersions.get(MODID);

        return FP2_DEBUG //accept any version in debug mode
               || remoteVersion == null //fp2 isn't present on the remote side, so it doesn't matter whether or not it's compatible
               || this.version.equals(remoteVersion); //if fp2 is present, the versions must match
    }

    protected void registerPackets() {
        class ServerboundHandler implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                ((IFarPlayerServer) ctx.getServerHandler()).fp2_IFarPlayerServer_handle(message);
                return null;
            }
        }

        @SideOnly(Side.CLIENT)
        class ClientboundHandler implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                ((IFarPlayerClient) ctx.getClientHandler()).fp2_IFarPlayerClient_handle(message);
                return null;
            }
        }

        @SideOnly(Side.SERVER)
        class ClientboundHandlerOnDedicatedServer implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                throw new IllegalStateException("attempted to handle clientbound packet on dedicated server: " + className(message));
            }
        }

        IMessageHandler<IMessage, IMessage> serverboundHandler = new ServerboundHandler();
        IMessageHandler<IMessage, IMessage> clientboundHandler = IS_DEDICATED_SERVER ? new ClientboundHandlerOnDedicatedServer() : new ClientboundHandler();

        int id = 0;

        //serverbound packets
        NETWORK_WRAPPER.registerMessage(serverboundHandler, CPacketClientConfig.class, id++, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(serverboundHandler, CPacketDropAllTiles.class, id++, Side.SERVER);

        //clientbound packets
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketHandshake.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketSessionBegin.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketSessionEnd.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketTileData.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketUnloadTile.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketUnloadTiles.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketUpdateConfig.Merged.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(clientboundHandler, SPacketUpdateConfig.Server.class, id++, Side.CLIENT);
    }
}
