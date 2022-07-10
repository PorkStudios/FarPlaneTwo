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
 */

package net.daporkchop.fp2.core.util.threading.locks.multi;

import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An alternative to the standard JDK {@link Semaphore} whose permits can additionally be {@link #acquire(int) acquired} through a {@link SyncOperation}, which allows them to
 * be executed in parallel with other {@link SyncOperation}s using {@link SyncAggregator}.
 *
 * @author DaPorkchop_
 */
@SuppressWarnings("JavadocReference")
public class MultiSemaphore {
    private final Sync sync;

    /**
     * @see Semaphore#Semaphore(int)
     */
    public MultiSemaphore(int permits) {
        this.sync = new NonfairSync(permits);
    }

    /**
     * @see Semaphore#Semaphore(int, boolean)
     */
    public MultiSemaphore(int permits, boolean fair) {
        this.sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * @return a {@link SyncOperation} which will acquire a single permit from this semaphore
     */
    public SyncOperation<?> prepareAcquire() {
        return this.sync.prepareAcquireShared(1);
    }

    /**
     * @see Semaphore#acquire(int)
     */
    public void acquire() throws InterruptedException {
        this.sync.acquireSharedInterruptibly(1);
    }

    /**
     * @see Semaphore#acquireUninterruptibly()
     */
    public void acquireUninterruptibly() {
        this.sync.acquireShared(1);
    }

    /**
     * @see Semaphore#tryAcquire()
     */
    public boolean tryAcquire() {
        return this.sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * @see Semaphore#tryAcquire(long, TimeUnit)
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * @see Semaphore#release()
     */
    public void release() {
        this.sync.releaseShared(1);
    }

    /**
     * @param permits the number of permits to acquire
     * @return a {@link SyncOperation} which will acquire the given number of permits from this semaphore
     */
    public SyncOperation<?> prepareAcquire(int permits) {
        return this.sync.prepareAcquireShared(notNegative(permits, "permits"));
    }

    /**
     * @see Semaphore#acquire(int)
     */
    public void acquire(int permits) throws InterruptedException {
        this.sync.acquireSharedInterruptibly(notNegative(permits, "permits"));
    }

    /**
     * @see Semaphore#acquireUninterruptibly(int)
     */
    public void acquireUninterruptibly(int permits) {
        this.sync.acquireShared(notNegative(permits, "permits"));
    }

    /**
     * @see Semaphore#tryAcquire(int)
     */
    public boolean tryAcquire(int permits) {
        return this.sync.nonfairTryAcquireShared(notNegative(permits, "permits")) >= 0;
    }

    /**
     * @see Semaphore#tryAcquire(int, long, TimeUnit)
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(notNegative(permits, "permits"), unit.toNanos(timeout));
    }

    /**
     * @see Semaphore#release(int)
     */
    public void release(int permits) {
        this.sync.releaseShared(notNegative(permits, "permits"));
    }

    /**
     * @see Semaphore#availablePermits()
     */
    public int availablePermits() {
        return this.sync.getPermits();
    }

    /**
     * @see Semaphore#drainPermits()
     */
    public int drainPermits() {
        return this.sync.drainPermits();
    }

    /**
     * @see Semaphore#reducePermits(int)
     */
    protected void reducePermits(int reduction) {
        this.sync.reducePermits(notNegative(reduction, "reduction"));
    }

    /**
     * @see Semaphore#isFair()
     */
    public boolean isFair() {
        return this.sync instanceof FairSync;
    }

    /**
     * @see Semaphore#hasQueuedThreads()
     */
    public boolean hasQueuedThreads() {
        return this.sync.hasQueuedThreads();
    }

    /**
     * @see Semaphore#getQueueLength()
     */
    public int getQueueLength() {
        return this.sync.getQueueLength();
    }

    /**
     * @see Semaphore#getQueuedThreads()
     */
    protected Collection<Thread> getQueuedThreads() {
        return this.sync.getQueuedThreads();
    }

    @Override
    public String toString() {
        return super.toString() + "[Permits = " + this.sync.getPermits() + ']';
    }

    /**
     * Synchronization implementation for {@link MultiSemaphore}.
     */
    private static abstract class Sync extends AbstractQueuedMultiSynchronizer {
        protected Sync(int permits) {
            this.setState(permits);
        }

        protected final int getPermits() {
            return this.getState();
        }

        protected final int nonfairTryAcquireShared(int acquires) {
            do {
                int available = this.getState();
                int remaining = available - acquires;

                if (remaining < 0 || this.compareAndSetState(available, remaining)) {
                    return remaining;
                }
            } while (true);
        }

        @Override
        protected final boolean tryReleaseShared(int releases) {
            do {
                int current = this.getState();
                int next = addExact(current, releases);

                if (this.compareAndSetState(current, next)) {
                    return true;
                }
            } while (true);
        }

        protected final void reducePermits(int reductions) {
            do {
                int current = this.getState();
                int next = subtractExact(current, reductions);

                if (this.compareAndSetState(current, next)) {
                    return;
                }
            } while (true);
        }

        protected final int drainPermits() {
            do {
                int current = this.getState();

                if (current == 0 || this.compareAndSetState(current, 0)) {
                    return current;
                }
            } while (true);
        }
    }

    /**
     * Non-fair version of {@link Sync}.
     */
    private static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        private NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return this.nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * Fair version of {@link Sync}.
     */
    private static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        private FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            do {
                if (this.hasQueuedPredecessors()) {
                    return -1;
                }

                int available = this.getState();
                int remaining = available - acquires;

                if (remaining < 0 || this.compareAndSetState(available, remaining)) {
                    return remaining;
                }
            } while (true);
        }
    }
}
