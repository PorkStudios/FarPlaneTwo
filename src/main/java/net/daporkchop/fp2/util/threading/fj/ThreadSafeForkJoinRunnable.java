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

package net.daporkchop.fp2.util.threading.fj;

import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Alternative to {@link RecursiveAction} which may be safely {@link #join()}ed from multiple threads.
 *
 * @author DaPorkchop_
 */
public abstract class ThreadSafeForkJoinRunnable extends ForkJoinTask<Void> {
    protected static final long STARTED_OFFSET = PUnsafe.pork_getOffset(ThreadSafeForkJoinRunnable.class, "started");

    protected volatile int started = 0;

    @Override
    public Void getRawResult() {
        return null;
    }

    @Override
    protected void setRawResult(Void value) {
        //no-op
    }

    @Override
    protected boolean exec() {
        if (PUnsafe.compareAndSwapInt(this, STARTED_OFFSET, 0, 1)) {
            this.compute();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Actually runs the task.
     */
    protected abstract void compute();
}
