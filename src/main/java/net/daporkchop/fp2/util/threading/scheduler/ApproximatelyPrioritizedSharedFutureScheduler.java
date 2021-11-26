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

package net.daporkchop.fp2.util.threading.scheduler;

import lombok.NonNull;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerGroupBuilder;
import net.daporkchop.fp2.util.datastructure.ConcurrentUnboundedPriorityBlockingQueue;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Implementation of {@link Scheduler} whose {@link CompletableFuture}s are shared for all occurrences of the same parameter value, and whose tasks are executed approximately
 * in priority.
 * <p>
 * This functions similarly to {@link SharedFutureScheduler}, but is initialized with a {@link Comparator}. The {@link Comparator} does not have to provide accurate comparison
 * across all possible parameter values, but only needs to partition parameters into smaller parameter spaces. Tasks are executed in comparison order as much as reasonably
 * possible (i.e. it may not be perfect). Accuracy can be improved by reducing the duration of an individual task and splitting tasks up into multiple sub-tasks which can
 * be executed recursively or using {@link #scatterGather(List)}, although this is not recommended as the overhead imposed by each task is fairly substantial. Recursive
 * tasks are only permitted to recurse into parameters which are strictly less than the current one, attempts to do otherwise will throw an exception.
 * <p>
 * It is, of course, possible to use a {@link Comparator} which does accurate comparisons between all distinct parameter values. However, this will likely result in
 * an undesirable bias towards a certain keys when doing recursive actions.
 *
 * @author DaPorkchop_
 */
public class ApproximatelyPrioritizedSharedFutureScheduler<P, V> extends SharedFutureScheduler<P, V> {
    protected final AtomicLong ctr = new AtomicLong(Long.MIN_VALUE); //we assume this will never overflow - a perhaps naïve assumption, but still, 2⁶⁴ IS a very large number...
    protected final Comparator<P> initialComparator;

    public ApproximatelyPrioritizedSharedFutureScheduler(@NonNull Function<Scheduler<P, V>, Function<P, V>> functionFactory, @NonNull WorkerGroupBuilder builder, @NonNull Comparator<P> initialComparator) {
        super(functionFactory, builder);

        this.initialComparator = initialComparator;
    }

    @Override
    protected Supplier<Deque<SharedFutureScheduler<P, V>.Task>> recursionStackFactory() {
        return () -> new ArrayDeque<SharedFutureScheduler<P, V>.Task>() {
            @Override
            public void push(SharedFutureScheduler<P, V>.Task task) {
                checkState(this.isEmpty() || PorkUtil.<Task>uncheckedCast(this.peekFirst()).compareTo(uncheckedCast(task)) > 0);
                super.push(task);
            }
        };
    }

    @Override
    protected BlockingQueue<SharedFutureScheduler<P, V>.Task> createTaskQueue() {
        return new ConcurrentUnboundedPriorityBlockingQueue<>();
    }

    @Override
    protected SharedFutureScheduler<P, V>.Task createTask(@NonNull P param) {
        return new Task(param);
    }

    @Override
    protected void unqueue(@NonNull SharedFutureScheduler<P, V>.Task task) {
        this.queue.remove(task);
    }

    @Override
    protected void awaitJoin(@NonNull SharedFutureScheduler<P, V>.Task task) {
        Deque<SharedFutureScheduler<P, V>.Task> recursionStack = this.recursionStack.get();
        Task parent = uncheckedCast(recursionStack.peekFirst());
        if (parent != null) { //this is a recursive task! we should make sure that the child task is less than the current one
            checkArg(parent.compareTo(uncheckedCast(task)) > 0, "task at %s tried to recurse upwards to %s!", parent, task);
        }

        while (!task.isDone()) {
            this.pollAndExecuteSingleTask();
        }
    }

    @Override
    protected SharedFutureScheduler<P, V>.Task pollSingleTask() {
        Deque<SharedFutureScheduler<P, V>.Task> recursionStack = this.recursionStack.get();
        Task parent = uncheckedCast(recursionStack.peekFirst());
        if (parent != null) { //this is a recursive task! we should make sure that the task we get is less than the current one
            Task polledTask = PorkUtil.<ConcurrentUnboundedPriorityBlockingQueue<Task>>uncheckedCast(this.queue).pollLess(parent);
            if (polledTask == null) {
                //sleep to avoid high CPU while spinning...
                //this is actually pretty yucky, but unfortunately i don't see any way to avoid having to spin here other than making some kind of semaphore which
                //  is somehow aware of key ranges somehow.
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1L));
            }
            return polledTask;
        } else {
            return super.pollSingleTask();
        }
    }

    @Override
    public List<V> scatterGather(@NonNull List<P> params) {
        Deque<SharedFutureScheduler<P, V>.Task> recursionStack = this.recursionStack.get();
        Task parent = uncheckedCast(recursionStack.peekFirst());
        if (parent != null) { //this is a recursive task! we should make sure that all of the child tasks are less than the current one
            for (P param : params) {
                checkArg(this.initialComparator.compare(parent.param, param) > 0, "task %s tried to recurse upwards to %s!", parent.param, param);
            }
        }

        return super.scatterGather(params);
    }

    @Override
    protected List<V> gather(@NonNull List<SharedFutureScheduler<P, V>.Task> tasks) {
        //we don't want to race to begin each task before joining: the tasks are higher-priority than the current task, so we only have to join them
        //  which will guarantee they'll be started as long as no tasks with even higher priority are in the queue
        List<V> values = new ArrayList<>(tasks.size());
        for (SharedFutureScheduler<P, V>.Task task : tasks) {
            values.add(task.join());
        }
        return values;
    }

    /**
     * @author DaPorkchop_
     */
    protected class Task extends SharedFutureScheduler<P, V>.Task implements Comparable<Task> {
        protected final long tieBreak = ApproximatelyPrioritizedSharedFutureScheduler.this.ctr.getAndIncrement();

        public Task(@NonNull P param) {
            super(param);
        }

        @Override
        public int compareTo(Task o) {
            int d;
            if ((d = ApproximatelyPrioritizedSharedFutureScheduler.this.initialComparator.compare(this.param, o.param)) != 0
                || (d = Long.compare(this.tieBreak, o.tieBreak)) != 0) {
                return d;
            }

            checkState(this.param.equals(o.param), "%s != %s", this.param, o.param);
            return 0;
        }
    }
}
