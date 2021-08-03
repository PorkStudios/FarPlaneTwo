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

import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    protected final Set<Long> addresses = ConcurrentHashMap.newKeySet();

    public DirectMemoryAllocator() {
        PCleaner.cleaner(this, new Releaser(this.addresses));
    }

    @Override
    public long alloc(long size) {
        long addr = PUnsafe.allocateMemory(positive(size, "size"));
        this.addresses.add(addr);
        return addr;
    }

    @Override
    public void free(long address) {
        checkArg(this.addresses.remove(address), "can't free address 0x016x (which isn't owned by this allocator)", address);
        PUnsafe.freeMemory(address);
    }

    /**
     * Cleans up any memory allocated by a {@link DirectMemoryAllocator} which wasn't freed.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static final class Releaser implements Runnable {
        protected final Set<Long> addresses;

        @Override
        public void run() {
            if (this.addresses.isEmpty()) { //nothing to do
                return;
            }

            bigWarning("{} memory blocks allocated by {} were not freed!", this.addresses.size(), DirectMemoryAllocator.class.getCanonicalName());
            this.addresses.forEach(PUnsafe::freeMemory);
        }
    }
}
