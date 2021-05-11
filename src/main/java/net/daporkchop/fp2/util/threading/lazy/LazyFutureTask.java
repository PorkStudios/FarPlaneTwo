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

package net.daporkchop.fp2.util.threading.lazy;

import io.netty.util.concurrent.EventExecutor;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.lib.concurrent.PExecutors;
import net.daporkchop.lib.concurrent.future.DefaultPFuture;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link CompletableFuture} task which is executed lazily.
 *
 * @author DaPorkchop_
 */
public abstract class LazyFutureTask<V> extends CompletableFuture<V> implements RunnableFuture<V> {
    protected static final long STARTED_OFFSET = PUnsafe.pork_getOffset(LazyFutureTask.class, "started");

    public static <V> List<V> scatterGather(@NonNull LazyFutureTask<V>... tasks) {
        //first pass: invoke all tasks that haven't been invoked yet
        for (LazyFutureTask<V> task : tasks) {
            task.run();
        }

        //second pass: gather all results into a list
        return Stream.of(tasks).map(LazyFutureTask::join).collect(Collectors.toList());
    }

    protected volatile int started = 0;

    /**
     * Executes this task if it hasn't begun execution yet.
     */
    @Override
    public void run() {
        //attempt to start execution
        if (PUnsafe.compareAndSwapInt(this, STARTED_OFFSET, 0, 1)) {
            try {
                //compute return value and set return status
                this.complete(this.compute());
            } catch (Throwable t) {
                //complete self with exceptional return code
                this.completeExceptionally(t);
            }
        }
    }

    protected abstract V compute();

    @Override
    public V join() {
        if (this.started == 0) { //consider running the task if it hasn't run yet
            this.run();
        }

        return super.join();
    }
}
