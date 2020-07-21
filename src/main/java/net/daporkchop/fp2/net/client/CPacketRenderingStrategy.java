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

package net.daporkchop.fp2.net.client;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.Config;
import net.daporkchop.fp2.net.server.SPacketRenderingStrategy;
import net.daporkchop.fp2.strategy.RenderStrategy;
import net.daporkchop.fp2.strategy.common.IFarContext;
import net.daporkchop.fp2.util.IFarPlayer;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Setter
@Getter
@Accessors(fluent = true, chain = true)
public class CPacketRenderingStrategy implements IMessage {
    @NonNull
    protected RenderStrategy strategy;

    @Override
    public void fromBytes(ByteBuf buf) {
        this.strategy = RenderStrategy.fromOrdinal(buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.strategy.ordinal());
    }

    public static class Handler implements IMessageHandler<CPacketRenderingStrategy, IMessage> {
        @Override
        public IMessage onMessage(CPacketRenderingStrategy message, MessageContext ctx) {
            if (message.strategy == Config.renderStrategy) {
                ServerThreadExecutor.INSTANCE.execute(() -> {
                    //send the packet here to ensure that it's sent before adding the player to the tracker
                    NETWORK_WRAPPER.sendTo(new SPacketRenderingStrategy().strategy(Config.renderStrategy), ctx.getServerHandler().player);

                    ((IFarContext) ctx.getServerHandler().player.world).fp2_tracker().playerAdd(ctx.getServerHandler().player);
                    ((IFarPlayer) ctx.getServerHandler().player).fp2_markReady();
                });
            }
            return null;
        }
    }
}
