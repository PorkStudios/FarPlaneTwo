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

package net.daporkchop.fp2.gl.opengl;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * All known OpenGL extensions.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public enum GLExtension {
    GL_ARB_base_instance(GLVersion.OpenGL42, false),
    GL_ARB_compatibility(GLVersion.OpenGL30, false) {
        @Override
        public boolean supported(@NonNull OpenGL gl) {
            return gl.extensions().contains(this);
        }
    },
    GL_ARB_compute_shader(GLVersion.OpenGL43, false),
    GL_ARB_draw_elements_base_vertex(GLVersion.OpenGL32, false),
    GL_ARB_draw_indirect(GLVersion.OpenGL40, false),
    GL_ARB_instanced_arrays(GLVersion.OpenGL33, false),
    GL_ARB_multi_draw_indirect(GLVersion.OpenGL43, false),
    GL_ARB_uniform_buffer_object(GLVersion.OpenGL31, true);

    @NonNull
    private final GLVersion coreVersion;

    @Getter
    private final boolean glsl;

    /**
     * Checks whether or not this extension is a core feature in the given OpenGL context.
     *
     * @param gl the context
     */
    public boolean core(@NonNull OpenGL gl) {
        return gl.version().compareTo(this.coreVersion) >= 0;
    }

    /**
     * Checks whether or not this extension is supported in the given OpenGL context.
     *
     * @param gl the context
     */
    public boolean supported(@NonNull OpenGL gl) {
        return gl.version().compareTo(this.coreVersion) >= 0 || gl.extensions().contains(this);
    }
}
