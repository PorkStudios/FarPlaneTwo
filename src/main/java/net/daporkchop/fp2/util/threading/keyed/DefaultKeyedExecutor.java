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
import net.daporkchop.fp2.util.threading.workergroup.WorkerGroupBuilder;
import net.daporkchop.fp2.util.threading.workergroup.WorldWorkerGroup;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
//nothing here needs to be manually synchronized - it's all accessed from inside (ObjObj)ConcurrentHashMap#compute, which synchronizes on the bucket
public class DefaultKeyedExecutor<K> extends AbstractRefCounted implements KeyedExecutor<K>, Runnable {
    protected final Map<K, TaskQueue<K>> queues;
    protected final BlockingQueue<TaskQueue<K>> queue;

    protected final WorldWorkerGroup group;
    protected volatile boolean running = true;

    public DefaultKeyedExecutor(@NonNull WorkerGroupBuilder builder) {
        this.queues = this.createQueues();
        this.queue = this.createQueue();

        //create all the threads, then start them
        this.group = builder.build(this);
    }

    protected Map<K, TaskQueue<K>> createQueues() {
        return new ConcurrentHashMap<>();
    }

    protected BlockingQueue<TaskQueue<K>> createQueue() {
        return new LinkedBlockingQueue<>();
    }

    protected TaskQueue<K> createQueue(@NonNull K key, @NonNull Runnable task) {
        return new TaskQueue<>(key, task);
    }

    @Override
    public void submit(@NonNull K keyIn, @NonNull Runnable task) {
        this.ensureNotReleased();
        this.queues.compute(keyIn, (key, queue) -> {
            if (queue == null) { //create new queue
                queue = this.createQueue(key, task).schedule(this.queue);
            } else { //add to existing queue
                queue.add(task);
            }
            return queue;
        });
    }

    @Override
    public void submitExclusive(@NonNull K keyIn, @NonNull Runnable task) {
        this.ensureNotReleased();
        this.queues.compute(keyIn, (key, queue) -> {
            if (queue == null) { //create new queue
                queue = this.createQueue(key, task).schedule(this.queue);
            } else { //replace contents of existing queue
                queue.clear();
                queue.add(task);
            }
            return queue;
        });
    }

    @Override
    public void cancel(@NonNull K keyIn, @NonNull Runnable task) {
        this.ensureNotReleased();
        this.queues.computeIfPresent(keyIn, (key, queue) -> {
            //remove task from queue
            // we'll rely on the worker threads to remove the queue itself from the map when they get around to it
            queue.remove(task);
            return queue;
        });
    }

    @Override
    public void cancelAll(@NonNull K keyIn) {
        this.ensureNotReleased();
        this.queues.computeIfPresent(keyIn, (key, queue) -> {
            //remove all tasks from queue
            // we'll rely on the worker threads to remove the queue itself from the map when they get around to it
            queue.clear();
            return queue;
        });
    }

    @Override
    public KeyedExecutor<K> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        //notify workers that we're shutting down
        this.running = false;

        //wait until all the workers have exited
        this.group.close();
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public void run() {
        Deque<Runnable> taskBuffer = new ArrayDeque<>();

        while (this.running) {
            try {
                //poll the queue, but don't wait indefinitely because we need to be able to exit if the executor stops running.
                // we don't want to use interrupts, because they can cause unwanted side-effects (such as closing NIO channels).
                TaskQueue<K> queue = this.queue.poll(1L, TimeUnit.SECONDS);
                if (queue != null) {
                    this.queues.compute(queue.key, (key, q) -> {
                        checkState(queue == q, "task queue has been replaced?!?");

                        if (q.isEmpty()) { //all tasks in the queue have been cancelled, so we can remove it
                            return null;
                        } else {
                            //move all tasks from queue into temporary task buffer
                            taskBuffer.add(q.poll());

                            return q;
                        }
                    });

                    if (taskBuffer.isEmpty()) { //all the tasks were cancelled before we got around to handling this key, so nothing needs to be processed
                        continue;
                    }

                    Exception t0 = null;
                    try {
                        taskBuffer.poll().run();
                    } catch (Exception t1) {
                        t0 = t1;
                    }

                    this.queues.compute(queue.key, (key, q) -> {
                        checkState(queue == q, "task queue has been replaced?!?");

                        if (q.isEmpty()) { //no new tasks have been added to this queue, so we can remove it
                            return null;
                        } else { //re-schedule this queue to be run again
                            return q.schedule(this.queue);
                        }
                    });

                    if (t0 != null) {
                        throw t0;
                    }
                }
            } catch (Exception e) {
                FP2_LOG.error(Thread.currentThread().getName(), e);
            } finally {
                taskBuffer.clear();
            }
        }
    }

    protected static class TaskQueue<K> extends ArrayDeque<Runnable> {
        protected final K key;

        public TaskQueue(@NonNull K key, @NonNull Runnable task) {
            this.key = key;
            this.add(task);
        }

        public TaskQueue<K> schedule(@NonNull BlockingQueue<TaskQueue<K>> queue) {
            //schedule this queue to be run
            checkState(queue.add(this), "failed to add queue for %s to execution queue!", this.key);

            return this;
        }
    }
}
