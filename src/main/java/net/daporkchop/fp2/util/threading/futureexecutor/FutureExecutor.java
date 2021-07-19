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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * An {@link Executor} which returns a {@link CompletableFuture} when submitting tasks.
 *
 * @author DaPorkchop_
 */
public interface FutureExecutor extends Executor, AutoCloseable {
    @Override
    default void execute(@NonNull Runnable runnable) {
        this.run(runnable);
    }

    /**
     * Executes the given {@link Runnable} on this executor.
     *
     * @param runnable the {@link Runnable} to run
     * @return a {@link CompletableFuture} which will be completed once the {@link Runnable} has returned
     */
    CompletableFuture<Void> run(@NonNull Runnable runnable);

    /**
     * Executes the given {@link Supplier} on this executor.
     *
     * @param supplier the {@link Supplier} to run
     * @return a {@link CompletableFuture} which will be completed with the {@link Supplier}'s return value
     */
    <V> CompletableFuture<V> supply(@NonNull Supplier<V> supplier);

    /**
     * Shuts down this executor.
     * <p>
     * This will cancel all tasks, and prevent any further ones from being submitted.
     */
    @Override
    void close();
}
