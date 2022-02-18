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

package net.daporkchop.fp2.impl.mc.forge1_16.server.world;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.event.EventBus;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockWorldEvent;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.server.ATMinecraftServer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.AbstractFarWorld1_16;
import net.minecraft.world.server.ServerWorld;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class FarWorldServer1_16 extends AbstractFarWorld1_16<ServerWorld> implements IFarWorldServer {
    protected Map<IFarRenderMode, IFarTileProvider> tileProvidersByMode;

    protected WorkerManager workerManager;
    protected FBlockWorld exactFBlockWorld;
    protected FEventBus eventBus = new EventBus();

    protected IntAxisAlignedBB coordLimits;

    public FarWorldServer1_16(@NonNull FP2Forge1_16 fp2, @NonNull ServerWorld world) {
        super(fp2, world);
    }

    protected void ensureInitialized() {
        checkState(this.coordLimits != null, "not initialized!");
    }

    @Override
    public void fp2_IFarWorldServer_init() {
        checkState(this.coordLimits == null, "already initialized!");

        this.coordLimits = this.fp2.eventBus().fireAndGetFirst(new GetCoordinateLimitsEvent(this)).get();

        this.workerManager = new DefaultWorkerManager(this.world.getServer().getRunningThread(), ServerThreadMarkedFutureExecutor1_16.getFor(this.world.getServer()));

        this.exactFBlockWorld = this.fp2.eventBus().fireAndGetFirst(new GetExactFBlockWorldEvent(this)).get();

        ImmutableMap.Builder<IFarRenderMode, IFarTileProvider> builder = ImmutableMap.builder();
        //TODO: uncomment this
        // IFarRenderMode.REGISTRY.forEachEntry((name, mode) -> builder.put(mode, mode.tileProvider(uncheckedCast(this))));
        this.tileProvidersByMode = builder.build();
    }

    @Override
    public void fp2_IFarWorld_close() {
        this.fp2_IFarWorldServer_forEachTileProvider(IFarTileProvider::close);
    }

    @Override
    public IntAxisAlignedBB fp2_IFarWorld_coordLimits() {
        this.ensureInitialized();
        return this.coordLimits;
    }

    @Override
    public WorkerManager fp2_IFarWorld_workerManager() {
        this.ensureInitialized();
        return this.workerManager;
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarTileProvider<POS, T> fp2_IFarWorldServer_tileProviderFor(@NonNull IFarRenderMode<POS, T> mode) {
        this.ensureInitialized();

        IFarTileProvider<POS, T> context = uncheckedCast(this.tileProvidersByMode.get(mode));
        checkArg(context != null, "cannot find tile provider for unknown render mode: %s", mode);
        return context;
    }

    @Override
    public void fp2_IFarWorldServer_forEachTileProvider(@NonNull Consumer<IFarTileProvider<?, ?>> action) {
        this.ensureInitialized();
        this.tileProvidersByMode.values().forEach(uncheckedCast(action));
    }

    @Override
    public Path fp2_IFarWorldServer_worldDirectory() {
        this.ensureInitialized();
        return ((ATMinecraftServer1_16) this.world.getServer()).getStorageSource().getDimensionPath(this.world.dimension()).toPath();
    }

    @Override
    public TerrainGeneratorInfo fp2_IFarWorldServer_terrainGeneratorInfo() {
        return new TerrainGeneratorInfo1_16(this.world);
    }

    @Override
    public FBlockWorld fp2_IFarWorldServer_fblockWorld() {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public int fp2_IFarWorldServer_seaLevel() {
        return this.world.getSeaLevel();
    }

    @Override
    public FEventBus fp2_IFarWorldServer_eventBus() {
        return this.eventBus;
    }
}
