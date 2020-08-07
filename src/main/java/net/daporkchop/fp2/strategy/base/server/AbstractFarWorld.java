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
import io.netty.util.concurrent.EventExecutor;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.fp2.strategy.base.server.task.GetPieceTask;
import net.daporkchop.fp2.strategy.base.server.task.LoadPieceAction;
import net.daporkchop.fp2.strategy.base.server.task.SavePieceTask;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.common.server.IFarGenerator;
import net.daporkchop.fp2.strategy.common.server.IFarScaler;
import net.daporkchop.fp2.strategy.common.server.IFarStorage;
import net.daporkchop.fp2.strategy.common.server.IFarWorld;
import net.daporkchop.fp2.util.threading.PriorityThreadFactory;
import net.daporkchop.fp2.util.threading.cachedblockaccess.CachedBlockAccess;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.threadfactory.ThreadFactoryBuilder;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.WorldServer;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld<POS extends IFarPos, P extends IFarPiece<POS>> implements IFarWorld<POS, P> {
    protected final WorldServer world;
    protected final RenderMode mode;

    protected final IFarGenerator<POS, P> generatorRough;
    protected final IFarGenerator<POS, P> generatorExact;
    protected final IFarScaler<POS, P> scaler;
    protected final IFarStorage<POS, P> storage;

    protected final Cache<POS, P> cache = CacheBuilder.newBuilder()
            .concurrencyLevel(FP2Config.generationThreads)
            .softValues()
            .build();
    protected final Set<POS> queuedPositions = ConcurrentHashMap.newKeySet();

    protected final LazyPriorityExecutor<TaskKey> executor;
    protected final EventExecutor nettyExecutor;

    public AbstractFarWorld(@NonNull WorldServer world, @NonNull RenderMode mode) {
        this.world = world;
        this.mode = mode;

        IFarGenerator<POS, P> generatorRough = this.mode().<POS, P>uncheckedGeneratorsRough().stream()
                .map(f -> f.apply(world))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        IFarGenerator<POS, P> generatorExact = this.mode().<POS, P>uncheckedGeneratorsExact().stream()
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
            generatorRough = generatorExact;
        }
        generatorExact.init(world);

        this.generatorRough = generatorRough;
        this.generatorExact = generatorExact;

        this.scaler = this.mode().uncheckedCreateScaler(world);
        this.storage = this.mode().uncheckedCreateStorage(world);

        this.executor = new LazyPriorityExecutor<>(FP2Config.generationThreads, new PriorityThreadFactory(
                new ThreadFactoryBuilder().daemon().collapsingId().formatId()
                        .name(PStrings.fastFormat("FP2 DIM%d Generation Thread #%%d", world.provider.getDimension()))
                        .priority(Thread.MIN_PRIORITY).build(),
                Thread.MIN_PRIORITY));
        this.nettyExecutor = this.executor.maxPriorityExecutor();
    }

    @Override
    public P getPieceLazy(@NonNull POS pos) {
        P piece = this.cache.getIfPresent(pos);
        if (piece == null && this.queuedPositions.add(pos)) {
            //piece is not in cache and was newly marked as queued
            TaskKey key = new TaskKey(TaskStage.LOAD, pos.level());
            this.executor.submit(new GetPieceTask<>(this, key, pos));
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

    public void pieceChanged(@NonNull P piece, @NonNull POS pos, long newTimestamp) {
        checkArg(newTimestamp != IFarPiece.PIECE_EMPTY);
        if (piece.isDirty()) { //only do stuff if the piece changed
            //save the piece
            this.executor.submit(new SavePieceTask<>(this, new TaskKey(TaskStage.SAVE, pos.level()), pos, piece));

            if (newTimestamp > 0L && pos.level() < FP2Config.maxLevels) {
                //the piece has been generated with the exact generator, schedule all output pieces for scaling
                this.scaler.outputs(pos).forEach(p -> this.schedulePieceForScaling(p, newTimestamp));
            }
        }
    }

    public void schedulePieceForScaling(@NonNull POS pos, long _newTimestamp) {
        checkArg(_newTimestamp != IFarPiece.PIECE_EMPTY);
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public void blockChanged(int x, int y, int z) {
        //TODO
    }

    @Override
    public CachedBlockAccess blockAccess() {
        return ((CachedBlockAccess.Holder) this.world).cachedBlockAccess();
    }

    @Override
    public void close() throws IOException {
        this.executor.shutdown();
        this.storage.close();
    }

}





