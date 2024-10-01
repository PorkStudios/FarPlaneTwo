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
import net.daporkchop.fp2.gl.util.type.GLIVec2;
import net.daporkchop.lib.common.annotation.param.NotNegative;

/**
 * An off-heap list of tightly packed {@code ivec2}s.
 * <p>
 * This is compatible with {@link net.daporkchop.fp2.gl.OpenGL#glUniform2i(int, java.nio.IntBuffer)} and the {@code std430} layout.
 *
 * @author DaPorkchop_
 */
public final class DirectIVec2List extends AbstractDirectList<GLIVec2> {
    public DirectIVec2List(@NonNull DirectMemoryAllocator alloc) {
        super(alloc, DEFAULT_INITIAL_CAPACITY, GLIVec2.BYTES);
    }

    @Override
    public void set(@NotNegative int index, @NonNull GLIVec2 value) {
        GLIVec2._set(this.getElementPtr(index, GLIVec2.BYTES), value);
    }

    @Override
    public GLIVec2 get(@NotNegative int index) {
        return GLIVec2._get(this.getElementPtr(index, GLIVec2.BYTES));
    }
}
