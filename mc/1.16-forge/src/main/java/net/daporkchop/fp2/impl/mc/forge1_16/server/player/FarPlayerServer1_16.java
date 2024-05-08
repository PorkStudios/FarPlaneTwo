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

package net.daporkchop.fp2.impl.mc.forge1_16.server.player;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.flow.FlowControl;
import net.daporkchop.fp2.core.server.player.AbstractFarPlayerServer;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.network.FP2Network1_16;
import net.daporkchop.lib.math.vector.Vec3d;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.vector.Vector3d;

import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FarPlayerServer1_16 extends AbstractFarPlayerServer {
    @NonNull
    protected final FP2Forge1_16 fp2;
    @NonNull
    protected final ServerPlayNetHandler netHandler;
    @NonNull
    protected final FlowControl flowControl;

    @Override
    public Vec3d fp2_IFarPlayer_position() {
        Vector3d position = this.netHandler.player.position();
        return Vec3d.of(position.x(), position.y(), position.z());
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet) {
        if (!this.closed) {
            FP2Network1_16.sendToPlayer(packet, this.netHandler.getConnection());
        }
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet, Consumer<Throwable> handler) {
        if (!this.closed) {
            FP2Network1_16.sendToPlayer(packet, this.netHandler.getConnection(), handler);
        }
    }

    @Override
    public FlowControl fp2_IFarPlayer_flowControl() {
        return this.flowControl;
    }
}
