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
import lombok.val;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TileCoordLimits;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.BakeOutput;
import net.daporkchop.fp2.core.engine.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.util.listener.ListenerList;
import net.daporkchop.fp2.core.util.threading.BlockingSupport;
import net.daporkchop.fp2.core.util.threading.scheduler.NoFutureScheduler;
import net.daporkchop.fp2.core.util.threading.scheduler.Scheduler;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.common.pool.recycler.Recycler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class BakeManager<VertexType extends AttributeStruct> extends AbstractReleasable implements FarTileCache.Listener, Consumer<TilePos> {
    private final AbstractFarRenderer<VertexType> renderer;

    private final FarTileCache tileCache;
    private final IRenderBaker<VertexType> baker;

    private final Scheduler<TilePos, Void> bakeScheduler;
    private final TileCoordLimits coordLimits;

    private final ListenerList<FarTileCache.Listener>.Handle tileCacheListenerHandle;

    private final Map<TilePos, Optional<BakeOutput<VertexType>>> pendingDataUpdates = DirectTilePosAccess.newPositionKeyedConcurrentHashMap();
    private final Map<TilePos, Boolean> pendingRenderableUpdates = DirectTilePosAccess.newPositionKeyedConcurrentHashMap();
    private final Semaphore dataUpdatesLock = new Semaphore(fp2().globalConfig().performance().maxBakesProcessedPerFrame());

    public BakeManager(@NonNull AbstractFarRenderer<VertexType> renderer, @NonNull FarTileCache tileCache, @NonNull IRenderBaker<VertexType> baker) {
        this.renderer = renderer;

        this.tileCache = tileCache;
        this.baker = baker;

        this.coordLimits = new TileCoordLimits(renderer.context().level().coordLimits());

        this.bakeScheduler = new NoFutureScheduler<>(this, this.renderer.context().level().workerManager().createChildWorkerGroup()
                .threads(fp2().globalConfig().performance().bakeThreads())
                .threadFactory(PThreadFactories.builder().daemon().minPriority().collapsingId().name("FP2 Rendering Thread #%d").build()));

        this.tileCacheListenerHandle = this.tileCache.addListener(this);
        this.tilesChanged(this.tileCache.getAllPositions());
    }

    @Override
    protected void doRelease() {
        //TODO: use try-with-resources here?
        this.tileCacheListenerHandle.close();

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
    public void tileChanged(@NonNull TilePos pos) {
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

    private void updateData(@NonNull TilePos pos, @NonNull Optional<BakeOutput<VertexType>> optionalBakeOutput) throws InterruptedException {
        BlockingSupport.managedAcquire(this.dataUpdatesLock, 1);

        this.pendingDataUpdates.merge(pos, optionalBakeOutput, (oldOutput, newOutput) -> {
            //release old bake output to avoid potential memory leak when silently replacing entries
            oldOutput.ifPresent(BakeOutput::close);
            return newOutput;
        });
    }

    private void updateRenderable(@NonNull TilePos pos, boolean renderable) {
        this.pendingRenderableUpdates.put(pos, renderable);
    }

    /**
     * Processes tile update events from completed tile bake operations, forwarding them to the bake storage and render index.
     *
     * @param bufferUploader the {@link BufferUploader} to use for uploading bake output data
     * @param bakeStorage    the {@link BakeStorage} to store uploaded tile data in
     * @param renderIndex    the {@link RenderIndex} to notify of changed tiles
     */
    public void tick(@NonNull BufferUploader bufferUploader, @NonNull BakeStorage<VertexType> bakeStorage, @NonNull RenderIndex<VertexType> renderIndex) {
        final int dataUpdatesCount = this.pendingDataUpdates.size();
        if (dataUpdatesCount != 0) {
            final long availableBufferUploaderCapacity = bufferUploader.estimateCapacity();
            final long maximumBufferUploaderCapacity = bufferUploader.maximumCapacity();

            //pre-allocate a bit of extra space in case it grows while we're iterating
            Map<TilePos, BakeOutput<VertexType>> dataUpdates = DirectTilePosAccess.newPositionKeyedHashMap(dataUpdatesCount + (dataUpdatesCount >> 3));

            //the total size (in bytes) of the data which is queued to be sent to the GPU
            long dataUploadSize = 0L;
            try {
                UPDATES_LOOP:
                for (TilePos pos : this.pendingDataUpdates.keySet()) {
                    Optional<BakeOutput<VertexType>> optionalValue;
                    do {
                        optionalValue = this.pendingDataUpdates.get(pos);
                        if (optionalValue.isPresent()) {
                            long dataSize = optionalValue.get().sizeBytes();

                            if (dataSize > maximumBufferUploaderCapacity) {
                                //this tile will always exceed the buffer uploader's non-blocking capacity, so we may as well upload it as soon as possible
                                //  as the GPU will end up getting stalled no matter what.
                                //that said, this should never actually occur, and if it ever does it's most likely a bug.
                            } else if (dataSize > availableBufferUploaderCapacity - dataUploadSize) {
                                //avoid uploading more data than the buffer uploader has available space to avoid stalling the GPU pipeline. we'll simply
                                //  skip all remaining data updates and defer them until the buffer uploader has more space available (which will hopefully
                                //  occur within the next few frames)
                                break UPDATES_LOOP;
                            }
                        }
                    } while (!this.pendingDataUpdates.remove(pos, optionalValue));

                    dataUploadSize += optionalValue.map(BakeOutput::sizeBytes).orElse(0L);
                    dataUpdates.put(pos, optionalValue.orElse(null));
                }

                //execute the bulk tile data update
                bakeStorage.update(dataUpdates);
                renderIndex.notifyTilesChanged(dataUpdates.keySet());
                bufferUploader.flush();
            } finally {
                //release all bake outputs (which are still sitting around in memory, and we now own the handles to)
                PResourceUtil.closeAll(dataUpdates.values());
            }

            //this is the best we can do of resetting a semaphore to its initial permit count
            this.dataUpdatesLock.drainPermits();
            this.dataUpdatesLock.release(Math.max(fp2().globalConfig().performance().maxBakesProcessedPerFrame() - this.pendingDataUpdates.size(), 0));
        }

        if (!this.pendingRenderableUpdates.isEmpty()) {
            //iterate over the renderable updates, sort them into separate sets and notify the render index
            val hidden = DirectTilePosAccess.newPositionHashSet();
            val shown = DirectTilePosAccess.newPositionHashSet();
            for (TilePos pos : this.pendingRenderableUpdates.keySet()) {
                (this.pendingRenderableUpdates.remove(pos) ? shown : hidden).add(pos);
            }
            renderIndex.updateHidden(hidden, shown);
        }
    }
}
