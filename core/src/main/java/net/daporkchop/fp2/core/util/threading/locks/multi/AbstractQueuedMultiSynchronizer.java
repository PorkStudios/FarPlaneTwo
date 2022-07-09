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
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Objects;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;

/**
 * A version of {@link AbstractQueuedSynchronizer} which allows executing multiple potentially blocking operations simultaneously by aggregating them through an
 * {@link SyncAggregator}.
 *
 * @author DaPorkchop_
 */
@SuppressWarnings({ "JavadocReference" })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractQueuedMultiSynchronizer extends AbstractOwnableSynchronizer {
    private static final long serialVersionUID = 3994232018730335587L;

    private static final long HEAD_OFFSET = PUnsafe.pork_getOffset(AbstractQueuedMultiSynchronizer.class, "head");
    private static final long TAIL_OFFSET = PUnsafe.pork_getOffset(AbstractQueuedMultiSynchronizer.class, "tail");
    private static final long STATE_OFFSET = PUnsafe.pork_getOffset(AbstractQueuedMultiSynchronizer.class, "state");

    private static final long NODE_WAITSTATUS_OFFSET = PUnsafe.pork_getOffset(Node.class, "waitStatus");
    private static final long NODE_NEXT_OFFSET = PUnsafe.pork_getOffset(Node.class, "next");

    /**
     * @see AbstractQueuedSynchronizer#spinForTimeoutThreshold
     */
    private static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000L;

    /**
     * @see AbstractQueuedSynchronizer.Node#head
     */
    private transient volatile Node head;
    /**
     * @see AbstractQueuedSynchronizer.Node#tail
     */
    private transient volatile Node tail;

    /**
     * @see AbstractQueuedSynchronizer.Node#state
     */
    private volatile int state;

    /**
     * @see AbstractQueuedSynchronizer#getState()
     */
    protected final int getState() {
        return this.state;
    }

    /**
     * @see AbstractQueuedSynchronizer#setState(int)
     */
    protected final void setState(int state) {
        this.state = state;
    }

    /**
     * @see AbstractQueuedSynchronizer#compareAndSetState(int, int)
     */
    protected final boolean compareAndSetState(int expected, int update) {
        return PUnsafe.compareAndSwapInt(this, STATE_OFFSET, expected, update);
    }

    /**
     * @see AbstractQueuedSynchronizer#compareAndSetHead(AbstractQueuedSynchronizer.Node)
     */
    private boolean compareAndSetHead(Node expected, Node update) {
        return PUnsafe.compareAndSwapObject(this, HEAD_OFFSET, expected, update);
    }

    /**
     * @see AbstractQueuedSynchronizer#compareAndSetTail(AbstractQueuedSynchronizer.Node, AbstractQueuedSynchronizer.Node)
     */
    private boolean compareAndSetTail(Node expected, Node update) {
        return PUnsafe.compareAndSwapObject(this, TAIL_OFFSET, expected, update);
    }

    //
    // QUEUING UTILITIES
    //

    /**
     * @see AbstractQueuedSynchronizer#enq(AbstractQueuedSynchronizer.Node)
     */
    private Node enq(Node node) {
        do {
            Node tail = this.tail;
            if (tail == null) { //there is no tail, we need to initialize the queue
                if (this.compareAndSetHead(null, new Node())) { //try to insert a dummy node at the head of the queue
                    this.tail = this.head;
                }
            } else { //there is already a queue, attempt insertion
                node.prev = tail; //set the current tail as the new node's 'prev'
                if (this.compareAndSetTail(tail, node)) { //try to replace the existing node with the new node as the tail
                    tail.next = node; //insertion was successful, update the old tail's 'next' pointer
                    return tail;
                }
            }
        } while (true); //spin until insertion is successful
    }

    /**
     * @see AbstractQueuedSynchronizer#addWaiter(AbstractQueuedSynchronizer.Node)
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);

        //try the fast path of enq(Node)
        Node tail = this.tail;
        if (tail != null) { //there is already a queue, attempt insertion
            node.prev = tail; //set the current tail as the new node's 'prev'
            if (this.compareAndSetTail(tail, node)) { //try to replace the existing node with the new node as the tail
                tail.next = node; //insertion was successful, update the old tail's 'next' pointer
                return node;
            }
        }

        //fast path failed, fall back to full enq() implementation
        this.enq(node);
        return node;
    }

    /**
     * @see AbstractQueuedSynchronizer#setHead(AbstractQueuedSynchronizer.Node)
     */
    private void setHead(Node node) {
        this.head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * @see AbstractQueuedSynchronizer#unparkSuccessor(AbstractQueuedSynchronizer.Node)
     */
    private void unparkSuccessor(Node node) {
        int waitStatus = node.waitStatus;
        if (waitStatus < 0) { //the node might need to be signalled, try to clear its status
            node.compareAndSetWaitStatus(waitStatus, 0);
        }

        //the thread to unpark is held in the successor, which is usually the next node
        Node successor = node.next;
        if (successor == null || successor.waitStatus > 0) { //if the successor is "null" or cancelled, traverse backwards from tail to find the actual successor
            successor = null;
            for (Node tail = this.tail; tail != null && tail != node; tail = tail.prev) {
                if (tail.waitStatus <= 0) {
                    successor = tail;
                }
            }
        }

        if (successor != null) { //unpark the successor's thread if any
            successor.signal();
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#doReleaseShared()
     */
    private void doReleaseShared() {
        do {
            Node head = this.head;
            if (head != null && head != this.tail) { //the queue has a head
                int waitStatus = head.waitStatus;
                if (waitStatus == Node.SIGNAL) { //the head node's successor's thread needs to be signalled, race to clear the status
                    if (head.compareAndSetWaitStatus(Node.SIGNAL, 0)) { //we won the race, unpark the successor's  thread
                        this.unparkSuccessor(head);
                    } else { //we lost the race, try again from the beginning in case the queue has been modified
                        continue;
                    }
                } else if (waitStatus == 0 //head node's thread doesn't need to be signalled, race to set its state to PROPAGATE
                           && !head.compareAndSetWaitStatus(0, Node.PROPAGATE)) { //we lost the race, try again from the beginning in case the queue has been modified
                    continue;
                }
            }

            if (head == this.head) { //this.head hasn't changed, we can safely exit
                return;
            } else {
                //the queue has been modified since we started, try again from the beginning
            }
        } while (true); //spin until successful
    }

    /**
     * @see AbstractQueuedSynchronizer#setHeadAndPropagate(AbstractQueuedSynchronizer.Node, int)
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node head = this.head; //save old head for future reference
        this.setHead(node);

        //determine whether or not we need to signal the next queued node
        if (propagate > 0 //propagation was explicitly requested by the caller
            || head == null || head.waitStatus < 0 //propagation was recorded
            || (head = this.head) == null || head.waitStatus < 0) {

            Node successor = node.next;
            if (successor == null //we don't know if the next node is shared, as it's apparently null
                || successor.isShared()) { //the next node is waiting in shared mode
                this.doReleaseShared();
            }
        }
    }

    //
    // HELPERS FOR ACQUIRE* VARIANTS
    //

    /**
     * @see AbstractQueuedSynchronizer#cancelAcquire(AbstractQueuedSynchronizer.Node)
     */
    private void cancelAcquire(Node node) {
        if (node == null) { //node doesn't exist, do nothing
            return;
        }

        node.thread = null;

        //find the predecessor
        Node predecessor = node.prev;
        while (predecessor.waitStatus > 0) { //skip cancelled predecessors
            node.prev = predecessor = predecessor.prev;
        }

        //the node which we apparently need to unsplice. if it isn't actually, it means that we lost the race against either another cancel or a signal, in which
        //  case the upcoming CASes will fail and no further action is necessary.
        Node predecessorNext = predecessor.next;

        //set the node's status to cancelled so that other Nodes can skip past it
        node.waitStatus = Node.CANCELLED;

        if (node == this.tail && this.compareAndSetTail(node, predecessor)) { //we're the tail, remove ourselves
            predecessor.compareAndSetNext(predecessorNext, null);
        } else {
            int waitStatus;
            if (predecessor != this.head //predecessor isn't the head of the queue
                && ((waitStatus = predecessor.waitStatus) == Node.SIGNAL //the predecessor is waiting for a signal
                    || (waitStatus <= 0 && predecessor.compareAndSetWaitStatus(waitStatus, Node.SIGNAL))) //we made the predecessor request a signal
                && predecessor.thread != null) { //the predecessor actually has a thread to unblock
                Node next = node.next;
                if (next != null && next.waitStatus <= 0) { //the node being cancelled's successor would like to be signalled
                    predecessor.compareAndSetNext(predecessorNext, next);
                }
            } else { //no signals are requested, wake up the succesor to propagate
                this.unparkSuccessor(node);
            }

            node.next = node; //this serves no purpose other than to help the GC out
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#shouldParkAfterFailedAcquire(AbstractQueuedSynchronizer.Node, AbstractQueuedSynchronizer.Node)
     */
    private boolean shouldParkAfterFailedAcquire(Node predecessor, Node node) {
        int waitStatus = predecessor.waitStatus;
        if (waitStatus == Node.SIGNAL) { //the node is already asking for a release to signal it, so it can safely park
            return true;
        } else if (waitStatus > 0) { //predecessor was cancelled
            do { //skip over predecessors
                node.prev = predecessor = predecessor.prev;
            } while (predecessor.waitStatus > 0);
            predecessor.next = node;

            return false;
        } else { //waitStatus is either 0 or PROPAGATE
            //request a signal, but don't park yet: the caller should retry to make sure we can't acquire before blocking
            predecessor.compareAndSetWaitStatus(waitStatus, Node.SIGNAL);

            return false;
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#selfInterrupt()
     */
    private void selfInterrupt() { //keeping this as a separate method was probably intended to keep method bytecode size down? seems unlikely though...
        Thread.currentThread().interrupt();
    }

    /**
     * @see AbstractQueuedSynchronizer#parkAndCheckInterrupt()
     */
    private boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    //
    // ACQUIRE* VARIATIONS
    //

    /**
     * @see AbstractQueuedSynchronizer#acquireQueued(AbstractQueuedSynchronizer.Node, int)
     */
    private boolean acquireQueued(Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            do {
                Node predecessor = node.predecessor();
                if (predecessor == this.head //the predecessor is the head of the queue, meaning this node is up next
                    && this.tryAcquire(arg)) { //try to acquire

                    this.setHead(node);
                    predecessor.next = null; //help GC
                    failed = false;
                    return interrupted;
                }

                if (this.shouldParkAfterFailedAcquire(predecessor, node) //we're supposed to park
                    && this.parkAndCheckInterrupt()) { //we were interrupted while parked
                    interrupted = true;
                }
            } while (true); //spin until successful
        } finally {
            if (failed) { //cancel the node if the acquire failed
                this.cancelAcquire(node);
            }
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#doAcquireInterruptibly(int)
     */
    private void doAcquireInterruptibly(int arg) throws InterruptedException {
        Node node = this.addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            do {
                Node predecessor = node.predecessor();
                if (predecessor == this.head //the predecessor is the head of the queue, meaning this node is up next
                    && this.tryAcquire(arg)) { //try to acquire

                    this.setHead(node);
                    predecessor.next = null; //help GC
                    failed = false;
                    return;
                }

                if (this.shouldParkAfterFailedAcquire(predecessor, node) //we're supposed to park
                    && this.parkAndCheckInterrupt()) { //we were interrupted while parked
                    throw new InterruptedException();
                }
            } while (true); //spin until successful
        } finally {
            if (failed) { //cancel the node if the acquire failed
                this.cancelAcquire(node);
            }
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#doAcquireNanos(int, long)
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) { //timeout has already expired lol
            return false;
        }

        long deadline = System.nanoTime() + nanosTimeout;
        Node node = this.addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            do {
                Node predecessor = node.predecessor();
                if (predecessor == this.head //the predecessor is the head of the queue, meaning this node is up next
                    && this.tryAcquire(arg)) { //try to acquire

                    this.setHead(node);
                    predecessor.next = null; //help GC
                    failed = false;
                    return true;
                }

                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) { //timeout has been reached
                    return false;
                }

                if (this.shouldParkAfterFailedAcquire(predecessor, node) //we're supposed to park
                    && nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) { //the remaining timeout is long enough to warrant blocking the thread
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) { //we were interrupted
                    throw new InterruptedException();
                }
            } while (true); //spin until successful
        } finally {
            if (failed) { //cancel the node if the acquire failed
                this.cancelAcquire(node);
            }
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#doAcquireShared(int)
     */
    private void doAcquireShared(int arg) {
        Node node = this.addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            do {
                Node predecessor = node.predecessor();
                if (predecessor == this.head) { //the predecessor is the head of the queue, meaning this node is up next
                    int result = this.tryAcquireShared(arg);
                    if (result >= 0) {
                        this.setHeadAndPropagate(node, result);
                        predecessor.next = null; //help GC
                        if (interrupted) { //interrupt ourself
                            this.selfInterrupt();
                        }
                        failed = false;
                        return;
                    }
                }

                if (this.shouldParkAfterFailedAcquire(predecessor, node) //we're supposed to park
                    && this.parkAndCheckInterrupt()) { //we were interrupted while parked
                    interrupted = true;
                }
            } while (true); //spin until successful
        } finally {
            if (failed) { //cancel the node if the acquire failed
                this.cancelAcquire(node);
            }
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#doAcquireSharedInterruptibly(int)
     */
    private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
        Node node = this.addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            do {
                Node predecessor = node.predecessor();
                if (predecessor == this.head) { //the predecessor is the head of the queue, meaning this node is up next
                    int result = this.tryAcquireShared(arg);
                    if (result >= 0) {
                        this.setHeadAndPropagate(node, result);
                        predecessor.next = null; //help GC
                        failed = false;
                        return;
                    }
                }

                if (this.shouldParkAfterFailedAcquire(predecessor, node) //we're supposed to park
                    && this.parkAndCheckInterrupt()) { //we were interrupted while parked
                    throw new InterruptedException();
                }
            } while (true); //spin until successful
        } finally {
            if (failed) { //cancel the node if the acquire failed
                this.cancelAcquire(node);
            }
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#doAcquireSharedNanos(int, long)
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) { //timeout has already expired lol
            return false;
        }

        long deadline = System.nanoTime() + nanosTimeout;
        Node node = this.addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            do {
                Node predecessor = node.predecessor();
                if (predecessor == this.head) { //the predecessor is the head of the queue, meaning this node is up next
                    int result = this.tryAcquireShared(arg);
                    if (result >= 0) {
                        this.setHeadAndPropagate(node, result);
                        predecessor.next = null; //help GC
                        failed = false;
                        return true;
                    }
                }

                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) { //timeout has been reached
                    return false;
                }

                if (this.shouldParkAfterFailedAcquire(predecessor, node) //we're supposed to park
                    && nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD) { //the remaining timeout is long enough to warrant blocking the thread
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) { //we were interrupted
                    throw new InterruptedException();
                }
            } while (true); //spin until successful
        } finally {
            if (failed) { //cancel the node if the acquire failed
                this.cancelAcquire(node);
            }
        }
    }

    //
    // MAIN EXPORTED METHODS
    //

    /**
     * @see AbstractQueuedSynchronizer#tryAcquire(int)
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see AbstractQueuedSynchronizer#tryRelease(int)
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see AbstractQueuedSynchronizer#tryAcquireShared(int)
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see AbstractQueuedSynchronizer#tryReleaseShared(int)
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see AbstractQueuedSynchronizer#isHeldExclusively(int)
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see AbstractQueuedSynchronizer#acquire(int)
     */
    public final void acquire(int arg) {
        if (!this.tryAcquire(arg) && this.acquireQueued(this.addWaiter(Node.EXCLUSIVE), arg)) {
            this.selfInterrupt();
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#acquireInterruptibly(int)
     */
    public final void acquireInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!this.tryAcquire(arg)) {
            this.doAcquireInterruptibly(arg);
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#tryAcquireNanos(int, long)
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return this.tryAcquire(arg) || this.doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * @see AbstractQueuedSynchronizer#release(int)
     */
    public final boolean release(int arg) {
        if (this.tryRelease(arg)) {
            Node head = this.head;
            if (head != null && head.waitStatus != 0) {
                this.unparkSuccessor(head);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#acquireShared(int)
     */
    public final void acquireShared(int arg) {
        if (this.tryAcquireShared(arg) < 0) {
            this.doAcquireShared(arg);
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#acquireSharedInterruptibly(int)
     */
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (this.tryAcquireShared(arg) < 0) {
            this.doAcquireSharedInterruptibly(arg);
        }
    }

    /**
     * @see AbstractQueuedSynchronizer#tryAcquireSharedNanos(int, long)
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return this.tryAcquireShared(arg) >= 0 || this.doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * @see AbstractQueuedSynchronizer#releaseShared(int)
     */
    public final boolean releaseShared(int arg) {
        if (this.tryReleaseShared(arg)) {
            this.doReleaseShared();
            return true;
        } else {
            return false;
        }
    }

    public SyncOperation<?> prepareAcquireExclusive(int arg) {
        return new AcquireExclusiveOperation(this, arg);
    }

    public SyncOperation<?> prepareAcquireShared(int arg) {
        return new AcquireSharedOperation(this, arg);
    }

    //
    // QUEUE INSPECTION METHODS
    //

    //TODO

    /**
     * @author DaPorkchop_
     * @see AbstractQueuedSynchronizer.Node
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Node {
        /**
         * @see AbstractQueuedSynchronizer.Node#SHARED
         */
        private static final Node SHARED = new Node();
        /**
         * @see AbstractQueuedSynchronizer.Node#EXCLUSIVE
         */
        private static final Node EXCLUSIVE = null;

        /**
         * @see AbstractQueuedSynchronizer.Node#CANCELLED
         */
        private static final int CANCELLED = 1;
        /**
         * @see AbstractQueuedSynchronizer.Node#SIGNAL
         */
        private static final int SIGNAL = -1;
        /**
         * @see AbstractQueuedSynchronizer.Node#CONDITION
         */
        private static final int CONDITION = -2;
        /**
         * @see AbstractQueuedSynchronizer.Node#PROPAGATE
         */
        private static final int PROPAGATE = -3;

        /**
         * @see AbstractQueuedSynchronizer.Node#waitStatus
         */
        private volatile int waitStatus;

        /**
         * @see AbstractQueuedSynchronizer.Node#prev
         */
        private volatile Node prev;
        /**
         * @see AbstractQueuedSynchronizer.Node#next
         */
        private volatile Node next;
        /**
         * @see AbstractQueuedSynchronizer.Node#thread
         */
        private volatile Thread thread;

        /**
         * @see AbstractQueuedSynchronizer.Node#nextWaiter
         */
        private Node nextWaiter;

        /**
         * @see AbstractQueuedSynchronizer.Node#Node(Thread, AbstractQueuedSynchronizer.Node)
         */
        private Node(Thread thread, Node mode) {
            this.thread = thread;
            this.nextWaiter = mode;
        }

        /**
         * @see AbstractQueuedSynchronizer.Node#Node(Thread, int)
         */
        private Node(Thread thread, int waitStatus) {
            this.thread = thread;
            this.waitStatus = waitStatus;
        }

        /**
         * @see AbstractQueuedSynchronizer.Node#isShared()
         */
        private boolean isShared() {
            return this.nextWaiter == SHARED;
        }

        /**
         * @see AbstractQueuedSynchronizer.Node#predecessor()
         */
        private Node predecessor() throws NullPointerException {
            return Objects.requireNonNull(this.prev);
        }

        /**
         * @see AbstractQueuedSynchronizer#compareAndSetWaitStatus(AbstractQueuedSynchronizer.Node, int, int)
         */
        private boolean compareAndSetWaitStatus(int expect, int update) {
            return PUnsafe.compareAndSwapInt(this, NODE_WAITSTATUS_OFFSET, expect, update);
        }

        /**
         * @see AbstractQueuedSynchronizer#compareAndSetNext(AbstractQueuedSynchronizer.Node, AbstractQueuedSynchronizer.Node, AbstractQueuedSynchronizer.Node)
         */
        private boolean compareAndSetNext(Node expect, Node update) {
            return PUnsafe.compareAndSwapObject(this, NODE_NEXT_OFFSET, expect, update);
        }

        /**
         * Signals this node's thread.
         */
        private void signal() {
            LockSupport.unpark(this.thread);
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static final class AcquireState {
        protected final Node node;
        protected boolean failed;

        protected Node predecessor;

        protected AcquireState(@NonNull AbstractQueuedMultiSynchronizer sync, boolean exclusive) {
            this.node = sync.addWaiter(exclusive ? Node.EXCLUSIVE : Node.SHARED);
            this.failed = true;
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static abstract class AbstractAcquireOperation extends SyncOperation<AcquireState> {
        @NonNull
        protected final AbstractQueuedMultiSynchronizer sync;
        protected final int arg;

        @Override
        protected boolean tryAcquire(AcquireState state) {
            Node node = state.node;
            Node predecessor = state.predecessor = node.predecessor();

            if (predecessor == this.sync.head && this.tryAcquire(state, predecessor, node)) {
                predecessor.next = null; //help GC
                state.failed = false;
                return true;
            } else {
                return false;
            }
        }

        protected abstract boolean tryAcquire(AcquireState state, Node predecessor, Node node);

        @Override
        protected boolean shouldParkAfterFailedAcquire(AcquireState state) {
            return this.sync.shouldParkAfterFailedAcquire(state.predecessor, state.node);
        }

        @Override
        protected void cancel(AcquireState state) {
            if (state.failed) {
                this.sync.cancelAcquire(state.node);
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static final class AcquireExclusiveOperation extends AbstractAcquireOperation {
        public AcquireExclusiveOperation(@NonNull AbstractQueuedMultiSynchronizer sync, int arg) {
            super(sync, arg);
        }

        @Override
        protected boolean tryEarly() {
            return this.sync.tryAcquire(this.arg);
        }

        @Override
        protected AcquireState createState() {
            return new AcquireState(this.sync, true);
        }

        @Override
        protected boolean tryAcquire(AcquireState state, Node predecessor, Node node) {
            AbstractQueuedMultiSynchronizer sync = this.sync;

            if (sync.tryAcquire(this.arg)) {
                sync.setHead(node);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static final class AcquireSharedOperation extends AbstractAcquireOperation {
        public AcquireSharedOperation(@NonNull AbstractQueuedMultiSynchronizer sync, int arg) {
            super(sync, arg);
        }

        @Override
        protected boolean tryEarly() {
            return this.sync.tryAcquireShared(this.arg) >= 0;
        }

        @Override
        protected AcquireState createState() {
            return new AcquireState(this.sync, false);
        }

        @Override
        protected boolean tryAcquire(AcquireState state, Node predecessor, Node node) {
            AbstractQueuedMultiSynchronizer sync = this.sync;

            int result = sync.tryAcquireShared(this.arg);
            if (result >= 0) {
                sync.setHeadAndPropagate(node, result);
                return true;
            } else {
                return false;
            }
        }
    }
}
