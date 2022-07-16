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

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Helper methods for working with {@link List}s.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ListUtils {
    /**
     * Checks whether the given {@link List} is permanently empty.
     * <p>
     * This may return false negatives, but never a false positive.
     *
     * @param list the list
     * @return whether the given {@link List} is permanently empty
     */
    public boolean isEmptyList(@NonNull List<?> list) {
        return list.isEmpty() && (list == Collections.emptyList() || list instanceof ImmutableList);
    }

    /**
     * Checks whether the given {@link List} is immutable.
     * <p>
     * This may return false negatives, but never a false positive.
     *
     * @param list the list
     * @return whether the given {@link List} is immutable
     */
    public boolean isImmutableList(@NonNull List<?> list) {
        return list instanceof ImmutableList || list == Collections.emptyList();
    }

    /**
     * Gets a {@link List} whose contents are the given element repeated the given number of times.
     *
     * @param element the element to repeat
     * @param times   the number of times to repeat the element
     * @return a {@link List} whose contents are the given element repeated the given number of times
     */
    public <E> List<E> repeat(E element, int times) {
        switch (notNegative(times, "times")) {
            case 0: //repeating the element zero times will yield an empty list
                return ImmutableList.of();
            case 1: //repeating the sequence once will have no effect
                return ImmutableList.of(element);
        }

        //can't use anonymous class because i need to implement RandomAccess
        @RequiredArgsConstructor
        final class RepeatingList extends AbstractList<E> implements RandomAccess {
            final E element;
            final int times;

            @Override
            public int size() {
                return this.times;
            }

            @Override
            public boolean isEmpty() {
                return this.times == 0;
            }

            @Override
            public E get(int index) {
                checkIndex(this.times, index);
                return this.element;
            }

            @Override
            public Object[] toArray() {
                return PArrays.filled(this.times, Object.class, this.element);
            }

            @Override
            public <T> T[] toArray(T[] a) {
                if (a.length < this.times) {
                    return PArrays.filled(this.times, PorkUtil.<Class<T>>uncheckedCast(a.getClass().getComponentType()), uncheckedCast(this.element));
                }

                Arrays.fill(a, 0, this.times, this.element);
                if (a.length > this.times) {
                    a[this.times] = null;
                }
                return a;
            }

            @Override
            public void forEach(@NonNull Consumer<? super E> action) {
                //preload all member variables into local variables to avoid dereferencing 'this' on every iteration
                E element = this.element;
                for (int i = this.times - 1; i >= 0; i--) {
                    action.accept(element);
                }
            }

            @Override
            public int indexOf(Object o) {
                //given value can either equal the first occurrence of the element or none at all
                return Objects.equals(o, this.element) ? 0 : -1;
            }

            @Override
            public int lastIndexOf(Object o) {
                //given value can either equal the last occurrence of the element or none at all
                return Objects.equals(o, this.element) ? this.times - 1 : -1;
            }

            @Override
            public int hashCode() {
                //compute hashCode of element only once
                int listHash = 1;
                for (int i = this.times - 1, elementHash = Objects.hashCode(this.element); i >= 0; i--) {
                    listHash = listHash * 31 + elementHash;
                }
                return listHash;
            }

            @Override
            public List<E> subList(int fromIndex, int toIndex) {
                checkRange(this.times, fromIndex, toIndex);
                return repeat(this.element, toIndex - fromIndex);
            }

            @Override
            public Spliterator<E> spliterator() {
                return this.stream().spliterator();
            }

            @Override
            public Stream<E> stream() {
                return IntStream.range(0, this.times).mapToObj(i -> this.element);
            }

            @Override
            public Stream<E> parallelStream() {
                return this.stream().parallel();
            }
        }

        return new RepeatingList(element, times);
    }

    /**
     * Repeats the contents of the given {@link List} the given number of times.
     * <p>
     * The returned {@link List} will not be mutable, but modifications to the original {@link List} will be visible in the returned {@link List}. It <i>may</i> preserve
     * the original {@link List}'s {@link RandomAccess} attributes.
     *
     * @param sequence the {@link List} containing the sequence to be repeated
     * @param times    the number of times to repeat the sequence
     * @return a {@link List} exposing a view of the repeated sequence
     */
    public <E> List<E> repeatSequence(@NonNull List<E> sequence, int times) {
        switch (notNegative(times, "times")) {
            case 0: //repeating the sequence zero times will yield an empty list, regardless of original size
                return ImmutableList.of();
            case 1: //repeating the sequence once will have no effect
                return sequence;
        }

        if (isImmutableList(sequence)) { //the list is immutable, check if we have some special cases for lists of the given size
            switch (sequence.size()) {
                case 0: //repeating an empty sequence will yield an empty list, regardless of original size
                    return ImmutableList.of();
                case 1: //repeating a sequence with one element can be optimized into simply repeating the element
                    return repeat(sequence.get(0), times);
            }
        }

        @RequiredArgsConstructor
        abstract class AbstractWrappedRepeatingList extends AbstractList<E> implements RandomAccess {
            protected final List<E> sequence;
            protected final int times;

            @Override
            public int size() {
                return multiplyExact(this.times, this.sequence.size());
            }

            @Override
            public Object[] toArray() {
                Object[] original = this.sequence.toArray();
                Object[] repeated = new Object[multiplyExact(this.times, original.length)];
                for (int i = 0; i < repeated.length; i += original.length) {
                    System.arraycopy(original, 0, repeated, i, original.length);
                }
                Arrays.fill(original, null); //help GC (?)
                return repeated;
            }

            @Override
            public void forEach(@NonNull Consumer<? super E> action) {
                //preload all member variables into local variables to avoid dereferencing 'this' on every iteration
                List<E> sequence = this.sequence;
                for (int i = this.times - 1; i >= 0; i--) {
                    sequence.forEach(action);
                }
            }

            @Override
            public Spliterator<E> spliterator() {
                return this.stream().spliterator();
            }

            @Override
            public Stream<E> stream() {
                return IntStream.range(0, this.times).boxed().flatMap(i -> this.sequence.stream());
            }

            @Override
            public Stream<E> parallelStream() {
                return IntStream.range(0, this.times).parallel().boxed().flatMap(i -> this.sequence.parallelStream());
            }
        }

        return BinMath.isPow2(times) ?
                       new AbstractWrappedRepeatingList(sequence, times) {
                           final int shift = Integer.numberOfTrailingZeros(this.times);

                           @Override
                           public E get(int index) {
                               checkIndex(index >= 0 && (index >> this.shift) < this.sequence.size(), index);
                               return this.sequence.get(index & (this.times - 1));
                           }
                       } :
                       new AbstractWrappedRepeatingList(sequence, times) {
                           @Override
                           public E get(int index) {
                               checkIndex(index >= 0 && index < this.size(), index);
                               return this.sequence.get(index % this.times);
                           }
                       };
    }

    /**
     * Repeats each element in the given {@link List} the given number of times.
     * <p>
     * The returned {@link List} will not be mutable, but modifications to the original {@link List} will be visible in the returned {@link List}. It <i>may</i> preserve
     * the original {@link List}'s {@link RandomAccess} attributes.
     *
     * @param sequence the {@link List} whose elements are to be repeated
     * @param times    the number of times to repeat each element in the {@link List}
     * @return a {@link List} exposing a view of the {@link List} whose elements are repeated
     */
    public <E> List<E> repeatElements(@NonNull List<E> sequence, int times) {
        switch (notNegative(times, "times")) {
            case 0: //repeating each element zero times will yield an empty list, regardless of original size
                return ImmutableList.of();
            case 1: //repeating each element once will have no effect
                return sequence;
        }

        if (isImmutableList(sequence)) { //the list is immutable, check if we have some special cases for lists of the given size
            switch (sequence.size()) {
                case 0: //repeating an empty list will yield an empty list, regardless of original size
                    return ImmutableList.of();
                case 1: //repeating the elements of a list with one element can be optimized into simply repeating the element
                    return repeat(sequence.get(0), times);
            }
        }

        @RequiredArgsConstructor
        abstract class AbstractElementsRepeatingList extends AbstractList<E> implements RandomAccess {
            protected final List<E> sequence;
            protected final int times;

            @Override
            public int size() {
                return multiplyExact(this.times, this.sequence.size());
            }

            @Override
            public Object[] toArray() {
                Object[] original = this.sequence.toArray();
                Object[] repeated = new Object[multiplyExact(this.times, original.length)];
                for (int i = 0; i < original.length; i++) {
                    Arrays.fill(repeated, i * this.times, multiplyExact(i + 1, this.times), original[i]);
                    original[i] = null; //help GC (?)
                }
                return repeated;
            }

            @Override
            public void forEach(@NonNull Consumer<? super E> action) {
                this.sequence.forEach(element -> {
                    for (int i = this.times - 1; i >= 0; i--) {
                        action.accept(element);
                    }
                });
            }

            @Override
            public Spliterator<E> spliterator() {
                return this.stream().spliterator();
            }

            @Override
            public Stream<E> stream() {
                return this.sequence.stream().flatMap(element -> IntStream.range(0, this.times).mapToObj(i -> element));
            }

            @Override
            public Stream<E> parallelStream() {
                return this.sequence.parallelStream().flatMap(element -> IntStream.range(0, this.times).mapToObj(i -> element).parallel());
            }
        }

        return BinMath.isPow2(times) ?
                       new AbstractElementsRepeatingList(sequence, times) {
                           final int shift = Integer.numberOfTrailingZeros(this.times);

                           @Override
                           public E get(int index) {
                               checkIndex(index >= 0 && (index >> this.shift) < this.sequence.size(), index);
                               return this.sequence.get(index >> this.shift);
                           }
                       } :
                       new AbstractElementsRepeatingList(sequence, times) {
                           @Override
                           public E get(int index) {
                               checkIndex(index >= 0 && index < this.size(), index);
                               return this.sequence.get(index / this.times);
                           }
                       };
    }
}
