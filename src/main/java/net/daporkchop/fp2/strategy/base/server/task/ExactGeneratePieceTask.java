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

package net.daporkchop.fp2.strategy.base.server.task;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.server.AbstractFarWorld;
import net.daporkchop.fp2.strategy.base.server.TaskKey;
import net.daporkchop.fp2.strategy.base.server.TaskStage;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class ExactGeneratePieceTask<POS extends IFarPos, P extends IFarPiece<POS>> extends AbstractPieceTask<POS, P, Void> {
    public ExactGeneratePieceTask(@NonNull AbstractFarWorld<POS, P> world, @NonNull TaskKey key, @NonNull POS pos) {
        super(world, key, pos, TaskStage.EXACT);

        checkArg(pos.level() == 0, "cannot do exact generation at level %d!", pos.level());
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, Void>> before(@NonNull TaskKey key) throws Exception {
        return Stream.empty();
    }

    @Override
    public P run(@NonNull List<Void> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        long newTimestamp = this.world.exactActive().remove(this.pos);
        if (newTimestamp < 0L) { //probably impossible, but this means that another task scheduled for the same piece already ran before this one
            LOGGER.warn("Duplicate generation task scheduled for piece at {}!", this.pos);
            this.setSuccess(null); //explicitly complete the future
            return null;
        }

        P piece = this.world.getRawPieceBlocking(this.pos);
        if (piece.timestamp() >= newTimestamp) {
            return piece;
        }

        piece.writeLock().lock();
        try {
            if (piece.timestamp() >= newTimestamp) {
                return piece;
            }

            piece.clear(); //reset piece contents
            this.world.generatorExact().generate(this.world.blockAccess(), piece); //generate piece
            piece.updateTimestamp(newTimestamp);
            piece.markDirty();

            piece.readLock().lock(); //downgrade lock
        } finally {
            piece.writeLock().unlock();
        }

        try {
            this.world.pieceChanged(piece);
        } finally {
            piece.readLock().unlock();
        }

        return piece;
    }
}
