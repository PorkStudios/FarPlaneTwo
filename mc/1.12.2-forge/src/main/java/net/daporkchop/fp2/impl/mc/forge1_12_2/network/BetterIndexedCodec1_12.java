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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * An alternative to {@link net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec} which doesn't rely on any
 *
 * @author DaPorkchop_
 */
@ChannelHandler.Sharable
public final class BetterIndexedCodec1_12<P extends IPacket> extends MessageToMessageCodec<FMLProxyPacket, WrappedPacket1_12<P>> {
    private final Byte2ObjectMap<MethodHandle> id2ctor = new Byte2ObjectOpenHashMap<>();
    private final Reference2ByteMap<Class<? extends P>> type2id = new Reference2ByteOpenHashMap<>();

    public BetterIndexedCodec1_12() {
        super(uncheckedCast(FMLProxyPacket.class), uncheckedCast(WrappedPacket1_12.class));
    }

    public BetterIndexedCodec1_12<P> addPacket(byte discriminator, Class<? extends P> type) {
        this.id2ctor.put(discriminator, MethodHandles.lookup().findConstructor(type, MethodType.methodType(void.class)).asType(MethodType.methodType(IPacket.class)));
        this.type2id.put(type, discriminator);
        return this;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, WrappedPacket1_12<P> wrapped, List<Object> out) throws Exception {
        String channel = ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get();
        Class<?> clazz = wrapped.payload.getClass();
        if (!this.type2id.containsKey(clazz)) {
            throw new RuntimeException("Undefined discriminator for message type " + clazz.getName() + " in channel " + channel);
        }
        byte id = this.type2id.getByte(clazz);

        ByteBuf buffer = UnpooledByteBufAllocator.DEFAULT.heapBuffer();
        try {
            buffer.writeByte(id);
            wrapped.payload.write(DataOut.wrapView(buffer));
            out.add(wrapped.withPayload(new FMLProxyPacket(new PacketBuffer(buffer), channel)));
        } catch (Throwable t) {
            buffer.release();
            throw t;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
        ByteBuf payload = msg.payload().duplicate();
        try {
            if (payload.readableBytes() < 1) {
                fp2().log().error("The FMLIndexedCodec has received an empty buffer on channel %s, likely a result of a LAN server issue. Pipeline parts : %s", ctx.channel().attr(NetworkRegistry.FML_CHANNEL), ctx.pipeline().toString());
            }
            byte id = payload.readByte();
            MethodHandle ctor = this.id2ctor.get(id);
            if (ctor == null) {
                throw new NullPointerException("Undefined message for discriminator " + id + " in channel " + msg.channel());
            }

            @SuppressWarnings("unchecked")
            P newMsg = (P) (IPacket) ctor.invokeExact();
            newMsg.read(DataIn.wrapView(payload));
            out.add(new WrappedPacket1_12<>(newMsg, Objects.requireNonNull(msg.getOrigin()), null));
        } finally {
            payload.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        fp2().log().error(this.getClass().getName() + " exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }
}
