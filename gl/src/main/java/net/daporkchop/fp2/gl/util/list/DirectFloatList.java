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

package net.daporkchop.fp2.gl.util.list;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.util.AbstractDirectList;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * An off-heap list of tightly packed {@code float}s.
 * <p>
 * This is compatible with {@link net.daporkchop.fp2.gl.OpenGL#glUniform1f(int, java.nio.FloatBuffer)} and the {@code std430} layout.
 *
 * @author DaPorkchop_
 */
public final class DirectFloatList extends AbstractDirectList<Float> {
    public DirectFloatList(@NonNull DirectMemoryAllocator alloc) {
        super(alloc, DEFAULT_INITIAL_CAPACITY, Float.BYTES);
    }

    @Override
    public void set(@NotNegative int index, @NonNull Float value) {
        PUnsafe.putFloat(this.getElementPtr(index, Float.BYTES), value);
    }

    @Override
    public Float get(@NotNegative int index) {
        return PUnsafe.getFloat(this.getElementPtr(index, Float.BYTES));
    }
}
