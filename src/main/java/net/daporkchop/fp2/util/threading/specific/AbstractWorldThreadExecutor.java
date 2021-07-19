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

package net.daporkchop.fp2.util.threading.specific;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.threading.WorkerGroup;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RunnableFuture;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractWorldThreadExecutor implements WorldThreadExecutor {
    @NonNull
    protected final Thread thread;
    @Getter
    @NonNull
    protected final World world;

    protected final Set<WorkerGroup> groups = new ReferenceOpenHashSet<>();
    protected final Map<Thread, WorkerGroup> threadToGroup = new Reference2ObjectOpenHashMap<>();

    protected final Queue<Task<?>> queue = new LinkedList<>();

    protected boolean running = false;

    @Override
    public synchronized void addChild(@NonNull WorkerGroup group) {
        checkState(Thread.currentThread() == this.thread, "%s isn't the expected world thread %s", Thread.currentThread(), this.thread);

        checkState(!this.groups.contains(group), "duplicate worker group");
        checkState(group.threads().stream().noneMatch(this.threadToGroup::containsKey), "duplicate thread");

        this.groups.add(group);
        group.threads().forEach(thread -> this.threadToGroup.put(thread, group));
    }

    @Override
    public synchronized void removeChild(@NonNull WorkerGroup group) {
        checkState(Thread.currentThread() == this.thread, "%s isn't the expected world thread %s", Thread.currentThread(), this.thread);

        checkState(this.groups.contains(group), "unknown worker group");
        checkState(group.threads().stream().allMatch(this.threadToGroup::containsKey), "unknown thread");

        this.groups.remove(group);
        group.threads().forEach(this.threadToGroup::remove);

        //cancel and remove all tasks scheduled by threads in this group
        for (Iterator<Task<?>> itr = this.queue.iterator(); itr.hasNext(); ) {
            Task<?> task = itr.next();
            if (task.group == group) {
                task.cancel(false);
                itr.remove();
            }
        }
    }

    @Override
    public synchronized <V> CompletableFuture<V> supply(@NonNull Supplier<V> supplier) {
        WorkerGroup group = this.threadToGroup.get(Thread.currentThread());
        checkState(group != null, "task submitted by unknown thread: %s", Thread.currentThread());

        Task<V> task = new Task<>(group, supplier);
        this.queue.add(task);
        return task;
    }

    /**
     * Does a single piece of work.
     *
     * @return whether or not there is more work remaining in the queue
     */
    protected boolean doWork() {
        checkState(Thread.currentThread() == this.thread);

        Task<?> task;
        synchronized (this) {
            task = this.queue.poll();
        }

        if (task != null) {
            task.run();
            return !this.queue.isEmpty();
        } else {
            return false; //task queue is empty, we're done for now
        }
    }

    @Override
    public synchronized void start() {
        checkState(!this.running, "already running");
        this.running = true;
    }

    @Override
    public synchronized void close() {
        checkState(this.running, "not running");
        this.running = false;

        checkState(this.groups.isEmpty());
        checkState(this.threadToGroup.isEmpty());
        checkState(this.queue.isEmpty());
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class Task<V> extends CompletableFuture<V> implements RunnableFuture<V> {
        @NonNull
        protected WorkerGroup group;
        @NonNull
        protected Supplier<V> supplier;

        @Override
        public void run() {
            checkState(this.supplier != null, "already run?!?");

            try {
                this.complete(this.supplier.get());
            } catch (Throwable t) {
                this.completeExceptionally(t);
            } finally {
                this.group = null;
                this.supplier = null;
            }
        }
    }
}
