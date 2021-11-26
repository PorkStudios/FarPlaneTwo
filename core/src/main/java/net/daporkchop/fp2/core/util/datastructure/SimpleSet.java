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

package net.daporkchop.fp2.core.util.datastructure;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Alternative to {@link Set} with a much simpler API.
 *
 * @author DaPorkchop_
 */
public interface SimpleSet<E> extends IDatastructure<SimpleSet<E>>, Iterable<E> {
    /**
     * @return the number of points in this set
     */
    long count();

    /**
     * @return whether or not this set is empty
     */
    default boolean isEmpty() {
        return this.count() == 0L;
    }

    /**
     * Removes every point in this set.
     */
    void clear();

    //
    // generic methods
    //

    /**
     * Adds the given value to this set.
     *
     * @param value the value to add
     * @return whether or not this set was modified as a result of this change
     */
    boolean add(@NonNull E value);

    /**
     * Removes the given value from this set.
     *
     * @param value the value to remove
     * @return whether or not this set was modified as a result of this change
     */
    boolean remove(@NonNull E value);

    /**
     * Checks whether or not this set contains the given value.
     *
     * @param value the value to check for
     * @return whether or not this set contains the given point
     */
    boolean contains(@NonNull E value);

    /**
     * Runs the given callback function for every value in this set.
     *
     * @param callback the callback function
     */
    void forEach(@NonNull Consumer<? super E> callback);

    @Override
    @Deprecated
    default Iterator<E> iterator() {
        //buffer the whole thing into a list in order to get it as an iterator
        List<E> list = new ArrayList<>(toInt(this.count()));
        this.forEach(list::add);
        return list.iterator();
    }
}
