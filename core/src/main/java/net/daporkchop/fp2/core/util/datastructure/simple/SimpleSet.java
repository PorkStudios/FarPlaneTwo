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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base code for a simple {@link Set} implementation.
 *
 * @author DaPorkchop_
 */
public abstract class SimpleSet<E> extends SimpleCollection<E> implements Set<E> {
    @Override
    @Deprecated
    public Iterator<E> iterator() {
        //buffer the whole thing into a list in order to get it as an iterator
        List<E> list = new ArrayList<>(this.size());

        //noinspection UseBulkOperation: doing that will likely cause it to use an iterator, resulting in infinite recursion
        this.forEach(list::add);
        return list.iterator();
    }

    @Override
    public int hashCode() {
        class State implements Consumer<E> {
            int hash = 0;

            @Override
            public void accept(E value) {
                if (value != null) {
                    this.hash += value.hashCode();
                }
            }
        }

        State state = new State();
        this.forEach(state);
        return state.hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Set) {
            Set<?> other = (Set<?>) obj;
            if (this.size() != other.size()) { //sizes don't match, so the sets can't be equal: abort!
                return false;
            }

            //same as AbstractSet
            try {
                return this.containsAll(other);
            } catch (ClassCastException | NullPointerException unused) { //i'm not sure why AbstractSet catches an NPE, but whatever
                return false;
            }
        } else {
            return false;
        }
    }
}
