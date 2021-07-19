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

import lombok.NonNull;
import net.daporkchop.fp2.util.threading.WorkerGroup;
import net.minecraft.world.World;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An executor which accepts tasks from async workers and executes them on specific world's thread.
 *
 * @author DaPorkchop_
 */
public interface WorldThreadExecutor extends AutoCloseable {
    /**
     * @return the world whose thread this executor will execute tasks on
     */
    World world();

    /**
     * Informs this executor that the threads in the given {@link WorkerGroup} may begin to submit tasks to this executor.
     *
     * @param group the {@link WorkerGroup} to add
     */
    void addChild(@NonNull WorkerGroup group);

    /**
     * Informs this executor that the threads in the given {@link WorkerGroup} will no longer submit tasks to this executor.
     * <p>
     * This will also result in the cancellation of any pending tasks submitted by threads in the given {@link WorkerGroup}.
     *
     * @param group the {@link WorkerGroup} to remove
     */
    void removeChild(@NonNull WorkerGroup group);

    /**
     * Executes the given {@link Supplier} on this thread.
     *
     * @param supplier the {@link Supplier} to run
     * @return a {@link CompletableFuture} which will be completed with the {@link Supplier}'s return value
     */
    <V> CompletableFuture<V> supply(@NonNull Supplier<V> supplier);

    void start();

    @Override
    void close();
}
