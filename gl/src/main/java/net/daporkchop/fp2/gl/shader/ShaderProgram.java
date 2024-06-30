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

package net.daporkchop.fp2.gl.shader;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.function.plain.QuadConsumer;
import net.daporkchop.lib.common.function.plain.TriFunction;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of a linked OpenGL shader program.
 *
 * @author DaPorkchop_
 */
public abstract class ShaderProgram extends GLObject.Normal {
    protected ShaderProgram(Builder<?, ?> builder) throws ShaderLinkageException {
        super(builder.gl, builder.gl.glCreateProgram());

        try {
            //attach shaders and link, then detach shaders again
            for (Shader shader : builder.shaders) {
                this.gl.glAttachShader(this.id, shader.id());
            }
            builder.configurePreLink(this.id);
            this.gl.glLinkProgram(this.id);
            for (Shader shader : builder.shaders) {
                this.gl.glDetachShader(this.id, shader.id());
            }

            //check for errors
            if (this.gl.glGetProgrami(this.id, GL_LINK_STATUS) == GL_FALSE) {
                throw new ShaderLinkageException(this.gl.glGetProgramInfoLog(this.id));
            }

            builder.configurePostLink(this.id);
        } catch (Throwable t) { //clean up if something goes wrong
            this.gl.glDeleteProgram(this.id);
            throw PUnsafe.throwException(t);
        }
    }

    @Override
    protected final void delete() {
        this.gl.glDeleteProgram(this.id);
    }

    /**
     * Executes the given action with this program bound as the active program.
     *
     * @param action the action to run while the shader is bound
     */
    public final void bind(Runnable action) {
        this.checkOpen();
        int old = this.gl.glGetInteger(GL_CURRENT_PROGRAM);
        try {
            this.gl.glUseProgram(this.id);
            action.run();
        } finally {
            this.gl.glUseProgram(old);
        }
    }

    /**
     * Executes the given action with this program bound as the active program.
     *
     * @param action the action to run while the shader is bound, will be called with a {@link UniformSetter} which may be used to set shader uniforms
     */
    public final void bind(Consumer<UniformSetter> action) {
        this.checkOpen();
        int old = this.gl.glGetInteger(GL_CURRENT_PROGRAM);
        try {
            this.gl.glUseProgram(this.id);
            action.accept(new BoundUniformSetter(this.gl));
        } finally {
            this.gl.glUseProgram(old);
        }
    }

    /**
     * Immediately binds this shader program to the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound program when rendering is done. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this shader bound will not cause future issues.
     *
     * @return a {@link UniformSetter} which may be used to set shader uniforms, and will remain valid until another shader is bound
     */
    public final UniformSetter bindUnsafe() {
        this.checkOpen();
        this.gl.glUseProgram(this.id);
        return new BoundUniformSetter(this.gl);
    }

    /**
     * Gets the location of the uniform with the given name.
     *
     * @param name the uniform name
     * @return the uniform's location
     */
    public final int uniformLocation(String name) {
        this.checkOpen();
        return this.gl.glGetUniformLocation(this.id, name);
    }

    /**
     * Set uniform values in this shader.
     *
     * @param action a function which will be called with a {@link UniformSetter} which may be used to set shader uniforms
     */
    public final void setUniforms(Consumer<UniformSetter> action) {
        this.checkOpen();
        if (this.gl.supports(GLExtension.GL_ARB_separate_shader_objects)) {
            action.accept(new DSAUniformSetter(this.gl, this.id));
        } else {
            this.bind(action);
        }
    }

    /**
     * A handle for setting uniform values for this shader.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static abstract class UniformSetter {
        protected final OpenGL gl;

        public abstract void set(int location, int v0);

        public abstract void set(int location, int v0, int v1);

        public abstract void set(int location, int v0, int v1, int v2);

        public abstract void set(int location, int v0, int v1, int v2, int v3);

        public abstract void set(int location, float v0);

        public abstract void set(int location, float v0, float v1);

        public abstract void set(int location, float v0, float v1, float v2);

        public abstract void set(int location, float v0, float v1, float v2, float v3);

        public abstract void set1(int location, IntBuffer value);

        public abstract void set2(int location, IntBuffer value);

        public abstract void set3(int location, IntBuffer value);

        public abstract void set4(int location, IntBuffer value);

        public abstract void set1(int location, FloatBuffer value);

        public abstract void set2(int location, FloatBuffer value);

        public abstract void set3(int location, FloatBuffer value);

        public abstract void set4(int location, FloatBuffer value);
    }

    /**
     * Sets shader uniforms using the legacy {@code glUniform*} functions which operate on the active shader program.
     *
     * @author DaPorkchop_
     */
    static final class BoundUniformSetter extends UniformSetter {
        BoundUniformSetter(OpenGL gl) {
            super(gl);
        }

        @Override
        public void set(int location, int v0) {
            this.gl.glUniform(location, v0);
        }

        @Override
        public void set(int location, int v0, int v1) {
            this.gl.glUniform(location, v0, v1);
        }

        @Override
        public void set(int location, int v0, int v1, int v2) {
            this.gl.glUniform(location, v0, v1, v2);
        }

        @Override
        public void set(int location, int v0, int v1, int v2, int v3) {
            this.gl.glUniform(location, v0, v1, v2, v3);
        }

        @Override
        public void set(int location, float v0) {
            this.gl.glUniform(location, v0);
        }

        @Override
        public void set(int location, float v0, float v1) {
            this.gl.glUniform(location, v0, v1);
        }

        @Override
        public void set(int location, float v0, float v1, float v2) {
            this.gl.glUniform(location, v0, v1, v2);
        }

        @Override
        public void set(int location, float v0, float v1, float v2, float v3) {
            this.gl.glUniform(location, v0, v1, v2, v3);
        }

        @Override
        public void set1(int location, IntBuffer value) {
            this.gl.glUniform1(location, value);
        }

        @Override
        public void set2(int location, IntBuffer value) {
            this.gl.glUniform2(location, value);
        }

        @Override
        public void set3(int location, IntBuffer value) {
            this.gl.glUniform3(location, value);
        }

        @Override
        public void set4(int location, IntBuffer value) {
            this.gl.glUniform4(location, value);
        }

        @Override
        public void set1(int location, FloatBuffer value) {
            this.gl.glUniform1(location, value);
        }

        @Override
        public void set2(int location, FloatBuffer value) {
            this.gl.glUniform2(location, value);
        }

        @Override
        public void set3(int location, FloatBuffer value) {
            this.gl.glUniform3(location, value);
        }

        @Override
        public void set4(int location, FloatBuffer value) {
            this.gl.glUniform4(location, value);
        }
    }

    /**
     * Sets shader uniforms using the modern bindless {@code glProgramUniform*} functions.
     *
     * @author DaPorkchop_
     */
    static final class DSAUniformSetter extends UniformSetter {
        private final int id;

        DSAUniformSetter(OpenGL gl, int id) {
            super(gl);
            this.id = id;
        }

        @Override
        public void set(int location, int v0) {
            this.gl.glProgramUniform(this.id, location, v0);
        }

        @Override
        public void set(int location, int v0, int v1) {
            this.gl.glProgramUniform(this.id, location, v0, v1);
        }

        @Override
        public void set(int location, int v0, int v1, int v2) {
            this.gl.glProgramUniform(this.id, location, v0, v1, v2);
        }

        @Override
        public void set(int location, int v0, int v1, int v2, int v3) {
            this.gl.glProgramUniform(this.id, location, v0, v1, v2, v3);
        }

        @Override
        public void set(int location, float v0) {
            this.gl.glProgramUniform(this.id, location, v0);
        }

        @Override
        public void set(int location, float v0, float v1) {
            this.gl.glProgramUniform(this.id, location, v0, v1);
        }

        @Override
        public void set(int location, float v0, float v1, float v2) {
            this.gl.glProgramUniform(this.id, location, v0, v1, v2);
        }

        @Override
        public void set(int location, float v0, float v1, float v2, float v3) {
            this.gl.glProgramUniform(this.id, location, v0, v1, v2, v3);
        }

        @Override
        public void set1(int location, IntBuffer value) {
            this.gl.glProgramUniform1(this.id, location, value);
        }

        @Override
        public void set2(int location, IntBuffer value) {
            this.gl.glProgramUniform2(this.id, location, value);
        }

        @Override
        public void set3(int location, IntBuffer value) {
            this.gl.glProgramUniform3(this.id, location, value);
        }

        @Override
        public void set4(int location, IntBuffer value) {
            this.gl.glProgramUniform4(this.id, location, value);
        }

        @Override
        public void set1(int location, FloatBuffer value) {
            this.gl.glProgramUniform1(this.id, location, value);
        }

        @Override
        public void set2(int location, FloatBuffer value) {
            this.gl.glProgramUniform2(this.id, location, value);
        }

        @Override
        public void set3(int location, FloatBuffer value) {
            this.gl.glProgramUniform3(this.id, location, value);
        }

        @Override
        public void set4(int location, FloatBuffer value) {
            this.gl.glProgramUniform4(this.id, location, value);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static abstract class Builder<S extends ShaderProgram, B extends Builder<S, B>> {
        protected final OpenGL gl;

        protected final EnumSet<ShaderType> allowedShaderTypes;
        protected final EnumSet<ShaderType> requiredShaderTypes;
        protected final EnumSet<ShaderType> addedShaderTypes = EnumSet.noneOf(ShaderType.class);
        protected final List<Shader> shaders = new ArrayList<>();

        protected final SamplerBindings samplers = new SamplerBindings();
        protected final BlockBindings SSBOs = BlockBindings.createSSBO();
        protected final BlockBindings UBOs = BlockBindings.createUBO();

        public final B addShader(@NonNull Shader shader) {
            checkState(this.allowedShaderTypes.contains(shader.type()), "shader type %s isn't allowed (must be one of: %s)", shader.type(), this.allowedShaderTypes);
            checkState(!this.addedShaderTypes.contains(shader.type()), "a %s shader was already added", shader.type());
            this.shaders.add(shader);
            this.addedShaderTypes.add(shader.type());
            return uncheckedCast(this);
        }

        public final B addSampler(@NotNegative int unit, @NonNull String name) {
            this.samplers.add(this.gl.limits().maxTextureUnits(), unit, name);
            return uncheckedCast(this);
        }

        public final B addSSBO(@NotNegative int bindingIndex, @NonNull String name) {
            this.SSBOs.add(this.gl.limits().maxShaderStorageBufferBindings(), bindingIndex, name);
            return uncheckedCast(this);
        }

        public final B addUBO(@NotNegative int bindingIndex, @NonNull String name) {
            this.UBOs.add(this.gl.limits().maxUniformBufferBindings(), bindingIndex, name);
            return uncheckedCast(this);
        }

        public final S build() throws ShaderLinkageException {
            if (!this.addedShaderTypes.containsAll(this.requiredShaderTypes)) {
                val missing = this.requiredShaderTypes.clone();
                missing.removeAll(this.addedShaderTypes);
                throw new IllegalStateException("missing shaders: " + missing);
            }

            return this.build0();
        }

        protected abstract S build0() throws ShaderLinkageException;

        protected void configurePreLink(int program) {
            //no-op
        }

        protected void configurePostLink(int program) {
            this.samplers.configurePostLink(this.gl, program);
            this.SSBOs.configurePostLink(this.gl, program);
            this.UBOs.configurePostLink(this.gl, program);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    protected static class SamplerBindings {
        protected final Map<String, Integer> bindings = new TreeMap<>();

        public void add(int maxTexUnits, int unit, @NonNull String name) {
            checkIndex(maxTexUnits, unit);
            checkArg(!this.bindings.containsKey(name), "sampler named '%s' is already configured", name);
            this.bindings.put(name, unit);
        }

        public void configurePostLink(OpenGL gl, int program) {
            if (this.bindings.isEmpty()) {
                return;
            }

            int oldProgram = gl.glGetInteger(GL_CURRENT_PROGRAM);
            try {
                gl.glUseProgram(program);

                this.bindings.forEach((name, unit) -> {
                    int location = gl.glGetUniformLocation(program, name);
                    if (location >= 0) { //sampler may have been optimized out, so only set it if it's present
                        gl.glUniform(location, unit);
                    }
                });
            } finally {
                gl.glUseProgram(oldProgram);
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    protected static class BlockBindings {
        public static BlockBindings createSSBO() {
            return new BlockBindings(
                    (gl, program, name) -> gl.glGetProgramResourceIndex(program, GL_SHADER_STORAGE_BLOCK, name),
                    OpenGL::glShaderStorageBlockBinding);
        }

        public static BlockBindings createUBO() {
            return new BlockBindings(OpenGL::glGetUniformBlockIndex, OpenGL::glUniformBlockBinding);
        }

        protected final TriFunction<OpenGL, Integer, String, Integer> getBlockIndex;
        protected final QuadConsumer<OpenGL, Integer, Integer, Integer> configureBlockBinding;

        protected final Map<String, Integer> bindings = new TreeMap<>();

        public void add(int maxBindings, int bindingIndex, @NonNull String name) {
            checkIndex(maxBindings, bindingIndex);
            checkArg(!this.bindings.containsValue(bindingIndex), "binding index %s is already configured", bindingIndex);
            checkArg(!this.bindings.containsKey(name), "binding block named '%s' is already configured", name);
            this.bindings.put(name, bindingIndex);
        }

        public void configurePostLink(OpenGL gl, int program) {
            if (this.bindings.isEmpty()) {
                return;
            }

            this.bindings.forEach((name, bindingIndex) -> {
                int blockIndex = this.getBlockIndex.apply(gl, program, name);
                if (blockIndex != GL_INVALID_INDEX) { //TODO: this check isn't useful, i'd prefer to do it the other way around (check if there are any unconfigured bindings)
                    //checkArg(blockIndex != GL_INVALID_INDEX, "unable to find shader storage block: %s", name);
                    this.configureBlockBinding.accept(gl, program, blockIndex, bindingIndex);
                }
            });
        }
    }
}
