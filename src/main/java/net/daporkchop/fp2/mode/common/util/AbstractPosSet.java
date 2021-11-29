/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.mode.common.util;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.core.util.datastructure.SimpleSet;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base class for implementations of {@link SimpleSet} optimized specifically for a specific {@link IFarPos} type.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractPosSet<POS extends IFarPos> implements SimpleSet<POS> {
    protected final NDimensionalIntSet[] delegates;

    public AbstractPosSet(int dimensions, int maxLevels) {
        NDimensionalIntSet.Builder builder = Datastructures.INSTANCE.nDimensionalIntSet()
                .dimensions(dimensions).threadSafe(false);

        this.delegates = new NDimensionalIntSet[maxLevels];
        for (int level = 0; level < maxLevels; level++) {
            this.delegates[level] = builder.build();
        }
    }

    @Override
    public int refCnt() {
        return this.delegates[0].refCnt();
    }

    @Override
    public SimpleSet<POS> retain() throws AlreadyReleasedException {
        this.delegates[0].retain();
        return this;
    }

    @Override
    public boolean release() throws AlreadyReleasedException {
        NDimensionalIntSet[] delegates = this.delegates;

        if (delegates[0].release()) {
            for (int level = 1; level < delegates.length; level++) {
                checkState(delegates[level].release(), "failed to release delegate set at level #%d!", level);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long count() {
        return Stream.of(this.delegates).mapToLong(NDimensionalIntSet::count).sum();
    }

    @Override
    public boolean isEmpty() {
        return Stream.of(this.delegates).allMatch(NDimensionalIntSet::isEmpty);
    }

    @Override
    public void clear() {
        Stream.of(this.delegates).forEach(NDimensionalIntSet::clear);
    }

    @Override
    public abstract boolean add(@NonNull POS value);

    @Override
    public abstract boolean remove(@NonNull POS value);

    @Override
    public abstract boolean contains(@NonNull POS value);

    @Override
    public abstract void forEach(@NonNull Consumer<? super POS> callback);
}
