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

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
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
import net.daporkchop.fp2.util.threading.scheduler.ApproximatelyPrioritizedSharedFutureScheduler;
import net.daporkchop.fp2.util.threading.scheduler.Scheduler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

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

    protected Set<POS> updatesPending = new ObjectRBTreeSet<>();
    protected long lastCompletedTick = -1L;
    protected boolean restoredPendingUpdates = false;

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

        this.root = new File(world.getChunkSaveLocation(), "fp2/" + this.mode().name().toLowerCase());
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
                ThreadingHelper.workerGroupBuilder()
                        .world(this.world)
                        .threads(FP2Config.generationThreads)
                        .threadFactory(PThreadFactories.builder().daemon().minPriority()
                                .collapsingId().name(PStrings.fastFormat("FP2 %s DIM%d Worker #%%d", mode.name(), world.provider.getDimension())).build()),
                PriorityTask.approxComparator());

        this.tracker = this.createTracker();

        WorldChangeListenerManager.add(this.world, this);
    }

    protected abstract IFarScaler<POS, T> createScaler();

    protected abstract IFarPlayerTracker<POS, T> createTracker();

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

    public boolean canGenerateRough(@NonNull POS pos) {
        return this.generatorRough != null && (pos.level() == 0 || this.lowResolution);
    }

    protected void scheduleForUpdate(@NonNull POS... positions) {
        this.scheduleForUpdate(Stream.of(positions));
    }

    @Synchronized("updatesPending")
    protected void scheduleForUpdate(@NonNull Stream<POS> positions) {
        positions.forEach(pos -> {
            for (; this.updatesPending.add(pos) && pos.level() < MAX_LODS; pos = uncheckedCast(pos.up())) {
            }
        });
    }

    @Override
    public void onTickEnd() {
        this.lastCompletedTick = this.world.getTotalWorldTime();
        checkState(this.lastCompletedTick >= 0L, "lastCompletedTick (%d) < 0?!?", this.lastCompletedTick);

        if (!this.restoredPendingUpdates) {
            this.restoredPendingUpdates = true;
            this.restorePendingUpdates();
        }

        this.flushUpdateQueue();
    }

    @Synchronized("updatesPending")
    protected void restorePendingUpdates() {
        //add all dirty tiles to update queue
        class Adder implements Consumer<POS> {
            long addedCount = 0L;
            long skippedCount = 0L;

            @Override
            public void accept(@NonNull POS pos) {
                if (pos.level() < FP2Config.maxLevels) {
                    this.addedCount++;
                    AbstractFarWorld.this.scheduler.schedule(AbstractFarWorld.this.updateTaskFor(pos));
                } else {
                    this.skippedCount++;
                }
            }
        }
        Adder adder = new Adder();
        this.storage.forEachDirtyPos(adder);
        FP2_LOG.info("restored {} dirty {} tiles in DIM{} (skipped {})", adder.addedCount, this.mode.name(), this.world.provider.getDimension(), adder.skippedCount);
    }

    @Synchronized("updatesPending")
    protected void flushUpdateQueue() {
        checkState(this.lastCompletedTick >= 0L, "flushed update queue before any game ticks were completed?!?");

        if (!this.updatesPending.isEmpty()) {
            this.storage.markAllDirty(StreamSupport.stream(Spliterators.spliterator(this.updatesPending, DISTINCT | NONNULL), false), this.lastCompletedTick)
                    .forEach(pos -> {
                        if (pos.level() < FP2Config.maxLevels) {
                            this.scheduler.schedule(this.updateTaskFor(pos));
                        }
                    });
            this.updatesPending.clear();
        }
    }

    @Synchronized("updatesPending")
    protected void shutdownUpdateQueue() {
        this.flushUpdateQueue();
        this.updatesPending = null;
    }

    @Override
    public IAsyncBlockAccess blockAccess() {
        return ((IAsyncBlockAccess.Holder) this.world).fp2_IAsyncBlockAccess$Holder_asyncBlockAccess();
    }

    @Override
    @SneakyThrows(IOException.class)
    public void close() {
        WorldChangeListenerManager.remove(this.world, this);

        this.scheduler.close();

        this.onTickEnd();
        this.shutdownUpdateQueue();

        FP2_LOG.trace("Shutting down storage in DIM{}", this.world.provider.getDimension());
        this.storage.close();
    }
}
