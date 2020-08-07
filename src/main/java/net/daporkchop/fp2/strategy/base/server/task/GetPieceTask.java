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

import io.netty.util.concurrent.GenericFutureListener;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Main task for loading pieces, this actually gets the piece from disk, creates a new one if absent and queues it for generation if it doesn't contain
 * any data.
 *
 * @author DaPorkchop_
 */
public class GetPieceTask<POS extends IFarPos, P extends IFarPiece<POS>> extends AbstractPieceTask<POS, P, Void> {
    public GetPieceTask(@NonNull AbstractFarWorld<POS, P> world, @NonNull TaskKey key, @NonNull POS pos) {
        super(world, key, pos);

        this.addListener(uncheckedCast(Callback.INSTANCE));
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, Void>> before(@NonNull TaskKey key) {
        return Stream.empty();
    }

    @Override
    public P run(@NonNull List<Void> params, @NonNull LazyPriorityExecutor<TaskKey> executor) {
        P piece = this.world.getRawPieceBlocking(this.pos);
        long version = piece.timestamp();
        if (version < IFarPiece.PIECE_ROUGH_COMPLETE) { //the piece has not been fully generated yet
            boolean supportsLowResolution = this.world.generatorRough().supportsLowResolution();
            if (this.pos.level() == 0 || supportsLowResolution) {
                //the piece can be generated using the rough generator
                executor.submit(new RoughGeneratePieceTask<>(this.world, this.key.withStage(TaskStage.ROUGH_GENERATE), this.pos).thenCopyStatusTo(this));
            } else {
                //the piece is at a lower detail than 0, and low-resolution generation is not an option
                //this will generate the piece and all pieces below it down to level 0 until the piece can be "generated" from scaled data
                executor.submit(new RoughScalePieceTask<>(this.world, this.key.withStage(TaskStage.ROUGH_SCALE), this.pos, 0).thenCopyStatusTo(this));
            }
            if (version == IFarPiece.PIECE_EMPTY) {
                return null; //don't store the piece in the world until it contains at least SOME data
            }
        }
        return piece;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Callback<POS extends IFarPos, P extends IFarPiece<POS>> implements GenericFutureListener<GetPieceTask<POS, P>> {
        private static final Callback INSTANCE = new Callback();

        @Override
        public void operationComplete(@NonNull GetPieceTask<POS, P> task) throws Exception {
            if (task.isSuccess()) {
                task.world.cache().put(task.pos, task.getNow());
            }
            task.world.queuedPositions().remove(task.pos);
        }
    }
}
