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

package net.daporkchop.fp2.core.util.datastructure.java.list;

import com.google.common.collect.Iterators;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Basic implementation of a constant-sized {@link List} which provides a view of a fixed slice of an array.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ArraySliceAsList<E> extends AbstractList<E> implements RandomAccess {
    /**
     * Gets a {@link List} which provides a view of the given slice of the given array.
     *
     * @param array  the array
     * @param offset the index of the beginning of the slice
     * @param length the length of the slice
     * @return a {@link List}
     */
    public static <E> List<E> wrap(@NonNull E[] array, int offset, int length) {
        checkRangeLen(array.length, offset, length);
        return new ArraySliceAsList<>(array, offset, length);
    }

    /**
     * Gets a {@link List} which provides a view of the given slice of the given array.
     *
     * @param array  the array
     * @param offset the index of the beginning of the slice
     * @param length the length of the slice
     * @return a {@link List}
     */
    public static <E> List<E> wrapUnchecked(@NonNull Object[] array, int offset, int length) {
        checkRangeLen(array.length, offset, length);
        return new ArraySliceAsList<>(uncheckedCast(array), offset, length);
    }

    protected final E[] array;
    protected final int offset;
    protected final int length;

    @Override
    public int size() {
        return this.length;
    }

    @Override
    public boolean isEmpty() {
        return this.length == 0;
    }

    @Override
    public E get(int index) {
        checkIndex(this.length, index);
        return this.array[this.offset + index];
    }

    @Override
    public E set(int index, E element) {
        checkIndex(this.length, index);

        E old = this.array[this.offset + index];
        this.array[this.offset + index] = element;
        return old;
    }

    @Override
    public Object[] toArray() {
        return this.array.clone();
    }

    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        if (a.length < this.length) {
            return Arrays.copyOfRange(this.array, this.offset, this.offset + this.length, uncheckedCast(a.getClass()));
        }

        System.arraycopy(this.array, this.offset, a, 0, this.length);
        if (a.length > this.length) {
            a[this.length] = null;
        }
        return a;
    }

    @Override
    public void replaceAll(@NonNull UnaryOperator<E> operator) {
        E[] array = this.array;
        for (int i = this.offset, lim = i + this.length; i < lim; i++) {
            array[i] = operator.apply(array[i]);
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super E> action) {
        E[] array = this.array;
        for (int i = this.offset, lim = i + this.length; i < lim; i++) {
            action.accept(array[i]);
        }
    }

    @Override
    public void sort(@NonNull Comparator<? super E> c) {
        Arrays.sort(this.array, this.offset, this.offset + this.length, c);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        checkRange(this.length, fromIndex, toIndex);
        return new ArraySliceAsList<>(this.array, this.offset + fromIndex, toIndex - fromIndex);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this.array, this.offset, this.offset + this.length, Spliterator.ORDERED);
    }
}
