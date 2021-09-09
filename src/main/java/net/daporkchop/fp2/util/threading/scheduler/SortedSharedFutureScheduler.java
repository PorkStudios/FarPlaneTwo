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
import net.daporkchop.fp2.util.datastructure.ConcurrentUnboundedPriorityBlockingQueue;
import net.daporkchop.fp2.util.threading.workergroup.WorkerGroupBuilder;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class SortedSharedFutureScheduler<P extends Comparable<? super P>, V> extends SharedFutureScheduler<P, V> {
    public SortedSharedFutureScheduler(@NonNull Function<Scheduler<P, V>, Function<P, V>> functionFactory, @NonNull WorkerGroupBuilder builder) {
        super(functionFactory, builder);
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
            return PorkUtil.<ConcurrentUnboundedPriorityBlockingQueue<Task>>uncheckedCast(this.queue).pollLess(parent, 1L, TimeUnit.SECONDS);
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
                checkArg(parent.param.compareTo(param) > 0, "task %s tried to recurse upwards to %s!", parent.param, param);
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
        public Task(@NonNull P param) {
            super(param);
        }

        @Override
        public int compareTo(Task o) {
            return this.param.compareTo(o.param);
        }
    }
}
