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

package net.daporkchop.fp2.gl.codegen.struct.interleaved;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.unsafe.PUnsafe;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInterleavedAttributeWriter<STRUCT extends AttributeStruct> extends NewAttributeWriter<STRUCT> {
    /*
     * Implementation note:
     *
     * All of the base method implementations are actually protected overloads of the real methods, which accept an
     * additional "stride" parameter indicating the real struct size in the layout. Generated code will implement the
     * real method and then delegate to the internal method type with the added stride parameter as a constant, which
     * will allow it to be treated as a constant in the writer code after inlining.
     */

    protected final DirectMemoryAllocator alloc;
    protected long address;

    protected AbstractInterleavedAttributeWriter(@NonNull NewAttributeFormat<STRUCT> format, @NonNull DirectMemoryAllocator alloc) {
        super(format);
        this.alloc = alloc;

        this.capacity = 16;
        this.address = alloc.alloc(this.capacity * format.size());
    }

    /**
     * @param address the base attribute pointer
     * @param index   the element index
     * @return a handle for the indexed element
     */
    protected abstract STRUCT handle(long address, @NotNegative int index);

    @Override
    public final STRUCT at(@NotNegative int index) {
        return this.handle(this.address, checkIndex(this.size, index));
    }

    @Override
    public final STRUCT append() {
        this.appendUninitialized();
        return this.handle(this.address, this.size - 1);
    }

    @Override
    public final void appendUninitialized() {
        if ((this.size = incrementExact(this.size)) > this.capacity) {
            this.grow(this.size);
        }
    }

    @Override
    public final void appendUninitialized(@Positive int count) {
        if ((this.size = addExact(this.size, positive(count, "count"))) > this.capacity) {
            this.grow(this.size);
        }
    }

    @Override
    public final void reserve(@NotNegative int count) {
        this.grow(addExact(this.capacity, notNegative(count, "count")));
    }

    /**
     * Resizes the internal buffer to fit at least the given number of elements.
     *
     * @param requiredCapacity the minimum required capacity
     */
    protected abstract void grow(@NotNegative int requiredCapacity);

    //called by generated code
    @SuppressWarnings("unused")
    protected final void grow(@Positive int requiredCapacity, @Positive long stride) {
        assert positive(requiredCapacity) > this.capacity;

        do {
            this.capacity = multiplyExact(this.capacity, 2);
        } while (requiredCapacity > this.capacity);

        this.address = this.alloc.realloc(this.address, this.capacity * stride);
    }

    @Override
    public abstract void copy(@NotNegative int src, @NotNegative int dst);

    //called by generated code
    @SuppressWarnings("unused")
    protected final void copy(@NotNegative int src, @NotNegative int dst, @Positive long stride) {
        checkIndex(this.size, src);
        checkIndex(this.size, dst);
        if (src != dst) {
            this.copySingle(this.address + src * stride, this.address + dst * stride);
        }
    }

    /**
     * Copies a single attribute element between the given memory addresses.
     *
     * @param src the source address
     * @param dst the destination address
     */
    protected abstract void copySingle(long src, long dst);

    @Override
    public abstract void copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length);

    //called by generated code
    @SuppressWarnings("unused")
    protected final void copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length, @Positive long stride) {
        checkRangeLen(this.size, src, length);
        checkRangeLen(this.size, dst, length);
        if (src != dst && length > 0) {
            PUnsafe.copyMemory(this.address + src * stride, this.address + dst * stride, length * stride);
        }
    }

    @Override
    public void close() {
        this.alloc.free(this.address);
    }
}
