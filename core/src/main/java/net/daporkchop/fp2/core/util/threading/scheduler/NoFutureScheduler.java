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
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerGroup;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerGroupBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link Scheduler} which always returns {@code null} in place of a {@link CompletableFuture}.
 * <p>
 * Due to the lack of {@link CompletableFuture}s, there are a number of major restrictions:<ul>
 * <li>it is impossible to cancel an already scheduled task</li>
 * <li>it is impossible to block until a given task is complete</li>
 * <li>recursive execution and all use of {@link #scatterGather(List)} is impossible, since there's no way of knowing when a task is complete</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
public class NoFutureScheduler<P> implements Scheduler<P, Void>, Runnable {
    protected static final Object ADDED_STATE = new Object[0];
    protected static final Object RUNNING_STATE = new Object[0];

    protected final Map<P, Object> parameterStates = this.createParameterStateMap();
    protected final BlockingQueue<P> queue = this.createTaskQueue();

    protected final Consumer<P> function;

    protected final WorkerGroup group;
    protected volatile boolean running = true;

    public NoFutureScheduler(@NonNull Consumer<P> function, @NonNull WorkerGroupBuilder builder) {
        this.function = function;

        this.group = builder.build(this);
    }

    protected Map<P, Object> createParameterStateMap() {
        return new ConcurrentHashMap<>();
    }

    protected BlockingQueue<P> createTaskQueue() {
        return new LinkedBlockingQueue<>();
    }

    protected void enqueue(@NonNull P param) {
        checkState(this.queue.add(param), "unable to add %s to queue!", param);
    }

    @Override
    public CompletableFuture<Void> schedule(@NonNull P param) {
        //race to insert the parameter into the map with a value of ADDED. there are 3 possible return values:
        //  - null: we won the race, and the parameter will be added to the queue
        //  - ADDED: another thread won the race, so the parameter has already been added to the queue
        //  - RUNNING: a worker has already started processing this parameter. we don't want to add it to the queue (to prevent the same parameter being executed
        //             multiple times at once), but the worker will notice the state change to ADDED and re-add it to the queue once processing is complete.
        if (this.parameterStates.put(param, ADDED_STATE) == null) {
            this.enqueue(param);
        }

        return null; //always return null lol
    }

    @Override
    public List<Void> scatterGather(@NonNull List<P> params) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public void run() {
        try {
            while (this.running) {
                //poll the queue, but don't wait indefinitely because we need to be able to exit if the executor stops running.
                // we don't want to use interrupts because they can cause unwanted side-effects (such as closing NIO channels).
                P param = this.queue.poll(1L, TimeUnit.SECONDS);

                if (param == null) { //the queue was empty
                    continue;
                }

                try {
                    //mark the parameter as "running" so that we'll be able to determine whether or not it's been re-scheduled while the task was running
                    checkState(this.parameterStates.replace(param, ADDED_STATE, RUNNING_STATE));

                    //pass the parameter to the function
                    this.function.accept(param);
                } catch (Throwable t) {
                    this.group.manager().handle(t);
                } finally {
                    //if the parameter was re-scheduled while running the function, it'll have been mapped to ADDED again and this removal will fail. it's our
                    //  responsibility to add it to the queue again now that execution has finished.
                    if (!this.parameterStates.remove(param, RUNNING_STATE)) {
                        this.enqueue(param);
                    }
                }
            }
        } catch (Exception e) { //should be impossible, but whatever
            fp2().log().error(Thread.currentThread().getName(), e);
        }
    }

    @Override
    public void close() {
        //notify workers that we're shutting down
        this.running = false;

        //wait until all the workers have exited
        this.group.close();
    }
}
