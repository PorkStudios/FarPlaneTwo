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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.lib.common.math.PMath;

import java.util.function.ObjLongConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractGLBitSet implements GLBitSet {
    public static final int BITS_PER_WORD = Integer.SIZE;
    public static final int BIT_WORD_SHIFT = Integer.numberOfTrailingZeros(BITS_PER_WORD);
    public static final int BIT_WORD_MASK = BITS_PER_WORD - 1;

    public static final int BYTES_PER_WORD = Integer.BYTES;
    public static final int BYTE_WORD_SHIFT = Integer.numberOfTrailingZeros(BYTES_PER_WORD);
    public static final int BYTE_WORD_MASK = BYTES_PER_WORD - 1;

    public static int words(int bits) {
        return PMath.roundUp(bits, BITS_PER_WORD) >> BIT_WORD_SHIFT;
    }

    @NonNull
    protected final OpenGL gl;

    protected int capacity;
    protected int words;

    @Override
    public void resize(int capacity) {
        this.resize0(this.capacity, this.words, this.capacity = notNegative(capacity, "capacity"), this.words = words(capacity));
    }

    protected abstract void resize0(int oldCapacity, int oldWords, int newCapacity, int newWords);

    public abstract void mapClient(int len, @NonNull ObjLongConsumer<Object> callback);
}
