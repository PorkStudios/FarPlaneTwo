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

import lombok.NonNull;
import net.daporkchop.fp2.core.util.BreakOutOfLambdaException;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.function.Consumer;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base code for a simple {@link Collection} implementation.
 *
 * @author DaPorkchop_
 */
public abstract class SimpleCollection<E> implements Collection<E> {
    /**
     * Empty method to force subclasses to implement this.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public abstract void forEach(@NonNull Consumer<? super E> callback);

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public Object[] toArray() {
        class State implements Consumer<E> {
            final Object[] array = new Object[SimpleCollection.this.size()];
            int i = 0;

            @Override
            public void accept(E e) {
                this.array[this.i] = e;
                this.i = incrementExact(this.i);
            }
        }

        State state = new State();
        this.forEach(state);
        checkState(state.i == state.array.length, "iterated over %d/%d elements!", state.i, state.array.length);
        return state.array;
    }

    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        class State implements Consumer<E> {
            final T[] array;
            int i = 0;

            State(T[] a) {
                int size = SimpleCollection.this.size();
                if (a.length < size) {
                    a = uncheckedCast(Array.newInstance(a.getClass().getComponentType(), size));
                } else if (a.length > size) {
                    a[size] = null;
                }
                this.array = a;
            }

            @Override
            public void accept(E e) {
                this.array[this.i] = uncheckedCast(e);
                this.i = incrementExact(this.i);
            }
        }

        State state = new State(a);
        this.forEach(state);
        checkState(state.i == state.array.length, "iterated over %d/%d elements!", state.i, state.array.length);
        return state.array;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        try {
            //check every point
            c.forEach(point -> {
                if (!this.contains(point)) {
                    throw BreakOutOfLambdaException.get();
                }
            });

            //every point was contained
            return true;
        } catch (BreakOutOfLambdaException e) {
            //a point wasn't contained, return false
            return false;
        }
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {//local class contains the return value without having to allocate a second object to get the return value
        class State implements Consumer<E> {
            boolean modified = false;

            @Override
            public void accept(E value) {
                //try to add each value and update the "modified" flag if successful
                if (SimpleCollection.this.add(value)) {
                    this.modified = true;
                }
            }
        }

        State state = new State();
        c.forEach(state);
        return state.modified;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException(); //TODO: implementing this would require buffering all the values...
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        //local class contains the return value without having to allocate a second object to get the return value
        class State implements Consumer<Object> {
            boolean modified = false;

            @Override
            public void accept(Object value) {
                //try to remove each value and update the "modified" flag if successful
                if (SimpleCollection.this.remove(value)) {
                    this.modified = true;
                }
            }
        }

        State state = new State();
        c.forEach(state);
        return state.modified;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        this.forEach(value -> builder.append(value == this ? "(this Collection)" : value).append(',').append(' '));

        int length = builder.length();
        if (length > 1) { //something has been added, need to fix up the tail
            builder.setCharAt(length - 2, ']');
            builder.setLength(length - 1);
        }
        return builder.toString();
    }
}
