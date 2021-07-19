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
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.fp2.util.threading.WorkerGroup;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
//nothing here needs to be manually synchronized - it's all accessed from inside (ObjObj)ConcurrentHashMap#compute, which synchronizes on the bucket
public class DefaultKeyedExecutor<K> extends AbstractRefCounted implements KeyedExecutor<K>, Runnable {
    protected final Map<K, TaskQueue> queues;
    protected final BlockingQueue<TaskQueue> queue;

    protected final WorkerGroup group;
    protected volatile boolean running = true;

    public DefaultKeyedExecutor(World world, int threads, @NonNull ThreadFactory threadFactory) {
        this.queues = this.createQueues();
        this.queue = this.createQueue();

        //create all the threads, then start them
        this.group = ThreadingHelper.startWorkers(world, threads, threadFactory, this);
    }

    protected Map<K, TaskQueue> createQueues() {
        return new ConcurrentHashMap<>();
    }

    protected BlockingQueue<TaskQueue> createQueue() {
        return new LinkedBlockingQueue<>();
    }

    protected TaskQueue createQueue(@NonNull K key, @NonNull Runnable task) {
        return new TaskQueue(key, task);
    }

    @Override
    public void submit(@NonNull K keyIn, @NonNull Runnable task) {
        this.ensureNotReleased();
        this.queues.compute(keyIn, (key, queue) -> {
            if (queue == null) { //create new queue
                queue = this.createQueue(key, task).schedule();
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
                queue = this.createQueue(key, task).schedule();
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
    public boolean updatePriorityFor(@NonNull K key, int priority) {
        this.ensureNotReleased();
        return false; //not supported
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
                TaskQueue queue = this.queue.poll(1L, TimeUnit.SECONDS);
                if (queue != null) {
                    queue.run(taskBuffer);
                }
            } catch (Exception e) {
                FP2_LOG.error(Thread.currentThread().getName(), e);
            } finally {
                taskBuffer.clear();
            }
        }
    }

    protected class TaskQueue extends ArrayDeque<Runnable> {
        protected final K key;

        public TaskQueue(@NonNull K key, @NonNull Runnable task) {
            this.key = key;
            this.add(task);
        }

        public TaskQueue schedule() {
            //schedule this queue to be run
            checkState(DefaultKeyedExecutor.this.queue.add(this), "failed to add queue for %s to execution queue!", this.key);

            return this;
        }

        public void run(@NonNull Deque<Runnable> taskBuffer) {
            DefaultKeyedExecutor.this.queues.compute(this.key, (key, queue) -> {
                checkState(this == queue, "task queue has been replaced?!?");

                if (queue.isEmpty()) { //all tasks in the queue have been cancelled, so we can remove it
                    return null;
                } else {
                    //move all tasks from queue into temporary task buffer
                    taskBuffer.addAll(queue);
                    queue.clear();

                    return queue;
                }
            });

            if (taskBuffer.isEmpty()) { //all the tasks were cancelled before we got around to handling this key, so nothing needs to be processed
                return;
            }

            Throwable t0 = null;
            for (Runnable r : taskBuffer) {
                if (!DefaultKeyedExecutor.this.running) { //executor is no longer running, exit ASAP by skipping remaining tasks
                    break;
                }

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

            DefaultKeyedExecutor.this.queues.compute(this.key, (key, queue) -> {
                checkState(this == queue, "task queue has been replaced?!?");

                if (queue.isEmpty()) { //no new tasks have been added to this queue, so we can remove it
                    return null;
                } else { //re-schedule this queue to be run again
                    return queue.schedule();
                }
            });

            if (t0 != null) {
                PUnsafe.throwException(t0);
            }
        }
    }
}
