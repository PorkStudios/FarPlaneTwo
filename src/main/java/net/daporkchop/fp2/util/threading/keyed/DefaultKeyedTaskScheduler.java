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

package net.daporkchop.fp2.util.threading.keyed;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.primitive.map.concurrent.ObjObjConcurrentHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
//nothing here needs to be manually synchronized - it's all accessed from inside (ObjObj)ConcurrentHashMap#compute, which synchronizes on the bucket
public class DefaultKeyedTaskScheduler<K> extends AbstractReleasable implements KeyedTaskScheduler<K> {
    protected final Map<K, TaskQueue> queues;
    protected final BlockingQueue<TaskQueue> queue;

    protected final Thread[] threads;
    protected volatile boolean running = true;

    public DefaultKeyedTaskScheduler(int threads, @NonNull ThreadFactory threadFactory) {
        this.queues = this.createQueues();
        this.queue = this.createQueue();

        this.threads = new Thread[positive(threads, "threads")];
        Runnable worker = new Worker();
        for (int i = 0; i < threads; i++) {
            (this.threads[i] = threadFactory.newThread(worker)).start();
        }
    }

    protected Map<K, TaskQueue> createQueues() {
        return new ObjObjConcurrentHashMap<>();
    }

    protected BlockingQueue<TaskQueue> createQueue() {
        return new LinkedBlockingQueue<>();
    }

    protected TaskQueue createQueue(@NonNull K key, @NonNull Runnable task) {
        return new TaskQueue(key, task);
    }

    @Override
    public void submit(@NonNull K keyIn, @NonNull Runnable task) {
        this.assertNotReleased();
        this.queues.compute(keyIn, (key, queue) -> {
            if (queue == null) { //create new queue
                queue = this.createQueue(key, task);
            } else { //add to existing queue
                queue.add(task);
            }
            return queue;
        });
    }

    @Override
    public void submitExclusive(@NonNull K keyIn, @NonNull Runnable task) {
        this.assertNotReleased();
        this.queues.compute(keyIn, (key, queue) -> {
            if (queue == null) { //create new queue
                queue = this.createQueue(key, task);
            } else { //replace contents of existing queue
                queue.clear();
                queue.add(task);
            }
            return queue;
        });
    }

    @Override
    protected void doRelease() {
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

    protected class TaskQueue extends ArrayDeque<Runnable> {
        protected final K key;

        public TaskQueue(@NonNull K key, @NonNull Runnable task) {
            this.key = key;
            this.add(task);

            //schedule this queue to be run
            DefaultKeyedTaskScheduler.this.queue.add(this);
        }

        public void run(@NonNull Deque<Runnable> taskBuffer) {
            DefaultKeyedTaskScheduler.this.queues.compute(this.key, (key, queue) -> {
                checkState(this == queue, "task queue has been replaced?!?");

                //move all tasks from queue into temporary task buffer
                taskBuffer.addAll(queue);
                queue.clear();

                return queue;
            });

            Throwable t0 = null;
            for (Runnable r : taskBuffer) {
                try {
                    r.run();
                } catch (Throwable t1) {
                    if (t0 == null) {
                        t0 = t1;
                    } else {
                        t0.addSuppressed(t1);
                    }
                }
            }

            DefaultKeyedTaskScheduler.this.queues.compute(this.key, (key, queue) -> {
                checkState(this == queue, "task queue has been replaced?!?");

                if (queue.isEmpty()) { //no new tasks have been added to this queue, so we can remove it
                    return null;
                } else { //re-schedule this queue to be run again
                    DefaultKeyedTaskScheduler.this.queue.add(this);
                    return queue;
                }
            });

            if (t0 != null) {
                PUnsafe.throwException(t0);
            }
        }
    }

    protected class Worker implements Runnable {
        @Override
        public void run() {
            Deque<Runnable> taskBuffer = new ArrayDeque<>();

            while (DefaultKeyedTaskScheduler.this.running) {
                try {
                    DefaultKeyedTaskScheduler.this.queue.take().run(taskBuffer);
                } catch (InterruptedException e) {
                    //gracefully exit on interrupt
                } catch (Exception e) {
                    LOGGER.error(Thread.currentThread().getName(), e);
                } finally {
                    taskBuffer.clear();
                }
            }
        }
    }
}
