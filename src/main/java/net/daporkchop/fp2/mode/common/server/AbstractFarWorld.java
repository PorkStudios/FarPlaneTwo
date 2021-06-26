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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.mode.common.server.storage.rocksdb.RocksStorage;
import net.daporkchop.fp2.server.worldlistener.IWorldChangeListener;
import net.daporkchop.fp2.server.worldlistener.WorldChangeListenerManager;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.asyncblockaccess.IAsyncBlockAccess;
import net.daporkchop.fp2.util.threading.keyed.KeyedDistinctScheduler;
import net.daporkchop.fp2.util.threading.keyed.KeyedExecutor;
import net.daporkchop.fp2.util.threading.keyed.PriorityKeyedExecutor;
import net.daporkchop.fp2.util.threading.keyed.KeyedReferencingScheduler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld<POS extends IFarPos, T extends IFarTile> implements IFarWorld<POS, T>, IWorldChangeListener {
    protected final WorldServer world;
    protected final IFarRenderMode<POS, T> mode;
    protected final File root;

    protected final IFarGeneratorRough<POS, T> generatorRough;
    protected final IFarGeneratorExact<POS, T> generatorExact;
    protected final IFarScaler<POS, T> scaler;

    protected final IFarStorage<POS, T> storage;

    protected final IFarPlayerTracker<POS> tracker;

    protected final Cache<POS, Compressed<POS, T>> tileCache = CacheBuilder.newBuilder() //cache for loaded tiles
            .concurrencyLevel(FP2Config.generationThreads)
            .weakValues()
            .build();

    protected final KeyedExecutor<POS> executor; //TODO: make these global rather than per-dimension
    protected final KeyedReferencingScheduler<POS, Compressed<POS, T>> loader;
    protected final KeyedDistinctScheduler<POS> updater;

    protected final boolean lowResolution;

    protected long currentTime = -1L;

    public AbstractFarWorld(@NonNull WorldServer world, @NonNull IFarRenderMode<POS, T> mode) {
        this.world = world;
        this.mode = mode;

        this.generatorRough = this.mode().roughGenerator(world);
        this.generatorExact = this.mode().exactGenerator(world);

        if (this.generatorRough == null) {
            FP2_LOG.warn("vo rough {} generator exists for world {} (type={}, generator={})! Falling back to exact generator, this will have serious performance implications.", mode.name(), world.provider.getDimension(), world.getWorldType(), Constants.getTerrainGenerator(world));
            //TODO: make the fallback generator smart! rather than simply getting the chunks from the world, do generation and population in
            // a volatile, in-memory world clone to prevent huge numbers of chunks/cubes from potentially being generated (and therefore saved)
        }

        this.lowResolution = FP2Config.performance.lowResolutionEnable && this.generatorRough != null && this.generatorRough.supportsLowResolution();

        this.scaler = this.createScaler();
        this.tracker = this.createTracker();

        this.root = new File(world.getChunkSaveLocation(), "fp2/" + this.mode().name().toLowerCase());
        this.storage = new RocksStorage<>(mode, this.root);

        FarServerWorker<POS, T> worker = new FarServerWorker<>(this);

        this.executor = new PriorityKeyedExecutor<>(
                FP2Config.generationThreads,
                PThreadFactories.builder().daemon().minPriority()
                        .collapsingId().name(PStrings.fastFormat("FP2 DIM%d Worker #%%d", world.provider.getDimension())).build());
        this.loader = new KeyedReferencingScheduler<>(this.executor, worker::roughGetTile);
        this.updater = new KeyedDistinctScheduler<>(this.executor, worker::updateTile);

        //add all dirty tiles to update queue
        this.storage.dirtyTracker().forEachDirtyPos((pos, timestamp) -> this.enqueueUpdate(pos));

        WorldChangeListenerManager.add(this.world, this);
    }

    protected abstract IFarScaler<POS, T> createScaler();

    protected abstract IFarPlayerTracker<POS> createTracker();

    protected abstract boolean anyVanillaTerrainExistsAt(@NonNull POS pos);

    @Override
    public CompletableFuture<Compressed<POS, T>> retainTileFuture(@NonNull POS pos) {
        return this.loader.retain(pos);
    }

    @Override
    public void releaseTileFuture(@NonNull POS pos) {
        this.loader.release(pos);
    }

    public Compressed<POS, T> loadTile(@NonNull POS pos) {
        Compressed<POS, T> compressedTile = this.storage.load(pos);
        //create new value if absent
        return compressedTile == null ? new Compressed<>(pos) : compressedTile;
    }

    public void saveTile(@NonNull Compressed<POS, T> tile) {
        //this is non-blocking (unless the rocksdb write-ahead log fills up)
        this.storage.store(tile.pos(), tile);
    }

    public void tileAvailable(@NonNull Compressed<POS, T> tile) {
        this.notifyPlayerTracker(tile);
    }

    public void tileChanged(@NonNull Compressed<POS, T> tile, boolean allowScale) {
        this.tileAvailable(tile);

        //save the tile
        this.saveTile(tile);

        if (allowScale && tile.pos().level() < FP2Config.maxLevels - 1) {
            this.scheduleForUpdate(this.scaler.outputs(tile.pos()));
        }
    }

    public void notifyPlayerTracker(@NonNull Compressed<POS, T> tile) {
        this.tracker.tileChanged(tile);
    }

    public boolean canGenerateRough(@NonNull POS pos) {
        return this.generatorRough != null && (pos.level() == 0 || this.lowResolution);
    }

    protected void enqueueUpdate(@NonNull POS pos) {
        this.updater.submit(pos);
    }

    protected void scheduleForUpdate(@NonNull Stream<POS> positions) {
        //if (this.exactActive.put(pos, newTimestamp) < 0L) {

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

        this.updater.release();
        this.loader.release();
        this.executor.release();

        FP2_LOG.trace("Shutting down storage in DIM{}", this.world.provider.getDimension());
        this.storage.close();
    }
}
