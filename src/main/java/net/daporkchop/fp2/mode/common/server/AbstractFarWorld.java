/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarContext;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.server.IFarStorage;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.ExactAsRoughGeneratorFallbackWrapper;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.util.threading.PriorityRecursiveExecutor;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.fp2.util.threading.executor.LazyTask;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.function.io.IOConsumer;
import net.daporkchop.lib.common.misc.file.PFiles;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld<POS extends IFarPos, P extends IFarPiece> implements IFarWorld<POS, P> {
    protected static final BiFunction<Boolean, Boolean, Boolean> BOOLEAN_OR = (a, b) -> a | b;

    protected final WorldServer world;
    protected final RenderMode mode;
    protected final File root;

    protected final IFarGeneratorRough<POS, P> generatorRough;
    protected final IFarGeneratorExact<POS, P> generatorExact;
    protected final IFarScaler<POS, P> scaler;

    protected final IFarStorage<POS, P> storage;

    //cache for loaded tiles
    protected final Cache<POS, Compressed<POS, P>> pieceCache = CacheBuilder.newBuilder()
            .concurrencyLevel(FP2Config.generationThreads)
            .weakValues()
            .build();

    //contains positions of all tiles that aren't done
    protected final Map<POS, Boolean> notDone = new ConcurrentHashMap<>(); //TODO: make this (and also exactActive) (and also the task queue) lazily overflow to disk, because the queue might get REALLY big

    //contains positions of all tiles that are going to be updated with the exact generator
    protected final ObjLongMap<POS> exactActive = new ObjLongConcurrentHashMap<>(-1L);
    protected final Queue<LazyTask<TaskKey, ?, ?>> pendingExactTasks = new ArrayDeque<>();

    protected final PriorityRecursiveExecutor<PriorityTask<POS>> executor; //TODO: make these global rather than per-dimension

    protected final boolean lowResolution;

    public AbstractFarWorld(@NonNull WorldServer world, @NonNull RenderMode mode) {
        this.world = world;
        this.mode = mode;

        IFarGeneratorRough<POS, P> generatorRough = this.mode().<POS, P>generatorsRough().stream()
                .map(f -> f.apply(world))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        IFarGeneratorExact<POS, P> generatorExact = this.mode().<POS, P>generatorsExact().stream()
                .map(f -> f.apply(world))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new IllegalStateException(PStrings.fastFormat(
                        "No exact generator could be found for world %d (type: %s), mode:%s",
                        world.provider.getDimension(),
                        world.getWorldType(),
                        this.mode()
                )));

        if (generatorRough == null) {
            LOGGER.warn("No rough generator exists for world {} (type: {})! Falling back to exact generator, this will have serious performance implications.", world.provider.getDimension(), world.getWorldType());
            generatorRough = new ExactAsRoughGeneratorFallbackWrapper<>(this.blockAccess(), generatorExact);
            //TODO: make the fallback generator smart! rather than simply getting the chunks from the world, do generation and population in
            // a volatile, in-memory world clone to prevent huge numbers of chunks/cubes from potentially being generated (and therefore saved)
        }

        (this.generatorRough = generatorRough).init(world);
        (this.generatorExact = generatorExact).init(world);

        this.lowResolution = FP2Config.performance.lowResolutionEnable && this.generatorRough.supportsLowResolution();

        this.scaler = this.mode().createScaler();
        this.root = new File(world.getChunkSaveLocation(), "fp2/" + this.mode().name().toLowerCase());
        this.storage = new FarStorage<>(this.root, this.mode().pieceVersion());

        this.executor = new PriorityRecursiveExecutor<>(
                FP2Config.generationThreads,
                PThreadFactories.builder().daemon().minPriority()
                        .collapsingId().name(PStrings.fastFormat("FP2 DIM%d Generation Thread #%%d", world.provider.getDimension())).build(),
                new FarServerWorker<>(this));

        //this.loadNotDone();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public Compressed<POS, P> getPieceLazy(@NonNull POS pos) {
        Compressed<POS, P> piece = this.pieceCache.getIfPresent(pos);
        if (piece == null || piece.timestamp() == Compressed.VALUE_BLANK) {
            if (this.notDone(pos, true)) {
                //piece is not in cache and was newly marked as queued
                this.executor.submit(new PriorityTask<>(TaskStage.LOAD, pos));
            }
            return null;
        }
        return piece;
    }

    public Compressed<POS, P> getPieceCachedOrLoad(@NonNull POS pos) {
        try {
            return this.pieceCache.get(pos, () -> {
                Compressed<POS, P> compressedPiece = this.storage.load(pos);
                //create new value if absent
                return compressedPiece == null ? new Compressed<>(pos) : compressedPiece;
            });
        } catch (ExecutionException e) {
            PUnsafe.throwException(e);
            throw new RuntimeException(e);
        }
    }

    public void savePiece(@NonNull Compressed<POS, P> piece) {
        //this is non-blocking (unless the leveldb write buffer fills up)
        this.storage.store(piece.pos(), piece);
    }

    public void pieceAvailable(@NonNull Compressed<POS, P> piece) {
        if (!piece.isGenerated()) {
            return;
        }

        //unmark piece as being incomplete
        this.notDone.remove(piece.pos());

        this.notifyPlayerTracker(piece);
    }

    public void pieceChanged(@NonNull Compressed<POS, P> piece) {
        if (!piece.isGenerated()) {
            return;
        }

        this.notifyPlayerTracker(piece);

        if (FP2Config.performance.savePartial || piece.isGenerated()) {
            //save the piece
            this.savePiece(piece);
        }

        if (piece.timestamp() >= 0L && piece.pos().level() < FP2Config.maxLevels - 1) {
            //the piece has been generated with the exact generator, schedule all output pieces for scaling
            long currTimestamp = piece.timestamp();

            this.scaler.outputs(piece.pos()).forEach(outputPos -> this.schedulePieceForUpdate(outputPos, currTimestamp));
        }
    }

    @SuppressWarnings("unchecked")
    public void notifyPlayerTracker(@NonNull Compressed<POS, P> piece) {
        ((IFarContext) this.world).tracker().pieceChanged(piece);
    }

    public boolean canGenerateRough(@NonNull POS pos) {
        return pos.level() == 0 || this.lowResolution;
    }

    protected void schedulePieceForUpdate(@NonNull POS pos, long newTimestamp) {
        //TODO: this is a race condition if an exact generation task queued in a previous tick occurs in between multiple block updates to
        // the same position during the current tick
        //TODO: wait, is this actually correct? actually, i think the above condition might actually cause it to regenerate the same piece
        // multiple times for that tick
        if (this.exactActive.put(pos, newTimestamp) < 0L) {
            //position wasn't previously queued for update
            synchronized (this.pendingExactTasks) {
                //this.pendingExactTasks.add(new ExactUpdatePieceTask<>(this, new TaskKey(TaskStage.EXACT, pos.level()), pos, TaskStage.EXACT));
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !this.pendingExactTasks.isEmpty()) {
            //fire pending tasks
            synchronized (this.pendingExactTasks) {
                //this.executor.submit(this.pendingExactTasks);
                this.pendingExactTasks.clear();
            }
        }
    }

    public boolean notDone(@NonNull POS pos, boolean persist) {
        if (persist) {
            return this.notDone.put(pos, Boolean.TRUE) != Boolean.TRUE;
        } else {
            this.notDone.merge(pos, Boolean.FALSE, BOOLEAN_OR);
            return false; //whatever
        }
    }

    @Override
    public void blockChanged(int x, int y, int z) {
        this.schedulePieceForUpdate(this.fromBlockCoords(x, y, z), this.world.getTotalWorldTime());
    }

    protected abstract POS fromBlockCoords(int x, int y, int z);

    @Override
    public AsyncBlockAccess blockAccess() {
        return ((AsyncBlockAccess.Holder) this.world).asyncBlockAccess();
    }

    @Override
    public void save() {
        try {
            this.saveNotDone();
        } catch (IOException e) {
            LOGGER.error("Unable to save in DIM" + this.world.provider.getDimension(), e);
        }
    }

    @Override
    public void close() throws IOException {
        MinecraftForge.EVENT_BUS.unregister(this);

        LOGGER.trace("Shutting down generation workers in DIM{}", this.world.provider.getDimension());
        this.executor.shutdown();
        LOGGER.trace("Shutting down storage in DIM{}", this.world.provider.getDimension());
        this.storage.close();
        this.saveNotDone();
    }

    protected void saveNotDone() throws IOException {
        if (FP2_DEBUG && (FP2Config.debug.disablePersistence || FP2Config.debug.disableWrite) || this.notDone.isEmpty()) {
            return;
        }

        LOGGER.trace("Saving incomplete pieces in DIM{}", this.world.provider.getDimension());

        File tmpFile = PFiles.ensureFileExists(new File(this.root, "notDone.tmp"));
        File file = new File(this.root, "notDone");

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try (DataOut out = ZSTD_DEF.get().compressionStream(DataOut.wrapNonBuffered(tmpFile))) {
            //piece load tasks
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

        Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    protected void loadNotDone() {
        /*Collection<GetPieceTask<POS, P, D>> loadTasks = new ArrayList<>();
        Collection<ExactUpdatePieceTask<POS, P, D>> exactGenerateTasks = new ArrayList<>();

        try {
            File file = new File(this.root, "notDone");
            if (FP2_DEBUG && (FP2Config.debug.disablePersistence || FP2Config.debug.disableRead) || !PFiles.checkFileExists(file)) {
                return; //don't bother attempting to load
            }

            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
            try (DataIn in = ZSTD_INF.get().decompressionStream(DataIn.wrapNonBuffered(file))) {
                //piece load tasks
                for (int size; (size = in.readVarInt()) > 0; ) {
                    in.readFully(buf.clear().ensureWritable(size), size);

                    POS pos = uncheckedCast(this.mode.readPos(buf));
                    if (this.notDone(pos, true) && pos.level() < FP2Config.maxLevels) {
                        loadTasks.add(new GetPieceTask<>(this, new TaskKey(TaskStage.LOAD, pos.level()), pos, TaskStage.LOAD));
                    }
                }

                //exact update tasks
                for (int size; (size = in.readVarInt()) > 0; ) {
                    in.readFully(buf.clear().ensureWritable(size), size);
                    long newTimestamp = in.readVarLong();

                    POS pos = uncheckedCast(this.mode.readPos(buf));
                    if (this.exactActive.put(pos, newTimestamp) < 0L && pos.level() < FP2Config.maxLevels) {
                        exactGenerateTasks.add(new ExactUpdatePieceTask<>(this, new TaskKey(TaskStage.EXACT, pos.level()), pos, TaskStage.EXACT));
                    }
                }
            } finally {
                buf.release();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to load incomplete tasks!", e);
        } finally {
            LOGGER.info("Loaded {} persisted piece load tasks and {} persisted exact generate tasks", loadTasks.size(), exactGenerateTasks.size());
            this.executor.submit(PorkUtil.<Collection<LazyTask<TaskKey, ?, ?>>>uncheckedCast(loadTasks));
            this.executor.submit(PorkUtil.<Collection<LazyTask<TaskKey, ?, ?>>>uncheckedCast(exactGenerateTasks));
        }*/
    }
}
