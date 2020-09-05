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

package net.daporkchop.fp2.strategy.base.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.base.server.task.ExactUpdatePieceTask;
import net.daporkchop.fp2.strategy.base.server.task.GetPieceTask;
import net.daporkchop.fp2.strategy.base.server.task.LoadPieceAction;
import net.daporkchop.fp2.strategy.base.server.task.SavePieceAction;
import net.daporkchop.fp2.strategy.common.IFarContext;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.common.server.IFarStorage;
import net.daporkchop.fp2.strategy.common.server.IFarWorld;
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.strategy.common.server.scale.IFarScaler;
import net.daporkchop.fp2.util.threading.keyed.DefaultKeyedTaskScheduler;
import net.daporkchop.fp2.util.threading.cachedblockaccess.CachedBlockAccess;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;
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

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld<POS extends IFarPos, P extends IFarPiece<POS>> implements IFarWorld<POS, P> {
    protected static final BiFunction<Boolean, Boolean, Boolean> BOOLEAN_OR = (a, b) -> a | b;

    protected final WorldServer world;
    protected final RenderMode mode;

    protected final IFarGeneratorRough<POS, P> generatorRough;
    protected final IFarGeneratorExact<POS, P> generatorExact;
    protected final IFarScaler<POS, P> scaler;
    protected final IFarStorage<POS, P> storage;

    //cache for loaded tiles
    protected final Cache<POS, P> cache = CacheBuilder.newBuilder()
            .concurrencyLevel(FP2Config.generationThreads)
            .softValues()
            .build();

    //contains positions of all tiles that aren't done
    protected final Map<POS, Boolean> notDone = new ConcurrentHashMap<>();

    //contains positions of all tiles that are going to be updated with the exact generator
    protected final ObjLongMap<POS> exactActive = new ObjLongConcurrentHashMap<>(-1L);
    protected final Queue<LazyTask<TaskKey, ?, ?>> pendingExactTasks = new ArrayDeque<>();

    protected final LazyPriorityExecutor<TaskKey> executor; //TODO: make these global rather than per-dimension
    protected final DefaultKeyedTaskScheduler<POS> ioExecutor;

    protected final boolean lowResolution;
    protected final boolean inaccurate;
    protected final boolean refine;
    protected final boolean refineProgressive;

    public AbstractFarWorld(@NonNull WorldServer world, @NonNull RenderMode mode) {
        this.world = world;
        this.mode = mode;

        IFarGeneratorRough<POS, P> generatorRough = this.mode().<POS, P>uncheckedGeneratorsRough().stream()
                .map(f -> f.apply(world))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        IFarGeneratorExact<POS, P> generatorExact = this.mode().<POS, P>uncheckedGeneratorsExact().stream()
                .map(f -> f.apply(world))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new IllegalStateException(PStrings.fastFormat(
                        "No exact generator could be found for world %d (type: %s), mode:%s",
                        world.provider.getDimension(),
                        world.getWorldType(),
                        this.mode()
                )));

        if (generatorRough != null) { //rough generator has been set, so use it
            generatorRough.init(world);
        } else { //rough generator wasn't set, use the exact generator for this as well
            LOGGER.warn("No rough generator exists for world {} (type: {})! Falling back to exact generator, this will have serious performance implications.", world.provider.getDimension(), world.getWorldType());
            generatorRough = new IFarGeneratorRough<POS, P>() {
                @Override
                public void init(@NonNull WorldServer world) {
                    //no-op
                }

                @Override
                public void generate(@NonNull P piece) {
                    //TODO: optimize this lol
                    generatorExact.generate(((CachedBlockAccess.Holder) AbstractFarWorld.this.world).cachedBlockAccess(), piece);
                }

                @Override
                public boolean supportsLowResolution() {
                    return false;
                }

                @Override
                public boolean isLowResolutionInaccurate() {
                    return false;
                }
            };
        }
        generatorExact.init(world);

        this.generatorRough = generatorRough;
        this.generatorExact = generatorExact;

        this.lowResolution = FP2Config.performance.lowResolutionEnable && this.generatorRough.supportsLowResolution();
        this.inaccurate = this.lowResolution && this.generatorRough.isLowResolutionInaccurate();
        this.refine = this.inaccurate && FP2Config.performance.lowResolutionRefine;
        this.refineProgressive = this.refine && FP2Config.performance.lowResolutionRefineProgressive;

        this.scaler = this.mode().uncheckedCreateScaler(world);
        this.storage = this.mode().uncheckedCreateStorage(world);

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
    public P getPieceLazy(@NonNull POS pos) {
        P piece = this.cache.getIfPresent(pos);
        if (piece == null || piece.timestamp() == IFarPiece.PIECE_EMPTY) {
            if (this.notDone(pos, true)) {
                //piece is not in cache and was newly marked as queued
                TaskKey key = new TaskKey(TaskStage.GET, pos.level());
                this.executor.submit(new GetPieceTask<>(this, key, pos, TaskStage.GET));
            }
            return null;
        }
        return piece;
    }

    public P getRawPieceBlocking(@NonNull POS pos) {
        try {
            return this.cache.get(pos, new LoadPieceAction<>(this, pos));
        } catch (ExecutionException e) {
            PUnsafe.throwException(e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void pieceChanged(@NonNull P piece) {
        if (piece.isEmpty()) {
            return;
        }

        this.cache.put(piece.pos(), piece);
        ((IFarContext) this.world).tracker().pieceChanged(piece);

        if (piece.isDone()) {
            //unmark piece as being incomplete
            this.notDone.remove(piece.pos());
        }

        if ((FP2Config.performance.savePartial || piece.isDone()) && piece.isDirty()) {
            //save the piece
            this.ioExecutor.submit(piece.pos(), new SavePieceAction<>(this, piece));
        }

        if (piece.timestamp() >= 0L && piece.isDirty() && piece.level() < FP2Config.maxLevels - 1) {
            //the piece has been generated with the exact generator, schedule all output pieces for scaling
            long currTimestamp = piece.timestamp();

            this.scaler.outputs(piece.pos()).forEach(outputPos -> this.schedulePieceForUpdate(outputPos, currTimestamp));
        }
    }

    protected void schedulePieceForUpdate(@NonNull POS pos, long newTimestamp) {
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
    public CachedBlockAccess blockAccess() {
        return ((CachedBlockAccess.Holder) this.world).cachedBlockAccess();
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
        this.storage.close();
        this.saveNotDone();
    }

    protected void saveNotDone() throws IOException {
        if (FP2Config.debug.disablePersistence || FP2Config.debug.disableWrite || this.notDone.isEmpty()) {
            return;
        }

        LOGGER.trace("Saving incomplete pieces in DIM{}", this.world.provider.getDimension());

        File root = this.storage.storageRoot();
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
        Collection<GetPieceTask<POS, P>> loadTasks = new ArrayList<>();

        try {
            File file = new File(this.storage.storageRoot(), "notDone");
            if (FP2Config.debug.disablePersistence || FP2Config.debug.disableRead || !PFiles.checkFileExists(file)) {
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
                    //TODO: do something with it
                }
            } finally {
                buf.release();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to load notDone pieces!", e);
        } finally {
            LOGGER.info("Loaded {} persisted piece load tasks", loadTasks.size());
            this.executor.submit(PorkUtil.<Collection<LazyTask<TaskKey, ?, ?>>>uncheckedCast(loadTasks));
        }
    }
}
