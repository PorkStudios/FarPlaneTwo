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

package net.daporkchop.fp2.gl.opengl.buffer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum BufferTarget {
    ARRAY_BUFFER(GL_ARRAY_BUFFER, GL_ARRAY_BUFFER_BINDING),
    COPY_READ_BUFFER(GL_COPY_READ_BUFFER, GL_COPY_READ_BUFFER_BINDING),
    COPY_WRITE_BUFFER(GL_COPY_WRITE_BUFFER, GL_COPY_WRITE_BUFFER_BINDING),
    DISPATCH_INDIRECT_BUFFER(GL_DISPATCH_INDIRECT_BUFFER, GL_DISPATCH_INDIRECT_BUFFER_BINDING),
    DRAW_INDIRECT_BUFFER(GL_DRAW_INDIRECT_BUFFER, GL_DRAW_INDIRECT_BUFFER_BINDING),
    ELEMENT_ARRAY_BUFFER(GL_ELEMENT_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER_BINDING),
    PIXEL_PACK_BUFFER(GL_PIXEL_PACK_BUFFER, GL_PIXEL_PACK_BUFFER_BINDING),
    PIXEL_UNPACK_BUFFER(GL_PIXEL_UNPACK_BUFFER, GL_PIXEL_UNPACK_BUFFER_BINDING),
    QUERY_BUFFER(GL_QUERY_BUFFER, GL_QUERY_BUFFER_BINDING),
    SHADER_STORAGE_BUFFER(GL_SHADER_STORAGE_BUFFER, GL_SHADER_STORAGE_BUFFER_BINDING),
    TEXTURE_BUFFER(GL_TEXTURE_BUFFER, GL_TEXTURE_BINDING_BUFFER),
    TRANSFORM_FEEDBACK_BUFFER(GL_TRANSFORM_FEEDBACK_BUFFER, GL_TRANSFORM_FEEDBACK_BUFFER_BINDING),
    UNIFORM_BUFFER(GL_UNIFORM_BUFFER, GL_UNIFORM_BUFFER_BINDING);

    private final int id;
    private final int binding;
}
