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

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.OpenGLConstants;
import net.daporkchop.fp2.gl.texture.GLTexture2D;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.function.Consumer;
import java.util.function.Function;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Wrapper around an OpenGL framebuffer object.
 *
 * @author DaPorkchop_
 */
public final class GLFramebuffer extends GLObject.Normal {
    public static GLFramebuffer create(@NonNull OpenGL gl) {
        return new GLFramebuffer(gl);
    }

    private GLFramebuffer(OpenGL gl) {
        super(gl, gl.supports(GLExtension.GL_ARB_direct_state_access) ? gl.glCreateFramebuffer() : gl.glGenFramebuffer());
    }

    @Override
    protected void delete() {
        this.gl.glDeleteFramebuffer(this.id);
    }

    @Override
    protected int debugLabelNamespace() {
        return GL_FRAMEBUFFER;
    }

    /**
     * Attaches the given mipmap level of the given texture to the given {@link FramebufferAttachment framebuffer attachment point} on this framebuffer.
     *
     * @param attachment the {@link FramebufferAttachment framebuffer attachment point}
     * @param texture    the texture to be attached
     * @param level      the mipmap level in the texture to be attached
     */
    public void attachTexture(@NonNull FramebufferAttachment attachment, @NonNull GLTexture2D texture, @NotNegative int level) {
        this.checkOpen();

        if (this.dsa) {
            this.gl.glNamedFramebufferTexture(this.id, attachment.id(), texture.id(), level);
        } else {
            this.bind(FramebufferTarget.READ_FRAMEBUFFER, framebufferTarget -> {
                texture.bind(textureTarget -> {
                    this.gl.glFramebufferTexture2D(framebufferTarget.id(), attachment.id(), textureTarget.id(), texture.id(), level);
                });
            });
        }
    }

    /**
     * Attaches the given renderbuffer to the given {@link FramebufferAttachment framebuffer attachment point} on this framebuffer.
     *
     * @param attachment   the {@link FramebufferAttachment framebuffer attachment point}
     * @param renderbuffer the renderbuffer to be attached
     */
    public void attachRenderbuffer(@NonNull FramebufferAttachment attachment, @NonNull GLRenderbuffer renderbuffer) {
        this.checkOpen();

        if (this.dsa) {
            this.gl.glNamedFramebufferRenderbuffer(this.id, attachment.id(), GL_RENDERBUFFER, renderbuffer.id());
        } else {
            this.bind(FramebufferTarget.READ_FRAMEBUFFER, framebufferTarget -> {
                renderbuffer.bind(() -> {
                    this.gl.glFramebufferRenderbuffer(framebufferTarget.id(), attachment.id(), GL_RENDERBUFFER, renderbuffer.id());
                });
            });
        }
    }

    /**
     * Validates this framebuffer status and ensures that it's {@link OpenGLConstants#GL_FRAMEBUFFER_COMPLETE complete}.
     *
     * @throws FramebufferException if this framebuffer is incomplete
     */
    public void checkComplete() throws FramebufferException {
        this.checkOpen();

        int status;
        if (this.dsa) {
            //the second argument should be irrelevant, from what i can tell it's always ignored unless the framebuffer id is 0
            status = this.gl.glCheckNamedFramebufferStatus(this.id, GL_FRAMEBUFFER);
        } else {
            status = this.bind(FramebufferTarget.READ_FRAMEBUFFER, target -> {
                return this.gl.glCheckFramebufferStatus(target.id());
            });
        }

        //throw an exception if the framebuffer is incomplete
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new FramebufferException(status);
        }
    }

    /**
     * Copies a rectangular region of one framebuffer to a rectangular region of another framebuffer.
     * <p>
     * If the source and destination rectangles are differently sized, the pixels will be resampled according to the given {@code filter}. If the source
     * and destination rectangles overlap and both {@code readFramebuffer} and {@code drawFramebuffer} are the same, the behavior is undefined.
     * <p>
     * Both {@code readFramebuffer} and {@code drawFramebuffer} may be {@code null}, in which case the default framebuffer will be used.
     *
     * @param gl              the OpenGL context
     * @param readFramebuffer the framebuffer to copy from, or {@code null} if the default framebuffer should be used
     * @param srcX0           the minimum X coordinate of the source rectangle (inclusive)
     * @param srcY0           the minimum Y coordinate of the source rectangle (inclusive)
     * @param srcX1           the maximum X coordinate of the source rectangle (exclusive)
     * @param srcY1           the maximum Y coordinate of the source rectangle (exclusive)
     * @param drawFramebuffer the framebuffer to copy to, or {@code null} if the default framebuffer should be used
     * @param dstX0           the minimum X coordinate of the destination rectangle (inclusive)
     * @param dstY0           the minimum Y coordinate of the destination rectangle (inclusive)
     * @param dstX1           the maximum X coordinate of the destination rectangle (exclusive)
     * @param dstY1           the maximum Y coordinate of the destination rectangle (exclusive)
     * @param mask            a bitmask indicating the pixel values to copy. May be a bitwise OR of {@link OpenGLConstants#GL_COLOR_BUFFER_BIT GL_COLOR_BUFFER_BIT},
     *                        {@link OpenGLConstants#GL_DEPTH_BUFFER_BIT GL_DEPTH_BUFFER_BIT} and {@link OpenGLConstants#GL_STENCIL_BUFFER_BIT GL_STENCIL_BUFFER_BIT}
     * @param filter          the filter which controls how pixel values should be resampled if the source and destination rectangles are differently sized
     */
    public static void blit(
            @NonNull OpenGL gl,
            GLFramebuffer readFramebuffer, int srcX0, int srcY0, int srcX1, int srcY1,
            GLFramebuffer drawFramebuffer, int dstX0, int dstY0, int dstX1, int dstY1,
            int mask, @NonNull FramebufferBlitFilter filter) {

        checkArg((mask & (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT)) == mask, "invalid blit buffer mask: %s", mask);

        int readId = 0;
        if (readFramebuffer != null) {
            readId = readFramebuffer.id();
            checkArg(gl == readFramebuffer.gl);
        }

        int drawId = 0;
        if (drawFramebuffer != null) {
            drawId = drawFramebuffer.id();
            checkArg(gl == drawFramebuffer.gl);
        }

        if (gl.supports(GLExtension.GL_ARB_direct_state_access)) {
            gl.glBlitNamedFramebuffer(readId, drawId, srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter.id());
        } else {
            //bind+unbind both framebuffers to the appropriate targets
            int oldRead = gl.glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            int oldDraw = gl.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
            try {
                bindSeparate(gl, readId, drawId);

                gl.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter.id());
            } finally {
                bindSeparate(gl, oldRead, oldDraw);
            }
        }
    }

    private static void bindSeparate(OpenGL gl, int readId, int drawId) {
        if (readId == drawId) {
            gl.glBindFramebuffer(GL_FRAMEBUFFER, readId);
        } else {
            gl.glBindFramebuffer(GL_READ_FRAMEBUFFER, readId);
            gl.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawId);
        }
    }

    /**
     * Executes the given action with this framebuffer bound to the given {@link FramebufferTarget framebuffer binding target}.
     * <p>
     * This will restore the previously bound framebuffer when the operation completes.
     *
     * @param target   the {@link FramebufferTarget framebuffer binding target} to bind the framebuffer to
     * @param callback the action to run
     */
    public void bindPreserving(FramebufferTarget target, Consumer<FramebufferTarget> callback) {
        this.checkOpen();
        int old = this.gl.glGetInteger(target.binding());
        try {
            this.gl.glBindFramebuffer(target.id(), this.id);
            callback.accept(target);
        } finally {
            this.gl.glBindFramebuffer(target.id(), old);
        }
    }

    /**
     * Executes the given action with this framebuffer bound to the given {@link FramebufferTarget framebuffer binding target}.
     * <p>
     * This will restore the previously bound framebuffer when the operation completes.
     *
     * @param target   the {@link FramebufferTarget framebuffer binding target} to bind the framebuffer to
     * @param callback the action to run
     */
    public <T> T bindPreserving(FramebufferTarget target, Function<FramebufferTarget, T> callback) {
        this.checkOpen();
        int old = this.gl.glGetInteger(target.binding());
        try {
            this.gl.glBindFramebuffer(target.id(), this.id);
            return callback.apply(target);
        } finally {
            this.gl.glBindFramebuffer(target.id(), old);
        }
    }

    /**
     * Immediately binds this framebuffer to the given {@link FramebufferTarget framebuffer binding target} in the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound framebuffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this framebuffer bound will not cause future issues.
     *
     * @param target the {@link FramebufferTarget framebuffer binding target} to bind the framebuffer to
     */
    public void bindUnsafe(FramebufferTarget target) {
        this.checkOpen();
        this.gl.glBindFramebuffer(target.id(), this.id);
    }

    /**
     * Executes the given action with this framebuffer bound to the given {@link FramebufferTarget framebuffer binding target}.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound framebuffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this framebuffer bound will not cause future issues.
     *
     * @param target   the {@link FramebufferTarget framebuffer binding target} to bind the framebuffer to
     * @param callback the action to run
     */
    public void bindUnsafe(FramebufferTarget target, Consumer<FramebufferTarget> callback) {
        this.bindUnsafe(target);
        callback.accept(target);
    }

    /**
     * Executes the given action with this framebuffer bound to the given {@link FramebufferTarget framebuffer binding target}.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound framebuffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this framebuffer bound will not cause future issues.
     *
     * @param target   the {@link FramebufferTarget framebuffer binding target} to bind the framebuffer to
     * @param callback the action to run
     */
    public <T> T bindUnsafe(FramebufferTarget target, Function<FramebufferTarget, T> callback) {
        this.bindUnsafe(target);
        return callback.apply(target);
    }

    /**
     * Executes the given action with this framebuffer bound to the given {@link FramebufferTarget framebuffer binding target}.
     *
     * @param target   the {@link FramebufferTarget framebuffer binding target} to bind the framebuffer to
     * @param callback the action to run
     */
    public void bind(FramebufferTarget target, Consumer<FramebufferTarget> callback) {
        if (OpenGL.PRESERVE_FRAMEBUFFER_BINDINGS_IN_METHODS) {
            this.bindPreserving(target, callback);
        } else {
            this.bindUnsafe(target, callback);
        }
    }

    /**
     * Executes the given action with this framebuffer bound to the given {@link FramebufferTarget framebuffer binding target}.
     *
     * @param target   the {@link FramebufferTarget framebuffer binding target} to bind the framebuffer to
     * @param callback the action to run
     */
    public <T> T bind(FramebufferTarget target, Function<FramebufferTarget, T> callback) {
        if (OpenGL.PRESERVE_FRAMEBUFFER_BINDINGS_IN_METHODS) {
            return this.bindPreserving(target, callback);
        } else {
            return this.bindUnsafe(target, callback);
        }
    }
}
