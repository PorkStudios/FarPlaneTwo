/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class ParallelDispatchEventAsFutureExecutor1_16 implements FutureExecutor {
    @NonNull
    protected final ParallelDispatchEvent event;
    @NonNull
    protected final Consumer<Throwable> exceptionHandler;

    @Override
    public CompletableFuture<Void> run(@NonNull Runnable runnable) {
        return this.wrap(this.event.enqueueWork(runnable));
    }

    @Override
    public <V> CompletableFuture<V> supply(@NonNull Supplier<V> supplier) {
        return this.wrap(this.event.enqueueWork(supplier));
    }

    private <V> CompletableFuture<V> wrap(CompletableFuture<V> future) {
        future.whenComplete((result, t) -> {
            if (t != null) {
                this.exceptionHandler.accept(t);
            }
        });
        return future;
    }

    @Override
    public void close() {
        //no-op
    }
}
