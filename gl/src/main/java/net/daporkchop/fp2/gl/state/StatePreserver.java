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

package net.daporkchop.fp2.gl.state;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.texture.TextureTarget;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A utility for backing up and restoring OpenGL state.
 *
 * @author DaPorkchop_
 */
public final class StatePreserver {
    public static Builder builder(OpenGL gl) {
        return new Builder(gl);
    }

    private final OpenGL gl;
    private final boolean compatibility;
    private final boolean samplerObjects;

    //fixed-function blend state
    private boolean blendEnabled;
    private int blendFuncSrcRGB;
    private int blendFuncDstRGB;
    private int blendFuncSrcA;
    private int blendFuncDstA;
    private int blendEquationRGB;
    private int blendEquationA;
    private float blendColorR;
    private float blendColorG;
    private float blendColorB;
    private float blendColorA;

    //fixed-function color state
    private float clearColorR;
    private float clearColorG;
    private float clearColorB;
    private float clearColorA;
    private boolean colorMaskR;
    private boolean colorMaskG;
    private boolean colorMaskB;
    private boolean colorMaskA;

    //fixed-function culling state
    private boolean cullEnabled;

    //fixed-function depth state
    private boolean depthEnabled;
    private boolean depthMask;
    private int depthFunc;
    private double depthClearValue;

    //fixed-function stencil state
    private boolean stencilEnabled;
    private int stencilMask;
    private int stencilFunc;
    private int stencilRef;
    private int stencilValueMask;
    private int stencilFail;
    private int stencilPassDepthPass;
    private int stencilPassDepthFail;
    private int stencilClearValue;

    //simple bindings
    private int boundProgram;
    private int boundVAO;

    //ordinary buffer bindings
    private final BufferTarget[] bufferTargets;
    private final int[] bufferBindings;

    //indexed buffer bindings
    private final IndexedBufferBinding[] indexedBufferBindings;

    //texture bindings
    private int activeTexture;
    private final TextureBinding[] textureBindings;

    StatePreserver(Builder builder) {
        this.gl = builder.gl;
        this.compatibility = this.gl.supports(GLExtension.GL_ARB_compatibility);
        this.samplerObjects = this.gl.supports(GLExtension.GL_ARB_sampler_objects);

        this.bufferTargets = builder.bufferTargets.toArray(new BufferTarget[0]);
        this.bufferBindings = new int[this.bufferTargets.length];

        this.indexedBufferBindings = builder.indexedBuffers.toArray(new IndexedBufferBinding[0]);
        this.textureBindings = builder.textures.toArray(new TextureBinding[0]);
    }

    /**
     * Backs up the current OpenGL state into this object.
     *
     * @return an {@link AutoCloseable} which must be {@link AutoCloseable#close() closed} to restore the original OpenGL state
     */
    public AutoCloseable backup() {
        OpenGL gl = this.gl;

        long tmpBuffer = PUnsafe.allocateMemory(4 * Float.BYTES);
        try {
            if (this.compatibility) {
                //back up most of the fixed-function state
                gl.glPushAttrib(GL_ALL_ATTRIB_BITS);
                gl.glPushClientAttrib(GL_ALL_CLIENT_ATTRIB_BITS);
            } else {
                //manually back up the most important fixed-function state using a billion function calls

                //fixed-function blend state
                this.blendEnabled = gl.glIsEnabled(GL_BLEND);
                this.blendFuncSrcRGB = gl.glGetInteger(GL_BLEND_SRC_RGB);
                this.blendFuncDstRGB = gl.glGetInteger(GL_BLEND_DST_RGB);
                this.blendFuncSrcA = gl.glGetInteger(GL_BLEND_SRC_ALPHA);
                this.blendFuncDstA = gl.glGetInteger(GL_BLEND_DST_ALPHA);
                this.blendEquationRGB = gl.glGetInteger(GL_BLEND_EQUATION_RGB);
                this.blendEquationA = gl.glGetInteger(GL_BLEND_EQUATION_ALPHA);
                gl.glGetFloat(GL_BLEND_COLOR, tmpBuffer);
                this.blendColorR = PUnsafe.getFloat(tmpBuffer + 0 * Float.BYTES);
                this.blendColorG = PUnsafe.getFloat(tmpBuffer + 1 * Float.BYTES);
                this.blendColorB = PUnsafe.getFloat(tmpBuffer + 2 * Float.BYTES);
                this.blendColorA = PUnsafe.getFloat(tmpBuffer + 3 * Float.BYTES);

                //fixed-function color state
                gl.glGetFloat(GL_COLOR_CLEAR_VALUE, tmpBuffer);
                this.clearColorR = PUnsafe.getFloat(tmpBuffer + 0 * Float.BYTES);
                this.clearColorG = PUnsafe.getFloat(tmpBuffer + 1 * Float.BYTES);
                this.clearColorB = PUnsafe.getFloat(tmpBuffer + 2 * Float.BYTES);
                this.clearColorA = PUnsafe.getFloat(tmpBuffer + 3 * Float.BYTES);
                gl.glGetBoolean(GL_COLOR_WRITEMASK, tmpBuffer);
                this.colorMaskR = PUnsafe.getBoolean(tmpBuffer + 0);
                this.colorMaskG = PUnsafe.getBoolean(tmpBuffer + 1);
                this.colorMaskB = PUnsafe.getBoolean(tmpBuffer + 2);
                this.colorMaskA = PUnsafe.getBoolean(tmpBuffer + 3);

                //fixed-function culling state
                this.cullEnabled = gl.glIsEnabled(GL_CULL_FACE);

                //fixed-function depth state
                this.depthEnabled = gl.glIsEnabled(GL_DEPTH_TEST);
                this.depthMask = gl.glGetBoolean(GL_DEPTH_WRITEMASK);
                this.depthFunc = gl.glGetInteger(GL_DEPTH_FUNC);
                this.depthClearValue = gl.glGetDouble(GL_DEPTH_CLEAR_VALUE);

                //fixed-function stencil state
                this.stencilEnabled = gl.glIsEnabled(GL_STENCIL_TEST);
                this.stencilMask = gl.glGetInteger(GL_STENCIL_WRITEMASK);
                this.stencilFunc = gl.glGetInteger(GL_STENCIL_FUNC);
                this.stencilRef = gl.glGetInteger(GL_STENCIL_REF);
                this.stencilValueMask = gl.glGetInteger(GL_STENCIL_VALUE_MASK);
                this.stencilFail = gl.glGetInteger(GL_STENCIL_FAIL);
                this.stencilPassDepthPass = gl.glGetInteger(GL_STENCIL_PASS_DEPTH_PASS);
                this.stencilPassDepthFail = gl.glGetInteger(GL_STENCIL_PASS_DEPTH_FAIL);
                this.stencilClearValue = gl.glGetInteger(GL_STENCIL_CLEAR_VALUE);
            }

            //simple bindings
            this.boundProgram = gl.glGetInteger(GL_CURRENT_PROGRAM);
            this.boundVAO = gl.glGetInteger(GL_VERTEX_ARRAY_BINDING);

            //ordinary buffer bindings
            BufferTarget[] bufferTargets = this.bufferTargets;
            int[] bufferBindings = this.bufferBindings;
            for (int i = 0; i < bufferTargets.length; i++) {
                bufferBindings[i] = gl.glGetInteger(bufferTargets[i].binding());
            }

            //indexed buffer bindings
            for (IndexedBufferBinding binding : this.indexedBufferBindings) {
                binding.buffer = gl.glGetInteger(binding.target.binding(), binding.index);
                //TODO: maybe we could back up the offset and size as well (for glBindBufferRange)
            }

            //texture bindings
            this.activeTexture = gl.glGetInteger(GL_ACTIVE_TEXTURE);
            for (TextureBinding binding : this.textureBindings) {
                gl.glActiveTexture(GL_TEXTURE0 + binding.unit);
                binding.texture = gl.glGetInteger(binding.target.binding());
                if (this.samplerObjects) { //back up the sampler object bound to the current texture unit
                    binding.sampler = gl.glGetInteger(GL_SAMPLER_BINDING);
                }
            }
        } finally {
            PUnsafe.freeMemory(tmpBuffer);
        }

        this.restoreDefaultValues();

        return this::restore;
    }

    private void restoreDefaultValues() {
        OpenGL gl = this.gl;

        //fixed-function blend state
        gl.glDisable(GL_BLEND);
        gl.glBlendFuncSeparate(GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
        gl.glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        gl.glBlendColor(0.0f, 0.0f, 0.0f, 0.0f);

        //fixed-function color state
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glColorMask(true, true, true, true);

        //fixed-function culling state
        gl.glDisable(GL_CULL_FACE);

        //fixed-function depth state
        gl.glDisable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LESS);
        gl.glClearDepth(0.0d);

        //fixed-function stencil state
        gl.glDisable(GL_STENCIL_TEST);
        gl.glStencilMask(0);
        gl.glStencilFunc(GL_ALWAYS, 0, -1);
        gl.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        gl.glClearStencil(0);

        //simple bindings
        gl.glUseProgram(0);
        gl.glBindVertexArray(0);

        //ordinary buffer bindings
        //no-op, we currently don't care about this

        //indexed buffer bindings
        //no-op, we currently don't care about this
        /*for (IndexedBufferBinding binding : this.indexedBufferBindings) {
            gl.glBindBufferBase(binding.target.id(), binding.index, 0);
        }*/

        //texture bindings
        //no-op, we currently don't care about this
        /*for (TextureBinding binding : this.textureBindings) {
            gl.glActiveTexture(GL_TEXTURE0 + binding.unit);
            gl.glBindTexture(binding.target.target(), 0);
            if (this.samplerObjects) {
                gl.glBindSampler(binding.unit, 0);
            }
        }*/
        gl.glActiveTexture(GL_TEXTURE0);
    }

    private void restore() {
        OpenGL gl = this.gl;

        if (this.compatibility) {
            //glPopClientAttrib() could modify the currently bound vertex array's state, so we'll switch to VAO #0 first
            gl.glBindVertexArray(0);

            gl.glPopClientAttrib();
            gl.glPopAttrib();
        } else {
            //fixed-function blend state
            glEnableOrDisable(gl, GL_BLEND, this.blendEnabled);
            gl.glBlendFuncSeparate(this.blendFuncSrcRGB, this.blendFuncDstRGB, this.blendFuncSrcA, this.blendFuncDstA);
            gl.glBlendEquationSeparate(this.blendEquationRGB, this.blendEquationA);
            gl.glBlendColor(this.blendColorR, this.blendColorG, this.blendColorB, this.blendColorA);

            //fixed-function color state
            gl.glClearColor(this.clearColorR, this.clearColorG, this.clearColorB, this.clearColorA);
            gl.glColorMask(this.colorMaskR, this.colorMaskG, this.colorMaskB, this.colorMaskA);

            //fixed-function culling state
            glEnableOrDisable(gl, GL_CULL_FACE, this.cullEnabled);

            //fixed-function depth state
            glEnableOrDisable(gl, GL_DEPTH_TEST, this.depthEnabled);
            gl.glDepthMask(this.depthMask);
            gl.glDepthFunc(this.depthFunc);
            gl.glClearDepth(this.depthClearValue);

            //fixed-function stencil state
            glEnableOrDisable(gl, GL_STENCIL_TEST, this.stencilEnabled);
            gl.glStencilMask(this.stencilMask);
            gl.glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilValueMask);
            gl.glStencilOp(this.stencilFail, this.stencilPassDepthFail, this.stencilPassDepthPass);
            gl.glClearStencil(this.stencilClearValue);
        }

        //simple bindings
        gl.glUseProgram(this.boundProgram);
        gl.glBindVertexArray(this.boundVAO);

        //ordinary buffer bindings
        BufferTarget[] bufferTargets = this.bufferTargets;
        int[] bufferBindings = this.bufferBindings;
        for (int i = 0; i < bufferTargets.length; i++) {
            gl.glBindBuffer(bufferTargets[i].id(), bufferBindings[i]);
        }

        //indexed binding targets
        for (IndexedBufferBinding binding : this.indexedBufferBindings) {
            gl.glBindBufferBase(binding.target.id(), binding.index, binding.buffer);
        }

        //texture bindings
        for (TextureBinding binding : this.textureBindings) {
            gl.glActiveTexture(GL_TEXTURE0 + binding.unit);
            gl.glBindTexture(binding.target.target(), binding.texture);
            if (this.samplerObjects) {
                gl.glBindSampler(binding.unit, binding.sampler);
            }
        }
        gl.glActiveTexture(this.activeTexture);
    }

    private static void glEnableOrDisable(OpenGL gl, int cap, boolean state) {
        if (state) {
            gl.glEnable(cap);
        } else {
            gl.glDisable(cap);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    static final class IndexedBufferBinding {
        @EqualsAndHashCode.Include
        final IndexedBufferTarget target;
        @EqualsAndHashCode.Include
        final int index;

        int buffer;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    static final class TextureBinding {
        @EqualsAndHashCode.Include
        final TextureTarget target;
        @EqualsAndHashCode.Include
        final int unit;

        int texture;
        int sampler;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Builder {
        final OpenGL gl;

        final EnumSet<BufferTarget> bufferTargets = EnumSet.noneOf(BufferTarget.class);

        final Set<IndexedBufferBinding> indexedBuffers = new HashSet<>();
        final Set<TextureBinding> textures = new HashSet<>();

        public Builder buffer(BufferTarget target) {
            checkArg(target.requiredExtension() == null || this.gl.supports(target.requiredExtension()), "context (%s) doesn't support %s (%s)", this.gl, target, target.requiredExtension());
            this.bufferTargets.add(target);
            return this;
        }

        public Builder indexedBuffer(IndexedBufferTarget target, @NotNegative int index) {
            checkArg(target.requiredExtension() == null || this.gl.supports(target.requiredExtension()), "context (%s) doesn't support %s (%s)", this.gl, target, target.requiredExtension());
            this.indexedBuffers.add(new IndexedBufferBinding(target, checkIndex(this.gl.glGetInteger(target.maxBindings()), index)));
            return this;
        }

        public Builder texture(TextureTarget target, @NotNegative int unit) {
            this.textures.add(new TextureBinding(target, checkIndex(this.gl.limits().maxTextureUnits(), unit)));
            return this;
        }

        public StatePreserver build() {
            return new StatePreserver(this);
        }
    }
}
