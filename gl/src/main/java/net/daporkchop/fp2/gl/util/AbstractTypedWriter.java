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

package net.daporkchop.fp2.gl.util;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of a CPU-side buffer for building sequences of typed data to be uploaded to
 * the GL.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractTypedWriter extends AbstractDirectVector {
    protected AbstractTypedWriter(@NonNull DirectMemoryAllocator alloc, @Positive int initialCapacity, @Positive int elementSize) {
        super(alloc, initialCapacity, elementSize);
    }

    /**
     * Copies the element at the given source index to the given destination index.
     *
     * @param src the source index
     * @param dst the destination index
     */
    public void copy(@NotNegative int src, @NotNegative int dst) {
        this.copy(src, dst, 1);
    }

    /**
     * Copies the elements starting at the given source index to the given destination index.
     * <p>
     * The behavior of this method is undefined if the two ranges overlap.
     *
     * @param src    the source index
     * @param dst    the destination index
     * @param length the number of elements to copy
     */
    public abstract void copy(@NotNegative int src, @NotNegative int dst, @NotNegative int length);

    /**
     * Copies the element at the given source index to the given destination index in the given destination writer.
     *
     * @param srcIndex  the source index
     * @param dstWriter the destination writer
     * @param dstIndex  the destination index
     */
    public void copyTo(@NotNegative int srcIndex, @NonNull AbstractTypedWriter dstWriter, @NotNegative int dstIndex) {
        this.copyTo(srcIndex, dstWriter, dstIndex, 1);
    }

    /**
     * Copies the elements starting at the given source index to the given destination index. in the given destination writer
     * <p>
     * The behavior of this method is undefined if the two ranges overlap.
     *
     * @param srcIndex  the source index
     * @param dstWriter the destination writer
     * @param dstIndex  the destination index
     * @param length    the number of elements to copy
     */
    public abstract void copyTo(@NotNegative int srcIndex, @NonNull AbstractTypedWriter dstWriter, @NotNegative int dstIndex, @NotNegative int length);
}
