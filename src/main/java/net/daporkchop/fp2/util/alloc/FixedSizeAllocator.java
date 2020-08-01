/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.primitive.lambda.IntIntFunction;
import net.daporkchop.lib.primitive.lambda.LongLongConsumer;

import java.util.BitSet;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link Allocator} which allocates memory in fixed-size slots.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FixedSizeAllocator extends BitSet implements Allocator { //extend BitSet to eliminate an indirection
    public static final IntIntFunction DEFAULT_GROW_FUNCTION = i -> {
        if (i == 0) {
            return 1;
        } else if ((i & (1 << 30)) != 0) { //highest bit is set
            if (i < Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else {
                throw new OutOfMemoryError();
            }
        } else {
            return i << 1;
        }
    };

    protected final long block;
    protected final IntIntFunction growFunction;
    protected final LongLongConsumer resizeCallback;
    protected int size;
    protected int fromIndex = 0;

    public FixedSizeAllocator(long blockSize, @NonNull LongLongConsumer resizeCallback) {
        this(blockSize, resizeCallback, 0);
    }

    public FixedSizeAllocator(long blockSize, @NonNull LongLongConsumer resizeCallback, int initialBlockCount) {
        this(blockSize, resizeCallback, DEFAULT_GROW_FUNCTION, initialBlockCount);
    }

    public FixedSizeAllocator(long blockSize, @NonNull LongLongConsumer resizeCallback, @NonNull IntIntFunction growFunction, int initialBlockCount) {
        this.block = positive(blockSize, "blockSize");
        this.size = notNegative(initialBlockCount, "initialBlockCount");
        this.resizeCallback = resizeCallback;
        this.growFunction = growFunction;
    }

    @Override
    public long alloc(long size) {
        checkArg(size == this.block, "size must be exactly block size (%d)", this.block);
        int slot = this.nextClearBit(this.fromIndex);
        this.set(slot);
        this.fromIndex = slot;

        if (slot >= this.size)   {
            int oldSize = this.size;
            this.size = this.growFunction.applyAsInt(oldSize);
            checkState(this.size > oldSize, "size (%d) must be greater than previous size (%d)", this.size, oldSize);
            this.resizeCallback.accept(this.block * oldSize, this.block * this.size);
        }
        return this.block * slot;
    }

    @Override
    public void free(long address) {
        int slot = toInt(address / this.block);
        this.clear(slot);
        if (slot < this.fromIndex)  {
            this.fromIndex = slot;
        }
    }

    @Override
    public void close() {
        //no-op
    }
}
