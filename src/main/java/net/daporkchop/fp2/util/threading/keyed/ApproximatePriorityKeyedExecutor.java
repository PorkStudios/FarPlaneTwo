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
import net.daporkchop.fp2.util.threading.ConcurrentUnboundedPriorityBlockingQueue;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class ApproximatePriorityKeyedExecutor<K> extends DefaultKeyedExecutor<K> {
    protected final AtomicLong ctr = new AtomicLong(Long.MIN_VALUE); //we assume this will never overflow - a perhaps naïve assumption, but still, 2⁶⁴ IS a very large number...
    protected final Comparator<K> initialComparator;

    @Deprecated
    public ApproximatePriorityKeyedExecutor(World world, int threads, @NonNull ThreadFactory threadFactory) {
        this(world, threads, threadFactory, (a, b) -> 0);
    }

    public ApproximatePriorityKeyedExecutor(World world, int threads, @NonNull ThreadFactory threadFactory, @NonNull Comparator<K> initialComparator) {
        super(world, threads, threadFactory);
        this.initialComparator = initialComparator;
    }

    @Override
    protected BlockingQueue<DefaultKeyedExecutor<K>.TaskQueue> createQueue() {
        return new ConcurrentUnboundedPriorityBlockingQueue<>();
    }

    @Override
    protected DefaultKeyedExecutor<K>.TaskQueue createQueue(@NonNull K key, @NonNull Runnable task) {
        return new TaskQueue(key, task);
    }

    @Override
    public boolean updatePriorityFor(@NonNull K keyIn, int priority) {
        super.updatePriorityFor(keyIn, priority);
        this.queues.computeIfPresent(keyIn, (key, q) -> {
            TaskQueue queue = (TaskQueue) q;
            if (queue.priority != priority && this.queue.remove(queue)) {
                queue.priority = priority;
                checkState(this.queue.add(queue), "unable to re-insert task?!?");
            }
            return queue;
        });
        return true;
    }

    protected class TaskQueue extends DefaultKeyedExecutor<K>.TaskQueue implements Comparable<TaskQueue> {
        protected final long tieBreak = ApproximatePriorityKeyedExecutor.this.ctr.getAndIncrement();
        protected int priority = Integer.MAX_VALUE;

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

        @Override
        public String toString() {
            return this.key + "@" + this.priority;
        }
    }
}
