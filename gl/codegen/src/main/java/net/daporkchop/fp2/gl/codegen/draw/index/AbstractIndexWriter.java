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

package net.daporkchop.fp2.gl.codegen.draw.index;

import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.draw.index.NewIndexFormat;
import net.daporkchop.fp2.gl.draw.index.NewIndexWriter;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractIndexWriter extends NewIndexWriter {
    protected final DirectMemoryAllocator alloc;
    protected long address;

    protected AbstractIndexWriter(NewIndexFormat format, DirectMemoryAllocator alloc) {
        super(format);
        this.alloc = alloc;

        this.capacity = 16;
        this.address = alloc.alloc(this.capacity * format.size());
    }

    @Override
    public final NewIndexWriter append(int value) {
        this.appendUninitialized();
        this.set(this.address, this.size - 1, value);
        return this;
    }

    /**
     * Sets a single element to the given value.
     *
     * @param address the base address
     * @param index   the destination index
     * @param value   the element value
     */
    protected abstract void set(long address, long index, int value);

    @Override
    protected final void grow(@NotNegative int oldCapacity, @NotNegative int newCapacity) {
        this.address = this.alloc.realloc(this.address, newCapacity * this.format().size());
    }

    @Override
    public final void copy(@NotNegative int src, @NotNegative int dst) {
        checkIndex(this.size, src);
        checkIndex(this.size, dst);
        this.copySingle(this.address, src, dst);
    }

    /**
     * Copies a single element.
     *
     * @param address the base address
     * @param src     the source index
     * @param dst     the destination index
     */
    protected abstract void copySingle(long address, long src, long dst);

    @Override
    public final void copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length) {
        checkRangeLen(this.size, src, length);
        checkRangeLen(this.size, dst, length);
        long size = this.format().size();
        if (src != dst && length > 0) {
            PUnsafe.copyMemory(this.address + src * size, this.address + dst * size, length * size);
        }
    }

    @Override
    public void close() {
        this.alloc.free(this.address);
    }
}
