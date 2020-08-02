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

package net.daporkchop.fp2.util;

import com.google.common.collect.Iterators;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.lib.common.pool.handle.Handle;
import net.daporkchop.lib.primitive.lambda.IntObjConsumer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A simple add-only collection, which orders elements based on a priority value.
 * <p>
 * Lower priorities always come first during iteration. (priority is probably a really bad name for this, but i couldn't think of anything better :P)
 * <p>
 * If two or more elements have the same priority, the order among themselves is undefined.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PriorityCollection<T> implements Iterable<T> {
    public static final int MAX_PRIORITY = Integer.MIN_VALUE;
    public static final int MIN_PRIORITY = Integer.MAX_VALUE;

    protected int[] priorities;
    protected T[] values;

    @Getter
    protected int size;

    public PriorityCollection() {
        this.priorities = new int[0];
        this.values = uncheckedCast(new Object[0]);
        this.size = 0;
    }

    public void add(int priority, T value) {
        //find the index to insert the value at
        int index = Arrays.binarySearch(this.priorities, priority);
        index ^= (index & 0x80000000) >> 31;

        //create bigger array
        int size = this.size + 1;
        int[] priorities = new int[size];
        T[] values = uncheckedCast(new Object[size]);

        //copy old values
        System.arraycopy(this.priorities, 0, priorities, 0, index);
        System.arraycopy(this.priorities, index, priorities, index + 1, this.size - index);
        System.arraycopy(this.values, 0, values, 0, index);
        System.arraycopy(this.values, index, values, index + 1, this.size - index);

        //add new values
        priorities[index] = priority;
        values[index] = value;

        //replace old stuff
        this.priorities = priorities;
        this.values = values;
        this.size = size;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.forArray(this.values);
    }

    @Override
    public void forEach(@NonNull Consumer<? super T> action) {
        for (T value : this.values) {
            action.accept(value);
        }
    }

    public void forEach(@NonNull IntObjConsumer<? super T> action) {
        for (int i = 0, size = this.size; i < size; i++) {
            action.accept(this.priorities[i], this.values[i]);
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return Arrays.spliterator(this.values);
    }

    public Stream<T> stream() {
        return Stream.of(this.values);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0, size = this.size; i < size; i++) {
            h ^= mix32(((long) Objects.hashCode(this.values[i]) << 32L) | (long) this.priorities[i]);
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof PriorityCollection) {
            PriorityCollection c = (PriorityCollection) obj;
            return this.size == c.size && Arrays.equals(this.priorities, c.priorities) && Arrays.equals(this.values, c.values);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this.size == 0) {
            return "";
        }

        try (Handle<StringBuilder> handle = STRINGBUILDER_POOL.get()) {
            StringBuilder builder = handle.get();
            builder.setLength(0);

            builder.append('[');
            for (int i = 0, size = this.size; i < size; i++) {
                builder.append(this.values[i]).append('=').append(this.priorities[i]).append(',');
            }
            builder.setCharAt(builder.length() - 1, ']');
            return builder.toString();
        }
    }

    @Override
    public PriorityCollection<T> clone() {
        return new PriorityCollection<>(this.priorities, this.values, this.size);
    }
}
