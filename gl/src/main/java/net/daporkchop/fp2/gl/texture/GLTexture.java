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

package net.daporkchop.fp2.gl.texture;

import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLObject;

import java.util.function.Consumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class GLTexture extends GLObject.Normal {
    protected GLTexture(OpenGL gl) {
        super(gl, gl.glGenTexture());
    }

    @Override
    protected final void delete() {
        this.gl.glDeleteTexture(this.id);
    }

    @Override
    public final void setDebugLabel(@NonNull CharSequence label) {
        this.gl.glObjectLabel(GL_TEXTURE, this.id, label);
    }

    @Override
    public final String getDebugLabel() {
        return this.gl.glGetObjectLabel(GL_TEXTURE, this.id);
    }

    /**
     * Executes the given action with this texture bound to the given {@link TextureTarget texture binding target}.
     * <p>
     * This will restore the previously bound texture when the operation completes.
     *
     * @param target   the {@link TextureTarget texture binding target} to bind the texture to
     * @param callback the action to run
     */
    public final void bindPreserving(TextureTarget target, Consumer<TextureTarget> callback) {
        this.checkOpen();
        int old = this.gl.glGetInteger(target.binding());
        try {
            this.gl.glBindTexture(target.id(), this.id);
            callback.accept(target);
        } finally {
            this.gl.glBindTexture(target.id(), old);
        }
    }

    /**
     * Immediately binds this texture to the given {@link TextureTarget texture binding target} in the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound texture when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this texture bound will not cause future issues.
     *
     * @param target the {@link TextureTarget texture binding target} to bind the texture to
     */
    public final void bindUnsafe(TextureTarget target) {
        this.checkOpen();
        this.gl.glBindTexture(target.id(), this.id);
    }

    /**
     * Executes the given action with this texture bound to the given {@link TextureTarget texture binding target}.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound texture when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this texture bound will not cause future issues.
     *
     * @param target   the {@link TextureTarget texture binding target} to bind the texture to
     * @param callback the action to run
     */
    public final void bindUnsafe(TextureTarget target, Consumer<TextureTarget> callback) {
        this.checkOpen();
        this.gl.glBindTexture(target.id(), this.id);
        callback.accept(target);
    }

    /**
     * Executes the given action with this texture bound to the given {@link TextureTarget texture binding target}.
     *
     * @param target   the {@link TextureTarget texture binding target} to bind the texture to
     * @param callback the action to run
     */
    public final void bind(TextureTarget target, Consumer<TextureTarget> callback) {
        if (OpenGL.PRESERVE_TEXTURE_BINDINGS_IN_METHODS) {
            this.bindPreserving(target, callback);
        } else {
            this.bindUnsafe(target, callback);
        }
    }
}
