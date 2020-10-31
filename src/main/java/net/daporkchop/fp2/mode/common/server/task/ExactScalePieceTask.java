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
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.TaskKey;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class ExactScalePieceTask<POS extends IFarPos, P extends IFarPiece, B extends IFarPieceBuilder> extends AbstractPieceTask<POS, P, B, CompressedPiece<POS, P, B>> {
    public ExactScalePieceTask(@NonNull AbstractFarWorld<POS, P, B> world, @NonNull TaskKey key, @NonNull POS pos, @NonNull TaskStage requestedBy) {
        super(world, key, pos, requestedBy);

        checkArg(pos.level() != 0, "cannot do scaling at level %d!", pos.level());
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, CompressedPiece<POS, P, B>>> before(@NonNull TaskKey key) throws Exception {
        return this.world.scaler().inputs(this.pos)
                .map(pos -> new GetPieceTask<>(this.world, key.withStageLevel(TaskStage.GET, pos.level()), pos, TaskStage.EXACT_SCALE));
    }

    @Override
    public CompressedPiece<POS, P, B> run(@NonNull List<CompressedPiece<POS, P, B>> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        long newTimestamp = this.world.exactActive().remove(this.pos);
        if (newTimestamp < 0L) { //probably impossible, but this means that another task scheduled for the same piece already ran before this one
            LOGGER.warn("Duplicate generation task scheduled for piece at {}!", this.pos);
            this.setSuccess(null); //explicitly complete the future
            return null;
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

        try {
            //TODO: re-enable this
            /*if (false && srcs.length > 0) { //get actual timestamp by computing the maximum input timestamp
                long effectiveNewTimestamp = Arrays.stream(srcs).mapToLong(IFarPiece::timestamp).max().orElse(newTimestamp);
                checkState(effectiveNewTimestamp >= newTimestamp, "effectiveNewTimestamp (%d) is somehow less than newTimestamp (%d)", effectiveNewTimestamp, newTimestamp);
                newTimestamp = effectiveNewTimestamp;
                if (piece.timestamp() >= newTimestamp) {
                    return piece;
                }
            }*/

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
            }
        } finally {
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

        return piece;
    }
}
