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

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RunnableFuture;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Simple base implementation of {@link MarkedFutureExecutor}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractMarkedFutureExecutor implements MarkedFutureExecutor {
    @NonNull
    protected final Thread thread;

    protected final Queue<Task<?>> queue = new LinkedList<>();

    protected volatile boolean running = false;

    @Override
    public synchronized void cancelAll(@NonNull Object marker) {
        checkState(this.running, "not running");

        //cancel and remove all tasks scheduled by threads in this group
        for (Iterator<Task<?>> itr = this.queue.iterator(); itr.hasNext(); ) {
            Task<?> task = itr.next();
            if (marker.equals(task.marker)) {
                task.cancel(false);
                itr.remove();
            }
        }
    }

    @Override
    public synchronized CompletableFuture<Void> run(@NonNull Object marker, @NonNull Runnable runnable) {
        checkState(this.running, "not running");

        Task<Void> task = new Task<>(marker, runnable);
        this.queue.add(task);
        return task;
    }

    @Override
    public synchronized <V> CompletableFuture<V> supply(@NonNull Object marker, @NonNull Supplier<V> supplier) {
        checkState(this.running, "not running");

        Task<V> task = new Task<>(marker, supplier);
        this.queue.add(task);
        return task;
    }

    /**
     * Does a single piece of work.
     *
     * @return whether or not there is more work remaining in the queue
     */
    public boolean doWork() {
        checkState(Thread.currentThread() == this.thread, "thread %s isn't allowed to do this executor's work (expected %s)", Thread.currentThread(), this.thread);

        Task<?> task;
        synchronized (this) {
            task = this.queue.poll();
        }

        if (task != null) {
            task.run();
            return !this.queue.isEmpty();
        } else {
            return false; //task queue is empty, we're done for now
        }
    }

    /**
     * Does all of the work remaining in the task queue.
     */
    public void doAllWork() {
        checkState(Thread.currentThread() == this.thread, "thread %s isn't allowed to do this executor's work (expected %s)", Thread.currentThread(), this.thread);

        RuntimeException root = null; //this exception will contain all exceptions thrown by tasks, if any
        while (true) {
            try {
                if (!this.doWork()) { //return value of false indicates that the queue is empty
                    break;
                }
            } catch (Throwable t) {
                if (root == null) { //create new root exception
                    root = new RuntimeException("uncaught exception(s) while handling scheduled client tasks");
                }
                root.addSuppressed(t);
            }
        }

        if (root != null) { //at least one exception was caught, throw 'em!
            throw root;
        }
    }

    protected synchronized void start() {
        checkState(!this.running, "already running");
        this.running = true;
    }

    @Override
    public synchronized void close() {
        checkState(this.running, "not running");
        this.running = false;

        if (!this.queue.isEmpty()) {
            Constants.bigWarning("%s: %d elements left in the queue after shutdown!", this, this.queue.size());
        }
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor
    protected static class Task<V> extends CompletableFuture<V> implements RunnableFuture<V> {
        @NonNull
        protected Object marker;
        @NonNull
        protected Object action;

        @Override
        public synchronized void run() {
            if (this.isCancelled()) { //if the task was cancelled, we don't have to do anything
                return;
            }

            try {
                if (this.action instanceof Runnable) {
                    ((Runnable) this.action).run();
                    this.complete(null);
                } else if (this.action instanceof Supplier) {
                    this.complete(uncheckedCast(((Supplier) this.action).get()));
                } else {
                    throw new IllegalArgumentException(PorkUtil.className(this.action));
                }
            } catch (Throwable t) {
                this.completeExceptionally(t);
            } finally {
                this.marker = null;
                this.action = null;
            }
        }

        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            if (super.cancel(mayInterruptIfRunning)) {
                //set marker and action to null to allow them to be garbage-collected
                this.marker = null;
                this.action = null;
                return true;
            } else {
                return false;
            }
        }
    }
}
