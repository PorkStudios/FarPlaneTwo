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
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.function.IntPredicate;
import java.util.function.ObjLongConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class GLBitSetHeap extends AbstractGLBitSet {
    @Getter(AccessLevel.NONE)
    protected int[] arr;

    protected int capacity;
    protected int offset;

    public GLBitSetHeap(@NonNull OpenGL gl) {
        super(gl);
    }

    @Override
    public void close() {
        this.arr = null;
    }

    @Override
    public void resize(int capacity) {
        this.arr = new int[words(notNegative(capacity, "capacity"))];
        this.capacity = capacity;
    }

    @Override
    public void offset(int offset) {
        this.offset = offset;
    }

    @Override
    public void set(@NonNull IntPredicate selector) {
        final int[] arr = this.arr;

        for (int wordIndex = 0, idx = this.offset, end = idx + this.capacity; idx < end; wordIndex++) {
            int word = 0;
            for (int bit = 0; idx < end && bit < BITS_PER_WORD; bit++, idx++) {
                if (selector.test(idx)) {
                    word |= 1 << bit;
                }
            }
            arr[wordIndex] = word;
        }
    }

    @Override
    public void mapClient(int off, int len, @NonNull ObjLongConsumer<Object> callback) {
        if (notNegative(len, "len") == 0) {
            callback.accept(null, 0L);
            return;
        } else if (off == this.offset && len <= this.capacity) {
            callback.accept(this.arr, PUnsafe.ARRAY_INT_BASE_OFFSET);
            return;
        }

        throw new UnsupportedOperationException("unimplemented");
    }
}
