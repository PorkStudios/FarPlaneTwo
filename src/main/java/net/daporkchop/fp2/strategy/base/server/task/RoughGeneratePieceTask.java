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
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.strategy.base.server.AbstractFarWorld;
import net.daporkchop.fp2.strategy.base.server.TaskKey;
import net.daporkchop.fp2.strategy.base.server.TaskStage;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Generates a piece using the rough generator.
 *
 * @author DaPorkchop_
 */
public class RoughGeneratePieceTask<POS extends IFarPos, P extends IFarPiece<POS>> extends AbstractPieceTask<POS, P, Void> {
    protected final boolean top;
    protected final boolean inaccurate;

    public RoughGeneratePieceTask(@NonNull AbstractFarWorld<POS, P> world, @NonNull TaskKey key, @NonNull POS pos, boolean top) {
        super(world, key, pos);

        this.top = top;

        boolean lowRes = pos.level() != 0;
        if (lowRes) {
            checkArg(FP2Config.performance.lowResolutionEnable, "low resolution rendering is disabled!");
            checkArg(world.generatorRough().supportsLowResolution(),
                    "rough generator (%s) cannot generate low-resolution piece at level %d!", world.generatorRough(), pos.level());
        }
        this.inaccurate = lowRes && world.inaccurate();
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, Void>> before(@NonNull TaskKey key) {
        return Stream.empty();
    }

    @Override
    public P run(@NonNull List<Void> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        P piece = this.world.getRawPieceBlocking(this.pos);
        long newTimestamp = this.inaccurate && this.world.refine()
                ? IFarPiece.pieceRough(this.pos.level()) //if the piece is inaccurate, it will need to be re-generated later based on scaled data
                : IFarPiece.PIECE_ROUGH_COMPLETE;
        if (piece.timestamp() >= newTimestamp) {
            return piece;
        }

        piece.writeLock().lock();
        try {
            if (piece.timestamp() >= newTimestamp) {
                return piece;
            }

            this.world.generatorRough().generate(this.world.blockAccess(), piece);
            piece.updateTimestamp(newTimestamp);
        } finally {
            piece.writeLock().unlock();
        }

        this.world.pieceChanged(piece);

        if (this.top && this.inaccurate && this.world.refine()) {
            //piece is low-resolution and inaccurate, we should now enqueue it to generate scaled data from the layer below
            executor.submit(new RoughScalePieceTask<>(this.world, this.key.withStage(TaskStage.ROUGH_SCALE).lowerTie(), this.pos,
                    this.world.refineProgressive() ? this.pos.level() - 1 : 0));
        }
        return piece;
    }
}
