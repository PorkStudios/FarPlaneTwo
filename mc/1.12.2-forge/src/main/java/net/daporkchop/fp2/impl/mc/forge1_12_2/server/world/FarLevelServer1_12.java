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

package net.daporkchop.fp2.impl.mc.forge1_12_2.server.world;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.event.EventBus;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockLevelEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.IFarLevelServer;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.server.ATMinecraftServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.TerrainGeneratorInfo1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.AbstractFarLevel1_12;
import net.minecraft.world.WorldServer;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class FarLevelServer1_12 extends AbstractFarLevel1_12<WorldServer> implements IFarLevelServer {
    protected Map<IFarRenderMode, IFarTileProvider> tileProvidersByMode;
    protected IFarTileProvider[] tileProviders;

    protected WorkerManager workerManager;
    protected ExactFBlockLevelHolder exactFBlockLevelHolder;
    protected FEventBus eventBus = new EventBus();

    protected IntAxisAlignedBB coordLimits;

    public FarLevelServer1_12(@NonNull FP2Forge1_12_2 fp2, @NonNull WorldServer world) {
        super(fp2, world);
    }

    @Override
    public IntAxisAlignedBB coordLimits() {
        checkState(this.coordLimits != null, "not initialized!");
        return this.coordLimits;
    }

    @Override
    public void init() {
        checkState(this.coordLimits == null, "already initialized!");

        this.coordLimits = this.fp2().eventBus().fireAndGetFirst(new GetCoordinateLimitsEvent(this)).get();

        this.workerManager = new DefaultWorkerManager(((ATMinecraftServer1_12) this.world.getMinecraftServer()).getServerThread(), ServerThreadMarkedFutureExecutor.getFor(this.world.getMinecraftServer()));

        this.exactFBlockLevelHolder = this.fp2().eventBus().fireAndGetFirst(new GetExactFBlockLevelEvent(this)).get();

        ImmutableMap.Builder<IFarRenderMode, IFarTileProvider> builder = ImmutableMap.builder();
        IFarRenderMode.REGISTRY.forEachEntry((name, mode) -> builder.put(mode, mode.tileProvider(uncheckedCast(this))));
        this.tileProvidersByMode = builder.build();

        this.tileProviders = this.tileProvidersByMode.values().toArray(new IFarTileProvider[0]);
    }

    @Override
    public void close() {
        this.forEachTileProvider(IFarTileProvider::close);
        this.exactFBlockLevelHolder.close();
    }

    @Override
    public WorkerManager workerManager() {
        return this.workerManager;
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarTileProvider<POS, T> tileProviderFor(@NonNull IFarRenderMode<POS, T> mode) {
        IFarTileProvider<POS, T> context = uncheckedCast(this.tileProvidersByMode.get(mode));
        checkArg(context != null, "cannot find tile provider for unknown render mode: %s", mode);
        return context;
    }

    @Override
    public void forEachTileProvider(@NonNull Consumer<IFarTileProvider<?, ?>> action) {
        for (IFarTileProvider tileProvider : this.tileProviders) {
            action.accept(uncheckedCast(tileProvider));
        }
    }

    @Override
    public Path levelDirectory() {
        return this.world.getChunkSaveLocation().toPath();
    }

    @Override
    public TerrainGeneratorInfo terrainGeneratorInfo() {
        return new TerrainGeneratorInfo1_12_2(this.world);
    }

    @Override
    public ExactFBlockLevelHolder exactBlockLevelHolder() {
        return this.exactFBlockLevelHolder;
    }

    @Override
    public int seaLevel() {
        return this.world.getSeaLevel();
    }

    @Override
    public FEventBus eventBus() {
        return this.eventBus;
    }
}
