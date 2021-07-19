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

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.threading.specific.WorldThreadExecutor;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
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
    private final Map<World, WorldThreadExecutor> WORLD_EXECUTORS = new ConcurrentHashMap<>();

    /**
     * Starts a new {@link WorkerGroup}.
     */
    public WorkerGroup startWorkers(World world, int threads, @NonNull ThreadFactory threadFactory, @NonNull Runnable r) {
        WorkerGroup group = new DefaultWorkerGroup(world, threads, threadFactory, r);
        group.threads().forEach(Thread::start);
        return group;
    }

    /**
     * Handles the given exception.
     *
     * @param t the exception to handle
     */
    public void handle(@NonNull Throwable t) {
        FP2_LOG.error(t);
        ServerThreadExecutor.INSTANCE.execute(() -> PUnsafe.throwException(t));
    }

    /**
     * Ensures that the given thread is valid for use as a worker thread.
     *
     * @param thread the thread to check
     * @return the thread
     */
    public Thread checkWorkerThread(@NonNull Thread thread) {
        checkArg(!(thread instanceof ForkJoinWorkerThread), "%s is a ForkJoinWorkerThread!", thread);
        checkArg(FMLCommonHandler.instance().getSide() != Side.CLIENT || !ClientThreadExecutor.INSTANCE.isClientThread(thread), "%s is the client thread!", thread);
        checkArg(!ServerThreadExecutor.INSTANCE.isServerThread(thread), "%s is the server thread!", thread);

        return thread;
    }

    public void putWorldThreadExecutor(@NonNull World worldIn, @NonNull WorldThreadExecutor executor) {
        checkArg(worldIn == executor.world(), "executor doesn't belong to world %s", worldIn);
        WORLD_EXECUTORS.compute(worldIn, (world, e) -> {
            checkState(e == null, "already stored an executor for world %s", world);

            executor.start();
            return executor;
        });
    }

    public void removeWorldThreadExecutor(@NonNull World worldIn) {
        WORLD_EXECUTORS.compute(worldIn, (world, executor) -> {
            checkState(executor != null, "no executor stored for world %s", world);

            executor.close();
            return null;
        });
    }

    public WorldThreadExecutor worldExecutor(@NonNull World world) {
        WorldThreadExecutor executor = WORLD_EXECUTORS.get(world);
        checkArg(executor != null, "no executor exists for world %s", world);
        return executor;
    }

    @Getter
    private static class DefaultWorkerGroup implements WorkerGroup {
        private final Set<Thread> threads;
        private World world;

        public DefaultWorkerGroup(World world, int size, @NonNull ThreadFactory threadFactory, @NonNull Runnable r) {
            this.threads = IntStream.range(0, positive(size, "size")).mapToObj(i -> threadFactory.newThread(r)).collect(Collectors.toSet());

            if (world != null) {
                worldExecutor(this.world = world).addChild(this);
            }
        }

        @Override
        public synchronized void close() {
            if (this.world != null) {
                worldExecutor(this.world).removeChild(this);
            }

            //we want to avoid calling Thread#interrupt(), because that can have annoying side effects such as closing NIO channels.

            //wait for all workers to shut down
            RuntimeException exception = new RuntimeException();
            for (Thread thread : this.threads) {
                do {
                    if (ServerThreadExecutor.INSTANCE.isServerThread()) { //some workers might be blocked waiting for the server thread to do something, so we should make sure to keep the queue drained
                        try {
                            ServerThreadExecutor.INSTANCE.workOffQueue();
                        } catch (Throwable e) {
                            FP2_LOG.error(PStrings.fastFormat("exception on ServerThreadExecutor while waiting for %s to exit", thread), e);
                            exception.addSuppressed(e);
                        }
                    }

                    try {
                        thread.join(50L);
                    } catch (InterruptedException e) {
                        FP2_LOG.error(PStrings.fastFormat("%s was interrupted while waiting for %s to exit", Thread.currentThread(), thread), e);
                        exception.addSuppressed(e);
                    }
                } while (thread.isAlive());
            }

            if (exception.getSuppressed().length != 0) { //if any exceptions were thrown, we should rethrow them
                throw exception;
            }
        }
    }
}
