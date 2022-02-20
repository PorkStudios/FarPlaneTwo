/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class OpenGLException extends RuntimeException {
    /**
     * Translate a GL error code to a String describing the error
     */
    private static String translateGLErrorString(int errorCode) {
        switch (errorCode) {
            case GL_NO_ERROR:
                return GL_NO_ERROR + " (NO_ERROR)";
            case GL_INVALID_ENUM:
                return GL_INVALID_ENUM + " (INVALID_ENUM)";
            case GL_INVALID_VALUE:
                return GL_INVALID_VALUE + " (INVALID_VALUE)";
            case GL_INVALID_OPERATION:
                return GL_INVALID_OPERATION + " (INVALID_OPERATION)";
            case GL_STACK_OVERFLOW:
                return GL_STACK_OVERFLOW + " (STACK_OVERFLOW)";
            case GL_STACK_UNDERFLOW:
                return GL_STACK_UNDERFLOW + " (STACK_UNDERFLOW)";
            case GL_OUT_OF_MEMORY:
                return GL_OUT_OF_MEMORY + " (OUT_OF_MEMORY)";
            /*case GL_TABLE_TOO_LARGE:
                return GL_TABLE_TOO_LARGE + " (TABLE_TOO_LARGE)";*/
            case GL_INVALID_FRAMEBUFFER_OPERATION:
                return GL_INVALID_FRAMEBUFFER_OPERATION + " (INVALID_FRAMEBUFFER_OPERATION)";
            default:
                return null;
        }
    }

    public OpenGLException(int errorCode) {
        super(translateGLErrorString(errorCode));
    }
}
