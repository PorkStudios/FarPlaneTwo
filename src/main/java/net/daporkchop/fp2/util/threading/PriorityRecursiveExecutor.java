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

package net.daporkchop.fp2.util.threading;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.lib.common.function.PFunctions;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Collection;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Deprecated
public class PriorityRecursiveExecutor<T extends Comparable<T> & Predicate<T>> implements Runnable {
    protected final ConcurrentUnboundedPriorityBlockingQueue<T> queue;
    protected final Consumer<T> worker;

    protected final Thread[] threads;

    protected volatile boolean running = true;

    public PriorityRecursiveExecutor(int threads, @NonNull ThreadFactory threadFactory, @NonNull Consumer<T> worker) {
        this.queue = new ConcurrentUnboundedPriorityBlockingQueue<>();
        this.worker = worker;

        this.threads = new Thread[positive(threads, "threads")];
        for (int i = 0; i < threads; i++) {
            (this.threads[i] = threadFactory.newThread(this)).start();
        }
    }

    public void submit(@NonNull T task) {
        this.queue.add(task);
    }

    public void submit(@NonNull Collection<T> tasks) {
        this.queue.addAll(tasks);
    }

    public void checkForHigherPriorityWork(@NonNull T root) {
        if (!this.running) {
            PUnsafe.throwException(new InterruptedException());
        }

        T task = this.queue.popIfMatches(root);
        if (task != null) {
            try {
                if (false) {
                    throw new InterruptedException(); //trick javac into letting me catch InterruptedException
                }
                this.worker.accept(task);
            } catch (InterruptedException e) {
                PUnsafe.throwException(e); //rethrow
            } catch (Exception e) {
                FP2_LOG.error(Thread.currentThread().getName(), e);
            }
        }
    }

    @SneakyThrows(InterruptedException.class)
    public void shutdown() {
        //set running flag to false
        this.running = false;

        do {
            //keep queue topped up with fake tasks
            this.queue.set.clear();
            if (this.queue.lock.availablePermits() < this.threads.length) {
                this.queue.lock.release(this.threads.length - this.queue.lock.availablePermits());
            }

            //sleep to avoid spinning at 100%
            //side note: i hate this, it's so ugly
            Thread.sleep(50L);

            //avoid deadlock by continuing to work off any server thread tasks that might pile up
            ServerThreadExecutor.INSTANCE.workOffQueue();
        } while (Stream.of(this.threads).anyMatch(Thread::isAlive));
    }

    @Override
    @Deprecated
    public void run() {
        while (this.running) {
            try {
                T val = this.queue.take();
                if (val == null) {
                    return; //null value means we need to exit
                }

                this.worker.accept(val);
            } catch (InterruptedException e) {
                //gracefully exit on interrupt
                return;
            } catch (Exception e) {
                FP2_LOG.error(Thread.currentThread().getName(), e);
            }
        }
    }
}
