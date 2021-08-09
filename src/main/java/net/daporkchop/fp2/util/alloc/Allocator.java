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

package net.daporkchop.fp2.util.alloc;

import net.daporkchop.lib.common.math.PMath;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Allocates memory regions in an arbitrary address space.
 * <p>
 * Unless otherwise specified, implementations are not thread-safe.
 *
 * @author DaPorkchop_
 */
public interface Allocator {
    /**
     * Allocates a region of the given size.
     *
     * @param size the size of the region to allocate
     * @return the starting address of the allocated region
     */
    long alloc(long size);

    /**
     * Modifies the size of an existing region.
     *
     * @param address the starting address of the region to free. Once re-allocated, this address is no longer valid and must be replaced with the new one.
     * @param size    the region's new size. If less than the current size, the region's contents will be truncated. If greater than the current size, the region's
     *                contents will be extended with undefined data.
     * @return the new starting address of the re-allocated region
     */
    default long realloc(long address, long size) {
        throw new UnsupportedOperationException(className(this));
    }

    /**
     * Frees an allocated region.
     *
     * @param address the starting address of the region to free
     */
    void free(long address);

    /**
     * A callback function which computes the next size to grow the data to.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface GrowFunction {
        GrowFunction DEFAULT = sqrt2(1 << 24L); // 16MiB

        /**
         * Gets a simple {@link GrowFunction} which grows the heap by multiples of {@code sqrt(2)}, rounded up to a given step.
         *
         * @param step the step size to round heap sizes to
         * @return a {@link GrowFunction} with the given step
         */
        static GrowFunction sqrt2(long step) {
            return (oldCapacity, increment) -> {
                final double SQRT2 = 1.4142135623730951d; //approximately sqrt(2)

                return PMath.roundUp(max(oldCapacity + increment, ceilL(oldCapacity * SQRT2)), step);
            };
        }

        /**
         * Computes the new capacity to grow to.
         *
         * @param oldCapacity the previous capacity
         * @param increment   the minimum amount by which the capacity should be increased
         * @return the new capacity. guaranteed to be at least {@code oldCapacity + increment}
         */
        long grow(long oldCapacity, long increment);
    }

    /**
     * Contains callbacks which manage allocation of a contiguous heap for an {@link Allocator}.
     * <p>
     * The names for {@link #brk(long)} and {@link #sbrk(long)} are inspired by the legacy POSIX syscalls of the same names.
     *
     * @author DaPorkchop_
     * @see <a href="https://linux.die.net/man/2/sbrk">Linux Programmer's Manual</a>
     */
    interface SequentialHeapManager {
        /**
         * Requests that the capacity be set to the given amount.
         *
         * @param capacity the requested capacity
         */
        void brk(long capacity);

        /**
         * Requests that the capacity be extended/truncated to the given amount.
         *
         * @param newCapacity the new capacity
         */
        void sbrk(long newCapacity);
    }
}
