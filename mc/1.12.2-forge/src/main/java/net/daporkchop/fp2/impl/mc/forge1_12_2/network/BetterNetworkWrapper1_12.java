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

package net.daporkchop.fp2.impl.mc.forge1_12_2.network;

import io.netty.channel.ChannelFutureListener;
import lombok.val;
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.core.network.IPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.EnumMap;
import java.util.function.BiConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An alternative to {@link net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper} which is both thread-safe and much more efficient.
 *
 * @author DaPorkchop_
 */
public final class BetterNetworkWrapper1_12<P extends IPacket> {
    private final EnumMap<Side, FMLEmbeddedChannel> channels;
    private final BetterIndexedCodec1_12<P> packetCodec;

    /**
     * @see net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper#SimpleNetworkWrapper(String)
     */
    public BetterNetworkWrapper1_12(String channelName) {
        this.packetCodec = new BetterIndexedCodec1_12<>();
        this.channels = NetworkRegistry.INSTANCE.newChannel(channelName, this.packetCodec);

        for (FMLEmbeddedChannel channel : this.channels.values()) {
            //replace the fml outbound handler with the stateless one
            channel.pipeline().replace(FMLOutboundHandler.class, FP2.MODID + ":outbound", BetterOutboundHandler1_12.INSTANCE);

            //add something to actually handle packets
            channel.pipeline().addLast("fp2:handle", new BetterInboundHandler1_12());
        }
    }

    /**
     * @see net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper#registerMessage(IMessageHandler, Class, int, Side)
     */
    public <REQ extends P> void registerMessage(BiConsumer<? super REQ, INetHandler> messageHandler, Class<REQ> requestMessageType, int discriminator, Side side) {
        this.packetCodec.addPacket(toByte(discriminator), requestMessageType);
        FMLEmbeddedChannel channel = this.channels.get(side);
        channel.pipeline().get(BetterInboundHandler1_12.class).addHandler(requestMessageType, messageHandler);
    }

    /**
     * @see net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper#sendTo(IMessage, EntityPlayerMP)
     */
    public void sendTo(P message, EntityPlayerMP player) {
        this.sendTo(message, player, null);
    }

    /**
     * @see net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper#sendTo(IMessage, EntityPlayerMP)
     */
    public void sendTo(P message, EntityPlayerMP player, ChannelFutureListener listener) {
        this.sendTo(message, player.connection.getNetworkManager(), listener, Side.SERVER);
    }

    /**
     * @see net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper#sendToServer(IMessage)
     */
    public void sendToServer(P message) {
        this.sendToServer(message, null);
    }

    /**
     * @see net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper#sendToServer(IMessage)
     */
    public void sendToServer(P message, ChannelFutureListener listener) {
        NetworkManager clientConnection = FMLCommonHandler.instance().getClientToServerNetworkManager();
        if (clientConnection != null) {
            this.sendTo(message, clientConnection, listener, Side.CLIENT);
        }
    }

    private void sendTo(P message, NetworkManager networkManager, ChannelFutureListener listener, Side side) {
        val wrapped = new WrappedPacket1_12<>(message, networkManager, listener);
        this.channels.get(side).writeAndFlush(wrapped).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
