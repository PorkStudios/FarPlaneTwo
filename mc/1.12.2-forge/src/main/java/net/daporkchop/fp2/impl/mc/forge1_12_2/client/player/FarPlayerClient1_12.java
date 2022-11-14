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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.player;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.player.AbstractFarPlayerClient;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.network.ATNetHandlerPlayClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.world.FWorldClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.world.level.FLevelClient1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.network.FP2Network1_12_2;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FarPlayerClient1_12 extends AbstractFarPlayerClient {
    @NonNull
    protected final FP2Forge1_12_2 fp2;
    @NonNull
    protected final FWorldClient1_12 world;
    @NonNull
    protected final NetHandlerPlayClient netHandlerPlayClient;

    @Override
    protected FLevelClient1_12_2 loadActiveLevel() {
        WorldClient worldClient = ((ATNetHandlerPlayClient1_12) this.netHandlerPlayClient).getWorld();
        return this.world.loadLevel(FP2Forge1_12_2.getIdentifierForWorld(worldClient), worldClient);
    }

    @CalledFromAnyThread
    @Override
    public void send(@NonNull IPacket packet) {
        FP2Network1_12_2.sendToServer(packet); //yuck, a *static* context?
    }
}
