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
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractInterleavedAttributeWriter<STRUCT extends AttributeStruct> extends AttributeWriter<STRUCT> {
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

    protected AbstractInterleavedAttributeWriter(@NonNull AttributeFormat<STRUCT> format, @NonNull DirectMemoryAllocator alloc) {
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
    protected final void grow(@NotNegative int oldCapacity, @NotNegative int newCapacity) {
        this.address = this.alloc.realloc(this.address, newCapacity * this.format().size());
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
    public final void copyTo(@NotNegative int srcIndex, AttributeWriter<STRUCT> dstWriterIn, @NotNegative int dstIndex, @NotNegative int length) {
        checkArg(this.getClass() == dstWriterIn.getClass(), "incompatible index formats: %s\n%s", this.format(), dstWriterIn.format());
        AbstractInterleavedAttributeWriter<STRUCT> dstWriter = (AbstractInterleavedAttributeWriter<STRUCT>) dstWriterIn;

        checkRangeLen(this.size, srcIndex, length);
        checkRangeLen(dstWriter.size, dstIndex, length);
        long size = this.format().size();
        if (length > 0) {
            PUnsafe.copyMemory(this.address + srcIndex * size, dstWriter.address + dstIndex * size, length * size);
        }
    }

    @Override
    public void close() {
        this.alloc.free(this.address);
    }
}
