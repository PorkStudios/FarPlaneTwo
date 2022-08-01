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

package net.daporkchop.fp2.core.util.recycler;

import lombok.NonNull;
import net.daporkchop.fp2.core.util.datastructure.java.list.ArraySliceAsList;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.core.util.GlobalAllocators.*;

/**
 * A simple recycler for re-usable object instances.
 * <p>
 * For convenience reasons, it is not <i>absolutely</i> necessary to release instances once no longer needed. This eliminates the need for users to add an additional
 * {@code try-finally} block every time an object is acquired from a recycler, at the cost of potentially degraded performance from having to allocate new instances when an
 * older instance fails to be released due to an exception.
 *
 * @author DaPorkchop_
 */
public interface Recycler<V> {
    /**
     * Gets an instance, either returning a previously released instance or allocating a new one if none are available.
     * <p>
     * The instance should be released using {@link #release(Object)} once no longer needed.
     *
     * @return the instance
     */
    V allocate();

    /**
     * Releases an instance, potentially saving it to be re-used in the future.
     * <p>
     * Once released, the instance must no longer be used in any way.
     *
     * @param value the instance to release
     */
    void release(@NonNull V value);

    //
    // BULK ALLOCATION FUNCTIONS
    //

    /**
     * Gets multiple instances at once, storing them in an array.
     *
     * @param count        the number of instances to get
     * @param arrayFactory a {@link IntFunction function} for allocating array instances
     * @return the array containing the allocated instances
     * @see #allocate()
     */
    default V[] allocate(int count, @NonNull IntFunction<V[]> arrayFactory) {
        return this.allocate(arrayFactory.apply(count));
    }

    /**
     * Gets multiple instances at once, storing them in an array.
     * <p>
     * The returned array must be released using {@link #release(Object[], int, ArrayAllocator)}.
     *
     * @param count the number of instances to get
     * @param alloc an {@link ArrayAllocator} for allocating array instances
     * @return the array containing the allocated instances
     * @see #allocate()
     */
    default V[] allocate(int count, @NonNull ArrayAllocator<V[]> alloc) {
        V[] arr = alloc.atLeast(count);
        try {
            return this.allocate(arr, 0, count);
        } catch (Throwable t) { //something went wrong, try to release original array
            try {
                alloc.release(arr);
            } catch (Throwable t1) {
                t.addSuppressed(t1);
            }

            PUnsafe.throwException(t);
            throw new AssertionError(); //impossible
        }
    }

    /**
     * Gets multiple instances at once, filling the given array with them.
     *
     * @param values the array to fill with instances
     * @return the array containing the allocated instances
     * @see #allocate()
     */
    default V[] allocate(@NonNull V[] values) {
        return this.allocate(values, 0, values.length);
    }

    /**
     * Gets multiple instances at once, filling the given array with them.
     *
     * @param values the array to fill with instances
     * @return the array containing the allocated instances
     * @see #allocate()
     */
    default V[] allocate(@NonNull V[] values, int off, int length) {
        for (int index = 0; index < length; index++) {
            values[off + index] = this.allocate();
        }
        return values;
    }

    //
    // BULK RELEASE METHODS
    //

    /**
     * Releases multiple instances at once from an array allocated by {@link #allocate(int, ArrayAllocator)}.
     * <p>
     * Once released, the instances must no longer be used in any way.
     *
     * @param values the array containing the instances to release
     * @param count  the number of instances that were allocated
     * @param alloc  the {@link ArrayAllocator} which allocated the original array
     */
    default void release(@NonNull V[] values, int count, @NonNull ArrayAllocator<V[]> alloc) {
        try {
            this.release(values, 0, count);
        } finally {
            alloc.release(values);
        }
    }

    /**
     * Releases multiple instances at once.
     * <p>
     * Once released, the instances must no longer be used in any way.
     *
     * @param values the array containing the instances to release
     * @see #release(Object)
     */
    default void release(@NonNull V[] values) {
        this.release(values, 0, values.length);
    }

    /**
     * Releases multiple instances at once.
     * <p>
     * Once released, the instances must no longer be used in any way.
     *
     * @param values the array containing the instances to release
     * @see #release(Object)
     */
    default void release(@NonNull V[] values, int off, int length) {
        try {
            for (int index = 0; index < length; index++) {
                this.release(values[off + index]);
            }
        } finally {
            Arrays.fill(values, off, off + length, null);
        }
    }

    /**
     * Releases multiple instances at once.
     * <p>
     * Once released, the instances must no longer be used in any way.
     *
     * @param values the {@link List} containing the instances to release
     * @see #release(Object)
     */
    default void release(@NonNull List<V> values) {
        values.forEach(this::release);
    }

    //
    // CALLBACK METHODS
    //

    /**
     * Executes the given {@link Consumer action} with a value allocated from this recycler.
     *
     * @param action the action to run
     */
    default void runWith(@NonNull Consumer<? super V> action) {
        V value = this.allocate();
        try {
            action.accept(value);
        } finally {
            this.release(value);
        }
    }

    /**
     * Executes the given {@link Function action} with a value allocated from this recycler.
     *
     * @param action the action to run
     * @return the action's return value
     */
    default <R> R getWith(@NonNull Function<? super V, ? extends R> action) {
        V value = this.allocate();
        try {
            return action.apply(value);
        } finally {
            this.release(value);
        }
    }

    /**
     * Executes the given {@link Consumer action} with multiple values allocated from this recycler.
     *
     * @param count  the number of values to allocate. Note that the array passed to the action may be longer than this; any excess elements should be ignored.
     * @param alloc  the {@link ArrayAllocator} to use for allocating the array
     * @param action the action to run. Note that it may be given a larger array than
     */
    default void runWith(int count, @NonNull ArrayAllocator<V[]> alloc, @NonNull Consumer<? super V[]> action) {
        V[] values = this.allocate(count, alloc);
        try {
            action.accept(values);
        } finally {
            this.release(values, count, alloc);
        }
    }

    /**
     * Executes the given {@link Consumer action} with multiple values allocated from this recycler.
     *
     * @param count  the number of values to allocate. Note that the array passed to the action may be longer than this; any excess elements should be ignored.
     * @param alloc  the {@link ArrayAllocator} to use for allocating the array
     * @param action the action to run. Note that it may be given a larger array than
     * @return the action's return value
     */
    default <R> R getWith(int count, @NonNull ArrayAllocator<V[]> alloc, @NonNull Function<? super V[], ? extends R> action) {
        V[] values = this.allocate(count, alloc);
        try {
            return action.apply(values);
        } finally {
            this.release(values, count, alloc);
        }
    }

    /**
     * Executes the given {@link Consumer action} with multiple values allocated from this recycler.
     *
     * @param count  the number of values to allocate. Note that the array passed to the action may be longer than this; any excess elements should be ignored.
     * @param action the action to run. Note that it may be given a larger array than
     */
    default void runWith(int count, @NonNull Consumer<? super List<V>> action) {
        ArrayAllocator<Object[]> alloc = ALLOC_OBJECT.get();

        Object[] values = alloc.atLeast(count);
        try {
            { //allocate instances
                int index = 0;
                try {
                    for (; index < count; index++) {
                        values[index] = this.allocate();
                    }
                } catch (Throwable t) {
                    try {
                        this.release(ArraySliceAsList.wrapUnchecked(values, 0, index));
                    } catch (Throwable t1) { //failed to release instance, save exception to be rethrown
                        t.addSuppressed(t1);
                    }

                    PUnsafe.throwException(t);
                    throw new AssertionError(); //impossible
                }
            }

            List<V> list = ArraySliceAsList.wrapUnchecked(values, 0, count);
            try {
                action.accept(list);
            } finally {
                this.release(list);
            }
        } finally {
            Arrays.fill(values, 0, count, null);
            alloc.release(values);
        }
    }

    /**
     * Executes the given {@link Consumer action} with multiple values allocated from this recycler.
     *
     * @param count  the number of values to allocate. Note that the array passed to the action may be longer than this; any excess elements should be ignored.
     * @param action the action to run. Note that it may be given a larger array than
     * @return the action's return value
     */
    default <R> R getWith(int count, @NonNull Function<? super List<V>, ? extends R> action) {
        ArrayAllocator<Object[]> alloc = ALLOC_OBJECT.get();

        Object[] values = alloc.atLeast(count);
        try {
            { //allocate instances
                int index = 0;
                try {
                    for (; index < count; index++) {
                        values[index] = this.allocate();
                    }
                } catch (Throwable t) {
                    try {
                        this.release(ArraySliceAsList.wrapUnchecked(values, 0, index));
                    } catch (Throwable t1) { //failed to release instance, save exception to be rethrown
                        t.addSuppressed(t1);
                    }

                    PUnsafe.throwException(t);
                    throw new AssertionError(); //impossible
                }
            }

            List<V> list = ArraySliceAsList.wrapUnchecked(values, 0, count);
            try {
                return action.apply(list);
            } finally {
                this.release(list);
            }
        } finally {
            Arrays.fill(values, 0, count, null);
            alloc.release(values);
        }
    }
}
