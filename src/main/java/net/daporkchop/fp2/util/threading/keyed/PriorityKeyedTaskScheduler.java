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

package net.daporkchop.fp2.util.threading.keyed;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.EqualsTieBreakComparator;
import net.daporkchop.fp2.util.threading.ConcurrentUnboundedPriorityBlockingQueue;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class PriorityKeyedTaskScheduler<K> implements KeyedTaskScheduler<K> {
    protected final Thread[] threads;
    protected final LoadingCache<K, Queue<QueuedTaskWrapper>> queueCache = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<K, Queue<QueuedTaskWrapper>>() {
                @Override
                public Queue<QueuedTaskWrapper> load(K key) throws Exception {
                    return new ArrayDeque<>();
                }
            });
    protected final BlockingQueue<QueuedTaskWrapper> queue;
    protected final LoadingCache<K, Executor> executorCache = CacheBuilder.newBuilder() //avoid allocating tons of lambda objects
            .weakValues()
            .build(new CacheLoader<K, Executor>() {
                @Override
                public Executor load(K key) throws Exception {
                    return task -> PriorityKeyedTaskScheduler.this.submit(key, task);
                }
            });
    protected volatile boolean running = true;

    public PriorityKeyedTaskScheduler(int threads, @NonNull ThreadFactory threadFactory) {
        this.queue = new ConcurrentUnboundedPriorityBlockingQueue<>(new EqualsTieBreakComparator<>(null, false, true));

        this.threads = new Thread[positive(threads, "threads")];

        Runnable worker = new Worker();
        for (int i = 0; i < threads; i++) {
            (this.threads[i] = threadFactory.newThread(worker)).start();
        }
    }

    @Override
    public void submit(@NonNull K key, @NonNull Runnable taskIn) {
        Queue<QueuedTaskWrapper> queue = this.queueCache.getUnchecked(key);
        QueuedTaskWrapper task = new QueuedTaskWrapper(key, taskIn, queue);
        synchronized (queue) {
            queue.add(task);
            if (queue.size() == 1) {
                //the only task in the queue is this one, so let's submit it now
                this.queue.add(task);
            }
        }
    }

    @Override
    public Executor keyed(@NonNull K key) {
        return this.executorCache.getUnchecked(key);
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

    @RequiredArgsConstructor
    protected final class QueuedTaskWrapper implements Runnable, Comparable<QueuedTaskWrapper> {
        @NonNull
        protected final K key;
        @NonNull
        protected final Runnable task;
        @NonNull
        protected final Queue<QueuedTaskWrapper> queue;

        @Override
        public void run() {
            try {
                this.task.run();
            } finally {
                synchronized (this.queue) {
                    QueuedTaskWrapper next = this.queue.poll();
                    checkState(next == this, "this task wasn't the next one in the queue!");
                    next = this.queue.peek();
                    if (next != null) {
                        PriorityKeyedTaskScheduler.this.queue.add(next);
                    }
                }
            }
        }

        @Override
        public int compareTo(QueuedTaskWrapper o) {
            return PorkUtil.<Comparable<K>>uncheckedCast(this.key).compareTo(o.key);
        }
    }

    protected class Worker implements Runnable {
        @Override
        public void run() {
            while (PriorityKeyedTaskScheduler.this.running) {
                try {
                    PriorityKeyedTaskScheduler.this.queue.take().run();
                } catch (InterruptedException e) {
                    //gracefully exit on interrupt
                } catch (Exception e) {
                    LOGGER.error(Thread.currentThread().getName(), e);
                }
            }

            //work off queue
            Runnable task;
            while ((task = PriorityKeyedTaskScheduler.this.queue.poll()) != null) {
                try {
                    task.run();
                } catch (Exception e) {
                    LOGGER.error(Thread.currentThread().getName(), e);
                }
            }
        }
    }
}
