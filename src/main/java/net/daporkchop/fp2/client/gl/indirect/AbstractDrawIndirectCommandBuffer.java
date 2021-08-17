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

package net.daporkchop.fp2.client.gl.indirect;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link IDrawIndirectCommandBuffer}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractDrawIndirectCommandBuffer<C extends IDrawIndirectCommand> extends AbstractRefCounted implements IDrawIndirectCommandBuffer<C> {
    @NonNull
    protected final Allocator alloc;
    protected final int mode;

    @Getter
    protected long capacity = 0L;
    protected long addr;

    protected boolean dirty = false; //extra field for use by implementations, may be ignored if not needed

    @Override
    public IDrawIndirectCommandBuffer<C> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        if (this.addr != 0L) {
            this.alloc.free(this.addr);
        }
    }

    @Override
    public void resize(long capacity) {
        if (notNegative(capacity, "capacity") == this.capacity) { //capacity is unchanged, do nothing
            return;
        }


        long commandSize = this.commandSize();
        this.addr = this.addr == 0L
                ? this.alloc.alloc(capacity * commandSize) //we should create a new allocation
                : this.alloc.realloc(this.addr, capacity * commandSize); //update existing allocation's capacity

        if (capacity > this.capacity) { //zero out undefined contents at tail end of buffer to ensure it's filled with "blank" no-op commands
            PUnsafe.setMemory(this.addr + this.capacity * commandSize, (capacity - this.capacity) * commandSize, (byte) 0);
        }

        this.capacity = capacity;
        this.dirty = true;
    }

    @Override
    public void load(@NonNull C command, long index) {
        command.load(this.addr + checkIndex(this.capacity, index) * this.commandSize());
    }

    @Override
    public void store(@NonNull C command, long index) {
        command.store(this.addr + checkIndex(this.capacity, index) * this.commandSize());

        this.dirty = true;
    }

    @Override
    public void clearRange(long index, long count) {
        checkRangeLen(this.capacity, index, count);

        long commandSize = this.commandSize();
        PUnsafe.setMemory(this.addr + index * commandSize, count * commandSize, (byte) 0);

        this.dirty = true;
    }

    @Override
    public void draw(long offset, long stride, long count) {
        checkIndex(offset >= 0L && offset < this.capacity, "offset(%d) is negative or >= capacity(%d)", offset, this.capacity);
        long minRequiredCapacity = offset + positive(stride, "stride") * (notNegative(count, "count") - 1);
        checkIndex(minRequiredCapacity < this.capacity,
                "drawing with offset(%d), stride(%d), count(%d) would require a capacity of at least %d, but it's only %d!",
                offset, stride, count, minRequiredCapacity, this.capacity);

        if (this.capacity == 0L || count == 0L) { //buffer is empty, do nothing
            return;
        }

        this.draw0(offset, stride, count);
    }

    protected void draw0(long offset, long stride, long count) {
        //assume all arguments are valid

        //we can't draw more than Integer.MAX_VALUE commands at a time, so split it up!
        //  lol look at this guy trying to support these hilariously large command counts, what a weirdo

        long commandSize = this.commandSize();
        int strideI = toInt(stride * commandSize);

        for (long currOffset = offset * commandSize, remaining = count, batchCount; (batchCount = min(remaining, Integer.MAX_VALUE)) != 0L; currOffset += batchCount * commandSize, remaining -= batchCount) {
            this.drawBatch(currOffset, toInt(batchCount), strideI);
        }
    }

    protected abstract void drawBatch(long offset, int count, int stride);
}
