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

package net.daporkchop.fp2.util.threading.executor;

import lombok.NonNull;
import net.daporkchop.fp2.util.threading.ConcurrentUnboundedPriorityBlockingQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class LazyPriorityExecutor<K extends LazyKey<K>> {
    @SuppressWarnings("unchecked")
    protected static final Comparator<LazyTask> COMPARATOR = (a, b) -> a.key() != null ? a.key().compareTo(b.key()) : -1;

    protected final Thread[] threads;

    protected final BlockingQueue<LazyTask<K, ?, ?>> queue;

    protected volatile boolean running = true;

    public LazyPriorityExecutor(int threads, @NonNull ThreadFactory threadFactory) {
        this.queue = new ConcurrentUnboundedPriorityBlockingQueue<>(uncheckedCast(COMPARATOR));

        this.threads = new Thread[positive(threads, "threads")];

        Runnable worker = new Worker();
        for (int i = 0; i < threads; i++) {
            (this.threads[i] = threadFactory.newThread(worker)).start();
        }
    }

    public void submit(@NonNull LazyTask<K, ?, ?> task) {
        this.queue.add(task);
    }

    public void submit(@NonNull Collection<LazyTask<K, ?, ?>> tasks) {
        this.queue.addAll(tasks);
    }

    public void shutdown() {
        this.running = false;

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
    }

    protected class Worker implements Runnable {
        @Override
        public void run() {
            while (LazyPriorityExecutor.this.running) {
                try {
                    this.runLazy(LazyPriorityExecutor.this.queue.take());
                } catch (InterruptedException e) {
                    //gracefully exit on interrupt
                } catch (Exception e) {
                    LOGGER.error(Thread.currentThread().getName(), e);
                }
            }
        }

        protected void runSingle() throws InterruptedException {
        }

        protected <T, R> void runLazy(@NonNull LazyTask<K, T, R> task) throws InterruptedException {
            try {
                List<T> params = this.runBefore(task.before(task.key().raiseTie()).collect(Collectors.toList()));

                R val = task.run(params, LazyPriorityExecutor.this);
                if (val != null) {
                    task.setSuccess(val);
                }
            } catch (InterruptedException e) {
                throw e; //rethrow
            } catch (Exception e) {
                LOGGER.error(Thread.currentThread().getName(), e);
                task.setFailure(e);
            }
        }

        protected <T> List<T> runBefore(@NonNull List<LazyTask<K, ?, T>> tasks) throws Exception {
            if (tasks.isEmpty()) {
                return Collections.emptyList();
            }

            LazyPriorityExecutor.this.queue.addAll(tasks);
            try {
                do {
                    LazyTask<K, ?, ?> task = LazyPriorityExecutor.this.queue.poll(50L, TimeUnit.MILLISECONDS);
                    if (task != null)   {
                        this.runLazy(task);
                    }
                } while (this.areAnyIncomplete(tasks));
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    LazyPriorityExecutor.this.queue.removeAll(tasks.stream()
                            .filter(f -> !f.isDone())
                            .peek(LazyTask::cancel)
                            .collect(Collectors.toList()));
                }
                throw e;
            }

            List<T> list = new ArrayList<>(tasks.size());
            for (int i = 0, size = tasks.size(); i < size; i++) {
                list.add(Objects.requireNonNull(tasks.get(i).get()));
            }
            return list;
        }

        protected <V> boolean areAnyIncomplete(@NonNull List<LazyTask<K, ?, V>> tasks) {
            for (int i = 0, size = tasks.size(); i < size; i++) {
                if (!tasks.get(i).isDone()) {
                    return true;
                }
            }
            return false;
        }
    }
}
