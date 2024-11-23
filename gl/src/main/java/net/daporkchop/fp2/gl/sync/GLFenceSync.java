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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLObject;

import java.util.concurrent.TimeUnit;

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
    private boolean flushed;

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

    /**
     * Waits for this fence sync object to be signalled.
     */
    public WaitStatus waitSignalled(long timeout, TimeUnit unit) {
        this.checkOpen();

        int flags = this.flushed ? 0 : GL_SYNC_FLUSH_COMMANDS_BIT;
        this.flushed = true;

        int status = this.gl.glClientWaitSync(this.sync, flags, unit.toNanos(timeout));
        switch (status) {
            case GL_ALREADY_SIGNALED:
                return WaitStatus.ALREADY_SIGNALED;
            case GL_TIMEOUT_EXPIRED:
                return WaitStatus.TIMEOUT_EXPIRED;
            case GL_CONDITION_SATISFIED:
                return WaitStatus.CONDITION_SATISFIED;
            case GL_WAIT_FAILED:
                this.gl.checkError(); //actually, we'll throw the OpenGL error
                return WaitStatus.WAIT_FAILED;
            default:
                throw new IllegalArgumentException("unknown status: " + status);
        }
    }

    @Override
    protected void delete() {
        this.gl.glDeleteSync(this.sync);
    }

    @Override
    public void setDebugLabel(@NonNull CharSequence label) {
        this.gl.glObjectPtrLabel(this.sync, label);
    }

    @Override
    public String getDebugLabel() {
        return this.gl.glGetObjectPtrLabel(this.sync);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    public enum WaitStatus {
        ALREADY_SIGNALED(GL_ALREADY_SIGNALED),
        TIMEOUT_EXPIRED(GL_TIMEOUT_EXPIRED),
        CONDITION_SATISFIED(GL_CONDITION_SATISFIED),
        WAIT_FAILED(GL_WAIT_FAILED),
        ;

        private final int id;
    }
}
