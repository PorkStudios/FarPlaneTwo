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
import net.daporkchop.fp2.gl.draw.command.DrawList;
import net.daporkchop.fp2.gl.draw.binding.DrawMode;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;

/**
 * Builder for {@link GLBitSet}s.
 *
 * @author DaPorkchop_
 */
public interface GLBitSetBuilder {
    /**
     * Hints that a {@link GLBitSet} implementation should be chosen which is optimized for usage with {@link DrawList#execute(DrawMode, DrawShaderProgram, GLBitSet)}
     * for the given {@link DrawList}.
     *
     * @param commandBuffer the {@link DrawList}
     */
    GLBitSetBuilder optimizeFor(@NonNull DrawList<?> commandBuffer);

    /**
     * @return the constructed {@link GLBitSet}
     */
    GLBitSet build();
}
