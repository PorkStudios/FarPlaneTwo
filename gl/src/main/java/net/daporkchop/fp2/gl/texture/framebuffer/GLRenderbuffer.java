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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.texture.TextureInternalFormat;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Wrapper around an OpenGL renderbuffer object.
 *
 * @author DaPorkchop_
 */
@Getter
public final class GLRenderbuffer extends GLObject.Normal {
    public static GLRenderbuffer create(@NonNull OpenGL gl) {
        return new GLRenderbuffer(gl);
    }

    private int width = -1;
    private int height = -1;

    private GLRenderbuffer(OpenGL gl) {
        super(gl, gl.supports(GLExtension.GL_ARB_direct_state_access) ? gl.glCreateRenderbuffer() : gl.glGenRenderbuffer());
    }

    @Override
    protected void delete() {
        this.gl.glDeleteRenderbuffer(this.id);
    }

    @Override
    protected int debugLabelNamespace() {
        return GL_RENDERBUFFER;
    }

    /**
     * Reallocates this renderbuffer's storage.
     *
     * @param internalFormat the new internal format for the pixel data
     * @param width          the new width
     * @param height         the new height
     */
    public void capacity(@NonNull TextureInternalFormat internalFormat, @NotNegative int width, @NotNegative int height) {
        this.checkOpen();

        if (this.dsa) {
            this.gl.glNamedRenderbufferStorage(this.id, internalFormat.id(), width, height);
        } else {
            this.bind(() -> {
                this.gl.glRenderbufferStorage(GL_RENDERBUFFER, internalFormat.id(), width, height);
            });
        }

        this.width = width;
        this.height = height;
    }

    /**
     * Executes the given action with this renderbuffer bound.
     * <p>
     * This will restore the previously bound renderbuffer when the operation completes.
     *
     * @param action the action to run
     */
    public void bindPreserving(Runnable action) {
        this.checkOpen();
        int old = this.gl.glGetInteger(GL_RENDERBUFFER_BINDING);
        try {
            this.gl.glBindRenderbuffer(GL_RENDERBUFFER, this.id);
            action.run();
        } finally {
            this.gl.glBindRenderbuffer(GL_RENDERBUFFER, old);
        }
    }

    /**
     * Immediately binds this renderbuffer to the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound renderbuffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this renderbuffer bound will not cause future issues.
     */
    public void bindUnsafe() {
        this.checkOpen();
        this.gl.glBindRenderbuffer(GL_RENDERBUFFER, this.id);
    }

    /**
     * Executes the given action with this renderbuffer bound.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound renderbuffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this renderbuffer bound will not cause future issues.
     *
     * @param action the action to run
     */
    public void bindUnsafe(Runnable action) {
        this.bindUnsafe();
        action.run();
    }

    /**
     * Executes the given action with this renderbuffer bound.
     *
     * @param action the action to run
     */
    public void bind(Runnable action) {
        if (OpenGL.PRESERVE_RENDERBUFFER_BINDINGS_IN_METHODS) {
            this.bindPreserving(action);
        } else {
            this.bindUnsafe(action);
        }
    }
}
