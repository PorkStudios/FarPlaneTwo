/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeAccessImpl;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.unsafe.PUnsafe;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class InterleavedAttributeWriterImpl<F extends InterleavedAttributeFormatImpl<F, S>, S> extends BaseAttributeAccessImpl<F> implements AttributeWriter<S> {
    protected long baseAddr;

    protected int index = -1;
    protected int capacity;

    public InterleavedAttributeWriterImpl(@NonNull F format) {
        super(format);

        this.resize(16);
    }

    @Override
    public void close() {
        this.gl.directMemoryAllocator().free(this.baseAddr);
    }

    @Override
    public int size() {
        return this.index + 1;
    }

    /**
     * Generated code overrides this and delegates to {@link #current_withStride_returnHandleFromAddr(long)} with the stride as a parameter.
     */
    @Override
    public abstract S current();

    protected long current_withStride_returnHandleFromAddr(@Positive long stride) {
        checkState(this.index >= 0, "writer is empty!");

        return this.baseAddr + this.index * stride;
    }

    /**
     * Generated code overrides this and delegates to {@link #at_withStride_returnHandleFromAddr(int, long)} with the stride as a parameter.
     */
    @Override
    public abstract S at(@NotNegative int index);

    protected long at_withStride_returnHandleFromAddr(@NotNegative int index, @Positive long stride) {
        checkIndex(index >= 0 && index <= this.index, index);

        return this.baseAddr + index * stride;
    }

    /**
     * Generated code overrides this and delegates to {@link #append_withStride_returnHandleFromAddr(long)} with the stride as a parameter.
     */
    @Override
    public abstract S append();

    protected long append_withStride_returnHandleFromAddr(@Positive long stride) {
        int index;
        if ((index = this.index = incrementExact(this.index)) == this.capacity) { //grow buffer if needed
            this.grow();
        }

        return this.baseAddr + index * stride;
    }

    @Override
    public AttributeWriter<S> appendUninitialized() {
        if ((this.index = incrementExact(this.index)) == this.capacity) { //grow buffer if needed
            this.grow();
        }
        return this;
    }

    /**
     * Generated code overrides this and delegates to {@link #grow_withStride(long)} with the stride as a parameter.
     */
    protected abstract void grow();

    /**
     * Actually resizes this writer's internal buffer.
     */
    protected void grow_withStride(@Positive long stride) {
        this.resize_withStride(multiplyExact(this.capacity, 2), stride);
    }

    /**
     * Generated code overrides this and delegates to {@link #resize_withStride(int, long)} with the stride as an additional parameter.
     */
    protected abstract void resize(@Positive int capacity);

    /**
     * Actually resizes this writer's internal buffer.
     */
    protected void resize_withStride(@Positive int capacity, @Positive long stride) {
        checkArg(capacity > this.capacity, "cannot resize from %d to %d", this.capacity, capacity);

        this.capacity = capacity;
        this.baseAddr = this.gl.directMemoryAllocator().realloc(this.baseAddr, capacity * stride);
    }

    /**
     * Generated code overrides this and delegates to {@link #copy_withStride(int, int, long)} with the stride as an additional parameter.
     */
    @Override
    public abstract AttributeWriter<S> copy(@NotNegative int src, @NotNegative int dst);

    protected AttributeWriter<S> copy_withStride(@NotNegative int src, @NotNegative int dst, @Positive long stride) {
        checkIndex(this.capacity, src);
        checkIndex(this.capacity, dst);

        if (src != dst) {
            this.copyBetweenAddresses(this.baseAddr + src * stride, this.baseAddr + dst * stride);
        }
        return this;
    }

    protected abstract void copyBetweenAddresses(long src, long dst);

    /**
     * Generated code overrides this and delegates to {@link #copy_withStride(int, int, int, long)}.
     */
    @Override
    public abstract AttributeWriter<S> copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length);

    protected AttributeWriter<S> copy_withStride(@NotNegative int src, @NotNegative int dst, @NotNegative int length, @Positive long stride) {
        checkRangeLen(this.capacity, src, length);
        checkRangeLen(this.capacity, dst, length);

        if (src != dst) {
            PUnsafe.copyMemory(this.baseAddr + src * stride, this.baseAddr + dst * stride, length * stride);
        }
        return this;
    }
}
