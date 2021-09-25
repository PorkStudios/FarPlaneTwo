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

package net.daporkchop.fp2.net.server;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.net.client.CPacketClientConfig;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.concurrent.TimeUnit;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * Notifies the client that the server is ready for the client to send its configuration.
 *
 * @author DaPorkchop_
 */
//TODO: this still seems to be being received too early sometimes
public class SPacketReady implements IMessage {
    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<SPacketReady, IMessage> {
        @Override
        public IMessage onMessage(SPacketReady message, MessageContext ctx) {
            GlobalEventExecutor.INSTANCE.schedule(() -> { //TODO: better workaround
                ThreadingHelper.scheduleTaskInWorldThread(ctx.getClientHandler().world, () -> {
                    ctx.getClientHandler().client.player.sendMessage(new TextComponentTranslation(MODID + ".playerJoinWarningMessage"));

                    Constants.FP2_LOG.debug("server notified us that we are ready to go!");
                    ((IFarWorldClient) ctx.getClientHandler().world).fp2_IFarWorld_init();

                    NETWORK_WRAPPER.sendToServer(new CPacketClientConfig().config(FP2Config.global()));
                });
            }, 1L, TimeUnit.SECONDS);
            return null;
        }
    }
}
