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

package net.daporkchop.fp2.mode.common.server.task;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.CompressedPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.TaskKey;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;

import java.util.List;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractScaleTask<POS extends IFarPos, P extends IFarPiece, B extends IFarPieceBuilder> extends AbstractPieceTask<POS, P, B, CompressedPiece<POS, P, B>> {
    public AbstractScaleTask(@NonNull AbstractFarWorld<POS, P, B> world, @NonNull TaskKey key, @NonNull POS pos, @NonNull TaskStage requestedBy) {
        super(world, key, pos, requestedBy);

        checkArg(pos.level() != 0, "cannot do scaling at level %d!", pos.level());
    }

    @Override
    public CompressedPiece<POS, P, B> run(@NonNull List<CompressedPiece<POS, P, B>> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        long newTimestamp = this.computeNewTimestamp();
        if (this.isDone()) {
            return this.getNow();
        }

        CompressedPiece<POS, P, B> piece = this.world.getRawPieceBlocking(this.pos);
        if (piece.timestamp() >= newTimestamp) {
            return piece;
        }

        //inflate pieces into array
        P[] srcs = uncheckedCast(this.world.mode().pieceArray(params.size()));
        for (int i = 0, len = srcs.length; i < len; i++) {
            params.get(i).readLock().lock();
            srcs[i] = params.get(i).inflate();
        }

        piece.writeLock().lock();
        try {
            if (piece.timestamp() >= newTimestamp) {
                return piece;
            }

            SimpleRecycler<B> builderRecycler = uncheckedCast(this.pos.mode().builderRecycler());
            B builder = builderRecycler.allocate();
            try {
                builder.reset(); //ensure builder is reset

                this.world.scaler().scale(srcs, builder);
                piece.set(newTimestamp, builder);
            } finally {
                builderRecycler.release(builder);
            }

            piece.readLock().lock(); //downgrade lock
        } finally {
            piece.writeLock().unlock();

            SimpleRecycler<P> pieceRecycler = uncheckedCast(this.pos.mode().pieceRecycler());
            for (int i = 0, len = srcs.length; i < len; i++) {
                if (srcs[i] != null) {
                    pieceRecycler.release(srcs[i]);
                }
                params.get(i).readLock().unlock();
            }
        }

        try {
            this.world.pieceChanged(piece);
        } finally {
            piece.readLock().unlock();
        }

        return this.finish(piece, executor);
    }

    protected abstract long computeNewTimestamp();

    protected CompressedPiece<POS, P, B> finish(@NonNull CompressedPiece<POS, P, B> piece, @NonNull LazyPriorityExecutor<TaskKey> executor) {
        return piece;
    }
}
