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
import net.daporkchop.fp2.gl.shader.introspection.GLSLType;
import net.daporkchop.fp2.gl.shader.introspection.Uniform;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of a linked OpenGL shader program.
 *
 * @author DaPorkchop_
 */
public abstract class ShaderProgram extends GLObject.Normal {
    protected ShaderProgram(@NonNull Builder<?, ?> builder) throws ShaderLinkageException {
        super(builder.gl, builder.gl.glCreateProgram());

        try {
            //attach shaders and link, then detach shaders again
            for (Shader shader : builder.shaders) {
                this.gl.glAttachShader(this.id, shader.id());
            }
            builder.configurePreLink(uncheckedCast(this));
            this.gl.glLinkProgram(this.id);
            for (Shader shader : builder.shaders) {
                this.gl.glDetachShader(this.id, shader.id());
            }

            //check for errors
            if (this.gl.glGetProgrami(this.id, GL_LINK_STATUS) == GL_FALSE) {
                throw new ShaderLinkageException(this.gl.glGetProgramInfoLog(this.id));
            }

            builder.configurePostLink(uncheckedCast(this));
        } catch (Throwable t) { //clean up if something goes wrong
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    protected final void delete() {
        this.gl.glDeleteProgram(this.id);
    }

    @Override
    public void setDebugLabel(@NonNull CharSequence label) {
        this.gl.glObjectLabel(GL_PROGRAM, this.id, label);
    }

    @Override
    public String getDebugLabel() {
        return this.gl.glGetObjectLabel(GL_PROGRAM, this.id);
    }

    /**
     * Executes the given action with this program bound as the active program.
     *
     * @param action the action to run while the shader is bound
     */
    public final void bind(@NonNull Runnable action) {
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
    public final void bind(@NonNull Consumer<UniformSetter> action) {
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
    public final int uniformLocation(@NonNull String name) {
        this.checkOpen();
        return this.gl.glGetUniformLocation(this.id, name);
    }

    /**
     * Set uniform values in this shader.
     *
     * @param action a function which will be called with a {@link UniformSetter} which may be used to set shader uniforms
     */
    public final void setUniforms(@NonNull Consumer<UniformSetter> action) {
        this.checkOpen();
        if (this.gl.supports(GLExtension.GL_ARB_separate_shader_objects)) {
            action.accept(new DSAUniformSetter(this.gl, this.id));
        } else {
            this.bind(action);
        }
    }

    /**
     * @return all active uniforms in this shader program, excluding those which are part of an interface block
     */
    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    protected final Uniform[] getActiveUniforms() throws UnsupportedOperationException {
        this.checkOpen();

        if (!this.gl.supports(GLExtension.GL_ARB_program_interface_query)) {
            return this.getActiveUniformsLegacy();
        }

        int activeResources = this.gl.glGetProgramInterfacei(this.id, GL_UNIFORM, GL_ACTIVE_RESOURCES);
        int bufSize = this.gl.glGetProgramInterfacei(this.id, GL_UNIFORM, GL_MAX_NAME_LENGTH);

        Uniform[] result = new Uniform[activeResources];
        int resultSize = 0;

        for (int resourceIndex = 0; resourceIndex < activeResources; resourceIndex++) {
            int[] buf = new int[3];
            this.gl.glGetProgramResourceiv(this.id, GL_UNIFORM, resourceIndex, new int[]{ GL_BLOCK_INDEX, GL_TYPE, GL_ARRAY_SIZE }, null, buf);
            if (buf[0] >= 0) {
                //this uniform is part of a uniform block, skip it
                continue;
            }

            String name = this.gl.glGetProgramResourceName(this.id, GL_UNIFORM, resourceIndex, bufSize);
            result[resultSize++] = new Uniform(name, GLSLType.get(buf[1]), buf[2]);
        }
        result = Arrays.copyOf(result, resultSize);
        assert Arrays.equals(result, this.getActiveUniformsLegacy()) //sanity check because i'm not entirely sure how reliable the old API is
                : "Program introspection: " + Arrays.toString(result) + ", Legacy: " + Arrays.toString(this.getActiveUniformsLegacy());
        return result;
    }

    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    private Uniform[] getActiveUniformsLegacy() throws UnsupportedOperationException {
        this.checkOpen();

        int activeUniforms = this.gl.glGetProgrami(this.id, GL_ACTIVE_UNIFORMS);
        int bufSize = this.gl.glGetProgrami(this.id, GL_ACTIVE_UNIFORM_MAX_LENGTH);

        Uniform[] result = new Uniform[activeUniforms];
        int resultSize = 0;

        for (int uniformIndex = 0; uniformIndex < activeUniforms; uniformIndex++) {
            if (this.gl.glGetActiveUniformsi(this.id, uniformIndex, GL_UNIFORM_BLOCK_INDEX) >= 0) {
                //this uniform is part of a uniform block, skip it
                continue;
            } else if (this.gl.supports(GLExtension.GL_ARB_shader_atomic_counters) && this.gl.glGetActiveUniformsi(this.id, uniformIndex, GL_UNIFORM_ATOMIC_COUNTER_BUFFER_INDEX) >= 0) {
                //this uniform is actually an atomic counter, skip it
                continue;
            }

            String name = this.gl.glGetActiveUniformName(this.id, uniformIndex, bufSize);
            GLSLType type = GLSLType.get(this.gl.glGetActiveUniformsi(this.id, uniformIndex, GL_UNIFORM_TYPE));
            int size = this.gl.glGetActiveUniformsi(this.id, uniformIndex, GL_UNIFORM_SIZE);
            result[resultSize++] = new Uniform(name, type, size);
        }
        return Arrays.copyOf(result, resultSize);
    }

    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    protected final String[] getUniformBlockNames() throws UnsupportedOperationException {
        this.checkOpen();

        if (!this.gl.supports(GLExtension.GL_ARB_program_interface_query)) {
            return this.getUniformBlockNamesLegacy();
        }

        String[] result = this.getActiveResourceNames(GL_UNIFORM_BLOCK);
        assert Arrays.equals(result, this.getUniformBlockNamesLegacy()) //sanity check because i'm not entirely sure how reliable the old API is
                : "Program introspection: " + Arrays.toString(result) + ", Legacy: " + Arrays.toString(this.getUniformBlockNamesLegacy());
        return result;
    }

    @GLRequires(GLExtension.GL_ARB_uniform_buffer_object)
    private String[] getUniformBlockNamesLegacy() throws UnsupportedOperationException {
        this.checkOpen();

        int activeUniformBlocks = this.gl.glGetProgrami(this.id, GL_ACTIVE_UNIFORM_BLOCKS);
        String[] result = new String[activeUniformBlocks];
        for (int uniformBlockIndex = 0; uniformBlockIndex < activeUniformBlocks; uniformBlockIndex++) {
            int bufSize = this.gl.glGetActiveUniformBlocki(this.id, uniformBlockIndex, GL_UNIFORM_BLOCK_NAME_LENGTH);
            result[uniformBlockIndex] = this.gl.glGetActiveUniformBlockName(this.id, uniformBlockIndex, bufSize);
        }
        return result;
    }

    @GLRequires(GLExtension.GL_ARB_shader_storage_buffer_object)
    protected final String[] getShaderStorageBlockNames() throws UnsupportedOperationException {
        return this.getActiveResourceNames(GL_SHADER_STORAGE_BLOCK);
    }

    @GLRequires(GLExtension.GL_ARB_program_interface_query)
    protected final String[] getActiveResourceNames(int programInterface) throws UnsupportedOperationException {
        this.checkOpen();

        int activeResources = this.gl.glGetProgramInterfacei(this.id, programInterface, GL_ACTIVE_RESOURCES);
        int bufSize = this.gl.glGetProgramInterfacei(this.id, programInterface, GL_MAX_NAME_LENGTH);
        String[] result = new String[activeResources];
        for (int resourceIndex = 0; resourceIndex < activeResources; resourceIndex++) {
            result[resourceIndex] = this.gl.glGetProgramResourceName(this.id, programInterface, resourceIndex, bufSize);
        }
        return result;
    }

    /**
     * A handle for setting uniform values for this shader.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static abstract class UniformSetter {
        protected final OpenGL gl;

        //@formatter:off
        public abstract void set1i(int location, int v0);
        public abstract void set2i(int location, int v0, int v1);
        public abstract void set3i(int location, int v0, int v1, int v2);
        public abstract void set4i(int location, int v0, int v1, int v2, int v3);
        public abstract void set1ui(int location, int v0);
        public abstract void set2ui(int location, int v0, int v1);
        public abstract void set3ui(int location, int v0, int v1, int v2);
        public abstract void set4ui(int location, int v0, int v1, int v2, int v3);
        public abstract void set1f(int location, float v0);
        public abstract void set2f(int location, float v0, float v1);
        public abstract void set3f(int location, float v0, float v1, float v2);
        public abstract void set4f(int location, float v0, float v1, float v2, float v3);
        public abstract void set1i(int location, IntBuffer value);
        public abstract void set2i(int location, IntBuffer value);
        public abstract void set3i(int location, IntBuffer value);
        public abstract void set4i(int location, IntBuffer value);
        public abstract void set1ui(int location, IntBuffer value);
        public abstract void set2ui(int location, IntBuffer value);
        public abstract void set3ui(int location, IntBuffer value);
        public abstract void set4ui(int location, IntBuffer value);
        public abstract void set1f(int location, FloatBuffer value);
        public abstract void set2f(int location, FloatBuffer value);
        public abstract void set3f(int location, FloatBuffer value);
        public abstract void set4f(int location, FloatBuffer value);
        //@formatter:on
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
        public void set1i(int location, int v0) {
            this.gl.glUniform1i(location, v0);
        }

        @Override
        public void set2i(int location, int v0, int v1) {
            this.gl.glUniform2i(location, v0, v1);
        }

        @Override
        public void set3i(int location, int v0, int v1, int v2) {
            this.gl.glUniform3i(location, v0, v1, v2);
        }

        @Override
        public void set4i(int location, int v0, int v1, int v2, int v3) {
            this.gl.glUniform4i(location, v0, v1, v2, v3);
        }

        @Override
        public void set1ui(int location, int v0) {
            this.gl.glUniform1ui(location, v0);
        }

        @Override
        public void set2ui(int location, int v0, int v1) {
            this.gl.glUniform2ui(location, v0, v1);
        }

        @Override
        public void set3ui(int location, int v0, int v1, int v2) {
            this.gl.glUniform3ui(location, v0, v1, v2);
        }

        @Override
        public void set4ui(int location, int v0, int v1, int v2, int v3) {
            this.gl.glUniform4ui(location, v0, v1, v2, v3);
        }

        @Override
        public void set1f(int location, float v0) {
            this.gl.glUniform1f(location, v0);
        }

        @Override
        public void set2f(int location, float v0, float v1) {
            this.gl.glUniform2f(location, v0, v1);
        }

        @Override
        public void set3f(int location, float v0, float v1, float v2) {
            this.gl.glUniform3f(location, v0, v1, v2);
        }

        @Override
        public void set4f(int location, float v0, float v1, float v2, float v3) {
            this.gl.glUniform4f(location, v0, v1, v2, v3);
        }

        @Override
        public void set1i(int location, IntBuffer value) {
            this.gl.glUniform1i(location, value);
        }

        @Override
        public void set2i(int location, IntBuffer value) {
            this.gl.glUniform2i(location, value);
        }

        @Override
        public void set3i(int location, IntBuffer value) {
            this.gl.glUniform3i(location, value);
        }

        @Override
        public void set4i(int location, IntBuffer value) {
            this.gl.glUniform4i(location, value);
        }

        @Override
        public void set1ui(int location, IntBuffer value) {
            this.gl.glUniform1ui(location, value);
        }

        @Override
        public void set2ui(int location, IntBuffer value) {
            this.gl.glUniform2ui(location, value);
        }

        @Override
        public void set3ui(int location, IntBuffer value) {
            this.gl.glUniform3ui(location, value);
        }

        @Override
        public void set4ui(int location, IntBuffer value) {
            this.gl.glUniform4ui(location, value);
        }

        @Override
        public void set1f(int location, FloatBuffer value) {
            this.gl.glUniform1f(location, value);
        }

        @Override
        public void set2f(int location, FloatBuffer value) {
            this.gl.glUniform2f(location, value);
        }

        @Override
        public void set3f(int location, FloatBuffer value) {
            this.gl.glUniform3f(location, value);
        }

        @Override
        public void set4f(int location, FloatBuffer value) {
            this.gl.glUniform4f(location, value);
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
        public void set1i(int location, int v0) {
            this.gl.glProgramUniform1i(this.id, location, v0);
        }

        @Override
        public void set2i(int location, int v0, int v1) {
            this.gl.glProgramUniform2i(this.id, location, v0, v1);
        }

        @Override
        public void set3i(int location, int v0, int v1, int v2) {
            this.gl.glProgramUniform3i(this.id, location, v0, v1, v2);
        }

        @Override
        public void set4i(int location, int v0, int v1, int v2, int v3) {
            this.gl.glProgramUniform4i(this.id, location, v0, v1, v2, v3);
        }

        @Override
        public void set1ui(int location, int v0) {
            this.gl.glProgramUniform1ui(this.id, location, v0);
        }

        @Override
        public void set2ui(int location, int v0, int v1) {
            this.gl.glProgramUniform2ui(this.id, location, v0, v1);
        }

        @Override
        public void set3ui(int location, int v0, int v1, int v2) {
            this.gl.glProgramUniform3ui(this.id, location, v0, v1, v2);
        }

        @Override
        public void set4ui(int location, int v0, int v1, int v2, int v3) {
            this.gl.glProgramUniform4ui(this.id, location, v0, v1, v2, v3);
        }

        @Override
        public void set1f(int location, float v0) {
            this.gl.glProgramUniform1f(this.id, location, v0);
        }

        @Override
        public void set2f(int location, float v0, float v1) {
            this.gl.glProgramUniform2f(this.id, location, v0, v1);
        }

        @Override
        public void set3f(int location, float v0, float v1, float v2) {
            this.gl.glProgramUniform3f(this.id, location, v0, v1, v2);
        }

        @Override
        public void set4f(int location, float v0, float v1, float v2, float v3) {
            this.gl.glProgramUniform4f(this.id, location, v0, v1, v2, v3);
        }

        @Override
        public void set1i(int location, IntBuffer value) {
            this.gl.glProgramUniform1i(this.id, location, value);
        }

        @Override
        public void set2i(int location, IntBuffer value) {
            this.gl.glProgramUniform2i(this.id, location, value);
        }

        @Override
        public void set3i(int location, IntBuffer value) {
            this.gl.glProgramUniform3i(this.id, location, value);
        }

        @Override
        public void set4i(int location, IntBuffer value) {
            this.gl.glProgramUniform4i(this.id, location, value);
        }

        @Override
        public void set1ui(int location, IntBuffer value) {
            this.gl.glProgramUniform1ui(this.id, location, value);
        }

        @Override
        public void set2ui(int location, IntBuffer value) {
            this.gl.glProgramUniform2ui(this.id, location, value);
        }

        @Override
        public void set3ui(int location, IntBuffer value) {
            this.gl.glProgramUniform3ui(this.id, location, value);
        }

        @Override
        public void set4ui(int location, IntBuffer value) {
            this.gl.glProgramUniform4ui(this.id, location, value);
        }

        @Override
        public void set1f(int location, FloatBuffer value) {
            this.gl.glProgramUniform1f(this.id, location, value);
        }

        @Override
        public void set2f(int location, FloatBuffer value) {
            this.gl.glProgramUniform2f(this.id, location, value);
        }

        @Override
        public void set3f(int location, FloatBuffer value) {
            this.gl.glProgramUniform3f(this.id, location, value);
        }

        @Override
        public void set4f(int location, FloatBuffer value) {
            this.gl.glProgramUniform4f(this.id, location, value);
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

        protected void configurePreLink(S program) {
            //no-op
        }

        protected void configurePostLink(S program) {
            this.samplers.configurePostLink(program);
            this.SSBOs.configurePostLink(this.gl, program);
            this.UBOs.configurePostLink(this.gl, program);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    protected static final class SamplerBindings {
        private final Map<String, Integer> bindings = new TreeMap<>();

        public void add(int maxTexUnits, int unit, @NonNull String name) {
            checkIndex(maxTexUnits, unit);
            checkArg(!this.bindings.containsKey(name), "sampler named '%s' is already configured", name);
            this.bindings.put(name, unit);
        }

        public void configurePostLink(ShaderProgram program) {
            List<Uniform> unboundSamplerUniforms = Arrays.stream(program.getActiveUniforms())
                    .filter(uniform -> uniform.type().isSampler() && !this.bindings.containsKey(uniform.name()))
                    .collect(Collectors.toList());
            if (!unboundSamplerUniforms.isEmpty()) {
                throw new ShaderLinkageException("Program contains unbound samplers: " + unboundSamplerUniforms);
            }

            if (this.bindings.isEmpty()) {
                return;
            }

            program.bind(uniformSetter ->
                    this.bindings.forEach((name, unit) -> {
                        int location = program.uniformLocation(name);
                        if (location >= 0) { //sampler may have been optimized out, so only set it if it's present
                            uniformSetter.set1i(location, unit);
                        }
                    }));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    protected static abstract class BlockBindings {
        public static BlockBindings createSSBO() {
            return new BlockBindings(GLExtension.GL_ARB_shader_storage_buffer_object) {
                @Override
                protected String[] getBlockNames(@NonNull ShaderProgram program) {
                    return program.getShaderStorageBlockNames();
                }

                @Override
                protected void setBlockBinding(@NonNull OpenGL gl, int program, int blockIndex, int bindingIndex) {
                    gl.glShaderStorageBlockBinding(program, blockIndex, bindingIndex);
                }
            };
        }

        public static BlockBindings createUBO() {
            return new BlockBindings(GLExtension.GL_ARB_uniform_buffer_object) {
                @Override
                protected String[] getBlockNames(@NonNull ShaderProgram program) {
                    return program.getUniformBlockNames();
                }

                @Override
                protected void setBlockBinding(@NonNull OpenGL gl, int program, int blockIndex, int bindingIndex) {
                    gl.glUniformBlockBinding(program, blockIndex, bindingIndex);
                }
            };
        }

        private final GLExtension requiredExtension;
        private final Map<String, Integer> bindings = new TreeMap<>();

        public final void add(@NotNegative int maxBindings, @NotNegative int bindingIndex, @NonNull String name) {
            checkIndex(maxBindings, bindingIndex);
            checkArg(!this.bindings.containsValue(bindingIndex), "binding index %s is already configured", bindingIndex);
            checkArg(!this.bindings.containsKey(name), "binding block named '%s' is already configured", name);
            this.bindings.put(name, bindingIndex);
        }

        public final void configurePostLink(@NonNull OpenGL gl, @NonNull ShaderProgram program) {
            if (!gl.supports(this.requiredExtension)) {
                //if the extension isn't supported, no bindings should be configured and the shader obviously can't contain any unbound blocks
                checkState(this.bindings.isEmpty(), "extension %s isn't supported, but a binding was configured?!?", this.requiredExtension);
                return;
            }

            //iterate over all the the interface blocks of this sort in the program
            String[] blockNames = this.getBlockNames(program);
            List<String> unboundBlockNames = null;
            for (int blockIndex = 0; blockIndex < blockNames.length; blockIndex++) {
                String blockName = blockNames[blockIndex];
                Integer bindingIndex = this.bindings.get(blockName);
                if (bindingIndex != null) {
                    //the interface block's binding index is defined, use it!
                    this.setBlockBinding(gl, program.id(), blockIndex, bindingIndex);
                } else {
                    //the interface block isn't bound! remember it for later and keep going
                    if (unboundBlockNames == null) {
                        unboundBlockNames = new ArrayList<>();
                    }
                    unboundBlockNames.add(blockName);
                }
            }

            if (unboundBlockNames != null) {
                //at least one interface block isn't bound
                throw new ShaderLinkageException("Program contains unbound " + this.requiredExtension + ": " + unboundBlockNames);
            }

            //Note that the builder may have defined additional bindings for interface blocks not present in the shader. Unfortunately, throwing
            //  an exception in this case doesn't make much sense, since the "non-existent" interface block may actually have been defined in the
            //  GLSL source code and then subsequently optimized away by the driver. Since there isn't any way for the program introspection API
            //  to differentiate between something which has been optimized away and something which was never there in the first place, throwing
            //  or logging an error would result in a lot of false positives.
        }

        protected abstract String[] getBlockNames(@NonNull ShaderProgram program);

        protected abstract void setBlockBinding(@NonNull OpenGL gl, int programId, int blockIndex, int bindingIndex);
    }
}
