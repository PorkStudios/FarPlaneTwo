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
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Re-implementation of {@link ArrayDeque} which allocates arrays using {@link Constants#ALLOC_OBJECT}, and can also shrink the array if enough elements are removed.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 * @see ArrayDeque
 */
public class RecyclingArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, AutoCloseable {
    /**
     * The minimum capacity that we'll use for a newly created deque.
     * Must be a power of 2.
     */
    protected static final int MIN_CAPACITY = 16;

    protected static int capacity(int capacity) {
        checkArg(notNegative(capacity, "capacity") <= (1 << 30), "capacity must not be greater than %d (given: %d)", 1 << 30, capacity);

        return max(MIN_CAPACITY, 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(capacity - 1)));
    }

    protected ArrayAllocator<Object[]> alloc;
    protected Object[] elements;

    protected int len;
    protected int head;
    protected int tail;

    public RecyclingArrayDeque() {
        this(0);
    }

    public RecyclingArrayDeque(int capacity) {
        this.elements = (this.alloc = ALLOC_OBJECT.get()).exactly(capacity(capacity));
        this.len = this.elements.length;
    }

    public RecyclingArrayDeque(@NonNull Collection<? extends E> src) {
        this(src.size());
        this.addAll(src);
    }

    protected void tryGrow() {
        if (this.head == this.tail) {
            this.grow();
        }
    }

    protected void grow() {
        checkState(this.head == this.tail, "grow() may only be called when array is full!");

        this.resize(this.len + 1, this.len);
    }

    protected void tryShrink() {
        if (this.len > (MIN_CAPACITY << 1) && this.size() < (this.len >> 2)) {
            this.shrink();
        }
    }

    protected void shrink() {
        checkState(this.size() < (this.len >> 2), "shrink() may only be called when array is 1/4 full");
        checkState(this.len > (MIN_CAPACITY << 1), "old capacity is already the minimum capacity!");

        this.resize(this.len >> 1, this.size());
    }

    protected void resize(int newCapacity, int newSize) {
        newCapacity = capacity(newCapacity);

        checkArg(newSize < newCapacity, "newSize (%d) must be less than newCapacity (%d)", newSize, newCapacity);

        ArrayAllocator<Object[]> oldAlloc = this.alloc;
        Object[] oldElements = this.elements;
        int oldLen = this.len;

        ArrayAllocator<Object[]> newAlloc = ALLOC_OBJECT.get();
        Object[] newElements = newAlloc.exactly(newCapacity);
        int newLen = newElements.length;

        this.copyElements(newElements);

        Arrays.fill(oldElements, null);
        oldAlloc.release(oldElements);

        this.alloc = newAlloc;
        this.elements = newElements;
        this.len = newLen;
        this.head = 0;
        this.tail = newSize;
    }

    protected <T> T[] copyElements(@NonNull T[] dst) {
        if (this.head < this.tail) {
            System.arraycopy(this.elements, this.head, dst, 0, this.size());
        } else if (this.head > this.tail) {
            int headPortionLen = this.len - this.head;
            System.arraycopy(this.elements, this.head, dst, 0, headPortionLen);
            System.arraycopy(this.elements, 0, dst, headPortionLen, this.tail);
        } else {
            int headPortionLen = this.len - this.head;
            System.arraycopy(this.elements, this.head, dst, 0, headPortionLen);
            System.arraycopy(this.elements, 0, dst, headPortionLen, this.head);
        }
        return dst;
    }

    @Override
    public void addFirst(@NonNull E val) {
        this.elements[this.head = (this.head - 1) & (this.len - 1)] = val;
        this.tryGrow();
    }

    @Override
    public void addLast(@NonNull E val) {
        this.elements[this.tail] = val;
        this.tail = (this.tail + 1) & (this.len - 1);
        this.tryGrow();
    }

    @Override
    public boolean offerFirst(@NonNull E val) {
        this.addFirst(val);
        return true;
    }

    @Override
    public boolean offerLast(@NonNull E val) {
        this.addLast(val);
        return true;
    }

    @Override
    public E removeFirst() {
        E val = this.pollFirst();
        if (val == null) {
            throw new NoSuchElementException();
        }
        return val;
    }

    @Override
    public E removeLast() {
        E val = this.pollLast();
        if (val == null) {
            throw new NoSuchElementException();
        }
        return val;
    }

    @Override
    public E pollFirst() {
        E val = uncheckedCast(this.elements[this.head]);
        if (val == null) { //deque is empty
            return null;
        }

        this.elements[this.head] = null;
        this.head = (this.head + 1) & (this.len - 1);
        this.tryShrink();
        return val;
    }

    @Override
    public E pollLast() {
        int t = (this.tail - 1) & (this.len - 1);
        E val = uncheckedCast(this.elements[t]);
        if (val == null) { //deque is empty
            return null;
        }

        this.elements[t] = null;
        this.tail = t;
        this.tryShrink();
        return val;
    }

    @Override
    public E getFirst() {
        E val = uncheckedCast(this.elements[this.head]);
        if (val == null) {
            throw new NoSuchElementException();
        }
        return val;
    }

    @Override
    public E getLast() {
        E val = uncheckedCast(this.elements[(this.tail - 1) & (this.len - 1)]);
        if (val == null) {
            throw new NoSuchElementException();
        }
        return val;
    }

    @Override
    public E peekFirst() {
        return uncheckedCast(this.elements[this.head]);
    }

    @Override
    public E peekLast() {
        return uncheckedCast(this.elements[(this.tail - 1) & (this.len - 1)]);
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o != null) {
            int mask = this.len - 1;
            Object val;
            for (int idx = this.head; (val = this.elements[idx]) != null; idx = (idx + 1) & mask) {
                if (o.equals(val)) {
                    this.delete(idx);
                    this.tryShrink();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o != null) {
            int mask = this.len - 1;
            Object val;
            for (int idx = (this.tail - 1) & mask; (val = this.elements[idx]) != null; idx = (idx - 1) & mask) {
                if (o.equals(val)) {
                    this.delete(idx);
                    this.tryShrink();
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean delete(int idx) {
        int mask = this.len - 1;
        int front = (idx - this.head) & mask;
        int back = (this.tail - idx) & mask;

        // Invariant: head <= idx < tail mod circularity
        if (front >= ((this.tail - this.head) & mask)) {
            throw new ConcurrentModificationException();
        }

        // Optimize for least element motion
        if (front < back) {
            if (this.head <= idx) {
                System.arraycopy(this.elements, this.head, this.elements, this.head + 1, front);
            } else { // Wrap around
                System.arraycopy(this.elements, 0, this.elements, 1, idx);
                this.elements[0] = this.elements[mask];
                System.arraycopy(this.elements, this.head, this.elements, this.head + 1, mask - this.head);
            }
            this.elements[this.head] = null;
            this.head = (this.head + 1) & mask;
            return false;
        } else {
            if (idx < this.tail) { // Copy the null tail as well
                System.arraycopy(this.elements, idx + 1, this.elements, idx, back);
                this.tail = this.tail - 1;
            } else { // Wrap around
                System.arraycopy(this.elements, idx + 1, this.elements, idx, mask - idx);
                this.elements[mask] = this.elements[0];
                System.arraycopy(this.elements, 1, this.elements, 0, this.tail);
                this.tail = (this.tail - 1) & mask;
            }
            return true;
        }
    }

    @Override
    public boolean add(@NonNull E val) {
        this.addLast(val);
        return true;
    }

    @Override
    public boolean offer(@NonNull E val) {
        return this.offerLast(val);
    }

    @Override
    public E remove() {
        return this.removeFirst();
    }

    @Override
    public E poll() {
        return this.pollFirst();
    }

    @Override
    public E element() {
        return this.getFirst();
    }

    @Override
    public E peek() {
        return this.peekFirst();
    }

    @Override
    public void push(@NonNull E val) {
        this.addFirst(val);
    }

    @Override
    public E pop() {
        return this.removeFirst();
    }

    @Override
    public int size() {
        return (this.tail - this.head) & (this.len - 1);
    }

    @Override
    public boolean isEmpty() {
        return this.head == this.tail;
    }

    @Override
    public boolean contains(Object o) {
        if (o != null) {
            int mask = this.len - 1;
            Object val;
            for (int idx = this.head; (val = this.elements[idx]) != null; idx = (idx + 1) & mask) {
                if (o.equals(val)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return this.removeFirstOccurrence(o);
    }

    @Override
    public void clear() {
        if (!this.isEmpty()) {
            Arrays.fill(this.elements, null);

            if (this.len > MIN_CAPACITY) {
                this.alloc.release(this.elements);
                this.elements = (this.alloc = ALLOC_OBJECT.get()).exactly(MIN_CAPACITY);
                this.len = MIN_CAPACITY;
            }

            this.head = this.tail = 0;
        }
    }

    @Override
    public void close() {
        if (!this.isEmpty()) {
            Arrays.fill(this.elements, null);
        }

        this.alloc.release(this.elements);
        this.alloc = null;
        this.elements = null;
    }

    @Override
    public Object[] toArray() {
        return this.copyElements(new Object[this.size()]);
    }

    @Override
    public <T1> T1[] toArray(@NonNull T1[] arr) {
        int size = this.size();
        if (arr.length < size) {
            arr = uncheckedCast(Array.newInstance(arr.getClass().getComponentType(), size));
        }
        this.copyElements(arr);
        if (arr.length > size) {
            arr[size] = null;
        }
        return arr;
    }

    @Override
    public void forEach(@NonNull Consumer<? super E> action) {
        int mask = this.len - 1;
        for (int idx = this.head, lim = this.tail; idx != lim; idx = (idx + 1) & mask) {
            action.accept(uncheckedCast(this.elements[idx]));
        }
    }

    @Override
    public Iterator<E> iterator() {
        return this.stream().iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spliterator<E> spliterator() {
        return this.stream().spliterator();
    }

    @Override
    public Stream<E> stream() {
        if (this.head > this.tail) {
            int headPortionLen = this.len - this.head;
            return uncheckedCast(Stream.concat(
                    Arrays.stream(this.elements, this.head, headPortionLen),
                    Arrays.stream(this.elements, 0, this.tail)));
        } else {
            return uncheckedCast(Arrays.stream(this.elements, this.head, this.size()));
        }
    }

    @Override
    public Stream<E> parallelStream() {
        return this.stream().parallel();
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return this.removeIf(c::contains);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return this.removeIf(((Predicate<? super E>) c::contains).negate());
    }

    @Override
    public boolean removeIf(@NonNull Predicate<? super E> filter) {
        int mask = this.len - 1;
        int writePos = this.head;

        //iterate through the whole list, removing elements as we go
        for (int readPos = this.head; readPos != this.tail; readPos = (readPos + 1) & mask) {
            if (!filter.test(uncheckedCast(this.elements[readPos]))) {
                //filter didn't match: preserve the element by moving it backwards in the array
                this.elements[writePos] = this.elements[readPos];
                writePos = (writePos + 1) & mask;
            }
        }

        if (writePos != this.tail) { //some elements were removed
            if (writePos == this.head) { //everything was removed
                this.clear();
            } else {
                int newTail = writePos;

                //fill no longer used slots with nulls
                for (; writePos != this.tail; writePos = (writePos + 1) & mask) {
                    this.elements[writePos] = null;
                }
                this.tail = newTail;

                this.tryShrink();
            }
            return true;
        } else { //nothing was removed
            return false;
        }
    }

    /**
     * Sorts the elements in this deque according to their natural comparison order.
     */
    public void sort() {
        this.sort(uncheckedCast(Comparator.naturalOrder()));
    }

    /**
     * Sorts the elements in this deque according to the given {@link Comparator}.
     *
     * @param comparator the {@link Comparator} to use
     */
    public void sort(@NonNull Comparator<? super E> comparator) {
        if (this.size() <= 1) { //if the deque is empty or only contains 1 element, it's already sorted so we don't need to do anything
            return;
        }

        if (this.head > this.tail) { //the elements are not all stored sequentially in the backing array
            //we'll "resize" the array (without actually modifying the array size) in order to ensure they are stored sequentially in order to allow us to sort them
            int oldLen = this.len;
            this.resize(this.len, this.size());
            checkState(this.head < this.tail, "head must be less than tail!");
            checkState(this.len == oldLen, "len must be the same!");
        }

        Arrays.sort(this.elements, this.head, this.tail, uncheckedCast(comparator));
    }
}
