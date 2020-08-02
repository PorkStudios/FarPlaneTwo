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

import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class RoughScalePieceTask<POS extends IFarPos, P extends IFarPiece<POS>> extends AbstractPieceTask<POS, P, P> {
    public RoughScalePieceTask(@NonNull AbstractFarWorld<POS, P> world, @NonNull TaskKey key, @NonNull POS pos) {
        super(world, key, pos);
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, P>> before(@NonNull TaskKey key) {
        return this.world.scaler().inputs(this.pos)
                .map(pos -> new LoadPieceTask<>(this.world, key.withStageLevel(TaskStage.LOAD, pos.level()), pos));
    }

    @Override
    public P run(@NonNull List<P> params, @NonNull LazyPriorityExecutor<TaskKey> executor) {
        //create new destination piece
        //we just assume that this class will never be used for pieces that already exist :P
        //TODO: this assumption is actually wrong, we'll need to query the world
        P dst = uncheckedCast(this.world.mode().piece(this.pos));

        P[] srcs = params.toArray(uncheckedCast(this.world.mode().pieceArray(params.size())));

        for (int i = 0, len = srcs.length; i < len; i++) {
            srcs[i].readLock().lock();
        }
        dst.writeLock().lock();

        try {
            //actually do scaling
            this.world.scaler().scale(srcs, dst);

            //queue the piece to be saved
            this.world.savePiece(dst);
        } finally {
            dst.writeLock().unlock();
            for (int i = 0, len = srcs.length; i < len; i++) {
                srcs[i].readLock().unlock();
            }
        }

        return dst;
    }
}
