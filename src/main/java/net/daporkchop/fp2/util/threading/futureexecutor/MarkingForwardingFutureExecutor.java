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

package net.daporkchop.fp2.util.threading.futureexecutor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Simple {@link FutureExecutor} implementation which delegates to a given {@link MarkedFutureExecutor} while applying a unique marker.
 * <p>
 * This allows all submitted tasks to be cancelled immediately when this executor is shut down.
 * <p>
 * Newly created instances are assumed to be running immediately, even if their delegate executor isn't.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class MarkingForwardingFutureExecutor implements FutureExecutor {
    @NonNull
    protected final MarkedFutureExecutor delegate;

    protected final Object marker = new Object[0]; //arbitrary unique object

    protected volatile boolean running = true;

    @Override
    public synchronized CompletableFuture<Void> run(@NonNull Runnable runnable) {
        checkState(this.running, "not running");
        return this.delegate.run(this.marker, runnable);
    }

    @Override
    public synchronized <V> CompletableFuture<V> supply(@NonNull Supplier<V> supplier) {
        checkState(this.running, "not running");
        return this.delegate.supply(this.marker, supplier);
    }

    @Override
    public synchronized void close() {
        checkState(this.running, "not running");
        this.running = false;

        //cancel all tasks submitted by us
        this.delegate.cancelAll(this.marker);
    }
}
