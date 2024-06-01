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

package net.daporkchop.fp2.gl.sync;

import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLObject;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * An OpenGL fence sync object.
 *
 * @author DaPorkchop_
 */
public final class GLFenceSync extends GLObject {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_sync);

    public static GLFenceSync create(OpenGL gl) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLFenceSync(gl, gl.glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
    }

    private final long sync;

    private GLFenceSync(OpenGL gl, long sync) {
        super(gl);
        this.sync = sync;
    }

    /**
     * Checks if this fence sync object has been signalled.
     *
     * @return {@code true} if this fence sync object has been signalled, {@code false} otherwise
     */
    public boolean isSignalled() {
        this.checkOpen();
        return this.gl.glGetSync(this.sync, GL_SYNC_STATUS) == GL_SIGNALED;
    }

    @Override
    protected void delete() {
        this.gl.glDeleteSync(this.sync);
    }
}
