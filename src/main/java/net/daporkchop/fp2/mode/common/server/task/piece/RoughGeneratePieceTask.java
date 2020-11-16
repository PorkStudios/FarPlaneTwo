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
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.TaskKey;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.IReusablePersistent;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Generates a piece using the rough generator.
 *
 * @author DaPorkchop_
 */
public class RoughGeneratePieceTask<POS extends IFarPos, P extends IFarPiece, D extends IReusablePersistent>
        extends AbstractPieceTask<POS, P, D, Void> {
    protected final boolean inaccurate;

    public RoughGeneratePieceTask(@NonNull AbstractFarWorld<POS, P, D> world, @NonNull TaskKey key, @NonNull POS pos) {
        super(world, key, pos, TaskStage.LOAD);

        boolean lowRes = pos.level() != 0;
        if (lowRes) {
            checkArg(FP2Config.performance.lowResolutionEnable, "low resolution rendering is disabled!");
            checkArg(world.generatorRough().supportsLowResolution(),
                    "rough generator (%s) cannot generate low-resolution piece at level %d!", world.generatorRough(), pos.level());
        }
        this.inaccurate = lowRes && world.inaccurate();
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, Void>> before(@NonNull TaskKey key) throws Exception {
        return Stream.empty();
    }

    @Override
    public Compressed<POS, P> run(@NonNull List<Void> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        Compressed<POS, P> piece = this.world.getRawPieceBlocking(this.pos);
        long newTimestamp = this.inaccurate && this.world.refine()
                ? Compressed.valueRough(this.pos.level()) //if the piece is inaccurate, it will need to be re-generated later based on scaled data
                : Compressed.VALUE_ROUGH_COMPLETE;
        if (piece.timestamp() >= newTimestamp) {
            return piece;
        }

        piece.writeLock().lock();
        try {
            if (piece.timestamp() >= newTimestamp) {
                return piece;
            }

            SimpleRecycler<D> builderRecycler = uncheckedCast(this.pos.mode().builderRecycler());
            D builder = builderRecycler.allocate();
            try {
                builder.reset(); //ensure builder is reset

                try {
                    this.world.generatorRough().generate(this.pos, builder);
                } catch (RuntimeException e) {
                    Constants.LOGGER.error("while generating piece at " + this.pos, e);
                    builder.reset();
                }
                piece.set(newTimestamp, builder);
            } finally {
                builderRecycler.release(builder);
            }

            piece.readLock().lock(); //downgrade lock
        } finally {
            piece.writeLock().unlock();
        }

        try {
            this.world.pieceChanged(piece);
        } finally {
            piece.readLock().unlock();
        }

        if (this.inaccurate && this.world.refine()) {
            //piece is low-resolution and inaccurate, we should now enqueue it to generate scaled data from the layer below
            executor.submit(new RoughScalePieceTask<>(this.world, this.key.withStage(TaskStage.ROUGH_SCALE).lowerTie(), this.pos, TaskStage.ROUGH_GENERATE,
                    this.world.refineProgressive() ? this.pos.level() - 1 : 0).thenCopyStatusTo(this));
            return null; //return null so that this won't be complete until the piece is finished
        }
        return piece;
    }
}
