/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.util.threading;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class KeyedTaskScheduler<K> {
    @NonNull
    protected final Executor executor;

    protected final LoadingCache<K, Queue<Runnable>> queueCache = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<K, Queue<Runnable>>() {
                @Override
                public Queue<Runnable> load(K key) throws Exception {
                    return new ArrayDeque<>();
                }
            });

    public void submit(@NonNull K key, @NonNull Runnable task) {
        Queue<Runnable> queue = this.queueCache.getUnchecked(key);
        task = new QueuedTaskWrapper(task, queue);
        synchronized (queue) {
            queue.add(task);
            if (queue.size() == 1) {
                //the only task in the queue is this one, so let's submit it now
                this.executor.execute(task);
            }
        }
    }

    public Executor keyed(@NonNull K key) {
        return new KeyedExecutor(key);
    }

    @RequiredArgsConstructor
    protected final class QueuedTaskWrapper implements Runnable {
        @NonNull
        protected final Runnable task;
        @NonNull
        protected final Queue<Runnable> queue;

        @Override
        public void run() {
            try {
                this.task.run();
            } finally {
                synchronized (this.queue) {
                    Runnable next = this.queue.poll();
                    checkState(next == this, "this task wasn't the next one in the queue!");
                    next = this.queue.poll();
                    if (next != null) {
                        KeyedTaskScheduler.this.executor.execute(next);
                    }
                }
            }
        }
    }

    @RequiredArgsConstructor
    protected final class KeyedExecutor implements Executor {
        @NonNull
        protected final K key;

        @Override
        public void execute(@NonNull Runnable command) {
            KeyedTaskScheduler.this.submit(this.key, command);
        }
    }
}
