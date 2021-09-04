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
import net.daporkchop.fp2.util.datastructure.ConcurrentUnboundedPriorityBlockingQueue;
import net.daporkchop.fp2.util.threading.workergroup.WorkerGroupBuilder;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class ApproximatePriorityKeyedExecutor<K> extends DefaultKeyedExecutor<K> {
    protected final AtomicLong ctr = new AtomicLong(Long.MIN_VALUE); //we assume this will never overflow - a perhaps naïve assumption, but still, 2⁶⁴ IS a very large number...
    protected final Comparator<K> initialComparator;

    @Deprecated
    public ApproximatePriorityKeyedExecutor(@NonNull WorkerGroupBuilder builder) {
        this(builder, (a, b) -> 0);
    }

    public ApproximatePriorityKeyedExecutor(@NonNull WorkerGroupBuilder builder, @NonNull Comparator<K> initialComparator) {
        super(builder);
        this.initialComparator = initialComparator;
    }

    @Override
    protected BlockingQueue<DefaultKeyedExecutor.TaskQueue<K>> createQueue() {
        return new ConcurrentUnboundedPriorityBlockingQueue<>();
    }

    @Override
    protected DefaultKeyedExecutor.TaskQueue<K> createQueue(@NonNull K key, @NonNull Runnable task) {
        return new TaskQueue(key, task);
    }

    @Override
    public void submit(@NonNull K key, @NonNull Runnable task) {
        this.submit(key, task, 0);
    }

    @Override
    public void submitExclusive(@NonNull K key, @NonNull Runnable task) {
        super.submitExclusive(key, new PrioritizedRunnable(task, this.ctr.getAndIncrement(), 0));
    }

    @Override
    public void submit(@NonNull K key, @NonNull Runnable task, int priority) {
        super.submit(key, new PrioritizedRunnable(task, this.ctr.getAndIncrement(), priority));
    }

    @RequiredArgsConstructor
    protected static class PrioritizedRunnable implements Runnable, Comparable<PrioritizedRunnable> {
        @NonNull
        protected final Runnable delegate;
        protected final long tieBreak;
        protected final int priority;

        @Override
        public void run() {
            this.delegate.run();
        }

        @Override
        public int compareTo(PrioritizedRunnable o) {
            int d;
            if ((d = Integer.compare(this.priority, o.priority)) != 0
                || (d = Long.compare(this.tieBreak, o.tieBreak)) != 0) {
                return d;
            }
            return 0;
        }
    }

    protected class TaskQueue extends DefaultKeyedExecutor.TaskQueue<K> implements Comparable<TaskQueue> {
        protected long tieBreak;
        protected int priority;

        public TaskQueue(@NonNull K key, @NonNull Runnable task) {
            super(key, task);
        }

        @Override
        public int compareTo(TaskQueue o) {
            int d;
            if ((d = ApproximatePriorityKeyedExecutor.this.initialComparator.compare(this.key, o.key)) != 0
                || (d = Integer.compare(this.priority, o.priority)) != 0
                || (d = Long.compare(this.tieBreak, o.tieBreak)) != 0) {
                return d;
            }

            checkState(this.key.equals(o.key), "%s != %s", this.key, o.key);
            return 0;
        }

        //all of the following methods are safe because they are never called without holding the lock on this queue

        protected void recheckPriority() {
            if (this.isEmpty()) { //no tasks in queue, nothing to do!
                return;
            }

            boolean shouldAdd = ApproximatePriorityKeyedExecutor.this.queue.remove(this);

            //find the best task, then remove it so we can place it at the front of the queue
            PrioritizedRunnable bestTask = PorkUtil.<Stream<PrioritizedRunnable>>uncheckedCast(this.stream()).min(Comparator.naturalOrder()).get();
            checkState(super.remove(bestTask));
            this.addFirst(bestTask);

            this.tieBreak = bestTask.tieBreak;
            this.priority = bestTask.priority;

            if (shouldAdd) {
                super.schedule(ApproximatePriorityKeyedExecutor.this.queue);
            }
        }

        @Override
        public DefaultKeyedExecutor.TaskQueue<K> schedule(@NonNull BlockingQueue<DefaultKeyedExecutor.TaskQueue<K>> queue) {
            this.recheckPriority();
            return super.schedule(queue);
        }

        @Override
        public boolean add(@NonNull Runnable runnable) {
            super.add(runnable);
            this.recheckPriority();
            return true;
        }

        @Override
        public boolean remove(Object o) {
            if (super.remove(o)) {
                this.recheckPriority();
                return true;
            } else {
                return false;
            }
        }
    }
}
