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
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.mode.api.IFarContext;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @author DaPorkchop_
 */
@Setter
@Getter
public class SPacketRenderingStrategy implements IMessage {
    @NonNull
    protected IFarRenderMode<?, ?> mode;

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = IFarRenderMode.REGISTRY.get(Constants.readString(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        Constants.writeString(buf, this.mode.name());
    }

    public static class Handler implements IMessageHandler<SPacketRenderingStrategy, IMessage> {
        @Override
        public IMessage onMessage(SPacketRenderingStrategy message, MessageContext ctx) {
            IFarContext farContext = (IFarContext) ctx.getClientHandler().world;
            ClientThreadExecutor.INSTANCE.execute(() -> {
                Constants.LOGGER.debug("Server initiated FP2 session with render mode {}", message.mode);
                farContext.init(message.mode);
            });
            return null;
        }
    }
}
