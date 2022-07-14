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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.client.world.level;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.client.world.level.AbstractLevelClient;
import net.daporkchop.fp2.core.util.threading.futureexecutor.MarkedFutureExecutor;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.ATMinecraft1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.world.ATClientWorld1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.render.LevelRenderer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.world.FWorldClient1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.level.IFLevel1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.registry.GameRegistry1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;

/**
 * @author DaPorkchop_
 */
@Getter
public class FLevelClient1_16 extends AbstractLevelClient<FP2Forge1_16, ClientPlayNetHandler, FWorldClient1_16, ClientWorld, FLevelClient1_16> implements IFLevel1_16 {
    private final IntAxisAlignedBB coordLimits;
    private final GameRegistry1_16 registry;

    private final LevelRenderer1_16 renderer;

    public FLevelClient1_16(@NonNull FP2Forge1_16 fp2, @NonNull ClientWorld implLevel, @NonNull FWorldClient1_16 world, @NonNull Identifier id, @NonNull IntAxisAlignedBB coordLimits) {
        super(fp2, implLevel, world, id);

        this.coordLimits = coordLimits;
        this.registry = this.getForInit(() -> new GameRegistry1_16(implLevel));

        Minecraft mc = ((ATClientWorld1_16) implLevel).getMinecraft();
        this.renderer = this.getForInit(() -> this.workerManager().rootExecutor().supply(MarkedFutureExecutor.DEFAULT_MARKER, () -> new LevelRenderer1_16(mc, this)).join());
    }

    @Override
    protected WorkerManager createWorkerManager() {
        Minecraft mc = ((ATClientWorld1_16) this.implLevel()).getMinecraft();
        return new DefaultWorkerManager(((ATMinecraft1_16) mc).getGameThread(), ClientThreadMarkedFutureExecutor1_16.getFor(mc));
    }

    @Override
    protected void doClose() throws Exception {
        //noinspection Convert2MethodRef
        try (AutoCloseable closeSuper = () -> super.doClose()) {
            //close renderer on client thread
            this.workerManager().rootExecutor().run(MarkedFutureExecutor.DEFAULT_MARKER, this.renderer::close);
        }
    }
}
