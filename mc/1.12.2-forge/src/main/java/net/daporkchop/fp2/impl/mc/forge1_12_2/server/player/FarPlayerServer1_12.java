/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.server.player;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.server.player.AbstractFarPlayerServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.network.FP2Network1_12_2;
import net.daporkchop.lib.math.vector.Vec3d;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.function.Supplier;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FarPlayerServer1_12 extends AbstractFarPlayerServer {
    @NonNull
    protected final FP2Forge1_12_2 fp2;
    @NonNull
    protected final Supplier<EntityPlayerMP> player;

    @Override
    public Vec3d fp2_IFarPlayer_position() {
        EntityPlayerMP player = this.player.get();
        return Vec3d.of(player.posX, player.posY, player.posZ);
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet) {
        if (!this.closed) {
            FP2Network1_12_2.sendToPlayer(packet, this.player.get());
        }
    }
}
