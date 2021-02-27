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
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarStorage;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.ExactAsRoughGeneratorFallbackWrapper;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.util.threading.PriorityRecursiveExecutor;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.primitive.map.ObjLongMap;
import net.daporkchop.lib.primitive.map.concurrent.ObjLongConcurrentHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld<POS extends IFarPos, T extends IFarTile> implements IFarWorld<POS, T> {
    protected final WorldServer world;
    protected final IFarRenderMode<POS, T> mode;
    protected final File root;

    protected final IFarGeneratorRough<POS, T> generatorRough;
    protected final IFarGeneratorExact<POS, T> generatorExact;
    protected final IFarScaler<POS, T> scaler;

    protected final IFarStorage<POS, T> storage;

    protected final IFarPlayerTracker<POS> tracker;

    //cache for loaded tiles
    protected final Cache<POS, Compressed<POS, T>> tileCache = CacheBuilder.newBuilder()
            .concurrencyLevel(FP2Config.generationThreads)
            .weakValues()
            .build();

    //contains positions of all tiles that aren't done
    protected final Map<POS, Boolean> notDone = new ConcurrentHashMap<>(); //TODO: make this (and also exactActive) (and also the task queue) lazily overflow to disk, because the queue might get REALLY big

    //contains positions of all tiles that are going to be updated with the exact generator
    protected final ObjLongMap<POS> exactActive = new ObjLongConcurrentHashMap<>(-1L);
    protected final Collection<PriorityTask<POS>> pendingExactTasks = new ArrayList<>();

    protected final PriorityRecursiveExecutor<PriorityTask<POS>> executor; //TODO: make these global rather than per-dimension

    protected final boolean lowResolution;

    protected long currentTick = -1L;

    public AbstractFarWorld(@NonNull WorldServer world, @NonNull IFarRenderMode<POS, T> mode) {
        this.world = world;
        this.mode = mode;

        IFarGeneratorRough<POS, T> generatorRough = this.mode().roughGenerator(world);
        IFarGeneratorExact<POS, T> generatorExact = this.mode().exactGenerator(world);

        if (generatorRough == null) {
            LOGGER.warn("No rough generator exists for world {} (type: {})! Falling back to exact generator, this will have serious performance implications.", world.provider.getDimension(), world.getWorldType());
            generatorRough = new ExactAsRoughGeneratorFallbackWrapper<>(this.blockAccess(), generatorExact);
            //TODO: make the fallback generator smart! rather than simply getting the chunks from the world, do generation and population in
            // a volatile, in-memory world clone to prevent huge numbers of chunks/cubes from potentially being generated (and therefore saved)
        }

        (this.generatorRough = generatorRough).init(world);
        (this.generatorExact = generatorExact).init(world);

        this.lowResolution = FP2Config.performance.lowResolutionEnable && this.generatorRough.supportsLowResolution();

        this.scaler = this.createScaler();
        this.tracker = this.createTracker();

        this.root = new File(world.getChunkSaveLocation(), "fp2/" + this.mode().name().toLowerCase());
        this.storage = new FarStorage<>(this.root, this.mode().storageVersion());

        this.executor = new PriorityRecursiveExecutor<>(
                FP2Config.generationThreads,
                PThreadFactories.builder().daemon().minPriority()
                        .collapsingId().name(PStrings.fastFormat("FP2 DIM%d Generation Thread #%%d", world.provider.getDimension())).build(),
                new FarServerWorker<>(this));

        this.loadNotDone();

        MinecraftForge.EVENT_BUS.register(this);
    }

    protected abstract IFarScaler<POS, T> createScaler();

    protected abstract IFarPlayerTracker<POS> createTracker();

    @Override
    public Compressed<POS, T> getTileLazy(@NonNull POS pos) {
        Compressed<POS, T> tile = this.tileCache.getIfPresent(pos);
        if (tile == null || tile.timestamp() == Compressed.VALUE_BLANK) {
            if (this.notDone.putIfAbsent(pos, Boolean.TRUE) != Boolean.TRUE) {
                //tile is not in cache and was newly marked as queued
                this.executor.submit(new PriorityTask<>(TaskStage.LOAD, pos));
            }
            return null;
        }
        return tile;
    }

    public Compressed<POS, T> getTileCachedOrLoad(@NonNull POS pos) {
        try {
            return this.tileCache.get(pos, () -> {
                Compressed<POS, T> compressedTile = this.storage.load(pos);
                //create new value if absent
                return compressedTile == null ? new Compressed<>(pos) : compressedTile;
            });
        } catch (ExecutionException e) {
            PUnsafe.throwException(e);
            throw new RuntimeException(e);
        }
    }

    public void saveTile(@NonNull Compressed<POS, T> tile) {
        //this is non-blocking (unless the leveldb write buffer fills up)
        this.storage.store(tile.pos(), tile);
    }

    public void tileAvailable(@NonNull Compressed<POS, T> tile) {
        //unmark tile as being incomplete
        this.notDone.remove(tile.pos());

        this.notifyPlayerTracker(tile);
    }

    public void tileChanged(@NonNull Compressed<POS, T> tile, boolean allowScale) {
        this.tileAvailable(tile);

        //save the tile
        this.saveTile(tile);

        if (allowScale && tile.pos().level() < FP2Config.maxLevels - 1) {
            long currTimestamp = tile.timestamp();

            this.scaler.outputs(tile.pos()).forEach(outputPos -> this.scheduleTileForUpdate(outputPos, currTimestamp));
        }
    }

    @SuppressWarnings("unchecked")
    public void notifyPlayerTracker(@NonNull Compressed<POS, T> tile) {
        this.tracker.tileChanged(tile);
    }

    public boolean canGenerateRough(@NonNull POS pos) {
        return pos.level() == 0 || this.lowResolution;
    }

    protected void scheduleTileForUpdate(@NonNull POS pos, long newTimestamp) {
        //TODO: this is a race condition if an exact generation task queued in a previous tick occurs in between multiple block updates to
        // the same position during the current tick
        //TODO: wait, is this actually correct? actually, i think the above condition might actually cause it to regenerate the same tile
        // multiple times for that tick

        if (true) {
            throw new UnsupportedOperationException(); //TODO: currently, exactActive never gets cleared
        }

        if (this.exactActive.put(pos, newTimestamp) < 0L) {
            //position wasn't previously queued for update
            synchronized (this.pendingExactTasks) {
                this.pendingExactTasks.add(new PriorityTask<>(TaskStage.UPDATE, pos));
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            this.currentTick = this.world.getTotalWorldTime();
            if (!this.pendingExactTasks.isEmpty()) {
                //fire pending tasks
                synchronized (this.pendingExactTasks) {
                    this.executor.submit(this.pendingExactTasks);
                    this.pendingExactTasks.clear();
                }
            }
        }
    }

    @Override
    public void blockChanged(int x, int y, int z) {
        //TODO: re-enable this
        //this.scheduleTileForUpdate(this.fromBlockCoords(x, y, z), this.world.getTotalWorldTime());
    }

    protected abstract POS fromBlockCoords(int x, int y, int z);

    @Override
    public AsyncBlockAccess blockAccess() {
        return ((AsyncBlockAccess.Holder) this.world).asyncBlockAccess();
    }

    /*@Override
    public void save() {
        try {
            this.saveNotDone();
        } catch (IOException e) {
            LOGGER.error("Unable to save in DIM" + this.world.provider.getDimension(), e);
        }
    }*/ //TODO

    @Override
    @SneakyThrows(IOException.class)
    public void close() {
        MinecraftForge.EVENT_BUS.unregister(this);

        LOGGER.trace("Shutting down generation workers in DIM{}", this.world.provider.getDimension());
        this.executor.shutdown();
        LOGGER.trace("Shutting down storage in DIM{}", this.world.provider.getDimension());
        this.storage.close();
        this.saveNotDone();
    }

    protected void saveNotDone() throws IOException {
        /*if (FP2_DEBUG && (FP2Config.debug.disablePersistence || FP2Config.debug.disableWrite) || this.notDone.isEmpty()) {
            return;
        }

        LOGGER.trace("Saving incomplete tiles in DIM{}", this.world.provider.getDimension());

        File tmpFile = PFiles.ensureFileExists(new File(this.root, "notDone.tmp"));
        File file = new File(this.root, "notDone");

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try (DataOut out = ZSTD_DEF.get().compressionStream(DataOut.wrapNonBuffered(tmpFile))) {
            //tile load tasks
            this.notDone.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .forEach((IOConsumer<POS>) pos -> {
                        pos.writePos(buf.clear());
                        checkState(buf.isReadable(), "position %s serialized to 0 bytes!", pos);
                        out.writeVarInt(buf.readableBytes());
                        out.write(buf);
                    });
            out.writeVarInt(0);

            //exact generate tasks
            this.exactActive.forEach((pos, newTimestamp) -> {
                try {
                    pos.writePos(buf.clear());
                    checkState(buf.isReadable(), "position %s serialized to 0 bytes!", pos);
                    out.writeVarInt(buf.readableBytes());
                    out.writeVarLong(newTimestamp);
                    out.write(buf);
                } catch (IOException e) {
                    PUnsafe.throwException(e);
                    throw new RuntimeException(e);
                }
            });
            out.writeVarInt(0);
        } finally {
            buf.release();
        }

        Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);*/
    }

    protected void loadNotDone() {
        /*Collection<GetTileTask<POS, P, D>> loadTasks = new ArrayList<>();
        Collection<ExactUpdateTileTask<POS, P, D>> exactGenerateTasks = new ArrayList<>();

        try {
            File file = new File(this.root, "notDone");
            if (FP2_DEBUG && (FP2Config.debug.disablePersistence || FP2Config.debug.disableRead) || !PFiles.checkFileExists(file)) {
                return; //don't bother attempting to load
            }

            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
            try (DataIn in = ZSTD_INF.get().decompressionStream(DataIn.wrapNonBuffered(file))) {
                //tile load tasks
                for (int size; (size = in.readVarInt()) > 0; ) {
                    in.readFully(buf.clear().ensureWritable(size), size);

                    POS pos = uncheckedCast(this.mode.readPos(buf));
                    if (this.notDone(pos, true) && pos.level() < FP2Config.maxLevels) {
                        loadTasks.add(new GetTileTask<>(this, new TaskKey(TaskStage.LOAD, pos.level()), pos, TaskStage.LOAD));
                    }
                }

                //exact update tasks
                for (int size; (size = in.readVarInt()) > 0; ) {
                    in.readFully(buf.clear().ensureWritable(size), size);
                    long newTimestamp = in.readVarLong();

                    POS pos = uncheckedCast(this.mode.readPos(buf));
                    if (this.exactActive.put(pos, newTimestamp) < 0L && pos.level() < FP2Config.maxLevels) {
                        exactGenerateTasks.add(new ExactUpdateTileTask<>(this, new TaskKey(TaskStage.EXACT, pos.level()), pos, TaskStage.EXACT));
                    }
                }
            } finally {
                buf.release();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to load incomplete tasks!", e);
        } finally {
            LOGGER.info("Loaded {} persisted tile load tasks and {} persisted exact generate tasks", loadTasks.size(), exactGenerateTasks.size());
            this.executor.submit(PorkUtil.<Collection<LazyTask<TaskKey, ?, ?>>>uncheckedCast(loadTasks));
            this.executor.submit(PorkUtil.<Collection<LazyTask<TaskKey, ?, ?>>>uncheckedCast(exactGenerateTasks));
        }*/
    }
}
