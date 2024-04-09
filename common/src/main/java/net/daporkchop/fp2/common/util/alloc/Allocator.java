/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.common.util.alloc;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.stats.AbstractLongStatistics;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.function.LongConsumer;

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
@RequiredArgsConstructor
public abstract class Allocator {
    /**
     * An address which indicates an empty allocation.
     */
    public final long nullAddress;

    /**
     * Allocates a region of the given size.
     *
     * @param size the size of the region to allocate
     * @return the starting address of the allocated region. May be {@link #nullAddress} if the given size was {@code 0}
     */
    public abstract long alloc(@NotNegative long size);

    /**
     * Modifies the size of an existing region.
     * <p>
     * Equivalent to {@link #alloc(long) alloc(size)} if the given address is equal to {@link #nullAddress}.
     *
     * @param address the starting address of the region to free. Once re-allocated, this address is no longer valid and must be replaced with the new one.
     * @param size    the region's new size. If less than the current size, the region's contents will be truncated. If greater than the current size, the region's
     *                contents will be extended with undefined data.
     * @return the new starting address of the re-allocated region. May be {@link #nullAddress} if the given size was {@code 0}
     */
    public long realloc(long address, @NotNegative long size) {
        throw new UnsupportedOperationException(className(this));
    }

    /**
     * Frees an allocated region.
     * <p>
     * Does nothing if the given address is equal to {@link #nullAddress}.
     *
     * @param address the starting address of the region to free
     */
    public abstract void free(long address);

    /**
     * Equivalent to {@link #free(long) freeing} an old region followed by {@link #alloc(long) allocating} a new region with the given size.
     * <p>
     * Similar to {@link #realloc(long, long)}, except that old contents will not be preserved.
     *
     * @param address the starting address of the region to free
     * @param size    the size of the new region to allocate
     * @return the starting address of the newly allocated region. May be {@link #nullAddress} if the given size was {@code 0}
     */
    public long freealloc(long address, @NotNegative long size) { //TODO: add optimized overrides of this in implementations
        this.free(address);
        return this.alloc(size);
    }

    /**
     * Allocates multiple regions of the given sizes at once.
     *
     * @param sizes an array containing the sizes of the regions to allocate
     * @return an array containing the starting addresses of the newly allocated regions. Elements may be {@link #nullAddress} if the corresponding size was {@code 0}
     */
    public long[] multiAlloc(long @NotNegative [] sizes) {
        long[] result = PUnsafe.allocateUninitializedLongArray(sizes.length);
        int i = 0;
        try {
            for (; i < sizes.length; i++) {
                result[i] = this.alloc(sizes[i]);
            }
            return result;
        } catch (Throwable t) {
            for (int j = 0; j < i; j++) { //free everything we allocated prior to the failure
                this.free(result[j]);
            }
            throw t;
        }
    }

    /**
     * @return a {@link Stats} instance describing this allocator's current state
     */
    public abstract Stats stats();

    /**
     * A callback function which computes the next size to grow the data to.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface GrowFunction {
        /**
         * @return the default {@link GrowFunction}
         */
        static GrowFunction def() {
            return sqrt2(4L << 10L); // 4Ki
        }

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
         * Gets a simple {@link GrowFunction} which grows the heap by multiples of {@code 2}, rounded up to a given step.
         *
         * @param step the step size to round heap sizes to
         * @return a {@link GrowFunction} with the given step
         */
        static GrowFunction pow2(long step) {
            return (oldCapacity, increment) -> PMath.roundUp(max(oldCapacity + increment, multiplyExact(oldCapacity, 2L)), step);
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
    public interface SequentialHeapManager {
        /**
         * Gets a simple {@link SequentialHeapManager} whose {@link #brk(long)} and {@link #sbrk(long)} methods delegate to the same function.
         *
         * @param function the function
         */
        static SequentialHeapManager unified(@NonNull LongConsumer function) {
            return new SequentialHeapManager() {
                @Override
                public void brk(long capacity) {
                    function.accept(capacity);
                }

                @Override
                public void sbrk(long newCapacity) {
                    function.accept(newCapacity);
                }
            };
        }

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

    /**
     * @author DaPorkchop_
     */
    @Builder
    @Data
    public static final class Stats extends AbstractLongStatistics<Stats> {
        public static final Stats ZERO = builder().build();

        private final long heapRegions;
        private final long allocations;

        private final long allocatedSpace;
        private final long totalSpace;
    }
}
