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

package net.daporkchop.fp2.gl.opengl.attribute.texture;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class TextureWriter2DImpl extends BaseTextureWriterImpl<TextureFormat2DImpl> implements TextureWriter2D {
    protected final int width;
    protected final int height;

    protected final long addr;

    public TextureWriter2DImpl(@NonNull TextureFormat2DImpl format, int width, int height, long stride) {
        super(format);

        this.width = positive(width, "width");
        this.height = positive(height, "height");

        this.addr = this.gl.directMemoryAllocator().alloc(multiplyExact(multiplyExact(width, height), positive(stride, "stride")));
    }

    @Override
    public void close() {
        this.gl.directMemoryAllocator().free(this.addr);
    }

    /*@Override
    public void set(int x, int y, Object struct) {
        checkIndex(this.width, x);
        checkIndex(this.height, y);

        //well, it *isn't* being implicitly cast to a long - it's quite EXPLICITLY being cast to a long! no clue why intellij has decided to warn me about this...
        //noinspection IntegerMultiplicationImplicitCastToLong
        this.structFormat.copy(struct, null, this.addr + (long) ((y * this.width + x) * this.stride));
    }*/

    @Override
    public void setUnsignedNormalizedARGB8(int x, int y, int argb) {
        checkIndex(this.width, x);
        checkIndex(this.height, y);

        //noinspection IntegerMultiplicationImplicitCastToLong
        //TODO: this.structFormat.copyFromARGB(argb, null, this.addr + (long) ((y * this.width + x) * this.stride));
        if (true) {
            throw new UnsupportedOperationException();
        }
    }
}
