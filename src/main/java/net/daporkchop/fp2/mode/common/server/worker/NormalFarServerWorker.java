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

import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.piece.IFarPieceData;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.PriorityTask;
import net.daporkchop.fp2.util.SimpleRecycler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class NormalFarServerWorker<POS extends IFarPos, P extends IFarPiece, D extends IFarPieceData> extends AbstractFarServerWorker<POS, P, D, P> {
    public NormalFarServerWorker(AbstractFarWorld<POS, P, D> world) {
        super(world);
    }

    @Override
    public SimpleRecycler<P> scaleInputRecycler() {
        return this.world.mode().pieceRecycler();
    }

    @Override
    public P[] scaleInputArray(int length) {
        return uncheckedCast(this.world.mode().pieceArray(length));
    }

    //

    @Override
    public void roughGeneratePiece0(POS pos, Compressed<POS, P> compressedPiece, long newTimestamp) {
        SimpleRecycler<P> pieceRecycler = this.world.mode().pieceRecycler();
        P piece = pieceRecycler.allocate();
        try {
            long extra = this.world.generatorRough().generate(pos, piece, null, null);
            if (compressedPiece.set(newTimestamp, piece, extra)) {
                this.world.pieceChanged(compressedPiece);
            }
        } finally {
            pieceRecycler.release(piece);
        }
    }

    //

    @Override
    public List<Compressed<POS, P>> roughScaleInputs0(PriorityTask<POS> root, POS pos) {
        List<POS> inputPositions = this.world.pieceScaler().inputs(pos).collect(Collectors.toList());
        List<Compressed<POS, P>> out = new ArrayList<>(inputPositions.size());

        for (POS inputPos : inputPositions) {
            Compressed<POS, P> compressedInputPiece = this.world.getRawPieceBlocking(inputPos);
            if (inputPos.level() == 0) {
                this.roughGeneratePiece(root, inputPos, compressedInputPiece);
            } else {
                this.roughScalePiece(root, inputPos, compressedInputPiece);
            }
            out.add(compressedInputPiece);
        }
        return out;
    }

    @Override
    public void roughScalePiece0(POS pos, Compressed<POS, P> compressedPiece, P[] srcs, long newTimestamp) {
        SimpleRecycler<P> pieceRecycler = this.world.mode().pieceRecycler();
        P piece = pieceRecycler.allocate();
        try {
            long extra = this.world.pieceScaler().scale(srcs, piece);
            if (compressedPiece.set(newTimestamp, piece, extra)) {
                this.world.pieceChanged(compressedPiece);
            }
        } finally {
            pieceRecycler.release(piece);
        }
    }
}
