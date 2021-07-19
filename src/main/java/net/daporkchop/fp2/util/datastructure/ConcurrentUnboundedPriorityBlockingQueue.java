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

package net.daporkchop.fp2.util.datastructure;

import lombok.NonNull;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.util.Objects.*;

/**
 * Alternative to {@link UnboundedPriorityBlockingQueue} with better performance under high concurrency.
 *
 * @author DaPorkchop_
 */
public class ConcurrentUnboundedPriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    protected final Semaphore lock = new Semaphore(0);
    protected final NavigableSet<E> set;

    public ConcurrentUnboundedPriorityBlockingQueue() {
        this.set = new ConcurrentSkipListSet<>();
    }

    public ConcurrentUnboundedPriorityBlockingQueue(@NonNull Comparator<E> comparator) {
        this.set = new ConcurrentSkipListSet<>(comparator);
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
        if (this.set.add(e)) {
            this.lock.release();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (this.lock.tryAcquire()) {
            if (this.set.remove(o)) {
                return true;
            }
            this.lock.release();
        }
        return false;
    }

    @Override
    public E poll() {
        return this.lock.tryAcquire() ? requireNonNull(this.set.pollFirst()) : null;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return this.lock.tryAcquire(timeout, unit) ? requireNonNull(this.set.pollFirst()) : null;
    }

    @Override
    public E take() throws InterruptedException {
        this.lock.acquire();
        return this.set.pollFirst();
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
        try {
            return this.set.first();
        } catch (NoSuchElementException e) { //set is empty
            return null;
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

    //custom methods

    public E popIfMatches(@NonNull Predicate<E> condition) {
        if (this.lock.tryAcquire()) {
            E value;
            do {
                value = this.set.first();
                if (!condition.test(value)) {
                    this.lock.release();
                    return null;
                }
            } while (!this.set.remove(value));
            return value;
        }
        return null;
    }
}
