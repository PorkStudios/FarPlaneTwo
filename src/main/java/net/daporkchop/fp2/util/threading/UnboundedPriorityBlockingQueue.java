/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

import lombok.NonNull;
import net.daporkchop.fp2.util.EqualsTieBreakComparator;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Alternative to {@link PriorityBlockingQueue} backed by a tree map rather than a sorted array.
 * <p>
 * This should yield better performance for large queue sizes, has a lower base overhead and doesn't have a fixed maximum capacity.
 *
 * @author DaPorkchop_
 */
public class UnboundedPriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    protected final Lock lock = new ReentrantLock();
    protected final Condition notEmpty = this.lock.newCondition();
    protected final NavigableSet<E> set;

    public UnboundedPriorityBlockingQueue() {
        this(null);
    }

    public UnboundedPriorityBlockingQueue(Comparator<E> comparator) {
        this(comparator, false, true);
    }

    public UnboundedPriorityBlockingQueue(Comparator<E> comparator, boolean tieUseHashCode, boolean tieUp) {
        this.set = new TreeSet<>(new EqualsTieBreakComparator<>(comparator, tieUseHashCode, tieUp));
        System.identityHashCode(this.set); //compute identity hash code to prevent biased locking
    }

    @Override
    public Iterator<E> iterator() {
        return this.set.iterator();
    }

    @Override
    public boolean isEmpty() {
        return this.set.isEmpty();
    }

    @Override
    public int size() {
        return this.set.size();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean add(E e) {
        this.lock.lock();
        try {
            checkArg(this.set.add(e), "duplicate element: %s", e);
            this.notEmpty.signal();
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public E poll() {
        this.lock.lock();
        try {
            return this.set.pollFirst();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        this.lock.lock();
        try {
            E value = this.set.pollFirst();
            if (value == null && this.notEmpty.await(timeout, unit)) { //queue is empty and we were signalled while waiting
                checkState((value = this.set.pollFirst()) != null, "no values available after signal?!?");
            }
            return value;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        this.lock.lock();
        try {
            E value = this.set.pollFirst();
            if (value == null) { //queue is empty
                this.notEmpty.await();
                checkState((value = this.set.pollFirst()) != null, "no values available after signal?!?");
            }
            return value;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int drainTo(@NonNull Collection<? super E> c) {
        return this.drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(@NonNull Collection<? super E> c, int maxElements) {
        if (maxElements <= 0) {
            return 0;
        }

        this.lock.lock();
        try {
            int added = 0;
            for (E value; added < maxElements && (value = this.set.pollFirst()) != null; added++) {
                c.add(value);
            }
            return added;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public E peek() {
        this.lock.lock();
        try {
            return this.set.isEmpty() ? null : this.set.first();
        } finally {
            this.lock.unlock();
        }
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
