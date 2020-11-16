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
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.piece.IFarData;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.TaskKey;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.mode.common.server.task.data.GetDataTask;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public class AssemblePieceTask<POS extends IFarPos, P extends IFarPiece, D extends IFarData>
        extends AbstractPieceTask<POS, P, D, Compressed<POS, D>> {
    public AssemblePieceTask(@NonNull AbstractFarWorld<POS, P, D> world, @NonNull TaskKey key, @NonNull POS pos, @NonNull TaskStage requestedBy) {
        super(world, key, pos, requestedBy);
    }

    @Override
    public Stream<? extends LazyTask<TaskKey, ?, Compressed<POS, D>>> before(@NonNull TaskKey key) throws Exception {
        return Stream.of(new GetDataTask<>(this.world, key, this.pos, this.requestedBy));
    }

    @Override
    public Compressed<POS, P> run(@NonNull List<Compressed<POS, D>> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        Compressed<POS, D> compressedData = params.get(0);
        Compressed<POS, P> compressedPiece = this.world.getRawPieceBlocking(this.pos);

        boolean changed;
        compressedData.readLock().lock();
        try {
            long newTimestamp = compressedData.timestamp();
            if (compressedPiece.timestamp() >= newTimestamp) {
                return compressedPiece;
            }

            SimpleRecycler<D> dataRecycler = this.pos.mode().dataRecycler();
            SimpleRecycler<P> pieceRecycler = this.pos.mode().pieceRecycler();
            D data = compressedData.inflate(dataRecycler);
            P piece = pieceRecycler.allocate();
            try {
                long extra = this.world.assembler().assemble(data, piece);
                changed = compressedPiece.set(newTimestamp, piece, extra);
            } finally {
                pieceRecycler.release(piece);
                dataRecycler.release(data);
            }
        } finally {
            compressedData.readLock().unlock();
        }

        if (changed) {
            this.world.pieceChanged(compressedPiece);
        }
        return compressedPiece;
    }
}
