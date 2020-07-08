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
import net.daporkchop.fp2.strategy.common.TerrainRenderer;
import net.daporkchop.fp2.strategy.heightmap.HeightmapChunk;
import net.daporkchop.fp2.strategy.heightmap.HeightmapTerrainRenderer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @author DaPorkchop_
 */
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = true, chain = true)
public class SPacketHeightmapData implements IMessage {
    @NonNull
    protected HeightmapChunk chunk;

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunk = new HeightmapChunk(buf.readInt(), buf.readInt());
        this.chunk.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.chunk.x()).writeInt(this.chunk.z());
        this.chunk.toBytes(buf);
    }

    public static class Handler implements IMessageHandler<SPacketHeightmapData, IMessage> {
        @Override
        public IMessage onMessage(SPacketHeightmapData message, MessageContext ctx) {
            HeightmapTerrainRenderer renderer = (HeightmapTerrainRenderer) ((TerrainRenderer.Holder) ctx.getClientHandler().world).fp2_terrainRenderer();
            renderer.receiveRemoteChunk(message.chunk);
            return null;
        }
    }
}
