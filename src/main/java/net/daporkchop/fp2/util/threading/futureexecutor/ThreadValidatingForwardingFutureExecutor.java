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
import java.util.function.Predicate;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Simple {@link FutureExecutor} implementation which delegates to a given {@link FutureExecutor}, while applying a sanity check to ensure only threads
 * matching a given {@link Predicate} are allowed to submit tasks.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class ThreadValidatingForwardingFutureExecutor implements FutureExecutor {
    @NonNull
    protected final FutureExecutor delegate;
    @NonNull
    protected final Predicate<Thread> filter;

    @Override
    public CompletableFuture<Void> run(@NonNull Runnable runnable) {
        checkState(this.filter.test(Thread.currentThread()), "thread %s isn't allowed to submit tasks to this executor!", this.filter);
        return this.delegate.run(runnable);
    }

    @Override
    public <V> CompletableFuture<V> supply(@NonNull Supplier<V> supplier) {
        checkState(this.filter.test(Thread.currentThread()), "thread %s isn't allowed to submit tasks to this executor!", this.filter);
        return this.delegate.supply(supplier);
    }

    @Override
    public void close() {
        this.delegate.close();
    }
}
