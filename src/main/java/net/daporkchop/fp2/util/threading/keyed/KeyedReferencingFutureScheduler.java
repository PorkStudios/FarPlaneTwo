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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.util.datastructure.RecyclingArrayDeque;
import net.daporkchop.fp2.util.threading.lazy.LazyFutureTask;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class KeyedReferencingFutureScheduler<K, V> extends AbstractRefCounted {
    protected static final long TASK_DEPENDENCIES_OFFSET = PUnsafe.pork_getOffset(KeyedReferencingFutureScheduler.Task.class, "dependencies");

    protected static final Ref<Deque> RECURSION_TRACKERS = ThreadRef.soft(RecyclingArrayDeque::new);

    protected final KeyedExecutor<? super K> executor;
    protected final Map<K, Task> map = new ConcurrentHashMap<>();
    protected final Function<K, V> task;

    protected final int priority;

    public KeyedReferencingFutureScheduler(@NonNull KeyedExecutor<? super K> executor, @NonNull Function<K, V> task, int priority) {
        this.executor = executor.retain();
        this.task = task;
        this.priority = priority;
    }

    public CompletableFuture<V> retain(@NonNull K key) {
        return this.retainInternal(key);
    }

    protected Task retainInternal(@NonNull K keyIn) {
        this.ensureNotReleased();
        return this.map.compute(keyIn, (key, task) -> {
            if (task == null) { //there is no existing task for this key
                //create new task
                task = new Task(key);
            } else {
                //increment the task's reference count
                task.refCnt++;
            }

            return task;
        });
    }

    public void release(@NonNull K keyIn) {
        this.ensureNotReleased();

        class State implements BiFunction<K, Task, Task>, AutoCloseable {
            Task toCancel;

            @Override
            public Task apply(K key, Task task) {
                checkState(task != null, "attempted to release non-existent task for key: %s", key);

                if (--task.refCnt == 0) { //the task's reference count has reached 0
                    this.toCancel = task;

                    //remove task from executor
                    KeyedReferencingFutureScheduler.this.executor.cancel(key, task);

                    //set the task to null to have it removed from the map
                    task = null;
                }

                return task;
            }

            @Override
            public void close() {
                if (this.toCancel != null) { //the task was removed, cancel it
                    //we cancel the task here so that it doesn't happen inside of the ConcurrentHashMap#compute function, which doesn't like being
                    //  called multiple times from the same thread.
                    this.toCancel.cancel(false);
                }
            }
        }

        try (State state = new State()) {
            this.map.compute(keyIn, state);
        }
    }

    public void doWith(@NonNull List<K> keys, @NonNull Consumer<List<V>> callback) {
        this.ensureNotReleased();

        Deque<Task> recursionTracker = uncheckedCast(RECURSION_TRACKERS.get());
        Task[] tasks = uncheckedCast(keys.stream().map(this::retainInternal).toArray(KeyedReferencingFutureScheduler.Task[]::new));

        Task parent = recursionTracker.peek();
        if (parent != null) { //this is a recursive task, so we need to make sure the parent task is aware that it's resulted in child tasks being spawned
            if (!PUnsafe.compareAndSwapObject(parent, TASK_DEPENDENCIES_OFFSET, null, tasks)) { //there may only be one active scatter/gather per task at a time
                keys.forEach(this::release);
                throw new IllegalStateException(PStrings.fastFormat("task for %s has already started recursion!", parent.key));
            }

            try {
                callback.accept(Task.scatterGather(tasks));
            } finally {
                if (PUnsafe.compareAndSwapObject(parent, TASK_DEPENDENCIES_OFFSET, tasks, null)) { //don't release dependencies if they've already been released from
                    //  another thread (due to this task's cancellation)
                    keys.forEach(this::release);
                }
            }
        } else { //top-level task, do a simple scatter/gather
            try {
                callback.accept(Task.scatterGather(tasks));
            } finally {
                keys.forEach(this::release);
            }
        }
    }

    @Override
    protected void doRelease() {
        //cancel all pending tasks
        this.map.forEach(this.executor::cancel);
        this.map.clear();

        //release reference to executor
        this.executor.release();
    }

    /**
     * A {@link CompletableFuture} task which is executed by a {@link KeyedReferencingFutureScheduler}.
     *
     * @author DaPorkchop_
     */
    protected class Task extends LazyFutureTask<V> {
        @Getter
        protected final K key;

        //the task's current reference count
        protected int refCnt;

        //list of tasks whose results are required for the successful execution of the current task
        protected volatile Task[] dependencies = null;

        public Task(@NonNull K key) {
            this.key = key;
            this.refCnt = 1;

            //schedule self for execution
            KeyedReferencingFutureScheduler.this.executor.submit(key, this, KeyedReferencingFutureScheduler.this.priority);
        }

        @Override
        protected V compute() {
            KeyedReferencingFutureScheduler.this.ensureNotReleased();

            Deque<Task> recursionTracker = uncheckedCast(RECURSION_TRACKERS.get());
            recursionTracker.push(this);
            try {
                return KeyedReferencingFutureScheduler.this.task.apply(this.key);
            } finally {
                checkState(recursionTracker.pop() == this, "popped different task from recursion tracker?!?");
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (super.cancel(mayInterruptIfRunning)) {
                Task[] dependencies = PUnsafe.pork_swapObject(this, TASK_DEPENDENCIES_OFFSET, null);
                if (dependencies != null) { //this task was recursive and had some dependencies, let's release them since they aren't actually needed by this task any more
                    for (Task dependency : dependencies) {
                        dependency.scheduler().release(dependency.key);
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        public KeyedReferencingFutureScheduler<K, V> scheduler() { //hacky workaround to allow access to KeyedReferencingFutureScheduler.this
            return KeyedReferencingFutureScheduler.this;
        }
    }
}
