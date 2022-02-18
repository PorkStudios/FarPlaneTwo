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

package net.daporkchop.fp2.impl.mc.forge1_16.client.world;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.core.client.world.IFarWorldClient;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.ATMinecraft1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.world.ATClientWorld1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.AbstractFarWorld1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;

/**
 * @author DaPorkchop_
 */
public class FarWorldClient1_16 extends AbstractFarWorld1_16<ClientWorld> implements IFarWorldClient {
    protected final IntAxisAlignedBB coordLimits;
    protected final WorkerManager workerManager;

    public FarWorldClient1_16(@NonNull FP2Forge1_16 fp2, @NonNull ClientWorld world, @NonNull IntAxisAlignedBB coordLimits) {
        super(fp2, world);

        this.coordLimits = coordLimits;

        Minecraft mc = ((ATClientWorld1_16) world).getMinecraft();
        this.workerManager = new DefaultWorkerManager(((ATMinecraft1_16) mc).getGameThread(), ClientThreadMarkedFutureExecutor1_16.getFor(mc));
    }

    @Override
    public void fp2_IFarWorld_close() {
        //TODO:
        // this.workerManager.rootExecutor().run(MarkedFutureExecutor.DEFAULT_MARKER, this.renderer::close);
    }

    @Override
    public WorldRenderer fp2_IFarWorldClient_renderer() {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public IntAxisAlignedBB fp2_IFarWorld_coordLimits() {
        return this.coordLimits;
    }

    @Override
    public WorkerManager fp2_IFarWorld_workerManager() {
        return this.workerManager;
    }
}
