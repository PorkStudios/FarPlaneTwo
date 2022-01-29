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
@Deprecated
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
     * Sets the bit at the given index to {@code true}.
     *
     * @param index the index
     */
    void set(int index);

    /**
     * Sets the bit at the given index to {@code false}.
     *
     * @param index the index
     */
    void clear(int index);

    /**
     * Sets all bits to {@code false}.
     */
    void clear();

    /**
     * Sets all bits which intersect the given source {@link GLBitSet} to the source value, and all other bits to {@code false}.
     *
     * @param src the {@link GLBitSet}
     */
    void set(@NonNull GLBitSet src);

    /**
     * Sets all bits to the value returned by the given {@link IntPredicate} for the the corresponding index.
     *
     * @param selector the {@link IntPredicate}
     */
    void set(@NonNull IntPredicate selector);

    /**
     * Sets all bits which intersect the given source {@link GLBitSet} to the logical AND of their current value with the corresponding given values.
     *
     * @param src the {@link GLBitSet}
     */
    void and(@NonNull GLBitSet src);

    /**
     * Sets all bits in this bitset to the logical AND of their current value combined with the result of the given {@link IntPredicate} for the the corresponding index.
     * <p>
     * The {@link IntPredicate} may not be called for bits which are already {@code false}.
     *
     * @param selector the {@link IntPredicate}
     */
    void and(@NonNull IntPredicate selector);
}
