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
import net.daporkchop.fp2.gl.attribute.AttributeArray;
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
public abstract class InterleavedAttributeArrayImpl<F extends InterleavedAttributeFormatImpl<F, S>, S> extends BaseAttributeAccessImpl<F> implements AttributeArray<S> {
    protected final long baseAddr;
    protected final int length;

    public InterleavedAttributeArrayImpl(@NonNull F format, long baseAddr, @Positive int length) {
        super(format);

        this.baseAddr = baseAddr;
        this.length = length;
    }

    @Override
    public void close() {
        //no-op
    }

    /**
     * Generated code overrides this and delegates to {@link #element_withStride_returnHandleFromAddr(int, long)} with the stride as a parameter.
     */
    @Override
    public abstract S element(@NotNegative int index);

    protected long element_withStride_returnHandleFromAddr(@NotNegative int index, @Positive long stride) {
        checkIndex(this.length, index);

        return this.baseAddr + index * stride;
    }

    /**
     * Generated code overrides this and delegates to {@link #copy_withStride(int, int, long)} with the stride as an additional parameter.
     */
    @Override
    public abstract AttributeArray<S> copy(@NotNegative int src, @NotNegative int dst);

    protected AttributeArray<S> copy_withStride(@NotNegative int src, @NotNegative int dst, @Positive long stride) {
        checkIndex(this.length, src);
        checkIndex(this.length, dst);

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
    public abstract AttributeArray<S> copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length);

    protected AttributeArray<S> copy_withStride(@NotNegative int src, @NotNegative int dst, @NotNegative int length, @Positive long stride) {
        checkRangeLen(this.length, src, length);
        checkRangeLen(this.length, dst, length);

        if (src != dst) {
            PUnsafe.copyMemory(this.baseAddr + src * stride, this.baseAddr + dst * stride, length * stride);
        }
        return this;
    }
}
