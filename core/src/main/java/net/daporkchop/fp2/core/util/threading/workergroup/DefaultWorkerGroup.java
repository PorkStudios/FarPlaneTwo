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

package net.daporkchop.fp2.core.util.threading.workergroup;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.util.threading.BlockingSupport;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.core.util.threading.futureexecutor.MarkingForwardingFutureExecutor;
import net.daporkchop.fp2.core.util.threading.futureexecutor.ThreadValidatingForwardingFutureExecutor;
import net.daporkchop.lib.common.misc.string.PStrings;

import java.util.Set;
import java.util.stream.IntStream;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Default implementation of {@link WorkerGroup}.
 *
 * @author DaPorkchop_
 */
@Getter
public class DefaultWorkerGroup implements WorkerGroup {
    protected final DefaultWorkerManager manager;

    protected final Set<Thread> threads;
    protected final FutureExecutor worldExecutor;

    public DefaultWorkerGroup(@NonNull DefaultWorkerGroupBuilder builder, @NonNull Runnable r) {
        this.manager = builder.manager();

        this.threads = ImmutableSet.copyOf(IntStream.range(0, builder.threads())
                .mapToObj(i -> builder.threadFactory().newThread(r))
                .toArray(Thread[]::new));

        this.worldExecutor = new ThreadValidatingForwardingFutureExecutor(
                new MarkingForwardingFutureExecutor(this.manager.rootExecutor()),
                this.threads::contains);

        //attempt to insert all threads into THREADS_TO_GROUPS
        this.threads.forEach(thread -> {
            if (this.manager.threadsToGroups().putIfAbsent(thread, this) != null) { //insertion failed - safely remove all entries that may have been inserted, then throw exception
                this.threads.forEach(t -> this.manager.threadsToGroups().remove(t, this));
                throw new IllegalStateException(PStrings.fastFormat("unable to insert thread->group mapping %s->%s to THREADS_TO_GROUPS map?!?", thread, this));
            }
        });

        //it should be safe now to start all the threads
        this.threads.forEach(Thread::start);
    }

    @Override
    public void close() {
        checkState(!this.threads.contains(Thread.currentThread()), "thread %s cannot release it's own worker group!", Thread.currentThread());

        //we want to avoid interrupting the workers, because that can have annoying side effects (such as closing NIO channels).

        //closing the world executor will cancel all tasks which might have been pending execution on the server thread, thus preventing a deadlock in the case where
        //  we're currently on the server thread, but any of our workers was waiting for the server thread to do something.
        this.worldExecutor.close();

        //wait for all workers to shut down
        boolean interrupted = false;
        for (Thread thread : this.threads) {
            do {
                BlockingSupport.externalManagedUnblock(thread);

                try {
                    thread.join(50L);
                } catch (InterruptedException e) {
                    fp2().log().error(PStrings.fastFormat("%s was interrupted while waiting for %s to exit", Thread.currentThread(), thread), e);
                    interrupted = true;
                }
            } while (thread.isAlive());
        }

        //remove all threads from thread->group map now that they're shut down
        this.threads.forEach(thread -> checkState(this.manager.threadsToGroups().remove(thread, this), "unable to remove thread->group mapping %s->%s from THREADS_TO_GROUPS map?!?", thread, this));

        if (interrupted) { //restore interrupted state
            Thread.currentThread().interrupt();
        }
    }
}
