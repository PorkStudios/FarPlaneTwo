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
import net.daporkchop.fp2.mode.api.piece.IFarPieceData;
import net.daporkchop.fp2.mode.api.server.IFarStorage;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.ExactAsRoughGeneratorFallbackWrapper;
import net.daporkchop.fp2.mode.api.server.gen.IFarAssembler;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.server.scale.IFarDataScaler;
import net.daporkchop.fp2.mode.api.server.scale.IFarPieceScaler;
import net.daporkchop.fp2.mode.common.server.task.piece.ExactUpdatePieceTask;
import net.daporkchop.fp2.mode.common.server.task.piece.GetPieceTask;
import net.daporkchop.fp2.mode.common.server.action.LoadPieceAction;
import net.daporkchop.fp2.mode.common.server.action.SavePieceAction;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;
import net.daporkchop.fp2.util.threading.keyed.DefaultKeyedTaskScheduler;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.function.io.IOConsumer;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.common.util.PorkUtil;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld<POS extends IFarPos, P extends IFarPiece, D extends IFarPieceData> implements IFarWorld<POS, P, D> {
    protected static final BiFunction<Boolean, Boolean, Boolean> BOOLEAN_OR = (a, b) -> a | b;

    protected final WorldServer world;
    protected final RenderMode mode;

    protected final IFarGeneratorRough<POS, P, D> generatorRough;
    protected final IFarGeneratorExact<POS, P, D> generatorExact;
    protected final IFarPieceScaler<POS, P> pieceScaler;
    protected final IFarDataScaler<POS, D> dataScaler;
    protected final IFarAssembler<D, P> pieceAssembler;
    protected final IFarStorage<POS, P> pieceStorage;

    //cache for loaded tiles
    protected final Cache<POS, Compressed<POS, P>> cache = CacheBuilder.newBuilder()
            .concurrencyLevel(FP2Config.generationThreads)
            .softValues()
            .build();

    //contains positions of all tiles that aren't done
    protected final Map<POS, Boolean> notDone = new ConcurrentHashMap<>(); //TODO: make this (and also exactActive) (and also the task queue) lazily overflow to disk, because the queue might get REALLY big

    //contains positions of all tiles that are going to be updated with the exact generator
    protected final ObjLongMap<POS> exactActive = new ObjLongConcurrentHashMap<>(-1L);
    protected final Queue<LazyTask<TaskKey, ?, ?>> pendingExactTasks = new ArrayDeque<>();

    protected final LazyPriorityExecutor<TaskKey> executor; //TODO: make these global rather than per-dimension
    protected final DefaultKeyedTaskScheduler<POS> ioExecutor;

    protected final boolean lowResolution;
    protected final boolean inaccurate;
    protected final boolean refine;

    public AbstractFarWorld(@NonNull WorldServer world, @NonNull RenderMode mode) {
        this.world = world;
        this.mode = mode;

        IFarGeneratorRough<POS, P, D> generatorRough = this.mode().<POS, P, D>generatorsRough().stream()
                .map(f -> f.apply(world))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        IFarGeneratorExact<POS, P, D> generatorExact = this.mode().<POS, P, D>generatorsExact().stream()
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
        this.inaccurate = this.lowResolution && this.generatorRough.isLowResolutionInaccurate();
        this.refine = this.inaccurate && FP2Config.performance.lowResolutionRefine;

        this.pieceStorage = this.mode().uncheckedCreateStorage(world);

        if (this.mode().usesPieceData()) {
            this.pieceScaler = null;
            this.dataScaler = this.mode().createDataScaler();
            this.pieceAssembler = this.mode().createAssembler();
        } else {
            this.pieceScaler = this.mode().createPieceScaler();
            this.dataScaler = null;
            this.pieceAssembler = null;
        }

        this.executor = new LazyPriorityExecutor<>(
                FP2Config.generationThreads,
                PThreadFactories.builder().daemon().minPriority()
                        .collapsingId().name(PStrings.fastFormat("FP2 DIM%d Generation Thread #%%d", world.provider.getDimension())).build());
        this.ioExecutor = new DefaultKeyedTaskScheduler<>(
                FP2Config.ioThreads,
                PThreadFactories.builder().daemon().minPriority()
                        .collapsingId().name(PStrings.fastFormat("FP2 DIM%d IO Thread #%%d", world.provider.getDimension())).build());

        this.loadNotDone();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public Compressed<POS, P> getPieceLazy(@NonNull POS pos) {
        Compressed<POS, P> piece = this.cache.getIfPresent(pos);
        if (piece == null || piece.timestamp() == Compressed.VALUE_BLANK) {
            if (this.notDone(pos, true)) {
                //piece is not in cache and was newly marked as queued
                TaskKey key = new TaskKey(TaskStage.GET, pos.level());
                this.executor.submit(new GetPieceTask<>(this, key, pos, TaskStage.GET));
            }
            return null;
        }
        return piece;
    }

    public Compressed<POS, P> getRawPieceBlocking(@NonNull POS pos) {
        try {
            return this.cache.get(pos, new LoadPieceAction<>(this, pos));
        } catch (ExecutionException e) {
            PUnsafe.throwException(e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void pieceChanged(@NonNull Compressed<POS, P> piece) {
        if (piece.isBlank()) {
            return;
        }

        this.cache.put(piece.pos(), piece);
        ((IFarContext) this.world).tracker().pieceChanged(piece);

        if (piece.isDone()) {
            //unmark piece as being incomplete
            this.notDone.remove(piece.pos());
        }

        if (FP2Config.performance.savePartial || piece.isDone()) {
            //save the piece
            this.ioExecutor.submit(piece.pos(), new SavePieceAction<>(this, piece));
        }

        if (piece.timestamp() >= 0L && piece.pos().level() < FP2Config.maxLevels - 1) {
            //the piece has been generated with the exact generator, schedule all output pieces for scaling
            long currTimestamp = piece.timestamp();

            this.scaler.outputs(piece.pos()).forEach(outputPos -> this.schedulePieceForUpdate(outputPos, currTimestamp));
        }
    }

    protected void schedulePieceForUpdate(@NonNull POS pos, long newTimestamp) {
        //TODO: this is a race condition if an exact generation task queued in a previous tick occurs in between multiple block updates to
        // the same position during the current tick
        //TODO: wait, is this actually correct? actually, i think the above condition might actually cause it to regenerate the same piece
        // multiple times for that tick
        if (this.exactActive.put(pos, newTimestamp) < 0L) {
            //position wasn't previously queued for update
            synchronized (this.pendingExactTasks) {
                this.pendingExactTasks.add(new ExactUpdatePieceTask<>(this, new TaskKey(TaskStage.EXACT, pos.level()), pos, TaskStage.EXACT));
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !this.pendingExactTasks.isEmpty()) {
            //fire pending tasks
            synchronized (this.pendingExactTasks) {
                this.executor.submit(this.pendingExactTasks);
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
        LOGGER.trace("Shutting down IO workers in DIM{}", this.world.provider.getDimension());
        this.ioExecutor.shutdown();
        LOGGER.trace("Shutting down storage in DIM{}", this.world.provider.getDimension());
        this.pieceStorage.close();
        this.saveNotDone();
    }

    protected void saveNotDone() throws IOException {
        if (FP2_DEBUG && (FP2Config.debug.disablePersistence || FP2Config.debug.disableWrite) || this.notDone.isEmpty()) {
            return;
        }

        LOGGER.trace("Saving incomplete pieces in DIM{}", this.world.provider.getDimension());

        File root = this.pieceStorage.storageRoot();
        File tmpFile = PFiles.ensureFileExists(new File(root, "notDone.tmp"));
        File file = new File(root, "notDone");

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
        Collection<GetPieceTask<POS, P, D>> loadTasks = new ArrayList<>();
        Collection<ExactUpdatePieceTask<POS, P, D>> exactGenerateTasks = new ArrayList<>();

        try {
            File file = new File(this.pieceStorage.storageRoot(), "notDone");
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
                        loadTasks.add(new GetPieceTask<>(this, new TaskKey(TaskStage.GET, pos.level()), pos, TaskStage.GET));
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
        }
    }
}
