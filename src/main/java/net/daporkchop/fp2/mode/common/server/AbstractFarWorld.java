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

package net.daporkchop.fp2.mode.common.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.mode.common.server.storage.rocksdb.RocksStorage;
import net.daporkchop.fp2.server.worldlistener.IWorldChangeListener;
import net.daporkchop.fp2.server.worldlistener.WorldChangeListenerManager;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.fp2.util.threading.asyncblockaccess.IAsyncBlockAccess;
import net.daporkchop.fp2.util.threading.scheduler.Scheduler;
import net.daporkchop.fp2.util.threading.scheduler.ApproximatelyPrioritizedSharedFutureScheduler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld<POS extends IFarPos, T extends IFarTile> implements IFarWorld<POS, T>, IWorldChangeListener {
    protected static final int PRIORITY_UPDATE = 1;
    protected static final int PRIORITY_LOAD = -1;

    protected final WorldServer world;
    protected final IFarRenderMode<POS, T> mode;
    protected final File root;

    protected final IFarGeneratorRough<POS, T> generatorRough;
    protected final IFarGeneratorExact<POS, T> generatorExact;
    protected final IFarScaler<POS, T> scaler;

    protected final IFarStorage<POS, T> storage;

    protected final IFarPlayerTracker<POS, T> tracker;

    protected final Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler; //TODO: make this global rather than per-dimension

    protected final boolean lowResolution;

    protected long currentTime = -1L;

    public AbstractFarWorld(@NonNull WorldServer world, @NonNull IFarRenderMode<POS, T> mode) {
        this.world = world;
        this.mode = mode;

        this.generatorRough = this.mode().roughGenerator(world);
        this.generatorExact = this.mode().exactGenerator(world);

        if (this.generatorRough == null) {
            FP2_LOG.warn("no rough {} generator exists for world {} (type={}, generator={})! Falling back to exact generator, this will have serious performance implications.", mode.name(), world.provider.getDimension(), world.getWorldType(), Constants.getTerrainGenerator(world));
            //TODO: make the fallback generator smart! rather than simply getting the chunks from the world, do generation and population in
            // a volatile, in-memory world clone to prevent huge numbers of chunks/cubes from potentially being generated (and therefore saved)
        }

        this.lowResolution = FP2Config.performance.lowResolutionEnable && this.generatorRough != null && this.generatorRough.supportsLowResolution();

        this.scaler = this.createScaler();
        this.tracker = this.createTracker();

        this.root = new File(world.getChunkSaveLocation(), "fp2/" + this.mode().name().toLowerCase());
        this.storage = new RocksStorage<>(mode, this.root);

        this.scheduler = new ApproximatelyPrioritizedSharedFutureScheduler<>(
                scheduler -> new FarServerWorker<>(this, scheduler),
                ThreadingHelper.workerGroupBuilder()
                        .world(this.world)
                        .threads(FP2Config.generationThreads)
                        .threadFactory(PThreadFactories.builder().daemon().minPriority()
                                .collapsingId().name(PStrings.fastFormat("FP2 %s DIM%d Worker #%%d", mode.name(), world.provider.getDimension())).build()),
                Comparator.<PriorityTask<POS>, TaskStage>comparing(PriorityTask::stage).thenComparingInt(task -> task.pos().level()));

        //add all dirty tiles to update queue
        this.storage.dirtyTracker().forEachDirtyPos((pos, timestamp) -> this.enqueueUpdate(pos));

        WorldChangeListenerManager.add(this.world, this);
    }

    protected abstract IFarScaler<POS, T> createScaler();

    protected abstract IFarPlayerTracker<POS, T> createTracker();

    protected abstract boolean anyVanillaTerrainExistsAt(@NonNull POS pos);

    @Override
    public CompletableFuture<ITileHandle<POS, T>> requestLoad(@NonNull POS pos) {
        return this.scheduler.schedule(new PriorityTask<>(TaskStage.LOAD, pos));
    }

    public void tileAvailable(@NonNull ITileHandle<POS, T> handle) {
        this.notifyPlayerTracker(handle);
    }

    public void tileChanged(@NonNull ITileHandle<POS, T> handle, boolean allowScale) {
        this.tileAvailable(handle);

        if (allowScale && handle.pos().level() < FP2Config.maxLevels - 1) {
            this.scheduleForUpdate(this.scaler.outputs(handle.pos()));
        }
    }

    public void notifyPlayerTracker(@NonNull ITileHandle<POS, T> handle) {
        this.tracker.tileChanged(handle);
    }

    public boolean canGenerateRough(@NonNull POS pos) {
        return this.generatorRough != null && (pos.level() == 0 || this.lowResolution);
    }

    protected void enqueueUpdate(@NonNull POS pos) {
        //TODO: this.scheduler.schedule(new PriorityTask<>(TaskStage.UPDATE, pos));
    }

    protected void scheduleForUpdate(@NonNull Stream<POS> positions) {
        this.storage.dirtyTracker()
                .markDirty(positions, this.currentTime)
                .forEach(this::enqueueUpdate);
    }

    protected void scheduleForUpdate(@NonNull POS... positions) {
        this.scheduleForUpdate(Stream.of(positions));
    }

    @Override
    public void onTickEnd() {
        this.currentTime = this.world.getTotalWorldTime();
    }

    @Override
    public IAsyncBlockAccess blockAccess() {
        return ((IAsyncBlockAccess.Holder) this.world).asyncBlockAccess();
    }

    @Override
    @SneakyThrows(IOException.class)
    public void close() {
        WorldChangeListenerManager.remove(this.world, this);
        this.onTickEnd();

        this.scheduler.close();

        FP2_LOG.trace("Shutting down storage in DIM{}", this.world.provider.getDimension());
        this.storage.close();
    }
}
