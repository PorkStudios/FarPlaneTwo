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

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.draw.binding.DrawMode;
import net.daporkchop.fp2.gl.draw.index.IndexType;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * Methods for converting GL enum values to actual OpenGL integers.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class GLEnumUtil {
    public int from(@NonNull BufferUsage usage) {
        switch (usage) {
            case STREAM_DRAW:
                return GL_STREAM_DRAW;
            case STREAM_READ:
                return GL_STREAM_READ;
            case STREAM_COPY:
                return GL_STREAM_COPY;
            case STATIC_DRAW:
                return GL_STATIC_DRAW;
            case STATIC_READ:
                return GL_STATIC_READ;
            case STATIC_COPY:
                return GL_STATIC_COPY;
            case DYNAMIC_READ:
                return GL_DYNAMIC_READ;
            case DYNAMIC_DRAW:
                return GL_DYNAMIC_DRAW;
            case DYNAMIC_COPY:
                return GL_DYNAMIC_COPY;
            default:
                throw new IllegalArgumentException(usage.name());
        }
    }

    public int from(@NonNull DrawMode mode) {
        switch (mode) {
            case POINTS:
                return GL_POINTS;
            case LINES:
                return GL_LINES;
            case LINE_STRIP:
                return GL_LINE_STRIP;
            case LINE_LOOP:
                return GL_LINE_LOOP;
            case TRIANGLES:
                return GL_TRIANGLES;
            case TRIANGLE_STRIP:
                return GL_TRIANGLE_STRIP;
            case TRIANGLE_FAN:
                return GL_TRIANGLE_FAN;
            case QUADS:
                return GL_QUADS;
            default:
                throw new IllegalArgumentException(mode.name());
        }
    }

    public int from(@NonNull IndexType type) {
        switch (type) {
            case UNSIGNED_BYTE:
                return GL_UNSIGNED_BYTE;
            case UNSIGNED_SHORT:
                return GL_UNSIGNED_SHORT;
            case UNSIGNED_INT:
                return GL_UNSIGNED_INT;
            default:
                throw new IllegalArgumentException(type.name());
        }
    }
}
