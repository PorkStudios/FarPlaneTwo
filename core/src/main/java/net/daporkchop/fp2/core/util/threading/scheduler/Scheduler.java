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

package net.daporkchop.fp2.core.util.threading.scheduler;

import lombok.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A scheduler is initialized with a single {@link Function} which maps from a parameter type to a return type. The user may supply parameters to the scheduler, which will
 * be processed by the {@link Function} and the return value used to complete the task's {@link CompletableFuture}.
 * <p>
 * Schedulers enforce <strong>distinct parameters</strong>:<br>
 * If a parameter which is already queued for execution is submitted again, that parameter will <i>not</i> be enqueued a second time. A {@link CompletableFuture}
 * will be returned which references the original task, and the task will not be removed from the queue until it's completed or <i>all</i> of the
 * {@link CompletableFuture}s referencing it have been cancelled.<br>
 * If a parameter which has already begun execution is submitted again, that parameter <i>will</i> be enqueued a second time and a new {@link CompletableFuture}
 * returned. It is also guaranteed that the newly submitted task will not begin execution until the previous one is complete.<br>
 * As implementations are free to re-use the same {@link CompletableFuture} instance for all occurrences of the given parameter value, attempting to invoke
 * {@link CompletableFuture#cancel(boolean)} on the {@link CompletableFuture} returned by {@link #schedule(Object)} more than once will result in undefined behavior.
 * <p>
 * Schedulers permit <strong>task recursion</strong>:<br>
 * The {@link Function} is permitted to schedule new tasks and wait for their completion using the {@link #scatterGather(List)} method. This allows schedulers to
 * operate as a simple {@link ForkJoinPool}, but comes with a major caveat: there is a possibility for the dependency graph to contain loop(s), a scenario impossible
 * for a {@link ForkJoinPool}. Therefore, dependency loops will result in undefined behavior. Implementations may impose further restrictions on recursive parameter
 * values.<br>
 * Implementing a recursive task by {@link CompletableFuture#join()}ing the {@link CompletableFuture} returned by {@link #schedule(Object)} is not allowed and will
 * result in undefined behavior.
 *
 * @author DaPorkchop_
 */
public interface Scheduler<P, V> extends AutoCloseable {
    /**
     * Schedules the given parameter value for execution.
     *
     * @param param the parameter to be executed
     * @return a {@link CompletableFuture} which will be completed with the result of passing the given parameter to the scheduler function
     */
    CompletableFuture<V> schedule(@NonNull P param);

    /**
     * Schedules multiple parameters for execution, then blocks until all results are available.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * List<CompletableFuture<V>> futures = params.stream().map(scheduler::schedule).collect(Collectors.toList());
     * try {
     *     return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
     * } finally {
     *     futures.forEach(future -> future.cancel(false));
     * }
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param params the parameters
     * @return the resulting values
     */
    default List<V> scatterGather(@NonNull List<P> params) {
        List<CompletableFuture<V>> futures = params.stream().map(this::schedule).collect(Collectors.toList());
        try {
            return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        } finally {
            futures.forEach(f -> f.cancel(false));
        }
    }

    /**
     * Closes this scheduler.
     * <p>
     * This method will block until all worker threads have been shut down.
     */
    @Override
    void close();
}
