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
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.TaskKey;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Handles generation of a piece at a lower detail level.
 * <p>
 * This will cause all of the input pieces to be generated, and then use them as inputs for scaling of this piece.
 *
 * @author DaPorkchop_
 */
public class RoughScalePieceTask<POS extends IFarPos, P extends IFarPiece<POS>> extends AbstractPieceTask<POS, P, P> {
    protected final int targetDetail;

    public RoughScalePieceTask(@NonNull AbstractFarWorld<POS, P> world, @NonNull TaskKey key, @NonNull POS pos, @NonNull TaskStage requestedBy, int targetDetail) {
        super(world, key, pos, requestedBy);

        checkArg(pos.level() != 0, "cannot do scaling at level %d!", pos.level());
        checkArg(requestedBy != TaskStage.GET || targetDetail == 0, "only GET may target non-zero detail levels!");

        this.targetDetail = targetDetail;
        checkArg(targetDetail < pos.level(), "targetDetail (%d) must be less than the piece's detail level (%d)", targetDetail, pos.level());
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, P>> before(@NonNull TaskKey key) throws Exception {
        Stream<POS> inputs = this.world.scaler().inputs(this.pos);
        if (this.targetDetail == this.pos.level() - 1) {
            //this piece is one level above the target level, so the pieces should be read directly rather than scaling them
            return inputs.map(pos -> new GetPieceTask<>(this.world, key.withStageLevel(TaskStage.GET, pos.level()), pos, TaskStage.ROUGH_SCALE));
        } else {
            //this piece is more than one level above the target level, queue the pieces below for scaling as well
            return inputs.map(pos -> new RoughScalePieceTask<>(this.world, key.withStageLevel(TaskStage.ROUGH_SCALE, pos.level()), pos, TaskStage.ROUGH_SCALE, this.targetDetail));
        }
    }

    @Override
    public P run(@NonNull List<P> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        P piece = this.world.getRawPieceBlocking(this.pos);
        long newTimestamp = CompressedPiece.pieceRough(this.targetDetail);
        if (piece.timestamp() >= newTimestamp) {
            return piece;
        }

        P[] srcs = params.toArray(uncheckedCast(this.world.mode().pieceArray(params.size())));
        for (int i = 0, len = srcs.length; i < len; i++) {
            srcs[i].readLock().lock();
        }
        piece.writeLock().lock();
        try {
            if (piece.timestamp() >= newTimestamp) {
                return piece;
            }

            piece.clear(); //reset piece contents
            this.world.scaler().scale(srcs, piece);
            piece.postGenerate();
            piece.updateTimestamp(newTimestamp);
            piece.markDirty();

            piece.readLock().lock(); //downgrade lock
        } finally {
            piece.writeLock().unlock();
            for (int i = 0, len = srcs.length; i < len; i++) {
                srcs[i].readLock().unlock();
            }
        }

        try {
            this.world.pieceChanged(piece);
        } finally {
            piece.readLock().unlock();
        }

        if (this.targetDetail != 0 && this.world.refine()) {
            //continually re-scale the tile until the target detail reaches 0
            executor.submit(new RoughScalePieceTask<>(this.world, this.key.lowerTie(), this.pos, TaskStage.ROUGH_SCALE,
                    this.world.refineProgressive() ? this.targetDetail - 1 : 0).thenCopyStatusTo(this));
            return null; //return null so that this won't be complete until the piece is finished
        }
        return piece;
    }
}
