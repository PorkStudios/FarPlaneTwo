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

package net.daporkchop.fp2.core.mode.common.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.core.util.SimpleRecycler;
import net.daporkchop.fp2.core.util.threading.scheduler.NoFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.mode.common.client.index.IRenderIndex;
import net.daporkchop.fp2.core.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class BakeManager<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IFarTileCache.Listener<POS, T>, Consumer<POS>, Runnable {
    protected final AbstractFarRenderer<POS, T> renderer;
    protected final IFarRenderStrategy<POS, T, ?, ?, ?> strategy;

    protected final IFarTileCache<POS, T> tileCache;

    protected final IRenderIndex<POS, ?, ?, ?> index;
    protected final IRenderBaker<POS, T, ?> baker;

    protected final Scheduler<POS, Void> bakeScheduler;
    protected final IFarCoordLimits<POS> coordLimits;

    protected final Map<POS, Optional<IBakeOutput>> pendingDataUpdates = new ConcurrentHashMap<>();
    protected final Map<POS, Boolean> pendingRenderableUpdates = new ConcurrentHashMap<>();
    protected final AtomicBoolean isBulkUpdateQueued = new AtomicBoolean();
    protected final Semaphore dataUpdatesLock = new Semaphore(fp2().globalConfig().performance().maxBakesProcessedPerFrame());

    public BakeManager(@NonNull AbstractFarRenderer<POS, T> renderer, @NonNull IFarTileCache<POS, T> tileCache) {
        this.renderer = renderer;
        this.strategy = renderer.strategy();
        this.tileCache = tileCache;

        this.index = this.strategy.createIndex();
        this.baker = this.strategy.createBaker();
        this.coordLimits = this.renderer.mode().tileCoordLimits(renderer.context().level().coordLimits());

        this.bakeScheduler = new NoFutureScheduler<>(this, this.renderer.context().level().workerManager().createChildWorkerGroup()
                .threads(fp2().globalConfig().performance().bakeThreads())
                .threadFactory(PThreadFactories.builder().daemon().minPriority().collapsingId().name("FP2 Rendering Thread #%d").build()));

        this.tileCache.addListener(this, true);
    }

    @Override
    protected void doRelease() {
        this.tileCache.removeListener(this, false);

        //prevent workers from scheduling tasks on the client thread
        this.isBulkUpdateQueued.set(true);

        //reset permit count to maximum possible to prevent infinite blocking while shutting down executor
        this.dataUpdatesLock.drainPermits();
        this.dataUpdatesLock.release(Integer.MAX_VALUE);

        //this will block until all of the bake threads have exited
        this.bakeScheduler.close();

        //release all of the bake outputs and remove them from the map
        for (Iterator<POS> itr = this.pendingDataUpdates.keySet().iterator(); itr.hasNext(); ) {
            this.pendingDataUpdates.computeIfPresent(itr.next(), (pos, output) -> {
                output.ifPresent(IBakeOutput::release);
                return null;
            });
        }
    }

    @Override
    public void tileAdded(@NonNull ITileSnapshot<POS, T> tile) {
        this.notifyOutputs(tile.pos());
    }

    @Override
    public void tileModified(@NonNull ITileSnapshot<POS, T> tile) {
        this.notifyOutputs(tile.pos());
    }

    @Override
    public void tileRemoved(@NonNull POS pos) {
        //schedule the tile itself for re-bake
        this.notifyOutputs(pos);
    }

    protected void notifyOutputs(@NonNull POS pos) {
        //schedule all of the positions affected by the tile for re-bake
        this.baker.bakeOutputs(pos).forEach(outputPos -> {
            if (!outputPos.isLevelValid()) { //output tile is at an invalid zoom level, skip it
                return;
            }

            //schedule tile for baking
            this.bakeScheduler.schedule(outputPos);
        });
    }

    /**
     * Bakes the tile at the given position.
     *
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public void accept(@NonNull POS pos) { //this function is called from inside of bakeScheduler, which doesn't execute the task multiple times on the same position
        this.checkSelfRenderable(pos);
        this.checkParentsRenderable(pos);

        ITileSnapshot<POS, T>[] compressedInputTiles = uncheckedCast(this.tileCache.getTilesCached(this.baker.bakeInputs(pos)).toArray(ITileSnapshot[]::new));
        if (compressedInputTiles[0] == null //tile isn't cached any more
            || compressedInputTiles[0].isEmpty()) { //tile data is empty
            this.updateData(pos, Optional.empty());
            return;
        }

        SimpleRecycler<T> recycler = this.renderer.mode().tileRecycler();
        T[] srcs = this.renderer.mode().tileArray(compressedInputTiles.length);
        try {
            for (int i = 0; i < srcs.length; i++) { //inflate tiles
                if (compressedInputTiles[i] != null) {
                    srcs[i] = compressedInputTiles[i].loadTile(recycler);
                }
            }

            IBakeOutput output = this.strategy.createBakeOutput();
            try {
                this.baker.bake(pos, srcs, uncheckedCast(output));

                this.updateData(pos, !output.isEmpty() ? Optional.of(output.retain()) : Optional.empty());
            } finally {
                output.release();
            }
        } finally { //release tiles again
            for (T src : srcs) {
                if (src != null) {
                    recycler.release(src);
                }
            }
        }
    }

    protected void checkParentsRenderable(@NonNull POS posIn) {
        PorkUtil.<Stream<POS>>uncheckedCast(posIn.up().allPositionsInBB(1, 1)).forEach(this::checkSelfRenderable);
    }

    protected void checkSelfRenderable(@NonNull POS pos) {
        this.updateRenderable(pos,
                this.coordLimits.contains(pos)
                && this.tileCache.getTileCached(pos) != null
                && (pos.level() == 0 || PorkUtil.<Stream<POS>>uncheckedCast(pos.down().allPositionsInBB(1, 3))
                        .anyMatch(p -> this.coordLimits.contains(p) && this.tileCache.getTileCached(p) == null)));
    }

    protected void updateData(@NonNull POS pos, @NonNull Optional<IBakeOutput> optionalBakeOutput) {
        this.dataUpdatesLock.acquireUninterruptibly();

        this.pendingDataUpdates.merge(pos, optionalBakeOutput, (oldOutput, newOutput) -> {
            if (oldOutput.isPresent()) { //release old bake output to avoid potential memory leak when silently replacing entries
                oldOutput.get().release();
            }
            return newOutput;
        });

        if (this.isBulkUpdateQueued.compareAndSet(false, true)) { //we won the race to enqueue a bulk update!
            this.renderer.context().level().workerManager().workExecutor().run(this);
        }
    }

    protected void updateRenderable(@NonNull POS pos, boolean renderable) {
        this.pendingRenderableUpdates.put(pos, renderable);

        if (this.isBulkUpdateQueued.compareAndSet(false, true)) { //we won the race to enqueue a bulk update!
            this.renderer.context().level().workerManager().workExecutor().run(this);
        }
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public void run() { //executes a bulk update on the client thread
        this.isBulkUpdateQueued.set(false);

        int dataUpdatesSize = this.pendingDataUpdates.size();
        List<Map.Entry<POS, Optional<IBakeOutput>>> dataUpdates = new ArrayList<>(dataUpdatesSize + (dataUpdatesSize >> 3)); //pre-allocate a bit of extra space in case it grows while we're iterating
        for (Iterator<POS> itr = this.pendingDataUpdates.keySet().iterator(); itr.hasNext(); ) {
            this.pendingDataUpdates.compute(itr.next(), (pos, output) -> {
                dataUpdates.add(new AbstractMap.SimpleEntry<>(pos, output));
                return null;
            });
        }

        //this is the best we can do of resetting a semaphore to its initial permit count
        this.dataUpdatesLock.drainPermits();
        this.dataUpdatesLock.release(fp2().globalConfig().performance().maxBakesProcessedPerFrame());

        int renderableUpdatesSize = this.pendingRenderableUpdates.size();
        List<Map.Entry<POS, Boolean>> renderableUpdates = new ArrayList<>(renderableUpdatesSize + (renderableUpdatesSize >> 3)); //pre-allocate a bit of extra space in case it grows while we're iterating
        renderableUpdates.addAll(this.pendingRenderableUpdates.entrySet());
        renderableUpdates.forEach(update -> this.pendingRenderableUpdates.remove(update.getKey(), update.getValue())); //atomically remove the corresponding entries from the pending update queue

        //execute the bulk update
        this.index.update(uncheckedCast(dataUpdates), renderableUpdates);

        //release all bake outputs which are still sitting around in memory
        dataUpdates.forEach(update -> {
            if (update.getValue().isPresent()) {
                update.getValue().get().release();
            }
        });
    }
}
