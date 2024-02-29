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

package net.daporkchop.fp2.gl.attribute;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * The usage hints which an OpenGL buffer's storage may be created with.
 * <p>
 * {@code usage} can be broken down into two parts: first, the frequency of access (modification and usage), and second, the nature of that access. The frequency of access may be
 * one of these:
 * <ul>
 *     <li>{@code STREAM}<br>
 *     The data store contents will be modified once and used at most a few times.</li>
 *     <li>{@code STATIC}<br>
 *     The data store contents will be modified once and used many times.</li>
 *     <li>{@code DYNAMIC}<br>
 *     The data store contents will be modified repeatedly and used many times.</li>
 * </ul>
 * <p>
 * The nature of access may be one of these:
 * <ul>
 *     <li>{@code DRAW}<br>
 *     The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
 *     <li>{@code READ}<br>
 *     he data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
 *     <li>{@code COPY}<br>
 *     The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum BufferUsage {
    STREAM_DRAW(GL_STREAM_DRAW),
    STREAM_READ(GL_STREAM_READ),
    STREAM_COPY(GL_STREAM_COPY),
    STATIC_DRAW(GL_STATIC_DRAW),
    STATIC_READ(GL_STATIC_READ),
    STATIC_COPY(GL_STATIC_COPY),
    DYNAMIC_READ(GL_DYNAMIC_READ),
    DYNAMIC_DRAW(GL_DYNAMIC_DRAW),
    DYNAMIC_COPY(GL_DYNAMIC_COPY);

    private final int usage;
}
