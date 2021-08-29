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
import net.daporkchop.fp2.util.datastructure.ConcurrentUnboundedPriorityBlockingQueue;
import net.daporkchop.fp2.util.threading.workergroup.WorkerGroupBuilder;
import net.minecraft.world.World;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * @author DaPorkchop_
 */
public class SortedKeyedScheduler<K extends Comparable<? super K>> extends DefaultKeyedExecutor<K> {
    public SortedKeyedScheduler(@NonNull WorkerGroupBuilder builder) {
        super(builder);
    }

    @Override
    protected BlockingQueue<DefaultKeyedExecutor.TaskQueue<K>> createQueue() {
        return new ConcurrentUnboundedPriorityBlockingQueue<>();
    }

    @Override
    protected DefaultKeyedExecutor.TaskQueue<K> createQueue(@NonNull K key, @NonNull Runnable task) {
        return new TaskQueue(key, task);
    }

    protected static class TaskQueue<K extends Comparable<? super K>> extends DefaultKeyedExecutor.TaskQueue<K> implements Comparable<TaskQueue<K>> {
        public TaskQueue(@NonNull K key, @NonNull Runnable task) {
            super(key, task);
        }

        @Override
        public int compareTo(TaskQueue<K> o) {
            return this.key.compareTo(o.key);
        }
    }
}
