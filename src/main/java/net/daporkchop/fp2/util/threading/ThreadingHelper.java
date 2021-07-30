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

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor;
import net.daporkchop.fp2.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.util.threading.futureexecutor.MarkedFutureExecutor;
import net.daporkchop.fp2.util.threading.futureexecutor.MarkingForwardingFutureExecutor;
import net.daporkchop.fp2.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor;
import net.daporkchop.fp2.util.threading.futureexecutor.ThreadValidatingForwardingFutureExecutor;
import net.daporkchop.fp2.util.threading.workergroup.WorkerGroupBuilder;
import net.daporkchop.fp2.util.threading.workergroup.WorldWorkerGroup;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Global utilities for multithreading things.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ThreadingHelper {
    private final Map<Thread, WorldWorkerGroup> THREADS_TO_GROUPS = new ConcurrentHashMap<>();
    private final Set<Thread> BLOCKED_THREADS = ConcurrentHashMap.newKeySet();

    /**
     * @return a new {@link WorkerGroupBuilder}
     */
    public WorkerGroupBuilder workerGroupBuilder() {
        return new WorkerGroupBuilder() {
            @Override
            public WorldWorkerGroup build(@NonNull Runnable task) {
                this.validate();
                return new DefaultWorldWorkerGroup(this, task);
            }
        };
    }

    /**
     * Handles the given exception.
     *
     * @param world the world that the exception occurred in
     * @param t     the exception to handle
     */
    public void handle(@NonNull World world, @NonNull Throwable t) {
        //wrap the exception to make the current stack trace be included
        FP2_LOG.error(PStrings.fastFormat("exception in world %s on thread %s", world, Thread.currentThread()), t);
        scheduleTaskInWorldThread(world, () -> PUnsafe.throwException(t));
    }

    /**
     * Schedules a task to be executed on the given world's thread.
     *
     * @param world the world whose thread the task should be exected on
     * @param task  the task to execute
     */
    public CompletableFuture<Void> scheduleTaskInWorldThread(@NonNull World world, @NonNull Runnable task) {
        return workExecutorFor(world).run(task);
    }

    /**
     * Schedules a task to be executed on the given world's thread.
     *
     * @param world the world whose thread the task should be exected on
     * @param task  the task to execute
     */
    public <V> CompletableFuture<V> scheduleTaskInWorldThread(@NonNull World world, @NonNull Supplier<V> task) {
        return workExecutorFor(world).supply(task);
    }

    protected FutureExecutor workExecutorFor(@NonNull World world) {
        WorldWorkerGroup workerGroup = THREADS_TO_GROUPS.get(Thread.currentThread());
        if (workerGroup != null) { //this is a worker thread, return the world-specific executor thread
            checkArg(world == workerGroup.world(), "thread %s attempted to submit task for a world it doesn't belong to!", Thread.currentThread());
            return workerGroup.worldExecutor();
        }

        //if it isn't a worker thread, we can just submit the task to the root executor
        return rootExecutorFor(world);
    }

    protected MarkedFutureExecutor rootExecutorFor(@NonNull World world) {
        return world.isRemote
                ? ClientThreadMarkedFutureExecutor.getFor(Minecraft.getMinecraft())
                : ServerThreadMarkedFutureExecutor.getFor(FMLCommonHandler.instance().getMinecraftServerInstance());
    }

    /**
     * Ensures that the given thread is valid for use as a worker thread.
     *
     * @param thread the thread to check
     * @return the thread
     */
    public Thread checkWorkerThread(@NonNull Thread thread) {
        checkArg(!(thread instanceof ForkJoinWorkerThread), "%s is a ForkJoinWorkerThread!", thread);
        checkArg(FMLCommonHandler.instance().getSide() != Side.CLIENT || Minecraft.getMinecraft().thread != thread, "%s is the client thread!", thread);
        checkArg(FMLCommonHandler.instance().getMinecraftServerInstance().serverThread != thread, "%s is the server thread!", thread);

        return thread;
    }

    /**
     * Begins an interruptible blocking operation whose interruption is managed by {@link ThreadingHelper}.
     */
    public void managedBlock() {
        checkState(BLOCKED_THREADS.add(Thread.currentThread()), "recursively blocking task?!?");
    }

    /**
     * Ends an operation initiated by {@link #managedBlock()}.
     */
    @SneakyThrows(InterruptedException.class)
    public void managedUnblock() {
        if (BLOCKED_THREADS.remove(Thread.currentThread())) { //we were blocking, but we should still double-check to see if we've been interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        } else { //the thread has been unblocked by something else - wait for the interrupt to arrive
            LockSupport.park();

            throw new InterruptedException();
        }
    }

    /**
     * Interrupts a thread which was is currently doing a blocking operation managed by {@link #managedBlock()}.
     *
     * @param thread the thread
     */
    public void externalManagedUnblock(@NonNull Thread thread) {
        if (BLOCKED_THREADS.remove(thread)) { //we managed to remove the thread - let's interrupt it
            thread.interrupt();
        }
    }

    @Getter
    private static class DefaultWorldWorkerGroup extends AbstractReleasable implements WorldWorkerGroup {
        private final World world;
        private final Set<Thread> threads;
        private final FutureExecutor worldExecutor;

        public DefaultWorldWorkerGroup(@NonNull WorkerGroupBuilder builder, @NonNull Runnable r) {
            this.world = builder.world();
            this.threads = ImmutableSet.copyOf(IntStream.range(0, builder.threads())
                    .mapToObj(i -> builder.threadFactory().newThread(r))
                    .toArray(Thread[]::new));

            this.worldExecutor = new ThreadValidatingForwardingFutureExecutor(
                    new MarkingForwardingFutureExecutor(rootExecutorFor(this.world)),
                    this.threads::contains);

            //attempt to insert all threads into THREADS_TO_GROUPS
            this.threads.forEach(thread -> {
                if (THREADS_TO_GROUPS.putIfAbsent(thread, this) != null) { //insertion failed - safely remove all entries that may have been inserted, then throw exception
                    this.threads.forEach(t -> THREADS_TO_GROUPS.remove(t, this));
                    throw new IllegalStateException(PStrings.fastFormat("unable to insert thread->group mapping %s->%s to THREADS_TO_GROUPS map?!?", thread, this));
                }
            });

            //it should be safe now to start all the threads
            this.threads.forEach(Thread::start);
        }

        @Override
        public void release() throws AlreadyReleasedException {
            checkState(!this.threads.contains(Thread.currentThread()), "thread %s cannot release it's own worker group!", Thread.currentThread());
            super.release();
        }

        @Override
        protected void doRelease() {
            //we want to avoid interrupting the workers, because that can have annoying side effects (such as closing NIO channels).

            //closing the world executor will cancel all tasks which might have been pending execution on the server thread, thus preventing a deadlock in the case where
            //  we're currently on the server thread, but any of our workers was waiting for the server thread to do something.
            this.worldExecutor.close();

            //wait for all workers to shut down
            boolean interrupted = false;
            for (Thread thread : this.threads) {
                do {
                    externalManagedUnblock(thread);

                    try {
                        thread.join(50L);
                    } catch (InterruptedException e) {
                        FP2_LOG.error(PStrings.fastFormat("%s was interrupted while waiting for %s to exit", Thread.currentThread(), thread), e);
                        interrupted = true;
                    }
                } while (thread.isAlive());
            }

            //remove all threads from thread->group map now that they're shut down
            this.threads.forEach(thread -> checkState(THREADS_TO_GROUPS.remove(thread, this), "unable to remove thread->group mapping %s->%s from THREADS_TO_GROUPS map?!?", thread, this));

            if (interrupted) { //restore interrupted state
                Thread.currentThread().interrupt();
            }
        }
    }
}
