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

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum IndexedBufferTarget {
    TRANSFORM_FEEDBACK_BUFFER(GL_TRANSFORM_FEEDBACK_BUFFER, GL_TRANSFORM_FEEDBACK_BUFFER_BINDING,
            GL_TRANSFORM_FEEDBACK_BUFFER_START, GL_TRANSFORM_FEEDBACK_BUFFER_SIZE),
    SHADER_STORAGE_BUFFER(GL_SHADER_STORAGE_BUFFER, GL_SHADER_STORAGE_BUFFER_BINDING,
            GL_SHADER_STORAGE_BUFFER_START, GL_SHADER_STORAGE_BUFFER_SIZE),
    UNIFORM_BUFFER(GL_UNIFORM_BUFFER, GL_UNIFORM_BUFFER_BINDING,
            GL_UNIFORM_BUFFER_START, GL_UNIFORM_BUFFER_SIZE);

    private final int id;
    private final int binding;
    private final int bindingStart;
    private final int bindingSize;
}
