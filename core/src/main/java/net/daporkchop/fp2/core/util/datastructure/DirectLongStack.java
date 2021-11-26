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

package net.daporkchop.fp2.core.util.datastructure;

import lombok.NonNull;
import net.daporkchop.lib.primitive.lambda.LongIntConsumer;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.concurrent.atomic.AtomicLong;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An off-heap stack of {@code long}s.
 *
 * @author DaPorkchop_
 */
public class DirectLongStack {
    protected final AtomicLong cleanedAddr = new AtomicLong();
    protected long addr;
    protected int size;
    protected int capacity;

    public DirectLongStack() {
        this(64);
    }

    public DirectLongStack(int capacity) {
        this.capacity = positive(capacity, "capacity");
        this.cleanedAddr.set(this.addr = PUnsafe.allocateMemory((long) this.capacity << 3L));
        PCleaner.cleaner(this, this.cleanedAddr);
    }

    /**
     * @return the current mark
     */
    public int mark() {
        return this.size;
    }

    /**
     * Resets the stack to the given mark.
     *
     * @param mark the mark obtained at the position to restore
     */
    public void restore(int mark) {
        this.size = mark;
    }

    /**
     * Pushes a value onto the stack.
     *
     * @param l the value to be pushed
     */
    public void push(long l) {
        int size = this.size++;
        if (size == this.capacity) { //increase stack size
            this.grow();
        }

        PUnsafe.putLong(this.addr + ((long) size << 3L), l);
    }

    protected void grow() {
        this.capacity <<= 1;
        this.cleanedAddr.set(this.addr = PUnsafe.reallocateMemory(this.addr, (long) this.capacity << 3L));
    }

    /**
     * Removes all values from the stack.
     */
    public void clear() {
        this.size = 0;
    }

    /**
     * @return whether or not the stack is empty
     */
    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * Allows an external function to iterate over the contents of the stack.
     *
     * @param action a callback function which accepts the pointer to the base of the stack as well as the number of values
     */
    public void doWithValues(@NonNull LongIntConsumer action) {
        if (this.size > 0) {
            action.accept(this.addr, this.size);
        }
    }
}
