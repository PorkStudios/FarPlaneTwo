/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.util.threading.locks.multi;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MultiSemaphore {
    @NonNull
    private final Sync sync;

    protected static abstract class Sync extends AbstractQueuedMultiSynchronizer {
        protected Sync(int permits) {
            this.setState(permits);
        }

        protected final int getPermits() {
            return this.getState();
        }

        protected final int nonfairTryAcquireShared(int acquires) {
            for (; ; ) {
                int available = this.getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    this.compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }

        @Override
        protected final boolean tryReleaseShared(int releases) {
            for (; ; ) {
                int current = this.getState();
                int next = current + releases;
                if (next < current) // overflow
                {
                    throw new Error("Maximum permit count exceeded");
                }
                if (this.compareAndSetState(current, next)) {
                    return true;
                }
            }
        }

        protected final void reducePermits(int reductions) {
            for (; ; ) {
                int current = this.getState();
                int next = current - reductions;
                if (next > current) // underflow
                {
                    throw new Error("Permit count underflow");
                }
                if (this.compareAndSetState(current, next)) {
                    return;
                }
            }
        }

        protected final int drainPermits() {
            for (; ; ) {
                int current = this.getState();
                if (current == 0 || this.compareAndSetState(current, 0)) {
                    return current;
                }
            }
        }
    }

    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return this.nonfairTryAcquireShared(acquires);
        }
    }

    public MultiSemaphore(int permits) {
        this(new NonfairSync(permits));
    }

    public SyncOperation<?> prepareAcquire() {
        return this.sync.prepareAcquireShared(1);
    }

    public void acquire() throws InterruptedException {
        this.sync.acquireSharedInterruptibly(1);
    }

    public void acquireUninterruptibly() {
        this.sync.acquireShared(1);
    }

    public boolean tryAcquire() {
        return this.sync.nonfairTryAcquireShared(1) >= 0;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public void release() {
        this.sync.releaseShared(1);
    }

    public SyncOperation<?> prepareAcquire(int permits) {
        return this.sync.prepareAcquireShared(notNegative(permits, "permits"));
    }

    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.acquireSharedInterruptibly(permits);
    }

    public void acquireUninterruptibly(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.acquireShared(permits);
    }

    public boolean tryAcquire(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        return this.sync.nonfairTryAcquireShared(permits) >= 0;
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        return this.sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    public void release(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.releaseShared(permits);
    }

    public int availablePermits() {
        return this.sync.getPermits();
    }

    public int drainPermits() {
        return this.sync.drainPermits();
    }

    protected void reducePermits(int reduction) {
        if (reduction < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.reducePermits(reduction);
    }

    public boolean isFair() {
        return false; //TODO: this.sync instanceof FairSync;
    }

    public String toString() {
        return super.toString() + "[Permits = " + this.sync.getPermits() + ']';
    }
}
