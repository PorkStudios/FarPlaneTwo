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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.core.util.threading.futureexecutor.MarkedFutureExecutor;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Default implementation of {@link WorkerManager}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class DefaultWorkerManager implements WorkerManager {
    @NonNull
    protected final Thread rootThread;
    @NonNull
    protected final MarkedFutureExecutor rootExecutor;

    protected final Map<Thread, DefaultWorkerGroup> threadsToGroups = new ConcurrentHashMap<>();

    @Override
    public FutureExecutor workExecutor() {
        if (this.rootThread == Thread.currentThread()) { //this is the root thread
            return this.rootExecutor;
        }

        DefaultWorkerGroup workerGroup = this.threadsToGroups.get(Thread.currentThread());
        if (workerGroup != null) { //this thread belongs to a child worker group
            return workerGroup.worldExecutor();
        }

        throw new IllegalStateException("thread " + Thread.currentThread() + " doesn't have a work executor for submitting tasks to " + this + " (owned by " + this.rootThread + ')');
    }

    @Override
    public WorkerGroupBuilder createChildWorkerGroup() {
        return new DefaultWorkerGroupBuilder(this);
    }

    @Override
    public void handle(@NonNull Throwable t) {
        fp2().log().error("exception on thread %s", t, Thread.currentThread());
        this.rootExecutor.execute(() -> PUnsafe.throwException(t));
    }
}
