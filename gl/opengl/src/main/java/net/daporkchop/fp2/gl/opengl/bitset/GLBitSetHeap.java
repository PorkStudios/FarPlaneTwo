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

package net.daporkchop.fp2.gl.opengl.bitset;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.function.ObjLongConsumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class GLBitSetHeap extends AbstractGLBitSet {
    @Getter(AccessLevel.NONE)
    protected int[] arr = PorkUtil.EMPTY_INT_ARRAY;

    public GLBitSetHeap(@NonNull OpenGL gl) {
        super(gl);
    }

    @Override
    public void close() {
        this.arr = null;
    }

    @Override
    protected void resize0(int oldCapacity, int oldWords, int newCapacity, int newWords) {
        this.arr = Arrays.copyOf(this.arr, newWords);
    }

    @Override
    public void mapClient(int len, @NonNull ObjLongConsumer<Object> callback) {
        int[] arr;
        if (notNegative(len, "len") <= this.capacity) { //requested range is within this bitset's capacity, re-use it
            arr = this.arr;
        } else { //requested range is larger than this bitset's capacity, allocate a temporary array and copy stuff into it
            arr = new int[words(len)];
            System.arraycopy(this.arr, 0, arr, 0, this.words);
        }

        callback.accept(arr, PUnsafe.ARRAY_INT_BASE_OFFSET);
    }

    @Override
    public void set(int index) {
        this.arr[checkIndex(this.capacity, index) >> BIT_WORD_SHIFT] |= 1 << index;
    }

    @Override
    public void clear(int index) {
        this.arr[checkIndex(this.capacity, index) >> BIT_WORD_SHIFT] &= ~(1 << index);
    }

    @Override
    public void clear() {
        Arrays.fill(this.arr, 0);
    }

    @Override
    public void set(@NonNull GLBitSet _src) {
        AbstractGLBitSet src = (AbstractGLBitSet) _src;
        if (this.capacity == 0) { //this bitset is empty, do nothing
            return;
        } else if (src.capacity == 0) { //other bitset is empty, nothing intersects, so clear this bitset
            this.clear();
            return;
        }

        //copy shared data
        int sharedBits = min(this.capacity, src.capacity);
        int sharedWords = min(this.words, src.words);
        src.mapClient(sharedBits, (base, offset) -> PUnsafe.copyMemory(base, offset, this.arr, PUnsafe.ARRAY_INT_BASE_OFFSET, sharedWords * (long) BYTES_PER_WORD));

        //clear any bits beyond this bitset's capacity
        this.arr[sharedWords - 1] &= (1 << this.capacity) - 1;

        //fill rest of data with zeroes
        Arrays.fill(this.arr, sharedWords, this.words, 0);
    }

    @Override
    public void set(@NonNull IntPredicate selector) {
        final int[] arr = this.arr;

        for (int wordIndex = 0, bitIndex = 0, endBitIndex = this.capacity; bitIndex < endBitIndex; wordIndex++) {
            int word = 0;
            for (int mask = 1; mask != 0 && bitIndex < endBitIndex; mask <<= 1, bitIndex++) {
                if (selector.test(bitIndex)) {
                    word |= mask;
                }
            }
            arr[wordIndex] = word;
        }
    }

    @Override
    public void and(@NonNull GLBitSet _src) {
        AbstractGLBitSet src = (AbstractGLBitSet) _src;
        if (this.capacity == 0) { //this bitset is empty, do nothing
            return;
        } else if (src.capacity == 0) { //other bitset is empty, nothing intersects, so clear this bitset
            this.clear();
            return;
        }

        //AND shared data
        int sharedBits = min(this.capacity, src.capacity);
        int sharedWords = min(this.words, src.words);
        src.mapClient(sharedBits, (base, offset) -> {
            for (int wordIndex = 0; wordIndex < sharedWords; wordIndex++, offset += BYTES_PER_WORD) {
                this.arr[wordIndex] &= PUnsafe.getInt(base, offset);
            }
        });

        //fill rest of data with zeroes
        Arrays.fill(this.arr, sharedWords, this.words, 0);
    }

    @Override
    public void and(@NonNull IntPredicate selector) {
        final int[] arr = this.arr;

        for (int wordIndex = 0, bitIndex = 0, endBitIndex = this.capacity; bitIndex < endBitIndex; wordIndex++) {
            int word = arr[wordIndex];
            if (word == 0) { //all bits are false, no need to use the selector at all
                bitIndex += BITS_PER_WORD;
                continue;
            }

            for (int mask = 1; mask != 0 && bitIndex < endBitIndex; mask <<= 1, bitIndex++) {
                if ((word & mask) != 0 && !selector.test(bitIndex)) {
                    word &= ~mask;
                }
            }
            arr[wordIndex] = word;
        }
    }
}
