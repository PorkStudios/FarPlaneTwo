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

package net.daporkchop.fp2.gl.util.debug;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class GLDebugOutputUtil {
    /**
     * Configures OpenGL debug output for the given context.
     *
     * @param gl       the OpenGL context
     * @param callback a callback which will receive the debug messages
     */
    public static void configureDebugOutput(OpenGL gl, @NonNull GLDebugOutputCallback callback) {
        if (!OpenGL.DEBUG_OUTPUT) {
            return;
        }

        if ((gl.glGetInteger(GL_CONTEXT_FLAGS) & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
            throw new IllegalStateException("OpenGL debug output is enabled, but this isn't a debug context! " + gl);
        }

        if (gl.supports(GLExtension.GL_KHR_debug)) {
            gl.glEnable(GL_DEBUG_OUTPUT);
            gl.glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
            gl.glDebugMessageCallback(callback);
        } else if (gl.supports(GLExtension.GL_ARB_debug_output)) {
            gl.glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB);
            gl.glDebugMessageCallbackARB(callback);
        } else {
            throw new UnsupportedOperationException(unsupportedMessage(gl));
        }

        setEnabledDebugMessages(gl);
    }

    public static void setEnabledDebugMessages(OpenGL gl) {
        //enables all debug messages
        glDebugMessageControl(gl, GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, null, true);

        //suppress some weird texture thing which gets spammed by NVIDIA on every framebuffer clear
        glDebugMessageControl(gl, GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DONT_CARE, new int[]{ 0x20084 }, false);

        //suppress NVIDIA driver notifications indicating what kind of memory buffers are stored in (this would be very useful, but it gets
        // logged for every single glCopyBufferSubData)
        glDebugMessageControl(gl, GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DONT_CARE, new int[]{ 131185 }, false);
    }

    private static void glDebugMessageControl(OpenGL gl, int source, int type, int severity, int[] ids, boolean enabled) {
        if (gl.supports(GLExtension.GL_KHR_debug)) {
            gl.glDebugMessageControl(source, type, severity, ids, enabled);
        } else if (gl.supports(GLExtension.GL_ARB_debug_output)) {
            gl.glDebugMessageControlARB(source, type, severity, ids, enabled);
        } else {
            throw new UnsupportedOperationException(unsupportedMessage(gl));
        }
    }

    private static String unsupportedMessage(OpenGL gl) {
        return "OpenGL debug tracing is enabled, but neither " + GLExtension.GL_KHR_debug + " nor " + GLExtension.GL_ARB_debug_output + " are supported! " + gl;
    }
}
