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

import lombok.NonNull;

import java.util.BitSet;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link Allocator} which allocates memory in fixed-size slots.
 *
 * @author DaPorkchop_
 */
public final class FixedSizeAllocator extends BitSet implements Allocator { //extend BitSet to eliminate an indirection
    protected final long blockSize;
    protected final GrowFunction growFunction;
    protected final SequentialHeapManager manager;
    protected long capacity;
    protected int fromIndex = 0;

    public FixedSizeAllocator(long blockSize, @NonNull SequentialHeapManager manager) {
        this(blockSize, manager, GrowFunction.DEFAULT);
    }

    public FixedSizeAllocator(long blockSize, @NonNull SequentialHeapManager manager, @NonNull GrowFunction growFunction) {
        this.blockSize = positive(blockSize, "blockSize");
        this.manager = manager;
        this.growFunction = growFunction;

        this.manager.brk(this.capacity = this.growFunction.grow(0L, blockSize << 4L));
    }

    @Override
    public long alloc(long size) {
        checkArg(size == this.blockSize, "size must be exactly block size (%d)", this.blockSize);
        int slot = this.nextClearBit(this.fromIndex);
        this.set(slot);
        this.fromIndex = slot;

        long addr = slot * this.blockSize;
        if (addr >= this.capacity) {
            this.manager.sbrk(this.capacity = this.growFunction.grow(this.capacity, this.blockSize));
        }
        return addr;
    }

    @Override
    public void free(long address) {
        int slot = toInt(address / this.blockSize);
        this.clear(slot);
        if (slot < this.fromIndex) {
            this.fromIndex = slot;
        }
    }
}
