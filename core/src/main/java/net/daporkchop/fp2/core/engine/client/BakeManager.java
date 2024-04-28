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

import lombok.NonNull;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TileCoordLimits;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.BakeOutput;
import net.daporkchop.fp2.core.engine.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.util.threading.scheduler.NoFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.common.pool.recycler.Recycler;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public final class BakeManager<VertexType extends AttributeStruct> extends AbstractReleasable implements FarTileCache.Listener, Consumer<TilePos> {
    private final AbstractFarRenderer<VertexType> renderer;

    private final FarTileCache tileCache;
    private final BufferUploader bufferUploader;
    private final IRenderBaker<VertexType> baker;
    private final BakeStorage<VertexType> bakeStorage;
    private final RenderIndex<VertexType> renderIndex;

    private final Scheduler<TilePos, Void> bakeScheduler;
    private final TileCoordLimits coordLimits;

    private final Map<TilePos, Optional<BakeOutput<VertexType>>> pendingDataUpdates = DirectTilePosAccess.newPositionKeyedConcurrentHashMap();
    private final Map<TilePos, Boolean> pendingRenderableUpdates = DirectTilePosAccess.newPositionKeyedConcurrentHashMap();
    private final AtomicBoolean isBulkUpdateQueued = new AtomicBoolean();
    private final Semaphore dataUpdatesLock = new Semaphore(fp2().globalConfig().performance().maxBakesProcessedPerFrame());

    public BakeManager(@NonNull AbstractFarRenderer<VertexType> renderer, @NonNull FarTileCache tileCache, @NonNull BufferUploader bufferUploader, @NonNull IRenderBaker<VertexType> baker, @NonNull BakeStorage<VertexType> bakeStorage, @NonNull RenderIndex<VertexType> renderIndex) {
        this.renderer = renderer;

        this.tileCache = tileCache;
        this.bufferUploader = bufferUploader;
        this.baker = baker;
        this.bakeStorage = bakeStorage;
        this.renderIndex = renderIndex;

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
                output.ifPresent(BakeOutput::close);
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

        ITileSnapshot[] compressedInputTiles = this.tileCache.getTilesCached(this.baker.bakeInputs(pos)).toArray(ITileSnapshot[]::new);
        if (compressedInputTiles[0] == null //tile isn't cached any more
                || compressedInputTiles[0].isEmpty()) { //tile data is empty
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

            BakeOutput<VertexType> output = new BakeOutput<>(this.renderer.vertexFormat, this.renderer.indexFormat, this.renderer.alloc);
            try {
                this.baker.bake(pos, srcs, output);

                Optional<BakeOutput<VertexType>> optionalOutput;
                if (output.isEmpty()) {
                    output.close();
                    output = null;
                    optionalOutput = Optional.empty();
                } else {
                    optionalOutput = Optional.of(output);
                }

                this.updateData(pos, optionalOutput);
            } catch (Throwable t) {
                if (output != null) {
                    output.close();
                }
                throw t;
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

    private void checkParentsRenderable(@NonNull TilePos posIn) {
        posIn.up().allPositionsInBB(1, 1).forEach(this::checkSelfRenderable);
    }

    private void checkSelfRenderable(@NonNull TilePos pos) {
        if (this.coordLimits.contains(pos)) {
            //TODO: determining if a tile is renderable is currently pretty inefficient, maybe we can do it better (e.g. using Int3HashSet#countInRange() or a reference-counting index)
            //this.updateRenderable(pos,
            //        this.tileCache.getTileCached(pos) == null
            //                && (pos.level() == 0 || pos.down().allPositionsInBB(1, 3).anyMatch(p -> this.coordLimits.contains(p) && this.tileCache.getTileCached(p) == null)));

            //condition changed now that tiles default to being shown rather than hidden
            this.updateRenderable(pos,
                    pos.level() == 0 //level-0 tiles are always shown
                            || this.tileCache.getTileCached(pos) == null //if the tile isn't loaded, mark it as shown to prune unused entries from the render index's hidden set
                            || pos.down().allPositionsInBB(1, 3).anyMatch(p -> this.coordLimits.contains(p) && this.tileCache.getTileCached(p) == null));
        }
    }

    private void updateData(@NonNull TilePos pos, @NonNull Optional<BakeOutput<VertexType>> optionalBakeOutput) {
        this.dataUpdatesLock.acquireUninterruptibly();

        this.pendingDataUpdates.merge(pos, optionalBakeOutput, (oldOutput, newOutput) -> {
            //release old bake output to avoid potential memory leak when silently replacing entries
            oldOutput.ifPresent(BakeOutput::close);
            return newOutput;
        });

        this.enqueueBulkUpdate();
    }

    private void updateRenderable(@NonNull TilePos pos, boolean renderable) {
        this.pendingRenderableUpdates.put(pos, renderable);

        this.enqueueBulkUpdate();
    }

    private void enqueueBulkUpdate() {
        if (this.isBulkUpdateQueued.compareAndSet(false, true)) { //we won the race to enqueue a bulk update!
            this.renderer.context().level().workerManager().workExecutor().run(this::doBulkUpdate);
        }
    }

    private void doBulkUpdate() { //executes a bulk update on the client thread
        this.isBulkUpdateQueued.set(false);

        int dataUpdatesSize = this.pendingDataUpdates.size();

        //pre-allocate a bit of extra space in case it grows while we're iterating
        Map<TilePos, BakeOutput<VertexType>> dataUpdates = DirectTilePosAccess.newPositionKeyedHashMap(dataUpdatesSize + (dataUpdatesSize >> 3));
        try {
            for (TilePos pos : this.pendingDataUpdates.keySet()) {
                dataUpdates.put(pos, this.pendingDataUpdates.remove(pos).orElse(null));
            }

            //execute the bulk tile data update
            this.bakeStorage.update(dataUpdates);
            this.renderIndex.notifyTilesChanged(dataUpdates.keySet());
            this.bufferUploader.flush();
        } finally {
            dataUpdates.forEach((pos, output) -> { //release all bake outputs (which are still sitting around in memory)
                if (output != null) {
                    output.close();
                }
            });
        }

        //this is the best we can do of resetting a semaphore to its initial permit count
        this.dataUpdatesLock.drainPermits();
        this.dataUpdatesLock.release(fp2().globalConfig().performance().maxBakesProcessedPerFrame());

        //iterate over the renderable updates, sort them into separate sets and notify the render index
        Set<TilePos> hidden = DirectTilePosAccess.newPositionHashSet();
        Set<TilePos> shown = DirectTilePosAccess.newPositionHashSet();
        for (TilePos pos : this.pendingRenderableUpdates.keySet()) {
            (this.pendingRenderableUpdates.remove(pos) ? shown : hidden).add(pos);
        }
        this.renderIndex.updateHidden(hidden, shown);
    }
}
