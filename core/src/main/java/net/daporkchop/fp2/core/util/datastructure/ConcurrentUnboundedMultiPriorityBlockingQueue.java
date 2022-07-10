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

package net.daporkchop.fp2.core.util.datastructure;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.util.threading.locks.multi.MultiSemaphore;
import net.daporkchop.fp2.core.util.threading.locks.multi.StampedSignaller;
import net.daporkchop.fp2.core.util.threading.locks.multi.SyncAggregator;
import net.daporkchop.fp2.core.util.threading.locks.multi.SyncOperation;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Alternative to {@link UnboundedPriorityBlockingQueue} whose elements are distributed into multiple queue "buckets" with different priority levels.
 * <p>
 * The relative order of elements within a bucket is undefined, although it is likely to be approximately insertion order.
 *
 * @author DaPorkchop_
 */
public class ConcurrentUnboundedMultiPriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    protected final NavigableMap<E, Bucket<E>> buckets;
    protected final Comparator<? super E> bucketingComparator;

    protected final StampedSignaller bucketAddedSignaller = new StampedSignaller();
    protected volatile Syncs syncs;

    /**
     * @param bucketingComparator the {@link Comparator} used to distribute elements into individually prioritized buckets
     */
    public ConcurrentUnboundedMultiPriorityBlockingQueue(@NonNull Comparator<? super E> bucketingComparator) {
        this.buckets = new ConcurrentSkipListMap<>(bucketingComparator);
        this.bucketingComparator = bucketingComparator;

        this.rebuildSyncs();
    }

    @Override
    public Iterator<E> iterator() {
        return this.buckets.values().stream().flatMap(Bucket::stream).iterator();
    }

    @Override
    public boolean isEmpty() {
        return this.buckets.isEmpty() || this.buckets.values().stream().allMatch(Bucket::isEmpty);
    }

    @Override
    public int size() {
        return this.buckets.isEmpty() ? 0 : this.buckets.values().stream().mapToInt(Bucket::size).sum();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean add(E e) {
        Bucket<E> bucket = this.buckets.get(e);
        if (bucket == null) {
            bucket = this.computeBucket(e);
        }

        return bucket.add(e);
    }

    protected Bucket<E> computeBucket(E e) {
        Bucket<E> bucket = this.buckets.computeIfAbsent(e, Bucket::new);
        this.rebuildSyncs();
        return bucket;
    }

    protected synchronized void rebuildSyncs() {
        Bucket<E>[] buckets = uncheckedCast(this.buckets.values().toArray(new Bucket[0]));

        SyncOperation<?>[] syncOperations = new SyncOperation[buckets.length + 1];
        syncOperations[0] = this.bucketAddedSignaller.prepareAwaitNow();
        for (int i = 0; i < buckets.length; i++) {
            syncOperations[i + 1] = buckets[i].acquireOp;
        }

        this.syncs = new Syncs(buckets, Arrays.asList(syncOperations));
        this.bucketAddedSignaller.signalAll();
        syncOperations[0] = this.bucketAddedSignaller.prepareAwaitNow();
    }

    @Override
    public boolean remove(Object o) {
        //noinspection SuspiciousMethodCalls
        Bucket<E> bucket = this.buckets.get(o);
        return bucket != null && bucket.remove(o);
    }

    @Override
    public E poll() {
        for (Bucket<E> bucket : this.buckets.values()) { //try to poll() each bucket in priority order
            E value = bucket.poll();
            if (value != null) { //the bucket had a value available, return it
                return value;
            }
        }

        return null; //none of the buckets contained any values to poll
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanosTimeout = unit.toNanos(timeout);
        if (nanosTimeout <= 0L) { //timeout has already expired lol
            return null;
        }

        long deadline = System.nanoTime() + nanosTimeout;
        do {
            Syncs syncs = this.syncs;
            OptionalInt optionalIndex = SyncAggregator.awaitFirst(nanosTimeout, TimeUnit.NANOSECONDS, syncs.syncOperations);
            if (!optionalIndex.isPresent()) { //timeout has been reached
                return null;
            }

            int index = optionalIndex.getAsInt();
            if (index != 0) { //index 0 is reserved for bucketAddedSignaller
                Bucket<E> bucket = syncs.buckets[index - 1];
                return requireNonNull(bucket.queue.poll());
            }

            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) { //timeout has been reached
                return null;
            }
        } while (true);
    }

    @Override
    public E take() throws InterruptedException {
        do {
            Syncs syncs = this.syncs;

            int index = SyncAggregator.awaitFirst(syncs.syncOperations);
            if (index != 0) { //index 0 is reserved for bucketAddedSignaller
                Bucket<E> bucket = syncs.buckets[index - 1];
                return requireNonNull(bucket.queue.poll());
            }
        } while (true);
    }

    @Override
    public int drainTo(@NonNull Collection<? super E> c) {
        return this.drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(@NonNull Collection<? super E> c, int maxElements) {
        int added = 0;
        for (E value; added < maxElements && (value = this.poll()) != null; added++) {
            c.add(value);
        }
        return added;
    }

    @Override
    public E peek() {
        for (Bucket<E> bucket : this.buckets.values()) { //try to peek() each bucket in priority order
            E value = bucket.peek();
            if (value != null) { //the bucket had a value available, return it
                return value;
            }
        }

        return null;
    }

    //delegate methods

    @Override
    public void put(E e) {
        this.add(e);
    }

    @Override
    public boolean offer(E e) {
        this.add(e);
        return true;
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        this.add(e);
        return true;
    }

    //custom methods

    public E pollLess(@NonNull E curr) {
        for (Bucket<E> bucket : this.buckets.headMap(curr).values()) { //try to poll() each bucket less than the given key in priority order
            E value = bucket.poll();
            if (value != null) { //the bucket had a value available, return it
                return value;
            }
        }

        return null; //none of the buckets contained any values to poll
    }

    public E pollLess(@NonNull E curr, long timeout, TimeUnit unit) throws InterruptedException {
        long nanosTimeout = unit.toNanos(timeout);
        if (nanosTimeout <= 0L) { //timeout has already expired lol
            return null;
        }

        long deadline = System.nanoTime() + nanosTimeout;
        do {
            Syncs syncs = this.syncs;

            int upperBoundIndex = syncs.binarySearchLessIndex(curr) + 1;
            OptionalInt optionalIndex = SyncAggregator.awaitFirst(nanosTimeout, TimeUnit.NANOSECONDS, syncs.syncOperations.subList(0, upperBoundIndex));
            if (!optionalIndex.isPresent()) { //timeout has been reached
                return null;
            }

            int index = optionalIndex.getAsInt();
            if (index != 0) { //index 0 is reserved for bucketAddedSignaller
                Bucket<E> bucket = syncs.buckets[index - 1];
                return requireNonNull(bucket.queue.poll());
            }

            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) { //timeout has been reached
                return null;
            }
        } while (true);
    }

    public E takeLess(@NonNull E curr) throws InterruptedException {
        do {
            Syncs syncs = this.syncs;

            int upperBoundIndex = syncs.binarySearchLessIndex(curr) + 1;
            int index = SyncAggregator.awaitFirst(syncs.syncOperations.subList(0, upperBoundIndex));
            if (index != 0) { //index 0 is reserved for bucketAddedSignaller
                Bucket<E> bucket = syncs.buckets[index - 1];
                return requireNonNull(bucket.queue.poll());
            }
        } while (true);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class Syncs {
        @NonNull
        protected final Bucket<E>[] buckets;
        @NonNull
        protected final List<SyncOperation<?>> syncOperations;

        protected int binarySearchLessIndex(@NonNull E key) {
            Comparator<? super E> bucketingComparator = ConcurrentUnboundedMultiPriorityBlockingQueue.this.bucketingComparator;
            Bucket<E>[] buckets = this.buckets;

            int low = 0;
            int high = buckets.length - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                int cmp = bucketingComparator.compare(buckets[mid].key, key);
                if (cmp < 0) {
                    low = mid + 1;
                } else if (cmp > 0) {
                    high = mid - 1;
                } else {
                    return mid; //key found
                }
            }

            //key not found
            //return -(low + 1);
            return low;
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class Bucket<E> extends AbstractQueue<E> implements BlockingQueue<E> {
        protected final MultiSemaphore lock = new MultiSemaphore(0);
        protected final SyncOperation<?> acquireOp = this.lock.prepareAcquire();

        protected final Queue<E> queue = new ConcurrentLinkedQueue<>();

        @NonNull
        protected final E key;

        @Override
        public Iterator<E> iterator() {
            return this.queue.iterator();
        }

        @Override
        public int size() {
            return this.lock.availablePermits();
        }

        @Override
        public int remainingCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean add(E e) {
            if (this.queue.add(e)) {
                this.lock.release();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean remove(Object o) {
            if (this.lock.tryAcquire()) {
                if (this.queue.remove(o)) {
                    return true;
                }
                this.lock.release();
            }
            return false;
        }

        @Override
        public E poll() {
            return this.lock.tryAcquire() ? requireNonNull(this.queue.poll()) : null;
        }

        @Override
        public E poll(long timeout, TimeUnit unit) throws InterruptedException {
            return this.lock.tryAcquire(timeout, unit) ? requireNonNull(this.queue.poll()) : null;
        }

        @Override
        public E take() throws InterruptedException {
            this.lock.acquire();
            return requireNonNull(this.queue.poll());
        }

        @Override
        public int drainTo(@NonNull Collection<? super E> c) {
            return this.drainTo(c, Integer.MAX_VALUE);
        }

        @Override
        public int drainTo(@NonNull Collection<? super E> c, int maxElements) {
            int added = 0;
            for (E value; added < maxElements && (value = this.poll()) != null; added++) {
                c.add(value);
            }
            return added;
        }

        @Override
        public E peek() {
            return this.queue.peek();
        }

        //delegate methods

        @Override
        public void put(E e) {
            this.add(e);
        }

        @Override
        public boolean offer(E e) {
            this.add(e);
            return true;
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) {
            this.add(e);
            return true;
        }
    }
}
