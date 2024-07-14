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

import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractDirectList<E> extends AbstractDirectVector {
    protected AbstractDirectList(@NonNull DirectMemoryAllocator alloc, @Positive int initialCapacity, @Positive int elementSize) {
        super(alloc, initialCapacity, elementSize);
    }

    /**
     * Sets the element at the given index to the given value.
     *
     * @param index the index
     * @param value the value
     */
    public abstract void set(@NotNegative int index, @NonNull E value);

    /**
     * Gets the element at the given index.
     *
     * @param index the index
     * @return the value at the given index
     */
    public abstract E get(@NotNegative int index);

    /**
     * Adds the given value at the back of this list.
     *
     * @param value the value
     */
    public final void add(@NonNull E value) {
        this.appendUninitialized();
        this.set(this.size - 1, value);
    }
}
