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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.server.AbstractFarWorld;
import net.daporkchop.fp2.strategy.base.server.TaskKey;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.threading.executor.LazyTask;
import net.daporkchop.lib.concurrent.future.DefaultPFuture;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractPieceTask<POS extends IFarPos, P extends IFarPiece<POS>, T, R> extends DefaultPFuture<R> implements LazyTask<TaskKey, T, R> {
    protected final AbstractFarWorld<POS, P> world;
    protected final TaskKey key;

    public AbstractPieceTask(@NonNull AbstractFarWorld<POS, P> world, @NonNull TaskKey key) {
        super(world.nettyExecutor());

        this.world = world;
        this.key = key;
    }

    @Override
    public AbstractPieceTask<POS, P, T, R> setSuccess(R result) {
        super.setSuccess(result);
        return this;
    }

    @Override
    public AbstractPieceTask<POS, P, T, R> setFailure(Throwable cause) {
        super.setFailure(cause);
        return this;
    }
}
