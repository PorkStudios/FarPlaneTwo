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

package net.daporkchop.fp2.core.client.render;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.common.annotation.NotThreadSafe;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Wrapper for the current reversed-Z render state.
 * <p>
 * This does not actually deal with the reversed-Z projection (for that, see {@link net.daporkchop.fp2.core.client.MatrixHelper#reversedZ}), but
 * it does manage the OpenGL clip space settings via {@link GLExtension#GL_ARB_clip_control}.
 *
 * @author DaPorkchop_
 */
@Getter
@NotThreadSafe
public abstract class ReversedZ {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_clip_control); //glClipControl()

    /**
     * Reverses the given OpenGL depth function, so that greater becomes less and vice-versa.
     *
     * @param depthFunc the OpenGL depth function
     * @return the reversed OpenGL depth function
     * @throws IllegalArgumentException if the given value is not a valid OpenGL depth function
     */
    public static int reverseDepthFunc(int depthFunc) {
        switch (depthFunc) {
            case GL_LESS:
                return GL_GREATER;
            case GL_LEQUAL:
                return GL_GEQUAL;
            case GL_GREATER:
                return GL_LESS;
            case GL_GEQUAL:
                return GL_LEQUAL;

            //these depth functions work the same regardless of direction
            case GL_ALWAYS:
            case GL_NEVER:
            case GL_EQUAL:
            case GL_NOTEQUAL:
                return depthFunc;

            default:
                throw new IllegalArgumentException("Invalid OpenGL depth function: " + depthFunc);
        }
    }

    private final @NonNull FP2Client client;
    private final @NonNull OpenGL gl;

    @Accessors(fluent = false)
    private boolean active;

    public ReversedZ(@NonNull FP2Client client) {
        client.gl().checkSupported(REQUIRED_EXTENSIONS);

        this.client = client;
        this.gl = client.gl();
    }

    private boolean isEnabled() {
        return this.client.fp2().globalConfig().compatibility().reversedZ();
    }

    @CalledFromClientThread
    public final void activate() {
        if (this.isEnabled()) {
            checkState(!this.active, "Reversed-Z projection is already active!");
            this.active = true;

            this.toggle(true, GL_LOWER_LEFT, GL_ZERO_TO_ONE, 0.0d);
        }
    }

    @CalledFromClientThread
    public final void deactivate() {
        if (this.isEnabled()) {
            checkState(this.active, "Reversed-Z projection isn't active!");
            this.active = false;

            this.toggle(false, GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE, 1.0d);
        }
    }

    private void toggle(boolean active, int origin, int depth, double clearDepth) {
        int oldDepthFunc = this.gl.glGetInteger(GL_DEPTH_FUNC);

        this.gl.glClipControl(origin, depth);
        this.onToggle(active, oldDepthFunc, clearDepth);
    }

    //TODO: should we also negate the current polygon offset factor and units?
    protected void onToggle(boolean active, int oldDepthFunc, double clearDepth) {
        this.gl.glDepthFunc(reverseDepthFunc(oldDepthFunc));
        this.gl.glClearDepth(clearDepth);
    }

    @CalledFromClientThread
    public final int transformDepthFunc(int depthFunc) {
        return this.active ? reverseDepthFunc(depthFunc) : depthFunc;
    }

    @CalledFromClientThread
    public final float transformPolygonOffsetFactor(float factor) {
        return this.active ? -factor : factor;
    }

    @CalledFromClientThread
    public final float transformPolygonOffsetUnits(float units) {
        return this.active ? -units : units;
    }
}
