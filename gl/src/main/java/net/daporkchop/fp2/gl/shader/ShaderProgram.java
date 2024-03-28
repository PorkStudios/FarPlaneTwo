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
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.function.plain.QuadConsumer;
import net.daporkchop.lib.common.function.plain.TriFunction;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of a linked OpenGL shader program.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class ShaderProgram implements AutoCloseable {
    protected final OpenGL gl;
    protected final int id;

    protected ShaderProgram(Builder<?, ?> builder) throws ShaderLinkageException {
        this.gl = builder.gl;
        this.id = this.gl.glCreateProgram();

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
    public void close() {
        this.gl.glDeleteProgram(this.id);
    }

    /**
     * Executes the given action with this program bound as the active program.
     *
     * @param action the action to run
     */
    public final void bind(Runnable action) {
        int old = this.gl.glGetInteger(GL_CURRENT_PROGRAM);
        try {
            this.gl.glUseProgram(this.id);
            action.run();
        } finally {
            this.gl.glUseProgram(old);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static abstract class Builder<S extends ShaderProgram, B extends Builder<S, B>> {
        protected final OpenGL gl;

        protected final List<Shader> shaders = new ArrayList<>();

        protected final SamplerBindings samplers = new SamplerBindings();
        protected final BlockBindings SSBOs = BlockBindings.createSSBO();
        protected final BlockBindings UBOs = BlockBindings.createUBO();

        public B addSampler(@NotNegative int unit, @NonNull String name) {
            this.samplers.add(this.gl.limits().maxTextureUnits(), unit, name);
            return uncheckedCast(this);
        }

        public B addSSBO(@NotNegative int bindingIndex, @NonNull String name) {
            this.SSBOs.add(this.gl.limits().maxShaderStorageBuffers(), bindingIndex, name);
            return uncheckedCast(this);
        }

        public B addUBO(@NotNegative int bindingIndex, @NonNull String name) {
            this.UBOs.add(this.gl.limits().maxUniformBuffers(), bindingIndex, name);
            return uncheckedCast(this);
        }

        public abstract S build() throws ShaderLinkageException;

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
                checkArg(blockIndex != GL_INVALID_INDEX, "unable to find shader storage block: %s", name);

                this.configureBlockBinding.accept(gl, program, blockIndex, bindingIndex);
            });
        }
    }
}
