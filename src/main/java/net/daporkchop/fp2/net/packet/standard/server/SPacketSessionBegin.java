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

package net.daporkchop.fp2.net.packet.standard.server;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Sent by the server to tell the client to get ready to initiate a new session.
 *
 * @author DaPorkchop_
 */
@Getter
@Setter
public class SPacketSessionBegin implements IMessage {
    @NonNull
    protected IntAxisAlignedBB[] coordLimits;

    @Override
    public void fromBytes(ByteBuf buf) {
        int len = buf.readIntLE();

        this.coordLimits = new IntAxisAlignedBB[len];
        for (int i = 0; i < len; i++) {
            this.coordLimits[i] = new IntAxisAlignedBB(buf.readIntLE(), buf.readIntLE(), buf.readIntLE(), buf.readIntLE(), buf.readIntLE(), buf.readIntLE());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeIntLE(this.coordLimits.length);

        for (IntAxisAlignedBB bb : this.coordLimits) {
            buf.writeIntLE(bb.minX()).writeIntLE(bb.minY()).writeIntLE(bb.minZ())
                    .writeIntLE(bb.maxX()).writeIntLE(bb.maxY()).writeIntLE(bb.maxZ());
        }
    }

    @SideOnly(Side.CLIENT)
    public IFarWorldClient fakeWorldClient() {
        IntAxisAlignedBB[] coordLimits = this.coordLimits;

        return new IFarWorldClient() {
            @CalledFromClientThread
            @Override
            public void fp2_IFarWorld_init() {
                throw new UnsupportedOperationException();
            }

            @CalledFromClientThread
            @Override
            public void fp2_IFarWorld_close() {
                throw new UnsupportedOperationException();
            }

            @Override
            public IntAxisAlignedBB[] fp2_IFarWorld_coordLimits() {
                return coordLimits;
            }

            @Override
            public CompletableFuture<Void> fp2_IFarWorld_scheduleTask(@NonNull Runnable task) {
                return ThreadingHelper.scheduleTaskInClientThread(task);
            }

            @Override
            public <T> CompletableFuture<T> fp2_IFarWorld_scheduleTask(@NonNull Supplier<T> task) {
                return ThreadingHelper.scheduleTaskInClientThread(task);
            }
        };
    }
}
