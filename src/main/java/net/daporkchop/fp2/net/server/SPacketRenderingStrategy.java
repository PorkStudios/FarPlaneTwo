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

package net.daporkchop.fp2.net.server;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.strategy.RenderStrategy;
import net.daporkchop.fp2.strategy.common.TerrainRenderer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @author DaPorkchop_
 */
@NoArgsConstructor
@Setter
@Getter
@Accessors(fluent = true, chain = true)
public class SPacketRenderingStrategy implements IMessage {
    @NonNull
    protected RenderStrategy strategy;
    protected double seaLevel;

    @Override
    public void fromBytes(ByteBuf buf) {
        this.strategy = RenderStrategy.values()[buf.readInt()];
        this.seaLevel = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.strategy.ordinal())
                .writeDouble(this.seaLevel);
    }

    public static class Handler implements IMessageHandler<SPacketRenderingStrategy, IMessage> {
        @Override
        public IMessage onMessage(SPacketRenderingStrategy message, MessageContext ctx) {
            ((TerrainRenderer.Holder) ctx.getClientHandler().world).fp2_updateStrategy(message.strategy, message.seaLevel);
            return null;
        }
    }
}
