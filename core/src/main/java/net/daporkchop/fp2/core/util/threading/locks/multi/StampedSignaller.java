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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * A simple synchronization aid which allows threads to block until they receive a signal.
 * <p>
 * This is conceptually similar to both<br>
 * <ul>
 *     <li>{@link Object}'s {@link Object#wait()}/{@link Object#notify()}</li>
 *     <li>{@link Lock}'s {@link Condition#await()}/{@link Condition#signal()}</li>
 * </ul><br>
 * except that it does not require waiters to hold any locks in order to await or send signals.
 * <p>
 * Additionally, it supports "stamps": arbitrary {@code long} tokens which are invalidated every time a signal is broadcast. Waiters can await invalidation of a specific stamp,
 * allowing them to detect signals which may have been sent prior to retrieving the current stamp.
 *
 * @author DaPorkchop_
 */
//TODO: this isn't entirely race-free, but for now i don't care
public final class StampedSignaller {
    private static final long NODE_STATE_OFFSET = PUnsafe.pork_getOffset(Node.class, "state");

    private final AtomicLong generationCounter = new AtomicLong();
    private final Queue<Node> queue = new ConcurrentLinkedQueue<>();

    /**
     * Signals all waiters and invalidates the current stamp.
     */
    public void signalAll() {
        this.generationCounter.incrementAndGet();

        Queue<Node> queue = this.queue;
        for (Node waiter; (waiter = queue.poll()) != null; ) {
            if (waiter.casState(Node.WAITING, Node.SIGNALLED)) {
                LockSupport.unpark(waiter.thread);
            }
        }
    }

    /**
     * @return the current stamp
     */
    public long stamp() {
        return this.generationCounter.get();
    }

    /**
     * @return a {@link SyncOperation} which will block until a signal is received
     */
    public SyncOperation<?> prepareAwait() {
        return new SyncOperation<Node>() {
            @Override
            protected boolean tryEarly() {
                return false; //there's never anything to do
            }

            @Override
            protected Node createState() {
                Node node = new Node(Thread.currentThread());
                StampedSignaller.this.queue.add(node);
                return node;
            }

            @Override
            protected boolean tryComplete(Node node) {
                return node.state != Node.WAITING;
            }

            @Override
            protected boolean shouldParkAfterFailedComplete(Node node) {
                return node.state == Node.WAITING;
            }

            @Override
            protected void disposeState(Node node) {
                StampedSignaller.this.queue.remove(node);
            }
        };
    }

    /**
     * @return a {@link SyncOperation} which will block until the current stamp is invalidated
     */
    public SyncOperation<?> prepareAwaitNow() {
        return this.prepareAwait(this.stamp());
    }

    /**
     * @param stamp the stamp to await invalidation of
     * @return a {@link SyncOperation} which will block until the given stamp is invalidated
     */
    public SyncOperation<?> prepareAwait(long stamp) {
        return new SyncOperation<Node>() {
            @Override
            protected boolean tryEarly() {
                return StampedSignaller.this.generationCounter.get() != stamp;
            }

            @Override
            protected Node createState() {
                Node node = new Node(Thread.currentThread());
                StampedSignaller.this.queue.add(node);
                return node;
            }

            @Override
            protected boolean tryComplete(Node node) {
                return node.state != Node.WAITING || StampedSignaller.this.generationCounter.get() != stamp;
            }

            @Override
            protected boolean shouldParkAfterFailedComplete(Node node) {
                return node.state == Node.WAITING && StampedSignaller.this.generationCounter.get() == stamp;
            }

            @Override
            protected void disposeState(Node node) {
                if (node.casState(Node.WAITING, Node.CANCELLED)) {
                    StampedSignaller.this.queue.remove(node);
                }
            }
        };
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static final class Node {
        private static final int WAITING = 0;
        private static final int SIGNALLED = 1;
        private static final int CANCELLED = 2;

        @NonNull
        private final Thread thread;
        private volatile int state = WAITING;

        public boolean casState(int expect, int update) {
            return PUnsafe.compareAndSwapInt(this, NODE_STATE_OFFSET, expect, update);
        }
    }
}
