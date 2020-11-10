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

package net.daporkchop.fp2.mode.common.server.worker;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.piece.IFarPieceData;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.PriorityTask;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.mode.common.server.task.piece.RoughScalePieceTask;
import net.daporkchop.fp2.util.IReusablePersistent;
import net.daporkchop.fp2.util.SimpleRecycler;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractFarServerWorker<POS extends IFarPos, P extends IFarPiece, D extends IFarPieceData, S extends IReusablePersistent> implements Consumer<PriorityTask<POS>> {
    @NonNull
    protected final AbstractFarWorld<POS, P, D> world;

    public abstract SimpleRecycler<S> scaleInputRecycler();

    public abstract S[] scaleInputArray(int length);

    @Override
    public void accept(PriorityTask<POS> task) {
        switch (task.stage()) {
            case LOAD:
                this.loadPiece(task, task.pos());
                break;
            case REFINE:
                this.roughScalePiece(task, task.pos(), this.world.getRawPieceBlocking(task.pos()));
                break;
        }
    }

    //

    public void loadPiece(PriorityTask<POS> root, POS pos) {
        Compressed<POS, P> compressedPiece = this.world.getRawPieceBlocking(pos);

        if (compressedPiece.isGenerated()) {
            //this adds the piece to the cache, unmarks it as not done and notifies the player tracker
            this.world.pieceChanged(compressedPiece);
        } else { //the piece has not been fully generated yet
            if (pos.level() == 0 || this.world.lowResolution()) {
                //the piece can be generated using the rough generator
                this.roughGeneratePiece(root, pos, compressedPiece);
                if (pos.level() != 0 && this.world.refine()) {
                    //the generated piece is low-resolution, inaccurate and refine is enabled: enqueue this position to be refined
                    this.world.executor().submit(new PriorityTask<>(TaskStage.REFINE, pos));
                }
            } else {
                //the piece is at a lower detail than 0, and low-resolution generation is not an option
                //this will generate the piece and all pieces below it down to level 0 until the piece can be "generated" from scaled data
                this.roughScalePiece(root, pos, compressedPiece);
            }
        }
    }

    //

    public void roughGeneratePiece(PriorityTask<POS> root, POS pos, Compressed<POS, P> compressedPiece) {
        this.world.executor().checkForHigherPriorityWork(root);

        boolean inaccurate = pos.level() != 0 && this.world.inaccurate();
        long newTimestamp = inaccurate && this.world.refine()
                ? Compressed.valueRough(pos.level()) //if the piece is inaccurate, it will need to be re-generated later based on scaled data
                : Compressed.VALUE_ROUGH_COMPLETE;
        if (compressedPiece.timestamp() >= newTimestamp) {
            return;
        }

        this.roughGeneratePiece0(pos, compressedPiece, newTimestamp);
    }

    public abstract void roughGeneratePiece0(POS pos, Compressed<POS, P> compressedPiece, long newTimestamp);

    //

    public void roughScalePiece(PriorityTask<POS> root, POS pos, Compressed<POS, P> compressedPiece) {
        this.world.executor().checkForHigherPriorityWork(root);

        long newTimestamp = Compressed.valueRough(0);
        if (compressedPiece.timestamp() >= newTimestamp) {
            return;
        }

        //recursively scale children until we hit the bottom
        List<Compressed<POS, S>> compressedSrcs = this.roughScaleInputs0(root, pos);
        if (compressedPiece.timestamp() >= newTimestamp) {
            return;
        }

        //inflate sources
        SimpleRecycler<S> scaleInputRecycler = this.scaleInputRecycler();
        S[] srcs = this.scaleInputArray(compressedSrcs.size());
        for (int i = 0; i < compressedSrcs.size(); i++) {
            srcs[i] = compressedSrcs.get(i).inflate(scaleInputRecycler);
        }

        try {
            //actually do scaling
            this.roughScalePiece0(pos, compressedPiece, srcs, newTimestamp);
        } finally {
            for (S src : srcs) {
                if (src != null) {
                    scaleInputRecycler.release(src);
                }
            }
        }
    }

    public abstract List<Compressed<POS, S>> roughScaleInputs0(PriorityTask<POS> root, POS pos);

    public abstract void roughScalePiece0(POS pos, Compressed<POS, P> compressedPiece, S[] srcs, long newTimestamp);
}
