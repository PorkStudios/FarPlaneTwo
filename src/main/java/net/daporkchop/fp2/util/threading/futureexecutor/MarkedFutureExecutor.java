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
import java.util.function.Supplier;

/**
 * A {@link FutureExecutor} whose tasks are categorized using an arbitrary object as a marker.
 *
 * @author DaPorkchop_
 */
public interface MarkedFutureExecutor extends FutureExecutor {
    Object DEFAULT_MARKER = new Object[0];

    /**
     * @deprecated use {@link #run(Object, Runnable)}
     */
    @Override
    @Deprecated
    default CompletableFuture<Void> run(@NonNull Runnable runnable) {
        return this.run(DEFAULT_MARKER, runnable);
    }

    /**
     * @deprecated use {@link #supply(Object, Supplier)}
     */
    @Override
    default <V> CompletableFuture<V> supply(@NonNull Supplier<V> supplier) {
        return this.supply(DEFAULT_MARKER, supplier);
    }

    /**
     * Executes the given {@link Runnable} on this executor.
     *
     * @param marker   the task's marker
     * @param runnable the {@link Runnable} to run
     * @return a {@link CompletableFuture} which will be completed once the {@link Runnable} has returned
     */
    CompletableFuture<Void> run(@NonNull Object marker, @NonNull Runnable runnable);

    /**
     * Executes the given {@link Supplier} on this executor.
     *
     * @param marker   the task's marker
     * @param supplier the {@link Supplier} to run
     * @return a {@link CompletableFuture} which will be completed with the {@link Supplier}'s return value
     */
    <V> CompletableFuture<V> supply(@NonNull Object marker, @NonNull Supplier<V> supplier);

    /**
     * Cancels all pending tasks with the given marker.
     * <p>
     * Markers are compared using {@link Object#equals(Object)}.
     *
     * @param marker the marker
     */
    void cancelAll(@NonNull Object marker);
}
