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
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
public class KeyedDistinctScheduler<K> extends AbstractRefCounted {
    protected final KeyedExecutor<? super K> executor;
    protected final Map<K, Task> map = new ConcurrentHashMap<>();
    protected final Consumer<K> task;

    public KeyedDistinctScheduler(@NonNull KeyedExecutor<? super K> executor, @NonNull Consumer<K> task) {
        this.executor = executor.retain();
        this.task = task;
    }

    public void submit(@NonNull K keyIn) {
        this.ensureNotReleased();
        this.map.computeIfAbsent(keyIn, Task::new);
    }

    @Override
    protected void doRelease() {
        //cancel all pending tasks
        this.map.forEach(this.executor::cancel);
        this.map.clear();

        //release reference to scheduler
        this.executor.release();
    }

    protected class Task implements Runnable {
        protected final K key;

        public Task(@NonNull K key) {
            this.key = key;

            KeyedDistinctScheduler.this.executor.submit(this.key, this);
        }

        @Override
        public void run() {
            if (KeyedDistinctScheduler.this.map.remove(this.key, this)) {
                KeyedDistinctScheduler.this.task.accept(this.key);
            }
        }
    }
}
