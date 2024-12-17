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

package net.daporkchop.fp2.gl.texture.framebuffer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class FramebufferException extends IllegalStateException {
    /**
     * Translate a GL error code to a String describing the error
     */
    private static String translateFramebufferStatus(int status) {
        switch (status) {
            case GL_FRAMEBUFFER_COMPLETE:
                return GL_FRAMEBUFFER_COMPLETE + " (FRAMEBUFFER_COMPLETE)";
            case GL_FRAMEBUFFER_UNDEFINED:
                return GL_FRAMEBUFFER_UNDEFINED + " (FRAMEBUFFER_UNDEFINED)";
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT + " (FRAMEBUFFER_INCOMPLETE_ATTACHMENT)";
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT + " (FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)";
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER + " (FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER)";
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER + " (FRAMEBUFFER_INCOMPLETE_READ_BUFFER)";
            case GL_FRAMEBUFFER_UNSUPPORTED:
                return GL_FRAMEBUFFER_UNSUPPORTED + " (FRAMEBUFFER_UNSUPPORTED)";
            case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                return GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE + " (FRAMEBUFFER_INCOMPLETE_MULTISAMPLE)";
            case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
                return GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS + " (FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS)";
            default:
                return status + " (<unknown>)";
        }
    }

    public final int status;

    public FramebufferException(int status) {
        super(translateFramebufferStatus(status));
        this.status = status;
    }
}
