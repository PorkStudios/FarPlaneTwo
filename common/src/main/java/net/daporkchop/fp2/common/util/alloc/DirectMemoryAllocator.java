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

import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.annotation.ThreadSafe;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.primitive.map.LongLongMap;
import net.daporkchop.lib.primitive.map.concurrent.LongLongConcurrentHashMap;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.function.LongConsumer;
import java.util.stream.StreamSupport;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An {@link Allocator} which allocates actual memory in virtual address space.
 * <p>
 * This implementation is thread-safe.
 *
 * @author DaPorkchop_
 */
@ThreadSafe
public final class DirectMemoryAllocator extends Allocator implements AutoCloseable {
    private final LongLongMap allocations = new LongLongConcurrentHashMap(-1L); //TODO: replace this with a LongLongConcurrentSkipListMap once PorkLib supports it
    private final boolean zero;

    private PCleaner cleaner;

    public DirectMemoryAllocator() {
        this(false);
    }

    /**
     * @param zero if {@code true}, allocations will be initialized to zero
     */
    public DirectMemoryAllocator(boolean zero) {
        super(0L);
        this.cleaner = PCleaner.cleaner(this, new Releaser(this.allocations));
        this.zero = zero;
    }

    @Override
    public long alloc(@NotNegative long size) {
        this.checkOpen();

        long addr = PUnsafe.allocateMemory(notNegative(size, "size"));
        this.allocations.put(addr, size);

        if (this.zero) { //initialize all memory to zero
            PUnsafe.setMemory(addr, size, (byte) 0);
        }
        return addr;
    }

    @Override
    public long realloc(long address, @NotNegative long size) {
        this.checkOpen();

        notNegative(size, "size");
        long oldSize;
        if (address == 0L) { //no allocation existed previously, so there's nothing to remove
            oldSize = 0L;
        } else {
            oldSize = this.allocations.remove(address);
            checkArg(oldSize >= 0L, "can't reallocate address 0x%016x (which isn't owned by this allocator)", address);
        }
        address = PUnsafe.reallocateMemory(address, size);
        this.allocations.put(address, size);

        if (this.zero && oldSize < size) { //initialize new portion of memory to zero
            PUnsafe.setMemory(address + oldSize, size - oldSize, (byte) 0);
        }
        return address;
    }

    @Override
    public void free(long address) {
        this.checkOpen();

        if (address != 0L) {
            checkArg(this.allocations.remove(address) >= 0L, "can't free address 0x%016x (which isn't owned by this allocator)", address);
            PUnsafe.freeMemory(address);
        }
    }

    @Override
    public Stats stats() {
        this.checkOpen();

        long allocations = this.allocations.size();
        long totalSpace = StreamSupport.stream(this.allocations.values().spliterator(), false).mapToLong(Long::longValue).sum();

        return Stats.builder()
                .heapRegions(allocations).allocations(allocations)
                .allocatedSpace(totalSpace).totalSpace(totalSpace)
                .build();
    }

    private void checkOpen() {
        if (this.cleaner == null) {
            throw new IllegalStateException("already closed!");
        }
    }

    @Override
    public synchronized void close() {
        this.cleaner.clean();
        this.cleaner = null;
    }

    /**
     * Cleans up any memory allocated by a {@link DirectMemoryAllocator} which wasn't freed.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static final class Releaser implements Runnable {
        private final LongLongMap allocations;

        @Override
        public void run() {
            if (this.allocations.isEmpty()) { //nothing to do
                return;
            }

            System.err.printf("%d memory blocks allocated by %s (totalling %d bytes) were not freed!\n",
                    this.allocations.size(), DirectMemoryAllocator.class.getCanonicalName(), StreamSupport.stream(this.allocations.values().spliterator(), false).mapToLong(Long::longValue).sum());
            this.allocations.keySet().forEach((LongConsumer) PUnsafe::freeMemory);
        }
    }
}
