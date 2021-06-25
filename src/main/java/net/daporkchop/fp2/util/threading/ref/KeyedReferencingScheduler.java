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

package net.daporkchop.fp2.util.threading.ref;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.threading.keyed.KeyedTaskScheduler;
import net.daporkchop.fp2.util.threading.lazy.LazyFutureTask;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class KeyedReferencingScheduler<K extends Comparable<? super K>> extends AbstractReleasable {
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

    @NonNull
    protected final KeyedTaskScheduler<? super K> scheduler;
    @NonNull
    protected final Consumer<K> task;

    protected final Map<K, Task> map = new ConcurrentHashMap<>();

    public void retain(@NonNull K keyIn) {
        this.map.compute(keyIn, (key, task) -> {
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
        this.map.compute(keyIn, (key, task) -> {
            checkState(task != null, "attempted to release non-existent task for key: %s", key);

            if (--task.refCnt == 0 //the task's reference count has reached 0
                && task.cancel(false)) { //the task itself was marked as cancelled
                //remove task from scheduler
                this.scheduler.cancel(key, task);

                //set the task to null to have it removed from the map
                task = null;
            }

            return task;
        });
    }

    @Override
    protected void doRelease() {
        //TODO: cancel all tasks
    }

    /**
     * A {@link CompletableFuture} task which is executed by a {@link KeyedReferencingScheduler}.
     *
     * @author DaPorkchop_
     */
    protected class Task extends LazyFutureTask<Void> {
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
            KeyedReferencingScheduler.this.scheduler.submit(key, this);
        }

        @Override
        protected Void compute() {
            KeyedReferencingScheduler.this.task.accept(this.key);
            return null;
        }
    }
}
