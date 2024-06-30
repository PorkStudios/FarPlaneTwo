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

package net.daporkchop.fp2.gl.state;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper methods for binding OpenGL objects.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class BindingHelper {
    /**
     * Clears multiple consecutive indexed buffer binding targets to buffer ID {@code 0}.
     *
     * @param gl     the OpenGL context
     * @param target the indexed buffer binding target
     * @param first  the first binding index
     * @param count  the number of binding indices to clear
     */
    public static void bindIndexedBuffers(OpenGL gl, IndexedBufferTarget target, int first, int count) {
        int targetId = target.id();

        if (gl.supports(GLExtension.GL_ARB_multi_bind)) {
            // If possible, use glBindBuffersBase to bind all the buffers to 0
            gl.glBindBuffersBase(targetId, first, count);
        } else {
            // Fall back to using a loop
            for (int i = 0; i < count; i++) {
                gl.glBindBufferBase(targetId, first + i, 0);
            }
        }
    }

    /**
     * Binds the given buffers to multiple consecutive indexed buffer binding targets, starting from the given {@code first} index.
     *
     * @param gl      the OpenGL context
     * @param target  the indexed buffer binding target
     * @param first   the first binding index
     * @param buffers the IDs of the buffers to bind
     */
    public static void bindIndexedBuffers(OpenGL gl, IndexedBufferTarget target, int first, int[] buffers) {
        int targetId = target.id();

        if (gl.supports(GLExtension.GL_ARB_multi_bind)) {
            // If possible, use glBindBuffersBase to bind all the buffers to 0
            gl.glBindBuffersBase(targetId, first, buffers);
        } else {
            // Fall back to using a loop
            for (int i = 0; i < buffers.length; i++) {
                gl.glBindBufferBase(targetId, first + i, buffers[i]);
            }
        }
    }
}
