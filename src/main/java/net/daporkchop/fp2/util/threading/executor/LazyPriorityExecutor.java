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

package net.daporkchop.fp2.util.threading.executor;

import lombok.NonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class LazyPriorityExecutor<K extends Comparable<K>> {
    protected final Thread[] threads;

    protected final Comparator<LazyTask<K>> comparator;
    protected final BlockingQueue<LazyTask<K>> queue;

    protected volatile boolean running;

    public LazyPriorityExecutor(int threads, @NonNull ThreadFactory threadFactory) {
        this.comparator = Comparator.comparing(LazyTask::key);
        this.queue = new PriorityBlockingQueue<>(256, this.comparator);

        this.threads = new Thread[positive(threads, "threads")];

        Runnable worker = () -> {
            while (this.running) {
                try {
                    Stream<? extends LazyTask<K>> afterStream = this.queue.take().run();
                    if (afterStream != null) {
                        this.queue.addAll(afterStream.collect(Collectors.toList()));
                    }
                } catch (InterruptedException e) {
                    //gracefully exit on interrupt
                } catch (Exception e)   {
                    e.printStackTrace();
                }
            }
        };
        for (int i = 0; i < threads; i++) {
            (this.threads[i] = threadFactory.newThread(worker)).start();
        }
    }

    public void submit(@NonNull LazyTask<K> task) {
        this.queue.add(task);
    }
}
