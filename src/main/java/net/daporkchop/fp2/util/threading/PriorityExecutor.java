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

package net.daporkchop.fp2.util.threading;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.ObjIntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * {@link io.netty.util.concurrent.UnorderedThreadPoolEventExecutor} is ALMOST good enough, but not quite.
 * <p>
 * This is an executor that allows sorting tasks based on an arbitrary priority value.
 * <p>
 * Tasks without an explicitly set priority will be given minimum priority.
 *
 * @author DaPorkchop_
 */
public class PriorityExecutor implements Executor {
    protected static final long RUNNING_OFFSET = PUnsafe.pork_getOffset(PriorityExecutor.class, "running");

    protected static final int STATUS_ALIVE = 1;
    protected static final int STATUS_STOPPING = 2;
    protected static final int STATUS_CLOSED = 0;

    protected final Thread[] threads;
    protected final BlockingQueue<PrioritizedTask> queue = new PriorityBlockingQueue<>();

    protected volatile int running = STATUS_ALIVE;

    public PriorityExecutor(int corePoolSize) {
        this(corePoolSize, new DefaultThreadFactory(PriorityExecutor.class));
    }

    public PriorityExecutor(int corePoolSize, @NonNull ThreadFactory threadFactory) {
        this.threads = new Thread[positive(corePoolSize, "corePoolSize")];

        Runnable worker = () -> {
            try {
                while (this.running == STATUS_ALIVE) {
                    PrioritizedTask task = this.queue.poll(1L, TimeUnit.DAYS);
                    try {
                        task.task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                //silently stop
            }
        };
        for (int i = 0; i < corePoolSize; i++) {
            (this.threads[i] = threadFactory.newThread(worker)).start();
        }
    }

    @Override
    public void execute(@NonNull Runnable task) {
        this.execute(task, Integer.MAX_VALUE);
    }

    public void execute(@NonNull Runnable task, int priority) {
        checkState(this.running != STATUS_CLOSED, "not running!");
        this.queue.add(new PrioritizedTask(task, priority));
    }

    public Executor withPriority(int priority) {
        return task -> this.execute(task, priority);
    }

    public void shutdown(@NonNull ObjIntConsumer<Runnable> remainingTaskCallback) {
        checkState(PUnsafe.compareAndSwapInt(this, RUNNING_OFFSET, STATUS_ALIVE, STATUS_STOPPING), "already shut down!");

        //interrupt all workers
        for (Thread t : this.threads) {
            t.interrupt();
        }

        //wait for all workers to shut down
        for (Thread t : this.threads) {
            do {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } while (t.isAlive());
        }

        this.running = STATUS_CLOSED;

        //hand queued tasks to callback
        this.queue.forEach(task -> remainingTaskCallback.accept(task.task, task.priority));
    }

    @RequiredArgsConstructor
    private static class PrioritizedTask implements Comparable<PrioritizedTask> {
        @NonNull
        protected final Runnable task;
        protected final int priority;

        @Override
        public int compareTo(PrioritizedTask o) {
            return Integer.compare(this.priority, o.priority);
        }
    }
}
