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

package net.daporkchop.fp2.gl.bitset;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLResource;

import java.util.BitSet;
import java.util.function.IntPredicate;

/**
 * A {@link BitSet} in server memory.
 * <p>
 * Unlike a {@link BitSet}, a {@link GLBitSet} has a fixed capacity which is only resized on request. Reads outside of the capacity will always return
 * {@code false}, and writes outside of the capacity will be silently discarded.
 *
 * @author DaPorkchop_
 */
public interface GLBitSet extends GLResource {
    /**
     * @return this bitset's capacity
     */
    int capacity();

    /**
     * Sets the capacity of this bitset.
     * <p>
     * If the new capacity is less than the current capacity, the bitset's contents will be truncated. If greater than the current capacity, the
     * data will be extended with undefined contents.
     *
     * @param capacity the new capacity
     */
    void resize(int capacity);

    /**
     * @return the value by which indices are offset
     */
    int offset();

    /**
     * Sets the value by which indices are offset.
     *
     * @param offset value by which indices are offset
     */
    void offset(int offset);

    /**
     * Sets all bits to {@code false}.
     */
    void clear();

    /**
     * Sets all bits which intersect the given source {@link GLBitSet} to the source value, and all other bits to {@code false}.
     *
     * @param src the {@link GLBitSet} to copy values from
     */
    void set(@NonNull GLBitSet src);

    /**
     * Sets all bits to the value returned by the given {@link IntPredicate} for the the corresponding index.
     *
     * @param selector the {@link IntPredicate}
     */
    void set(@NonNull IntPredicate selector);
}
