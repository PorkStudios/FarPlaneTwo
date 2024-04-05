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

package net.daporkchop.fp2.gl.buffer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum BufferTarget {
    ARRAY_BUFFER(GL_ARRAY_BUFFER, GL_ARRAY_BUFFER_BINDING, null),
    COPY_READ_BUFFER(GL_COPY_READ_BUFFER, GL_COPY_READ_BUFFER_BINDING, GLExtension.GL_ARB_copy_buffer),
    COPY_WRITE_BUFFER(GL_COPY_WRITE_BUFFER, GL_COPY_WRITE_BUFFER_BINDING, GLExtension.GL_ARB_copy_buffer),
    DISPATCH_INDIRECT_BUFFER(GL_DISPATCH_INDIRECT_BUFFER, GL_DISPATCH_INDIRECT_BUFFER_BINDING, GLExtension.GL_ARB_compute_shader),
    DRAW_INDIRECT_BUFFER(GL_DRAW_INDIRECT_BUFFER, GL_DRAW_INDIRECT_BUFFER_BINDING, GLExtension.GL_ARB_draw_indirect),
    ELEMENT_ARRAY_BUFFER(GL_ELEMENT_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER_BINDING, null),
    PIXEL_PACK_BUFFER(GL_PIXEL_PACK_BUFFER, GL_PIXEL_PACK_BUFFER_BINDING, null), //from ARB_pixel_buffer_object, made core in 2.1
    PIXEL_UNPACK_BUFFER(GL_PIXEL_UNPACK_BUFFER, GL_PIXEL_UNPACK_BUFFER_BINDING, null),
    QUERY_BUFFER(GL_QUERY_BUFFER, GL_QUERY_BUFFER_BINDING, GLExtension.GL_ARB_query_buffer_object),
    SHADER_STORAGE_BUFFER(GL_SHADER_STORAGE_BUFFER, GL_SHADER_STORAGE_BUFFER_BINDING, GLExtension.GL_ARB_shader_storage_buffer_object),
    TEXTURE_BUFFER(GL_TEXTURE_BUFFER, GL_TEXTURE_BINDING_BUFFER, GLExtension.GL_ARB_texture_buffer_object),
    TRANSFORM_FEEDBACK_BUFFER(GL_TRANSFORM_FEEDBACK_BUFFER, GL_TRANSFORM_FEEDBACK_BUFFER_BINDING, GLExtension.GL_ARB_transform_feedback2),
    UNIFORM_BUFFER(GL_UNIFORM_BUFFER, GL_UNIFORM_BUFFER_BINDING, GLExtension.GL_ARB_uniform_buffer_object);

    private final int id;
    private final int binding;

    private final GLExtension requiredExtension;
}
