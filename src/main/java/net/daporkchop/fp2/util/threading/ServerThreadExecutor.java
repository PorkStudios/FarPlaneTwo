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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraftforge.common.WorldWorkerManager;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An {@link Executor} which executes submitted tasks on the server thread.
 * <p>
 * By using {@link WorldWorkerManager}, it is able to execute tasks in between ticks, and limit the number of tasks executed per tick to minimize tick lag.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerThreadExecutor implements WorldWorkerManager.IWorker, Executor {
    public static final ServerThreadExecutor INSTANCE = new ServerThreadExecutor();

    private static final long ADDED_OFFSET = PUnsafe.pork_getOffset(ServerThreadExecutor.class, "added");

    protected final Queue<Runnable> tasks = new ArrayDeque<>();
    protected volatile Thread serverThread;
    protected volatile int added = 0;

    @Override
    public boolean hasWork() {
        checkState(Thread.currentThread() == this.serverThread);
        return true; //always return true to ensure that forge doesn't remove us from the worker list
    }

    @Override
    public boolean doWork() {
        checkState(Thread.currentThread() == this.serverThread);

        Runnable task;
        synchronized (this.tasks) {
            task = this.tasks.poll();
        }

        if (task != null) {
            task.run();
            return !this.tasks.isEmpty();
        } else {
            return false; //task queue is empty, we're done for this tick
        }
    }

    @Override
    public void execute(@NonNull Runnable command) {
        checkState(this.added == 1, "executor is not currently active!");
        if (Thread.currentThread() == this.serverThread) {
            command.run(); //we're currently on the server thread, run the task now without queuing
        } else {
            synchronized (this.tasks) {
                checkState(this.added == 1, "executor is not currently active!");
                this.tasks.add(command);
            }
        }
    }

    public boolean isServerThread() {
        return this.isServerThread(Thread.currentThread());
    }

    public boolean isServerThread(@NonNull Thread thread) {
        checkState(this.added >= 1, "executor is not currently active!");
        return thread == this.serverThread;
    }

    public void checkServerThread() {
        checkState(this.isServerThread(), "not on server thread?!?");
    }

    public void startup() {
        checkState(PUnsafe.compareAndSwapInt(this, ADDED_OFFSET, 0, 1), "already added?!?");
        this.serverThread = Thread.currentThread();
        WorldWorkerManager.addWorker(this);
    }

    public void workOffQueue() {
        checkState(this.added >= 1, "executor is not currently active!");
        this.checkServerThread();

        //work off queue
        synchronized (this.tasks) {
            RuntimeException exception = null;
            Runnable task;
            while ((task = this.tasks.poll()) != null) {
                try {
                    task.run();
                } catch (Exception e) {
                    if (exception == null) {
                        exception = new RuntimeException();
                    }
                    exception.addSuppressed(e);
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
    }

    public void shutdown() {
        checkState(Thread.currentThread() == this.serverThread);
        checkState(PUnsafe.compareAndSwapInt(this, ADDED_OFFSET, 1, 2), "not added?!?");

        synchronized (this.tasks) {
            checkState(this.tasks.isEmpty());
        }

        this.serverThread = null;
        this.added = 0; //mark as ready to be added again
    }
}
