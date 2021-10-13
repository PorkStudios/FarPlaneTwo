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

package net.daporkchop.fp2.net;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.mode.api.player.IFarPlayerServer;
import net.daporkchop.fp2.net.packet.debug.client.CPacketDebugDropAllTiles;
import net.daporkchop.fp2.net.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.net.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.net.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.net.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.fp2.net.packet.standard.server.SPacketSessionEnd;
import net.daporkchop.fp2.net.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.net.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.net.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.net.packet.standard.server.SPacketUpdateConfig;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.daporkchop.fp2.util.annotation.RemovalPolicy;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class FP2Network {
    public static SimpleNetworkWrapper PROTOCOL_FP2 = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    @DebugOnly
    public static SimpleNetworkWrapper PROTOCOL_DEBUG = NetworkRegistry.INSTANCE.newSimpleChannel(MODID + "$debug");

    private boolean INITIALIZED = false;

    /**
     * Called during {@link FMLPreInitializationEvent}.
     */
    public synchronized void preInit() {
        checkState(!INITIALIZED, "already initialized!");
        INITIALIZED = true;

        registerStandard();
        registerDebug();
    }

    private void registerStandard() {
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

        IMessageHandler<IMessage, IMessage> serverboundHandler = new ServerboundHandler();
        IMessageHandler<IMessage, IMessage> clientboundHandler = IS_DEDICATED_SERVER ? new ClientboundHandlerOnDedicatedServer() : new ClientboundHandler();

        int id = 0;

        //serverbound packets
        PROTOCOL_FP2.registerMessage(serverboundHandler, CPacketClientConfig.class, id++, Side.SERVER);

        //clientbound packets
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketHandshake.class, id++, Side.CLIENT);
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketSessionBegin.class, id++, Side.CLIENT);
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketSessionEnd.class, id++, Side.CLIENT);
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketTileData.class, id++, Side.CLIENT);
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketUnloadTile.class, id++, Side.CLIENT);
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketUnloadTiles.class, id++, Side.CLIENT);
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketUpdateConfig.Merged.class, id++, Side.CLIENT);
        PROTOCOL_FP2.registerMessage(clientboundHandler, SPacketUpdateConfig.Server.class, id++, Side.CLIENT);
    }

    @DebugOnly(RemovalPolicy.DROP)
    private void registerDebug() {
        @DebugOnly
        class ServerboundHandler implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                if (!FP2_DEBUG) {
                    ctx.getServerHandler().disconnect(new TextComponentTranslation(MODID + ".debug.debugModeNotEnabled.server"));
                    return null;
                }

                ((IFarPlayerServer) ctx.getServerHandler()).fp2_IFarPlayerServer_handleDebug(message);
                return null;
            }
        }

        @SideOnly(Side.CLIENT)
        @DebugOnly
        class ClientboundHandler implements IMessageHandler<IMessage, IMessage> {
            @Override
            public IMessage onMessage(IMessage message, MessageContext ctx) {
                if (!FP2_DEBUG) {
                    ctx.getClientHandler().netManager.closeChannel(new TextComponentTranslation(MODID + ".debug.debugModeNotEnabled.client"));
                    return null;
                }

                ((IFarPlayerClient) ctx.getClientHandler()).fp2_IFarPlayerClient_handleDebug(message);
                return null;
            }
        }

        IMessageHandler<IMessage, IMessage> serverboundHandler = new ServerboundHandler();
        IMessageHandler<IMessage, IMessage> clientboundHandler = IS_DEDICATED_SERVER ? new ClientboundHandlerOnDedicatedServer() : new ClientboundHandler();

        int id = 0;

        //serverbound packets
        PROTOCOL_DEBUG.registerMessage(serverboundHandler, CPacketDebugDropAllTiles.class, id++, Side.SERVER);

        //clientbound packets
        PROTOCOL_DEBUG.registerMessage(clientboundHandler, SPacketDebugUpdateStatistics.class, id++, Side.CLIENT);
    }

    /**
     * Dummy {@link IMessageHandler} implementation for handling clientbound packets on a dedicated server.
     *
     * @author DaPorkchop_
     */
    @SideOnly(Side.SERVER)
    private static class ClientboundHandlerOnDedicatedServer implements IMessageHandler<IMessage, IMessage> {
        @Override
        public IMessage onMessage(IMessage message, MessageContext ctx) {
            throw new IllegalStateException("attempted to handle clientbound packet on dedicated server: " + className(message));
        }
    }
}
