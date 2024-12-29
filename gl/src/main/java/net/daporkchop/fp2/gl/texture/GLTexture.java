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
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.function.Consumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class GLTexture extends GLObject.Normal {
    protected final boolean immutableStorage;

    private final TextureTarget target;

    protected GLTexture(@NonNull OpenGL gl, @NonNull TextureTarget target) {
        super(gl, gl.supports(GLExtension.GL_ARB_direct_state_access) ? gl.glCreateTexture(target.id()) : gl.glGenTexture());

        try {
            this.immutableStorage = gl.supports(GLExtension.GL_ARB_texture_storage);

            this.target = target;
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    protected final void delete() {
        this.gl.glDeleteTexture(this.id);
    }

    @Override
    protected final int debugLabelNamespace() {
        return GL_TEXTURE;
    }

    /**
     * Executes the given action with this texture bound to the texture's {@link TextureTarget texture binding target}.
     * <p>
     * This will restore the previously bound texture when the operation completes.
     *
     * @param callback the action to run
     */
    public final void bindPreserving(Consumer<TextureTarget> callback) {
        this.checkOpen();
        int old = this.gl.glGetInteger(this.target.binding());
        try {
            this.gl.glBindTexture(this.target.id(), this.id);
            callback.accept(this.target);
        } finally {
            this.gl.glBindTexture(this.target.id(), old);
        }
    }

    /**
     * Immediately binds this texture to the texture's {@link TextureTarget texture binding target} in the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound texture when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this texture bound will not cause future issues.
     */
    public final void bindUnsafe() {
        this.checkOpen();
        this.gl.glBindTexture(this.target.id(), this.id);
    }

    /**
     * Executes the given action with this texture bound to the texture's {@link TextureTarget texture binding target}.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound texture when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this texture bound will not cause future issues.
     *
     * @param callback the action to run
     */
    public final void bindUnsafe(Consumer<TextureTarget> callback) {
        this.checkOpen();
        this.gl.glBindTexture(this.target.id(), this.id);
        callback.accept(this.target);
    }

    /**
     * Executes the given action with this texture bound to the texture's {@link TextureTarget texture binding target}.
     *
     * @param callback the action to run
     */
    public final void bind(Consumer<TextureTarget> callback) {
        if (OpenGL.PRESERVE_TEXTURE_BINDINGS_IN_METHODS) {
            this.bindPreserving(callback);
        } else {
            this.bindUnsafe(callback);
        }
    }

    /**
     * Immediately binds this texture to the texture's {@link TextureTarget texture binding target} on the specified texture unit in the OpenGL context.
     * <p>
     * This method <strong>may</strong> change the active texture unit.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound texture or previously active texture unit when the operation
     * completes. The user is responsible for ensuring that OpenGL state is preserved, or that leaving this texture bound and/or the active texture unit modified
     * will not cause future issues.
     *
     * @param unit the texture unit to bind the texture to (zero-indexed)
     */
    public final void bindToUnitUnsafe(int unit) {
        this.checkOpen();

        if (this.dsa) {
            // If possible, use glBindTextureUnit() to bind the texture directly
            this.gl.glBindTextureUnit(unit, this.id);
        } else {
            // Fall back to glActiveTexture() + glBindTexture()
            this.gl.glActiveTexture(GL_TEXTURE0 + unit);
            this.gl.glBindTexture(this.target.id(), this.id);
        }
    }
}
