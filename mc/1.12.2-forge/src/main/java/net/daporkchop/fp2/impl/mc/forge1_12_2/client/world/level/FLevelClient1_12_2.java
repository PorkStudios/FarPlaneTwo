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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.world.level;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FWorldClient;
import net.daporkchop.fp2.core.client.world.level.AbstractLevelClient;
import net.daporkchop.fp2.core.util.threading.futureexecutor.MarkedFutureExecutor;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.ATMinecraft1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.multiplayer.ATWorldClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.network.ATNetHandlerPlayClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.render.LevelRenderer1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.world.FWorldClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.level.IFarLevel1_12;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;

/**
 * @author DaPorkchop_
 */
@Getter
public class FLevelClient1_12_2 extends AbstractLevelClient<FP2Forge1_12_2, NetHandlerPlayClient, FWorldClient1_12, WorldClient, FLevelClient1_12_2> implements IFarLevel1_12 {
    private final IntAxisAlignedBB coordLimits;
    private final LevelRenderer1_12_2 renderer;

    public FLevelClient1_12_2(@NonNull FP2Forge1_12_2 fp2, @NonNull WorldClient implLevel, @NonNull FWorldClient1_12 world, @NonNull Identifier id, @NonNull IntAxisAlignedBB coordLimits) {
        super(fp2, implLevel, world, id);

        this.coordLimits = coordLimits;

        this.renderer = this.getForInit(() -> this.workerManager().rootExecutor().supply(MarkedFutureExecutor.DEFAULT_MARKER, () -> new LevelRenderer1_12_2(Minecraft.getMinecraft(), this)).join());
    }

    @Override
    protected WorkerManager createWorkerManager() {
        Minecraft mc = ((ATNetHandlerPlayClient1_12) ((ATWorldClient1_12) this.implLevel()).getConnection()).getClient();
        return new DefaultWorkerManager(((ATMinecraft1_12) mc).getThread(), ClientThreadMarkedFutureExecutor.getFor(mc));
    }

    @Override
    protected void doClose() throws Exception {
        //noinspection Convert2MethodRef
        try (AutoCloseable closeSuper = () -> super.doClose()) {
            this.workerManager().rootExecutor().run(MarkedFutureExecutor.DEFAULT_MARKER, this.renderer::close);
        }
    }
}
