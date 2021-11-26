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

package net.daporkchop.fp2.core.util.threading;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerGroup;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.LockSupport;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Utilities for thread blocking and unblocking.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class BlockingSupport {
    private final Set<Thread> BLOCKED_THREADS = ConcurrentHashMap.newKeySet();

    /**
     * Blocks while waiting for the given {@link CompletableFuture} to be completed.
     *
     * @param future the {@link CompletableFuture}
     * @return the {@link CompletableFuture}'s return value
     * @throws IllegalStateException if the current thread does not belong to a {@link WorkerGroup} which is a child of this {@link WorkerManager}
     */
    @SneakyThrows(InterruptedException.class)
    public static <V> V managedBlock(@NonNull CompletableFuture<V> future) {
        checkState(BLOCKED_THREADS.add(Thread.currentThread()), "recursively blocking task?!?");

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        } finally {
            if (BLOCKED_THREADS.remove(Thread.currentThread())) { //we were blocking, but we should still double-check to see if we've been interrupted
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } else { //the thread has been unblocked by something else - wait for the interrupt to arrive
                LockSupport.park();

                throw new InterruptedException();
            }
        }
    }

    /**
     * Interrupts a thread which was is currently blocked by {@link #managedBlock(CompletableFuture)}.
     *
     * @param thread the thread
     */
    public static void externalManagedUnblock(@NonNull Thread thread) {
        if (BLOCKED_THREADS.remove(thread)) { //we managed to remove the thread - let's interrupt it
            thread.interrupt();
        }
    }
}
