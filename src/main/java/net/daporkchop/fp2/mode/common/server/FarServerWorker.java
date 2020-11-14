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
import net.daporkchop.fp2.mode.api.piece.IFarData;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.util.SimpleRecycler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FarServerWorker<POS extends IFarPos, P extends IFarPiece, D extends IFarData> implements Consumer<PriorityTask<POS>> {
    @NonNull
    protected final AbstractFarWorld<POS, P, D> world;

    @Override
    public void accept(PriorityTask<POS> task) {
        switch (task.stage()) {
            case LOAD:
                this.loadPiece(task, task.pos());
                break;
        }
    }

    //

    public void loadPiece(PriorityTask<POS> root, POS pos) {
        Compressed<POS, P> compressedPiece = this.world.getRawPieceBlocking(pos);
        if (compressedPiece.isGenerated()) {
            //this adds the piece to the cache, unmarks it as not done and notifies the player tracker
            this.world.pieceChanged(compressedPiece);
        } else {
            //the piece has not been fully generated yet
            this.roughGetPiece(root, pos);
        }
    }

    //
    //
    // rough tasks
    //
    //

    public Compressed<POS, P> roughGetPiece(PriorityTask<POS> root, POS pos) {
        this.world.executor().checkForHigherPriorityWork(root);

        long newTimestamp = Compressed.VALUE_ROUGH_COMPLETE;
        Compressed<POS, P> compressedPiece = this.world.getRawPieceBlocking(pos);
        if (compressedPiece.timestamp() >= newTimestamp) { //break out early if piece is already done or newer
            return compressedPiece;
        }

        //this may generate or scale the data
        Compressed<POS, D> compressedData = this.roughGetData(root, pos);

        SimpleRecycler<D> dataRecycler = this.world.mode().dataRecycler();
        //this will cause extra compression and decompression if roughGetData() caused the data to be newly generated...
        D data = compressedData.inflate(dataRecycler);
        SimpleRecycler<P> pieceRecycler = this.world.mode().pieceRecycler();
        P piece = pieceRecycler.allocate();
        try {
            //assemble piece
            long extra = this.world.assembler().assemble(data, piece);

            if (compressedPiece.set(newTimestamp, piece, extra)) { //notify world of piece change
                this.world.pieceChanged(compressedPiece);
            }
        } finally {
            pieceRecycler.release(piece);
            dataRecycler.release(data);
        }

        return compressedPiece;
    }

    public Compressed<POS, D> roughGetData(PriorityTask<POS> root, POS pos) {
        if (pos.level() == 0 || this.world.lowResolution()) {
            //the piece can be generated using the rough generator
            return this.roughGenerateData(root, pos);
        } else {
            //the piece is at a lower detail than 0, and low-resolution generation is not an option
            //this will generate the piece and all pieces below it down to level 0 until the piece can be "generated" from scaled data
            return this.roughScaleData(root, pos);
        }
    }

    public Compressed<POS, D> roughGenerateData(PriorityTask<POS> root, POS pos) {
        this.world.executor().checkForHigherPriorityWork(root);

        long newTimestamp = Compressed.VALUE_ROUGH_COMPLETE;
        Compressed<POS, D> compressedData = this.world.getRawDataBlocking(pos);
        if (compressedData.timestamp() >= newTimestamp) { //break out early if piece is already done or newer
            return compressedData;
        }

        SimpleRecycler<D> dataRecycler = this.world.mode().dataRecycler();
        D data = dataRecycler.allocate();
        try {
            //generate data
            this.world.generatorRough().generate(pos, data);

            if (compressedData.set(newTimestamp, data, 0L)) { //only assemble piece if the data was changed
                this.world.dataChanged(compressedData);
            }
        } finally {
            dataRecycler.release(data);
        }

        return compressedData;
    }

    public Compressed<POS, D> roughScaleData(PriorityTask<POS> root, POS pos) {
        this.world.executor().checkForHigherPriorityWork(root);

        long newTimestamp = Compressed.VALUE_ROUGH_COMPLETE;
        Compressed<POS, D> compressedData = this.world.getRawDataBlocking(pos);
        if (compressedData.timestamp() >= newTimestamp) { //break out early if piece is already done or newer
            return compressedData;
        }

        //generate scale inputs
        List<POS> srcPositions = this.world.scaler().inputs(pos).collect(Collectors.toList());
        List<Compressed<POS, D>> compressedSrcs = new ArrayList<>(srcPositions.size());
        for (POS srcPosition : srcPositions) {
            //don't request the piece itself to be assembled, we only need piece data for scaling
            compressedSrcs.add(this.roughGetData(root, srcPosition));
        }

        //inflate sources
        SimpleRecycler<D> dataRecycler = this.world.mode().dataRecycler();
        D[] srcs = uncheckedCast(this.world.mode().pieceDataArray(compressedSrcs.size()));
        for (int i = 0; i < compressedSrcs.size(); i++) {
            srcs[i] = compressedSrcs.get(i).inflate(dataRecycler);
        }
        D dst = dataRecycler.allocate();

        try {
            //actually do scaling
            this.world.scaler().scale(srcs, dst);

            if (compressedData.set(newTimestamp, dst, 0L)) {
                this.world.dataChanged(compressedData);
            }
        } finally {
            dataRecycler.release(dst);
            for (D src : srcs) {
                if (src != null) {
                    dataRecycler.release(src);
                }
            }
        }

        return compressedData;
    }
}
