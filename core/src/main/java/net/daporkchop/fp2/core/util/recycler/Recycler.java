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
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.function.IntFunction;

/**
 * A simple recycler for re-usable object instances.
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
     * Gets multiple instances at once, filling the given array with them.
     *
     * @param values the array to fill with instances
     * @return the array containing the allocated instances
     * @see #allocate()
     */
    default V[] allocate(@NonNull V[] values) {
        int index = 0;
        try {
            for (; index < values.length; index++) {
                values[index] = this.allocate();
            }
            return values;
        } catch (Throwable t) { //something went wrong, free all the instances which were allocated so far before we abort
            for (int i = 0; i < index; i++) {
                try {
                    this.release(values[i]);
                } catch (Throwable t1) { //failed to release instance, save exception to be rethrown
                    t.addSuppressed(t1);
                }
                values[i] = null;
            }

            PUnsafe.throwException(t);
            throw new AssertionError(); //impossible
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
        int index = 0;
        try {
            for (; index < values.length; index++) {
                this.release(values[index]);
                values[index] = null;
            }
        } catch (Throwable t) { //something went wrong, try to continue freeing elements but save exception to rethrow later
            values[index] = null; //make sure to null out the element which failed to be released

            for (index++; index < values.length; index++) {
                try {
                    this.release(values[index]);
                } catch (Throwable t1) { //failed to release instance, save exception to be rethrown
                    t.addSuppressed(t1);
                }
                values[index] = null;
            }

            PUnsafe.throwException(t);
            throw new AssertionError(); //impossible
        }
    }
}
