/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.function.Consumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * An OpenGL sampler object.
 *
 * @author DaPorkchop_
 */
public final class GLSampler extends GLObject.Normal implements ISamplingParameters {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.of(GLExtension.GL_ARB_sampler_objects);

    public static GLSampler create(OpenGL gl) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLSampler(gl);
    }

    private GLSampler(OpenGL gl) {
        super(gl, gl.supports(GLExtension.GL_ARB_direct_state_access) ? gl.glCreateSampler() : gl.glGenSampler());
    }

    @Override
    protected void delete() {
        this.gl.glDeleteSampler(this.id);
    }

    @Override
    protected int debugLabelNamespace() {
        return GL_SAMPLER;
    }

    /**
     * Executes the given action with this sampler bound to the currently active texture unit.
     * <p>
     * This will restore the previously bound sampler when the operation completes.
     *
     * @param callback the action to run
     */
    public void bindPreserving(Runnable callback) {
        this.checkOpen();
        int activeUnit = this.gl.glGetInteger(GL_ACTIVE_TEXTURE) - GL_TEXTURE0;
        int oldSampler = this.gl.glGetInteger(GL_SAMPLER_BINDING);
        try {
            this.gl.glBindSampler(activeUnit, this.id);
            callback.run();
        } finally {
            this.gl.glBindSampler(activeUnit, oldSampler);
        }
    }

    /**
     * Immediately binds this sampler to the currently active texture unit in the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound sampler when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this sampler bound will not cause future issues.
     */
    public void bindUnsafe() {
        this.checkOpen();
        int activeUnit = this.gl.glGetInteger(GL_ACTIVE_TEXTURE) - GL_TEXTURE0;
        this.gl.glBindSampler(activeUnit, this.id);
    }

    /**
     * Executes the given action with this sampler bound to the currently active texture unit.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound sampler when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this sampler bound will not cause future issues.
     *
     * @param callback the action to run
     */
    public void bindUnsafe(Runnable callback) {
        this.bindUnsafe();
        callback.run();
    }

    /**
     * Executes the given action with this sampler bound to the currently active texture unit.
     *
     * @param callback the action to run
     */
    public void bind(Runnable callback) {
        if (OpenGL.PRESERVE_SAMPLER_BINDINGS_IN_METHODS) {
            this.bindPreserving(callback);
        } else {
            this.bindUnsafe(callback);
        }
    }

    /**
     * Executes the given action with this sampler bound to the given texture unit.
     * <p>
     * This will restore the previously bound sampler when the operation completes.
     *
     * @param unit     the texture unit to bind this sampler to
     * @param callback the action to run
     */
    public void bindToUnitPreserving(@NotNegative int unit, Runnable callback) {
        this.checkOpen();

        //get the sampler previously bound to the given texture unit
        int oldActiveUnit = this.gl.glGetInteger(GL_ACTIVE_TEXTURE) - GL_TEXTURE0;
        int oldSampler;
        try {
            this.gl.glActiveTexture(unit + GL_TEXTURE0);
            oldSampler = this.gl.glGetInteger(GL_SAMPLER_BINDING);
        } finally {
            this.gl.glActiveTexture(oldActiveUnit + GL_TEXTURE0);
        }

        //actually bind ourself to the texture unit and then restore the old binding
        try {
            this.gl.glBindSampler(unit, this.id);
            callback.run();
        } finally {
            this.gl.glBindSampler(unit, oldSampler);
        }
    }

    /**
     * Immediately binds this sampler to the given texture unit in the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound sampler when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this sampler bound will not cause future issues.
     *
     * @param unit the texture unit to bind this sampler to
     */
    public void bindToUnitUnsafe(@NotNegative int unit) {
        this.checkOpen();
        this.gl.glBindSampler(unit, this.id);
    }

    /**
     * Executes the given action with this sampler bound to the given texture unit.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound sampler when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this sampler bound will not cause future issues.
     *
     * @param unit     the texture unit to bind this sampler to
     * @param callback the action to run
     */
    public void bindToUnitUnsafe(@NotNegative int unit, Runnable callback) {
        this.bindToUnitUnsafe(unit);
        callback.run();
    }

    /**
     * Executes the given action with this sampler bound to the given texture unit.
     *
     * @param unit     the texture unit to bind this sampler to
     * @param callback the action to run
     */
    public void bindToUnit(@NotNegative int unit, Runnable callback) {
        if (OpenGL.PRESERVE_SAMPLER_BINDINGS_IN_METHODS) {
            this.bindToUnitPreserving(unit, callback);
        } else {
            this.bindToUnitUnsafe(unit, callback);
        }
    }

    @Override
    public void setParameter(int pname, int param) {
        this.checkOpen();
        this.gl.glSamplerParameter(this.id, pname, param);
    }

    @Override
    public void setParameter(int pname, float param) {
        this.checkOpen();
        this.gl.glSamplerParameter(this.id, pname, param);
    }

    @Override
    public void setParameters(@NonNull Consumer<ParameterSetter> action) {
        this.checkOpen();
        action.accept(new SamplerParameterSetter(this.gl, this.id));
    }

    /**
     * Sets sampler object parameters.
     *
     * @author DaPorkchop_
     */
    static final class SamplerParameterSetter extends ParameterSetter {
        private final int id;

        SamplerParameterSetter(OpenGL gl, int id) {
            super(gl);
            this.id = id;
        }

        @Override
        public void set(int pname, int param) {
            this.gl.glSamplerParameter(this.id, pname, param);
        }

        @Override
        public void set(int pname, float param) {
            this.gl.glSamplerParameter(this.id, pname, param);
        }
    }
}
