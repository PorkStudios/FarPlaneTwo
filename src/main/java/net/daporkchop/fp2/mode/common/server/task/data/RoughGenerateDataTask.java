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

package net.daporkchop.fp2.mode.common.server.task.data;

import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.piece.IFarData;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.common.server.TaskKey;
import net.daporkchop.fp2.mode.common.server.TaskStage;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.executor.LazyPriorityExecutor;
import net.daporkchop.fp2.util.threading.executor.LazyTask;

import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class RoughGenerateDataTask<POS extends IFarPos, P extends IFarPiece, D extends IFarData>
        extends AbstractDataTask<POS, P, D, Void> {
    protected final boolean inaccurate;

    public RoughGenerateDataTask(@NonNull AbstractFarWorld<POS, P, D> world, @NonNull TaskKey key, @NonNull POS pos, @NonNull TaskStage requestedBy) {
        super(world, key, pos, requestedBy);

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
    public Compressed<POS, D> run(@NonNull List<Void> params, @NonNull LazyPriorityExecutor<TaskKey> executor) throws Exception {
        Compressed<POS, D> compressedData = this.world.getRawDataBlocking(this.pos);
        long newTimestamp = this.inaccurate && this.world.refine()
                ? Compressed.valueRough(this.pos.level()) //if the piece is inaccurate, it will need to be re-generated later based on scaled data
                : Compressed.VALUE_ROUGH_COMPLETE;
        if (compressedData.timestamp() >= newTimestamp) {
            return compressedData;
        }

        compressedData.writeLock().lock();
        try {
            if (compressedData.timestamp() >= newTimestamp) {
                return compressedData;
            }

            SimpleRecycler<D> builderRecycler = uncheckedCast(this.pos.mode().builderRecycler());
            D builder = builderRecycler.allocate();
            try {
                builder.reset(); //ensure builder is reset

                this.world.generatorRough().generate(this.pos, builder);
                piece.set(newTimestamp, builder);
            } finally {
                builderRecycler.release(builder);
            }
        } finally {
            compressedData.writeLock().unlock();
        }
        return compressedData;
    }
}
