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
import net.daporkchop.fp2.util.threading.lazy.LazyFutureTask;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class KeyedReferencingScheduler<K, V> extends AbstractRefCounted {
    /*
     * Implementation details:
     *
     * The scheduler operates using a concurrent map of keys to tasks, using the synchronization guarantees of ConcurrentHashMap to ensure
     * that the same task key is not accessed concurrently.
     *
     * When a key is retained for which there is no existing task in the map, the task factory is used to create a new one which is subsequently
     * inserted into the map and submitted to the task scheduler.
     *
     * When a key is retained for which there is an existing task in the map, the existing task is returned.
     *
     * When a key is released, the corresponding task's reference count is decremented. If the reference count reaches 0 the task is cancelled
     * (removed from both the task scheduler and the map).
     */

    protected final KeyedExecutor<? super K> executor;
    protected final Map<K, Task> map = new ConcurrentHashMap<>();
    protected final Function<K, V> task;

    public KeyedReferencingScheduler(@NonNull KeyedExecutor<? super K> executor, @NonNull Function<K, V> task) {
        this.executor = executor.retain();
        this.task = task;
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
        this.map.compute(keyIn, (key, task) -> {
            checkState(task != null, "attempted to release non-existent task for key: %s", key);

            if (--task.refCnt == 0 //the task's reference count has reached 0
                && task.cancel(false)) { //the task itself was marked as cancelled
                //remove task from scheduler
                this.executor.cancel(key, task);

                //set the task to null to have it removed from the map
                task = null;
            }

            return task;
        });
    }

    public void doWith(@NonNull List<K> keys, @NonNull Consumer<List<V>> callback) {
        this.ensureNotReleased();
        List<Task> tasks = keys.stream().map(this::retainInternal).collect(Collectors.toList());

        try {
            callback.accept(Task.scatterGather(tasks));
        } finally {
            keys.forEach(this::release);
        }
    }

    @Override
    protected void doRelease() {
        //cancel all pending tasks
        this.map.forEach(this.executor::cancel);
        this.map.clear();

        //release reference to scheduler
        this.executor.release();
    }

    /**
     * A {@link CompletableFuture} task which is executed by a {@link KeyedReferencingScheduler}.
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
            KeyedReferencingScheduler.this.executor.submit(key, this);
        }

        @Override
        protected V compute() {
            return KeyedReferencingScheduler.this.task.apply(this.key);
        }
    }
}
