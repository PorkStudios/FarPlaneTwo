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

package net.daporkchop.fp2.core.util.datastructure.simple;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base code for a simple {@link List} implementation.
 *
 * @author DaPorkchop_
 */
public abstract class SimpleList<E> extends SimpleCollection<E> implements List<E>, RandomAccess {
    @Override
    public boolean contains(Object o) {
        //noinspection ListIndexOfReplaceableByContains
        return this.indexOf(o) >= 0;
    }

    @Override
    public boolean add(E e) {
        this.add(this.size(), e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        int index = this.indexOf(o);
        if (index >= 0) { //value was found, remove it
            this.remove(index);
            return true;
        } else { //the value wasn't present, do nothing
            return false;
        }
    }

    /**
     * Removes multiple elements in a range of sequential indices.
     * <p>
     * Conceptually implemented by
     * <blockquote><pre>{@code
     * for (ListIterator<E> itr = this.listIterator(fromIndex, fromIndex, toIndex); itr.hasNext(); ) {
     *     itr.next();
     *     itr.remove();
     * }
     * }</pre></blockquote>
     * except the implementation has the opportunity to optimize this beyond what the user could write.
     *
     * @param fromIndex the index of the first element to remove
     * @param toIndex   the index after the last element to remove
     */
    public void removeRange(int fromIndex, int toIndex) {
        for (ListIterator<E> itr = this.listIterator(fromIndex, fromIndex, toIndex); itr.hasNext(); ) {
            itr.next();
            itr.remove();
        }
    }

    @Override
    public void clear() {
        this.removeRange(0, this.size());
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0, size = this.size(); i < size; i++) {
            if (Objects.equals(o, this.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = this.size() - 1; i >= 0; i--) {
            if (Objects.equals(o, this.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        return this.addAll(this.size(), c);
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends E> c) {
        if (c.isEmpty()) {
            return false;
        }

        c.forEach(this.listIterator(index)::add);
        return true;
    }

    @Override
    public void replaceAll(@NonNull UnaryOperator<E> operator) {
        for (int i = 0, size = this.size(); i < size; i++) {
            this.set(i, operator.apply(this.get(i)));
        }
    }

    //
    // iterators
    //

    @Override
    public Iterator<E> iterator() {
        return this.listIterator();
    }

    @Override
    public ListIterator<E> listIterator() {
        return new DefaultListIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new DefaultListIterator(index);
    }

    /**
     * Similar to {@link #listIterator(int)}, except that the iterator is restricted to the given index range.
     *
     * @param index     the iterator's starting index
     * @param fromIndex the index of the first element to make accessible to the iterator
     * @param toIndex   the index after the last element to make accessible to the iterator
     * @return a {@link ListIterator} at the given starting index, restricted to iteration within the given range
     */
    public ListIterator<E> listIterator(int index, int fromIndex, int toIndex) {
        return new DefaultListIterator(index, fromIndex, toIndex);
    }

    //
    // streams
    //

    @Override
    public Spliterator<E> spliterator() {
        return this.stream().spliterator();
    }

    @Override
    public Stream<E> stream() {
        //noinspection SimplifyStreamApiCallChains
        return IntStream.range(0, this.size()).mapToObj(this::get);
    }

    @Override
    public Stream<E> parallelStream() {
        return this.stream().parallel();
    }

    //
    // sublist
    //

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new SubList(fromIndex, toIndex);
    }

    //
    // object stuff
    //

    @Override
    public int hashCode() {
        int hash = 1;
        for (int i = 0, size = this.size(); i < size; i++) {
            hash = hash * 31 + Objects.hashCode(this.get(i));
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof List) {
            if (obj instanceof RandomAccess) { //the other list is also random-access, we can iterate using indices
                List<?> other = (List<?>) obj;

                int size = this.size();
                if (size != other.size()) { //sizes don't match, abort!
                    return false;
                }

                for (int i = 0; i < size; i++) {
                    if (!Objects.equals(this.get(i), other.get(i))) { //values at the current index don't match, abort!
                        return false;
                    }
                }

                return true;
            } else { //the other list isn't random-access, we can't assume that get(int) or size() will be fast
                ListIterator<E> e1 = this.listIterator();
                ListIterator<?> e2 = ((List<?>) obj).listIterator();

                while (e1.hasNext() && e2.hasNext()) {
                    if (!Objects.equals(e1.next(), e2.next())) { //values at the current index don't match, abort!
                        return false;
                    }
                }

                //if one of the iterators still has values remaining, the sizes don't match
                return !(e1.hasNext() || e2.hasNext());
            }
        } else {
            return false;
        }
    }

    /**
     * Default {@link ListIterator} implementation returned by {@link #iterator} and {@link #listIterator}.
     *
     * @author DaPorkchop_
     */
    protected class DefaultListIterator implements ListIterator<E> {
        private final int fromIndex;
        private int toIndex;

        private int cursor;
        private int lastRet = -1;

        public DefaultListIterator() {
            this.fromIndex = 0;
            this.toIndex = SimpleList.this.size();
        }

        public DefaultListIterator(int startIndex) {
            int size = SimpleList.this.size();
            checkIndex(size, startIndex);

            this.fromIndex = 0;
            this.toIndex = size;

            this.cursor = startIndex;
        }

        public DefaultListIterator(int startIndex, int fromIndex, int toIndex) {
            int size = SimpleList.this.size();
            checkRange(size, fromIndex, toIndex);
            checkIndex(fromIndex, toIndex, startIndex);

            this.fromIndex = fromIndex;
            this.toIndex = toIndex;

            this.cursor = startIndex;
        }

        @Override
        public boolean hasNext() {
            return this.cursor != this.toIndex;
        }

        @Override
        public E next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            E next = SimpleList.this.get(this.cursor);
            this.lastRet = this.cursor++;
            return next;
        }

        @Override
        public int nextIndex() {
            return this.cursor;
        }

        @Override
        public boolean hasPrevious() {
            return this.cursor != this.fromIndex;
        }

        @Override
        public E previous() {
            if (!this.hasPrevious()) {
                throw new NoSuchElementException();
            }

            E previous = SimpleList.this.get(this.cursor - 1);
            this.lastRet = --this.cursor;
            return previous;
        }

        @Override
        public int previousIndex() {
            return this.cursor - 1;
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }

            SimpleList.this.remove(this.lastRet);
            if (this.lastRet < this.cursor) {
                this.cursor--;
            }
            this.lastRet = -1;

            this.toIndex--; //size has decreased, so we need to decrement the upper bound index
        }

        @Override
        public void set(E e) {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }

            SimpleList.this.set(this.lastRet, e);
        }

        @Override
        public void add(E e) {
            SimpleList.this.add(this.cursor, e);
            this.lastRet = -1;
            this.cursor++;

            this.toIndex++; //size has increased, so we need to increment the upper bound index
        }
    }

    /**
     * Default {@link SimpleList} implementation returned by {@link #subList(int, int)}.
     *
     * @author DaPorkchop_
     */
    protected class SubList extends SimpleList<E> {
        protected final int offset;
        @Getter
        protected int size;

        public SubList(int from, int to) {
            checkRange(SimpleList.this.size(), from, to);

            this.offset = from;
            this.size = to - from;
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            checkRange(this.size, fromIndex, toIndex);
            //offset the range and delegate to the parent in order to prevent multiple layers of wrapping
            return SimpleList.this.subList(this.offset + fromIndex, this.offset + toIndex);
        }

        @Override
        public void forEach(@NonNull Consumer<? super E> callback) {
            for (int i = 0; i < this.size; i++) {
                callback.accept(SimpleList.this.get(this.offset + i));
            }
        }

        @Override
        public E get(int index) {
            checkIndex(this.size, index);
            return SimpleList.this.get(this.offset + index);
        }

        @Override
        public E set(int index, E element) {
            checkIndex(this.size, index);
            return SimpleList.this.set(this.offset + index, element);
        }

        @Override
        public void add(int index, E element) {
            checkIndex(this.size, index);
            SimpleList.this.add(this.offset + index, element);
            this.size++;
        }

        @Override
        public E remove(int index) {
            checkIndex(this.size, index);
            E result = SimpleList.this.remove(this.offset + index);
            this.size--;
            return result;
        }

        @Override
        public void removeRange(int fromIndex, int toIndex) {
            SimpleList.this.removeRange(this.offset + fromIndex, this.offset + toIndex);
            this.size -= toIndex - fromIndex;
        }

        @Override
        public boolean addAll(int index, @NonNull Collection<? extends E> c) {
            checkIndex(this.size, index);

            int size = c.size();
            if (size == 0) {
                return false;
            }

            SimpleList.this.addAll(this.offset + index, c);
            this.size += size;
            return true;
        }

        @Override
        public ListIterator<E> listIterator() {
            return new SubListIterator(SimpleList.this.listIterator(this.offset, this.offset, this.offset + this.size));
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            checkIndex(this.size, index);
            return new SubListIterator(SimpleList.this.listIterator(this.offset + index, this.offset, this.offset + this.size));
        }

        @Override
        public ListIterator<E> listIterator(int index, int fromIndex, int toIndex) {
            checkRange(this.size, fromIndex, toIndex);
            return new SubListIterator(SimpleList.this.listIterator(this.offset + index, this.offset + fromIndex, this.offset + toIndex));
        }

        /**
         * Default {@link ListIterator} implementation used by {@link SubList}.
         *
         * @author DaPorkchop_
         */
        @RequiredArgsConstructor
        protected class SubListIterator implements ListIterator<E> {
            @NonNull
            private final ListIterator<E> delegate;

            @Override
            public boolean hasNext() {
                return this.delegate.hasNext();
            }

            @Override
            public E next() {
                return this.delegate.next();
            }

            @Override
            public int nextIndex() {
                return this.delegate.nextIndex() - SubList.this.offset;
            }

            @Override
            public boolean hasPrevious() {
                return this.delegate.hasPrevious();
            }

            @Override
            public E previous() {
                return this.delegate.previous();
            }

            @Override
            public int previousIndex() {
                return this.delegate.previousIndex() - SubList.this.offset;
            }

            @Override
            public void remove() {
                this.delegate.remove();
                SubList.this.size--;
            }

            @Override
            public void set(E e) {
                this.delegate.set(e);
            }

            @Override
            public void add(E e) {
                this.delegate.add(e);
                SubList.this.size++;
            }
        }
    }
}
