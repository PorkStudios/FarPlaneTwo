/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TileCoordLimits;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.engine.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.engine.client.index.IRenderIndex;
import net.daporkchop.fp2.core.engine.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.util.threading.BlockingSupport;
import net.daporkchop.fp2.core.util.threading.scheduler.NoFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class BakeManager extends AbstractReleasable implements FarTileCache.Listener, Consumer<TilePos>, Runnable {
    private final AbstractFarRenderer renderer;
    private final IFarRenderStrategy<?, ?, ?> strategy;

    private final FarTileCache tileCache;

    private final IRenderIndex<?, ?, ?> index;
    private final IRenderBaker<?> baker;

    private final Scheduler<TilePos, Void> bakeScheduler;
    private final TileCoordLimits coordLimits;

    private final Map<TilePos, Optional<IBakeOutput>> pendingDataUpdates = DirectTilePosAccess.newPositionKeyedConcurrentHashMap();
    private final Map<TilePos, Boolean> pendingRenderableUpdates = DirectTilePosAccess.newPositionKeyedConcurrentHashMap();
    private final AtomicBoolean isBulkUpdateQueued = new AtomicBoolean();
    private final Semaphore dataUpdatesLock = new Semaphore(fp2().globalConfig().performance().maxBakesProcessedPerFrame());

    public BakeManager(@NonNull AbstractFarRenderer renderer, @NonNull FarTileCache tileCache) {
        this.renderer = renderer;
        this.strategy = renderer.strategy();
        this.tileCache = tileCache;

        this.index = this.strategy.createIndex();
        this.baker = this.strategy.createBaker();
        this.coordLimits = new TileCoordLimits(renderer.context().level().coordLimits());

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
        for (Iterator<TilePos> itr = this.pendingDataUpdates.keySet().iterator(); itr.hasNext(); ) {
            this.pendingDataUpdates.computeIfPresent(itr.next(), (pos, output) -> {
                output.ifPresent(IBakeOutput::release);
                return null;
            });
        }
    }

    @Override
    public void tileAdded(@NonNull ITileSnapshot tile) {
        this.notifyOutputs(tile.pos());
    }

    @Override
    public void tileModified(@NonNull ITileSnapshot tile) {
        this.notifyOutputs(tile.pos());
    }

    @Override
    public void tileRemoved(@NonNull TilePos pos) {
        //schedule the tile itself for re-bake
        this.notifyOutputs(pos);
    }

    private void notifyOutputs(@NonNull TilePos pos) {
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
    public void accept(@NonNull TilePos pos) { //this function is called from inside of bakeScheduler, which doesn't execute the task multiple times on the same position
        this.checkSelfRenderable(pos);
        this.checkParentsRenderable(pos);

        //get the list of input tile positions
        List<TilePos> inputTilePositions = this.baker.bakeInputs(pos);

        //double-check that the original (provoking) tile position is one of the inputs
        int provokingTilePositionInputIndex = inputTilePositions.indexOf(pos);
        checkState(provokingTilePositionInputIndex >= 0, "bake inputs for %s don't include the tile itself: %s", pos, inputTilePositions);

        //get the input tile data from the tile cache
        ITileSnapshot[] compressedInputTiles = this.tileCache.getTilesCached(inputTilePositions)
                .toArray(new ITileSnapshot[inputTilePositions.size()]);

        //fast path: if the provoking tile was unloaded or is empty, we can skip baking it entirely
        if (compressedInputTiles[provokingTilePositionInputIndex] == null //tile isn't cached any more
            || compressedInputTiles[provokingTilePositionInputIndex].isEmpty()) { //tile data is empty
            this.updateData(pos, Optional.empty());
            return;
        }

        Recycler<Tile> recycler = Tile.recycler();
        Tile[] srcs = new Tile[compressedInputTiles.length];
        try {
            for (int i = 0; i < srcs.length; i++) { //inflate tiles
                if (compressedInputTiles[i] != null) {
                    srcs[i] = compressedInputTiles[i].loadTile(recycler, Tile.CODEC);
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
            for (Tile src : srcs) {
                if (src != null) {
                    recycler.release(src);
                }
            }
        }

        this.tileCache.tryCompressExistingTile(pos);
    }

    protected void checkParentsRenderable(@NonNull TilePos posIn) {
        PorkUtil.<Stream<TilePos>>uncheckedCast(posIn.up().allPositionsInBB(1, 1)).forEach(this::checkSelfRenderable);
    }

    protected void checkSelfRenderable(@NonNull TilePos pos) {
        if (this.coordLimits.contains(pos)) {
            this.updateRenderable(pos,
                    this.tileCache.getTileCached(pos) != null
                    && (pos.level() == 0 || PorkUtil.<Stream<TilePos>>uncheckedCast(pos.down().allPositionsInBB(1, 3))
                            .anyMatch(p -> this.coordLimits.contains(p) && this.tileCache.getTileCached(p) == null)));
        }
    }

    protected void updateData(@NonNull TilePos pos, @NonNull Optional<IBakeOutput> optionalBakeOutput) throws InterruptedException {
        BlockingSupport.managedAcquire(this.dataUpdatesLock, 1);

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

    protected void updateRenderable(@NonNull TilePos pos, boolean renderable) {
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

        //pre-allocate a bit of extra space in case it grows while we're iterating
        List<Map.Entry<TilePos, Optional<IBakeOutput>>> dataUpdates = new ArrayList<>(dataUpdatesSize + (dataUpdatesSize >> 3));
        for (Iterator<TilePos> itr = this.pendingDataUpdates.keySet().iterator(); itr.hasNext(); ) {
            this.pendingDataUpdates.compute(itr.next(), (pos, output) -> {
                dataUpdates.add(new AbstractMap.SimpleEntry<>(pos, output));
                return null;
            });
        }

        //this is the best we can do of resetting a semaphore to its initial permit count
        this.dataUpdatesLock.drainPermits();
        this.dataUpdatesLock.release(fp2().globalConfig().performance().maxBakesProcessedPerFrame());

        int renderableUpdatesSize = this.pendingRenderableUpdates.size();

        //pre-allocate a bit of extra space in case it grows while we're iterating
        List<Map.Entry<TilePos, Boolean>> renderableUpdates = new ArrayList<>(renderableUpdatesSize + (renderableUpdatesSize >> 3));
        renderableUpdates.addAll(this.pendingRenderableUpdates.entrySet());

        //atomically remove the corresponding entries from the pending update queue
        renderableUpdates.forEach(update -> this.pendingRenderableUpdates.remove(update.getKey(), update.getValue()));

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
