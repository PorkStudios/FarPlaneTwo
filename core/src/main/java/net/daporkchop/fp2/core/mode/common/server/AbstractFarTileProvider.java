/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.common.server;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TileCoordLimits;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.server.scale.VoxelScalerIntersection;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.api.server.storage.FTileStorage;
import net.daporkchop.fp2.core.engine.tile.ITileHandle;
import net.daporkchop.fp2.core.mode.common.server.storage.DefaultTileStorage;
import net.daporkchop.fp2.core.mode.common.server.tracking.AbstractTrackerManager;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.threading.scheduler.ApproximatelyPrioritizedSharedFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarTileProvider implements IFarTileProvider {
    protected final IFarLevelServer world;

    protected final IFarGeneratorRough generatorRough;
    protected final IFarGeneratorExact generatorExact;
    protected final IFarScaler scaler;

    protected final FTileStorage storage;

    protected final TileCoordLimits coordLimits;

    protected final AbstractTrackerManager trackerManager;

    protected final Scheduler<PriorityTask, ITileHandle> scheduler; //TODO: make this global rather than per-mode and per-dimension

    protected Set<TilePos> updatesPending = new ObjectRBTreeSet<>();
    protected long lastCompletedTick = -1L;

    protected boolean open = false;

    public AbstractFarTileProvider(@NonNull IFarLevelServer world) {
        this.world = world;

        this.coordLimits = new TileCoordLimits(world.coordLimits());

        this.generatorRough = fp2().eventBus().fireAndGetFirst(new RoughGeneratorCreationEvent(world, this)).orElse(null);
        this.generatorExact = fp2().eventBus().fireAndGetFirst(new ExactGeneratorCreationEvent(world, this))
                .orElseThrow(() -> new IllegalStateException("no exact generator available for world '" + world.id() + '\''));

        if (this.generatorRough == null) {
            fp2().log().warn("no rough generator exists for world '%s' (generator=%s)! Falling back to exact generator, this will have serious performance implications.", world.id(), world.terrainGeneratorInfo().implGenerator());
            //TODO: make the fallback generator smart! rather than simply getting the chunks from the world, do generation and population in
            // a volatile, in-memory world clone to prevent huge numbers of chunks/cubes from potentially being generated (and therefore saved)
        }

        //TODO: maybe i should use an event for this?
        this.scaler = new VoxelScalerIntersection(world, this);

        try {
            //open the tile storage
            this.storage = world.storageCategory().openOrCreateItem("tiles", DefaultTileStorage.factory(this));

            //create the task scheduler
            this.scheduler = new ApproximatelyPrioritizedSharedFutureScheduler<>(
                    scheduler -> new TileWorker(this, scheduler),
                    this.world.workerManager().createChildWorkerGroup()
                            .threads(fp2().globalConfig().performance().terrainThreads())
                            .threadFactory(PThreadFactories.builder().daemon().minPriority().collapsingId()
                                    .name(PStrings.fastFormat("FP2 %s Worker #%%d", world.id())).build()),
                    PriorityTask.approxComparator());

            //create the tracker manager
            this.trackerManager = this.createTracker();

            //register self to listen for events
            //TODO: figure out why i was registering this to the global event bus?
            // fp2().eventBus().registerWeak(this);
            world.eventBus().registerWeak(this);

            //mark self as open in order to make all resources be released on close
            this.open = true;
        } catch (Exception e) {
            //something went wrong, call close() to clean up any resources which may have been opened
            try {
                this.close();
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public void close() {
        //try-with-resources to ensure that everything is closed
        try (FTileStorage storage = this.storage;
             Scheduler<PriorityTask, ITileHandle> scheduler = this.scheduler;
             AbstractTrackerManager trackerManager = this.trackerManager) {

            if (this.open) { //the provider has been fully opened, we should unregister ourself from the event bus and flush all the queues
                fp2().eventBus().unregister(this);

                this.onTickEnd(null);
                this.shutdownUpdateQueue();
            }
        }

        //original shutdown order:
        /*this.trackerManager.close();

        fp2().eventBus().unregister(this);

        this.scheduler.close();

        this.onTickEnd(null);
        this.shutdownUpdateQueue();

        fp2().log().trace("Shutting down storage in world '%s'", this.world.id());
        this.storage.close();*/
    }

    protected abstract AbstractTrackerManager createTracker();

    protected abstract boolean anyVanillaTerrainExistsAt(@NonNull TilePos pos);

    protected boolean anyVanillaTerrainExistsAt(@NonNull List<TilePos> positions) {
        return positions.stream().anyMatch(this::anyVanillaTerrainExistsAt);
    }

    @Override
    public CompletableFuture<ITileHandle> requestLoad(@NonNull TilePos pos) {
        return this.scheduler.schedule(TaskStage.LOAD.taskForPosition(pos));
    }

    @Override
    public CompletableFuture<ITileHandle> requestUpdate(@NonNull TilePos pos) {
        return this.scheduler.schedule(TaskStage.UPDATE.taskForPosition(pos));
    }

    @Override
    public long currentTimestamp() {
        checkState(this.lastCompletedTick >= 0L, "no game ticks have been completed?!?");
        return this.lastCompletedTick;
    }

    public boolean canGenerateRough(@NonNull TilePos pos) {
        return this.generatorRough != null && this.generatorRough.canGenerate(pos);
    }

    protected void scheduleForUpdate(@NonNull TilePos pos) {
        this.scheduleForUpdate(ImmutableList.of(pos));
    }

    protected void scheduleForUpdate(@NonNull TilePos... positions) {
        this.scheduleForUpdate(Arrays.asList(positions));
    }

    protected void scheduleForUpdate(@NonNull Collection<TilePos> positions) {
        positions.forEach(pos -> checkArg(pos.level() == 0, "position must be at level 0! %s", pos));

        //noinspection SynchronizeOnNonFinalField
        synchronized (this.updatesPending) {
            this.updatesPending.addAll(positions);
        }
    }

    @FEventHandler
    protected abstract void onColumnSaved(ColumnSavedEvent event);

    @FEventHandler
    protected abstract void onCubeSaved(CubeSavedEvent event);

    @FEventHandler
    private void onTickEnd(TickEndEvent event) {
        this.lastCompletedTick = this.world.timestamp();
        checkState(this.lastCompletedTick >= 0L, "lastCompletedTick (%d) < 0?!?", this.lastCompletedTick);

        this.flushUpdateQueue();
    }

    @Synchronized("updatesPending")
    protected void flushUpdateQueue() {
        checkState(this.lastCompletedTick >= 0L, "flushed update queue before any game ticks were completed?!?");

        if (!this.updatesPending.isEmpty()) {
            //iterate up through all of the scaler outputs and enqueue them all for marking as dirty
            Collection<TilePos> last = this.updatesPending;
            for (int level = 0; level + 1 < EngineConstants.MAX_LODS; level++) {
                Collection<TilePos> next = this.scaler.uniqueOutputs(last);
                this.updatesPending.addAll(next);
                last = next;
            }

            //actually mark all of the queued tiles as dirty
            this.storage.multiMarkDirty(new ArrayList<>(this.updatesPending), this.lastCompletedTick);

            //clear the pending updates queue
            this.updatesPending.clear();
        }
    }

    @Synchronized("updatesPending")
    protected void shutdownUpdateQueue() {
        this.flushUpdateQueue();
        this.updatesPending = null;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static final class ExactGeneratorCreationEvent implements IFarGeneratorExact.CreationEvent {
        @NonNull
        private final IFarLevelServer world;
        @NonNull
        private final IFarTileProvider provider;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static final class RoughGeneratorCreationEvent implements IFarGeneratorRough.CreationEvent {
        @NonNull
        private final IFarLevelServer world;
        @NonNull
        private final IFarTileProvider provider;
    }
}
