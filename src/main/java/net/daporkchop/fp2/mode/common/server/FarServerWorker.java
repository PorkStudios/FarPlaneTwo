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

package net.daporkchop.fp2.mode.common.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.util.SimpleRecycler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FarServerWorker<POS extends IFarPos, P extends IFarPiece> implements Consumer<PriorityTask<POS>> {
    @NonNull
    protected final AbstractFarWorld<POS, P> world;

    @Override
    public void accept(PriorityTask<POS> task) {
        switch (task.stage()) {
            case LOAD:
                this.loadPiece(task, task.pos());
                break;
            case ROUGH:
                this.world.pieceAvailable(this.roughGetPiece(task, task.pos()));
                break;
            default:
                throw new IllegalArgumentException(Objects.toString(task));
        }
    }

    //

    public void loadPiece(PriorityTask<POS> root, POS pos) {
        Compressed<POS, P> compressedPiece = this.world.getPieceCachedOrLoad(pos);
        if (compressedPiece.isGenerated()) {
            //this unmarks the piece as not done and notifies the player tracker
            this.world.pieceAvailable(compressedPiece);
        } else {
            //the piece has not been fully generated yet
            //rather than getting the piece now, we enqueue it for rough generation later. this allows all of the LOAD tasks to run first, so that we can send the pieces that
            // are already available on disk as soon as possible
            this.world.executor.submit(new PriorityTask<>(TaskStage.ROUGH, pos));
        }
    }

    //
    //
    // rough tasks
    //
    //

    public Compressed<POS, P> roughGetPiece(PriorityTask<POS> root, POS pos) {
        if (this.world.canGenerateRough(pos)) {
            //the piece can be generated using the rough generator
            return this.roughGeneratePiece(root, pos);
        } else {
            //the piece is at a lower detail than 0, and low-resolution generation is not an option
            //this will generate the piece and all pieces below it down to level 0 until the piece can be "generated" from scaled data
            return this.roughScalePiece(root, pos);
        }
    }

    public Compressed<POS, P> roughGeneratePiece(PriorityTask<POS> root, POS pos) {
        checkArg(this.world.canGenerateRough(pos), "cannot do rough generation at %s!", pos);
        this.world.executor().checkForHigherPriorityWork(root);

        long newTimestamp = Compressed.VALUE_ROUGH_COMPLETE;
        Compressed<POS, P> compressedPiece = this.world.getPieceCachedOrLoad(pos);
        if (compressedPiece.timestamp() >= newTimestamp) { //break out early if piece is already done or newer
            return compressedPiece;
        }

        SimpleRecycler<P> pieceRecycler = this.world.mode().pieceRecycler();
        P piece = pieceRecycler.allocate();
        try {
            //generate piece
            long extra = this.world.generatorRough().generate(pos, piece);
            if (compressedPiece.set(newTimestamp, piece, extra)) { //only notify world if the piece was changed
                this.world.pieceChanged(compressedPiece);
            }
        } finally {
            pieceRecycler.release(piece);
        }

        return compressedPiece;
    }

    public Compressed<POS, P> roughScalePiece(PriorityTask<POS> root, POS pos) {
        this.world.executor().checkForHigherPriorityWork(root);

        long newTimestamp = Compressed.VALUE_ROUGH_COMPLETE;
        Compressed<POS, P> compressedPiece = this.world.getPieceCachedOrLoad(pos);
        if (compressedPiece.timestamp() >= newTimestamp) { //break out early if piece is already done or newer
            return compressedPiece;
        }

        //generate scale inputs
        List<POS> srcPositions = this.world.scaler().inputs(pos).collect(Collectors.toList());
        List<Compressed<POS, P>> compressedSrcs = new ArrayList<>(srcPositions.size());
        for (POS srcPosition : srcPositions) {
            //don't request the piece itself to be assembled, we only need piece data for scaling
            compressedSrcs.add(this.roughGetPiece(root, srcPosition));
        }

        //inflate sources
        SimpleRecycler<P> pieceRecycler = this.world.mode().pieceRecycler();
        P[] srcs = uncheckedCast(this.world.mode().pieceArray(compressedSrcs.size()));
        for (int i = 0; i < compressedSrcs.size(); i++) {
            srcs[i] = compressedSrcs.get(i).inflate(pieceRecycler);
        }
        P dst = pieceRecycler.allocate();

        try {
            //actually do scaling
            long extra = this.world.scaler().scale(srcs, dst);
            if (compressedPiece.set(newTimestamp, dst, extra)) {
                this.world.pieceChanged(compressedPiece);
            }
        } finally {
            pieceRecycler.release(dst);
            for (P src : srcs) {
                if (src != null) {
                    pieceRecycler.release(src);
                }
            }
        }

        return compressedPiece;
    }
}
