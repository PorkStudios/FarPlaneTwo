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

package net.daporkchop.fp2.mode.common.server.task.piece;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.piece.IFarData;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.TaskKey;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.List;
import java.util.stream.Stream;

/**
 * Main task for loading pieces, this actually gets the piece from disk, creates a new one if absent and queues it for generation if it doesn't contain
 * any data.
 *
 * @author DaPorkchop_
 */
public class GetPieceTask<POS extends IFarPos, P extends IFarPiece, D extends IFarData>
        extends AbstractPieceTask<POS, P, D, Void> {
    public GetPieceTask(@NonNull AbstractFarWorld<POS, P, D> world, @NonNull TaskKey key, @NonNull POS pos, @NonNull TaskStage requestedBy) {
        super(world, key, pos, requestedBy);

        world.notDone(pos, requestedBy == TaskStage.LOAD);
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, Void>> before(@NonNull TaskKey key) throws Exception {
        return Stream.empty();
    }

    @Override
    public Compressed<POS, P> run(@NonNull List<Void> params, @NonNull LazyPriorityExecutor<TaskKey> executor) {
        Compressed<POS, P> piece = this.world.getRawPieceBlocking(this.pos);

        piece.readLock().lock();
        try {
            if (piece.isGenerated()) {
                //this adds the piece to the cache, unmarks it as not done and notifies the player tracker
                this.world.pieceChanged(piece);
            } else { //the piece has not been fully generated yet
                if (this.pos.level() == 0 //only allow rough generation if the piece was requested for loading by a player's presence
                    || (this.requestedBy == TaskStage.LOAD && this.world.lowResolution())) {
                    //the piece can be generated using the rough generator
                    executor.submit(new RoughGeneratePieceTask<>(this.world, this.key.withStage(TaskStage.ROUGH_GENERATE), this.pos)
                            .thenCopyStatusTo(this));
                } else {
                    //the piece is at a lower detail than 0, and low-resolution generation is not an option
                    //this will generate the piece and all pieces below it down to level 0 until the piece can be "generated" from scaled data
                    executor.submit(new RoughScalePieceTask<>(this.world, this.key.withStage(TaskStage.ROUGH_SCALE), this.pos, TaskStage.LOAD, 0)
                            .thenCopyStatusTo(this));
                }
                if (piece.isBlank()) {
                    return null; //don't store the piece in the world until it contains at least SOME data
                }
            }
        } finally {
            piece.readLock().unlock();
        }
        return piece;
    }
}
