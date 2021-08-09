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

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongRBTreeMap;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An {@link Allocator} which allocates actual memory in virtual address space.
 * <p>
 * This implementation is thread-safe.
 *
 * @author DaPorkchop_
 */
public final class DirectMemoryAllocator implements Allocator {
    protected final Long2LongMap allocations = Long2LongMaps.synchronize(new Long2LongRBTreeMap());

    protected final boolean zero;

    public DirectMemoryAllocator() {
        this(false);
    }

    /**
     * @param zero whether or not uninitialized memory should be zeroed out
     */
    public DirectMemoryAllocator(boolean zero) {
        this.allocations.defaultReturnValue(-1L);
        PCleaner.cleaner(this, new Releaser(this.allocations));

        this.zero = zero;
    }

    @Override
    public long alloc(long size) {
        long addr = PUnsafe.allocateMemory(notNegative(size, "size"));
        this.allocations.put(addr, size);

        if (this.zero) { //initialize all memory to zero
            PUnsafe.setMemory(addr, size, (byte) 0);
        }
        return addr;
    }

    @Override
    public long realloc(long address, long size) {
        notNegative(size, "size");
        long oldSize = this.allocations.remove(address);
        checkArg(oldSize >= 0L, "can't reallocate address 0x016x (which isn't owned by this allocator)", address);
        address = PUnsafe.reallocateMemory(address, size);
        this.allocations.put(address, size);

        if (this.zero && oldSize < size) { //initialize new portion of memory to zero
            PUnsafe.setMemory(address + oldSize, size - oldSize, (byte) 0);
        }
        return address;
    }

    @Override
    public void free(long address) {
        checkArg(this.allocations.remove(address) >= 0L, "can't free address 0x016x (which isn't owned by this allocator)", address);
        PUnsafe.freeMemory(address);
    }

    /**
     * Cleans up any memory allocated by a {@link DirectMemoryAllocator} which wasn't freed.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static final class Releaser implements Runnable {
        protected final Long2LongMap allocations;

        @Override
        public void run() {
            if (this.allocations.isEmpty()) { //nothing to do
                return;
            }

            bigWarning("{} memory blocks allocated by {} (totalling {} bytes) were not freed!",
                    this.allocations.size(), this.allocations.values().stream().mapToLong(Long::longValue).sum(), DirectMemoryAllocator.class.getCanonicalName());
            this.allocations.keySet().forEach(PUnsafe::freeMemory);
        }
    }
}
