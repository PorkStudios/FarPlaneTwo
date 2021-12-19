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

package net.daporkchop.fp2.impl.mc.forge1_12_2;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.core.util.threading.futureexecutor.MarkedFutureExecutor;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.render.WorldRenderer1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.daporkchop.fp2.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class FakeFarWorldClient implements IFarWorldClient {
    protected final WorldClient parent;

    protected final IntAxisAlignedBB coordLimits;
    protected final WorkerManager workerManager;

    protected WorldRenderer1_12_2 renderer;

    public FakeFarWorldClient(@NonNull WorldClient parent, @NonNull IntAxisAlignedBB coordLimits) {
        this.parent = parent;

        this.coordLimits = coordLimits;
        this.workerManager = new DefaultWorkerManager(Minecraft.getMinecraft().thread, ClientThreadMarkedFutureExecutor.getFor(Minecraft.getMinecraft()));

        this.workerManager.rootExecutor().run(MarkedFutureExecutor.DEFAULT_MARKER, () -> this.renderer = new WorldRenderer1_12_2(Minecraft.getMinecraft(), this)).join();
    }

    @Override
    public void fp2_IFarWorld_init() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fp2_IFarWorld_close() {
        this.workerManager.rootExecutor().run(MarkedFutureExecutor.DEFAULT_MARKER, this.renderer::close);
    }

    @Override
    public WorldClient fp2_IFarWorld_implWorld() {
        return this.parent;
    }

    @Override
    public IntAxisAlignedBB fp2_IFarWorld_coordLimits() {
        return this.coordLimits;
    }

    @Override
    public WorkerManager fp2_IFarWorld_workerManager() {
        return this.workerManager;
    }

    @Override
    public int fp2_IFarWorld_dimensionId() {
        return this.parent.provider.getDimension();
    }

    @Override
    public long fp2_IFarWorld_timestamp() {
        return this.parent.getTotalWorldTime();
    }

    @Override
    public WorldRenderer fp2_IFarWorldClient_renderer() {
        checkState(this.renderer != null, "renderer hasn't been initialized!");
        return this.renderer;
    }

    @Override
    public GameRegistry1_12_2 fp2_IFarWorld_registry() {
        return GameRegistry1_12_2.get();
    }
}
