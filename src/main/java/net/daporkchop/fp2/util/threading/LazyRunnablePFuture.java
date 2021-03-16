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

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;
import net.daporkchop.lib.concurrent.PExecutors;
import net.daporkchop.lib.concurrent.PFuture;
import net.daporkchop.lib.concurrent.future.DefaultPFuture;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A {@link PFuture} which must be run manually (using {@link Runnable#run()}) in order to compute its value.
 *
 * @author DaPorkchop_
 */
public abstract class LazyRunnablePFuture<V> extends DefaultPFuture<V> implements Runnable {
    protected static final long STARTED_OFFSET = PUnsafe.pork_getOffset(LazyRunnablePFuture.class, "started");

    protected volatile int started = 0;

    public LazyRunnablePFuture() {
        super(PExecutors.FORKJOINPOOL);
    }

    @Override
    public void run() {
        if (!PUnsafe.compareAndSwapInt(this, STARTED_OFFSET, 0, 1)) {
            //the task has already started execution
            return;
        }

        //run function and complete future
        try {
            this.setSuccess(this.run0());
        } catch (Throwable t) {
            this.setFailure(t);
        }

        this.postRun();
    }

    /**
     * Computes the value to be returned by this future.
     *
     * @return the value
     */
    protected abstract V run0() throws Exception;

    /**
     * Called after the future is run.
     */
    protected void postRun() {
        //no-op
    }
}
