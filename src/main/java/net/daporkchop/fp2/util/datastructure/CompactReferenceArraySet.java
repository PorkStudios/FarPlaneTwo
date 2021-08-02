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

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import lombok.NonNull;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Spliterator.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Alternative to {@link ReferenceArraySet} which automatically shrinks the backing array when removing elements.
 *
 * @author DaPorkchop_
 */
public class CompactReferenceArraySet<E> extends AbstractSet<E> {
    protected Object[] elements = EMPTY_OBJECT_ARRAY;

    @Override
    public int size() {
        return this.elements.length;
    }

    @Override
    public boolean isEmpty() {
        return this.elements == EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null //since null values aren't allowed, contains(null) would always return false anyway
            || this.isEmpty()) { //if it's empty there's obviously no match regardless of the value
            return false;
        }

        for (Object element : this.elements) {
            if (element == o) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(@NonNull E e) {
        if (this.contains(e)) { //the value is already contained, so we don't need to do anything
            return false;
        }

        //extend elements array by 1 to append the new value, then replace old array
        Object[] newElements = Arrays.copyOf(this.elements, this.elements.length + 1);
        newElements[this.elements.length] = e;
        this.elements = newElements;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null //since null values aren't allowed, contains(null) would always return false anyway
            || this.isEmpty()) { //if it's empty there's obviously no match regardless of the value
            return false;
        }

        for (int i = 0; i < this.elements.length; i++) {
            if (this.elements[i] == o) { //we found a match! cut the element out of the array and abort
                Object[] newElements = this.elements.length == 1 ? EMPTY_OBJECT_ARRAY : new Object[this.elements.length - 1];
                System.arraycopy(this.elements, 0, newElements, 0, i);
                System.arraycopy(this.elements, i + 1, newElements, i, this.elements.length - 1 - i);
                this.elements = newElements;

                return true;
            }
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        return this.elements.clone();
    }

    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        Object[] elements = this.elements;
        if (a.length < elements.length) {
            a = uncheckedCast(Array.newInstance(a.getClass().getComponentType(), elements.length));
        }
        System.arraycopy(this.elements, 0, a, 0, elements.length);
        if (a.length > elements.length) {
            a[elements.length] = null;
        }
        return a;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return this.removeIf(((Predicate<? super E>) c::contains).negate());
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return this.removeIf(c::contains);
    }

    @Override
    public boolean removeIf(@NonNull Predicate<? super E> filter) {
        int writeIndex = 0;
        for (int readIndex = 0; readIndex < this.elements.length; readIndex++) {
            Object element = this.elements[readIndex];
            if (!filter.test(uncheckedCast(element))) { //filter didn't match, so we need to preserve this element
                this.elements[writeIndex++] = element;
            }
        }

        if (writeIndex != this.elements.length) { //some elements were removed
            this.elements = writeIndex == 0 ? EMPTY_OBJECT_ARRAY : Arrays.copyOf(this.elements, writeIndex);
            return true;
        } else { //nothing changed
            return false;
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super E> action) {
        for (Object element : this.elements) {
            action.accept(uncheckedCast(element));
        }
    }

    @Override
    public void clear() {
        //simply replace with empty array
        this.elements = EMPTY_OBJECT_ARRAY;
    }

    @Override
    public Iterator<E> iterator() {
        return uncheckedCast(Iterators.forArray(this.elements));
    }

    @Override
    public Spliterator<E> spliterator() {
        return uncheckedCast(Spliterators.spliterator(this.elements, IMMUTABLE | NONNULL));
    }

    @Override
    public Stream<E> stream() {
        return uncheckedCast(Stream.of(this.elements));
    }

    @Override
    public Stream<E> parallelStream() {
        return this.stream().parallel();
    }
}
