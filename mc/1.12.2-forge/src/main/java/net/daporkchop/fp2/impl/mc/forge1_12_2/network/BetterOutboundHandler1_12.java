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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

/**
 * An alternative to {@link net.minecraftforge.fml.common.network.FMLOutboundHandler} which is entirely stateless.
 *
 * @author DaPorkchop_
 */
@ChannelHandler.Sharable
public final class BetterOutboundHandler1_12 extends ChannelOutboundHandlerAdapter {
    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        WrappedPacket1_12<FMLProxyPacket> wrapped = (WrappedPacket1_12<FMLProxyPacket>) msg;
        FMLProxyPacket pkt = wrapped.payload;
        try {
            pkt.payload().retain();
            if (!wrapped.manager.isChannelOpen()) {
                //emulate behavior of NetworkDispatcher#sendProxy() (i don't know why we need to copy the packet, but that's how FML does it)
                pkt = pkt.copy();
            }

            if (wrapped.listener != null) {
                wrapped.manager.sendPacket(pkt, wrapped.listener);
            } else {
                wrapped.manager.sendPacket(pkt);
            }
        } finally {
            pkt.payload().release();
        }
    }
}
