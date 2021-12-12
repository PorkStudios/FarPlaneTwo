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

package net.daporkchop.fp2.core.mode.common.server;

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.util.threading.scheduler.ApproximatelyPrioritizedSharedFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.fp2.core.mode.common.server.storage.rocksdb.RocksStorage;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarTileProvider<POS extends IFarPos, T extends IFarTile> implements IFarTileProvider<POS, T> {
    protected final IFarWorldServer world;
    protected final IFarRenderMode<POS, T> mode;
    protected final Path root;

    protected final IFarGeneratorRough<POS, T> generatorRough;
    protected final IFarGeneratorExact<POS, T> generatorExact;
    protected final IFarScaler<POS, T> scaler;

    protected final IFarStorage<POS, T> storage;

    protected final IFarTrackerManager<POS, T> trackerManager;

    protected final Scheduler<PriorityTask<POS>, ITileHandle<POS, T>> scheduler; //TODO: make this global rather than per-mode and per-dimension

    protected final boolean lowResolution;

    protected Set<POS> updatesPending = new ObjectRBTreeSet<>();
    protected long lastCompletedTick = -1L;

    public AbstractFarTileProvider(@NonNull IFarWorldServer world, @NonNull IFarRenderMode<POS, T> mode) {
        this.world = world;
        this.mode = mode;

        this.generatorRough = this.mode().roughGenerator(world);
        this.generatorExact = this.mode().exactGenerator(world);

        if (this.generatorRough == null) {
            fp2().log().warn("no rough %s generator exists for DIM%d (generator=%s)! Falling back to exact generator, this will have serious performance implications.", mode.name(), world.fp2_IFarWorld_dimensionId(), world.fp2_IFarWorldServer_terrainGeneratorInfo().implGenerator());
            //TODO: make the fallback generator smart! rather than simply getting the chunks from the world, do generation and population in
            // a volatile, in-memory world clone to prevent huge numbers of chunks/cubes from potentially being generated (and therefore saved)
        }

        this.lowResolution = this.generatorRough != null && this.generatorRough.supportsLowResolution();

        this.scaler = mode.scaler(world);

        this.root = world.fp2_IFarWorldServer_worldDirectory().resolve(MODID).resolve(this.mode().name().toLowerCase());
        this.storage = new RocksStorage<>(this, this.root);

        this.scheduler = new ApproximatelyPrioritizedSharedFutureScheduler<>(
                scheduler -> task -> {
                    switch (task.stage()) {
                        case LOAD:
                            return new AbstractTileTask.Load<>(this, scheduler, task.pos()).get();
                        case UPDATE:
                            return new AbstractTileTask.Update<>(this, scheduler, task.pos()).get();
                        default:
                            throw new IllegalArgumentException("unknown or stage in task: " + task);
                    }
                },
                this.world.fp2_IFarWorld_workerManager().createChildWorkerGroup()
                        .threads(fp2().globalConfig().performance().terrainThreads())
                        .threadFactory(PThreadFactories.builder().daemon().minPriority().collapsingId()
                                .name(PStrings.fastFormat("FP2 %s DIM%d Worker #%%d", mode.name(), world.fp2_IFarWorld_dimensionId())).build()),
                PriorityTask.approxComparator());

        this.trackerManager = this.createTracker();

        //TODO: figure out why i was registering this here?
        // fp2().eventBus().registerWeak(this);

        world.fp2_IFarWorldServer_eventBus().registerWeak(this);
    }

    protected abstract IFarTrackerManager<POS, T> createTracker();

    protected abstract boolean anyVanillaTerrainExistsAt(@NonNull POS pos);

    protected PriorityTask<POS> taskFor(@NonNull TaskStage stage, @NonNull POS pos) {
        return PriorityTask.forStageAndPosition(stage, pos);
    }

    protected PriorityTask<POS> loadTaskFor(@NonNull POS pos) {
        return this.taskFor(TaskStage.LOAD, pos);
    }

    protected PriorityTask<POS> updateTaskFor(@NonNull POS pos) {
        return this.taskFor(TaskStage.UPDATE, pos);
    }

    @Override
    public CompletableFuture<ITileHandle<POS, T>> requestLoad(@NonNull POS pos) {
        return this.scheduler.schedule(this.loadTaskFor(pos));
    }

    @Override
    public CompletableFuture<ITileHandle<POS, T>> requestUpdate(@NonNull POS pos) {
        return this.scheduler.schedule(this.updateTaskFor(pos));
    }

    public boolean canGenerateRough(@NonNull POS pos) {
        return this.generatorRough != null && (pos.level() == 0 || this.lowResolution);
    }

    protected void scheduleForUpdate(@NonNull POS... positions) {
        this.scheduleForUpdate(Stream.of(positions));
    }

    @Synchronized("updatesPending")
    protected void scheduleForUpdate(@NonNull Stream<POS> positions) {
        positions.forEach(pos -> {
            for (; this.updatesPending.add(pos) && pos.isLevelValid(); pos = uncheckedCast(pos.up())) {
            }
        });
    }

    @FEventHandler
    protected abstract void onColumnSaved(ColumnSavedEvent event);

    @FEventHandler
    protected abstract void onCubeSaved(CubeSavedEvent event);

    @FEventHandler
    private void onTickEnd(TickEndEvent event) {
        this.lastCompletedTick = this.world.fp2_IFarWorld_timestamp();
        checkState(this.lastCompletedTick >= 0L, "lastCompletedTick (%d) < 0?!?", this.lastCompletedTick);

        this.flushUpdateQueue();
    }

    @Synchronized("updatesPending")
    protected void flushUpdateQueue() {
        checkState(this.lastCompletedTick >= 0L, "flushed update queue before any game ticks were completed?!?");

        if (!this.updatesPending.isEmpty()) {
            this.storage.markAllDirty(StreamSupport.stream(Spliterators.spliterator(this.updatesPending, DISTINCT | NONNULL), false), this.lastCompletedTick)
                    .count(); //arbitrary lightweight terminal operation
            this.updatesPending.clear();
        }
    }

    @Synchronized("updatesPending")
    protected void shutdownUpdateQueue() {
        this.flushUpdateQueue();
        this.updatesPending = null;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void close() {
        this.trackerManager.close();

        fp2().eventBus().unregister(this);

        this.scheduler.close();

        this.onTickEnd(null);
        this.shutdownUpdateQueue();

        fp2().log().trace("Shutting down storage in DIM%d", this.world.fp2_IFarWorld_dimensionId());
        this.storage.close();
    }
}
