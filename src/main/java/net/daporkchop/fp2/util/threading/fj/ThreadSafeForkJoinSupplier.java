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
import java.util.concurrent.RecursiveTask;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Alternative to {@link RecursiveTask} which may be safely {@link #join()}ed from multiple threads.
 *
 * @author DaPorkchop_
 */
public abstract class ThreadSafeForkJoinSupplier<V> extends ForkJoinTask<V> {
    protected static final long VALUE_OFFSET = PUnsafe.pork_getOffset(ThreadSafeForkJoinSupplier.class, "value");

    protected static final Object RUNNING_VALUE = new Object[0];
    protected static final Object NULL_VALUE = new Object[0];

    protected volatile Object value = null;

    @Override
    public V getRawResult() {
        Object value = this.value;
        return value != RUNNING_VALUE && value != NULL_VALUE ? uncheckedCast(value) : null;
    }

    @Override
    protected void setRawResult(V value) {
        this.value = value;
    }

    @Override
    protected boolean exec() {
        if (PUnsafe.compareAndSwapObject(this, VALUE_OFFSET, null, RUNNING_VALUE)) {
            this.value = fallbackIfNull(this.compute(), NULL_VALUE);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Actually runs the task.
     */
    protected abstract V compute();
}
