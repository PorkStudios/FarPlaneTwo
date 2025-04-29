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

package net.daporkchop.fp2.core.client.shader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.Shader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.shader.source.IncludePreprocessor;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A wrapper around a {@link ShaderProgram} which allows shaders to be reloaded.
 * <p>
 * Reload functionality is implemented in {@link ReloadableShaderRegistry}.
 *
 * @author DaPorkchop_
 */
public final class ReloadableShaderProgram<P extends ShaderProgram> {
    final ShaderMacros macros;

    final Function<OpenGL, ShaderProgram.Builder<P, ?>> builderFactory; //this is some truly enterprise code
    final ImmutableList<DeferredShaderReference> shaders;
    final SetupFunction<?> setupFunction;

    P program;

    ReloadableShaderProgram(Builder<P, ?, ?> builder) {
        this.macros = builder.macros;

        this.builderFactory = builder.builderFactory;
        this.shaders = builder.shaders.build();
        this.setupFunction = builder.buildSetupFunction();

        //we won't compile the program immediately, as all the shaders will be compiled at once when the registry is reloaded
        //this.program = this.compile(builder.fp2.client().gl(), builder.fp2.client().resourceProvider());
    }

    List<Shader.CompileTask> compileAsync(OpenGL gl, ResourceProvider resourceProvider) {
        List<Shader.CompileTask> compileTasks = new ArrayList<>(this.shaders.size());
        try {
            //compile each of the referenced shaders
            for (val shader : this.shaders) {
                compileTasks.add(Shader.compileAsync(gl, shader.type,
                        new IncludePreprocessor(gl, resourceProvider)
                                .addVersionHeader()
                                .define(this.macros.defines())
                                .include(shader.identifier)));
            }

            return compileTasks;
        } catch (Throwable t) {
            throw PResourceUtil.closeAllSuppressed(t, compileTasks);
        }
    }

    ShaderProgram.LinkTask<P> linkAsync(OpenGL gl, List<Shader> shaders) {
        val builder = this.builderFactory.apply(gl);
        if (this.setupFunction != null) {
            this.setupFunction.setup(uncheckedCast(builder));
        }
        for (val shader : shaders) { //add all the compiled shaders to the program for linking
            builder.addShader(shader);
        }
        return builder.buildAsync();
    }

    P compileSync(OpenGL gl, ResourceProvider resourceProvider) throws ShaderCompilationException, ShaderLinkageException {
        List<Shader> compiledShaders = new ArrayList<>(this.shaders.size());
        try (val ignored = PResourceUtil.lazyCloseAll(compiledShaders)) {
            //compile each of the referenced shaders
            for (val shader : this.shaders) {
                compiledShaders.add(Shader.compile(gl, shader.type,
                        new IncludePreprocessor(gl, resourceProvider)
                                .addVersionHeader()
                                .define(this.macros.defines())
                                .include(shader.identifier)));
            }

            val builder = this.builderFactory.apply(gl);
            if (this.setupFunction != null) {
                this.setupFunction.setup(uncheckedCast(builder));
            }
            for (val shader : compiledShaders) { //add all the compiled shaders to the program for linking
                builder.addShader(shader);
            }
            return builder.build();
        }
    }

    /**
     * @return the actual {@link ShaderProgram} referred to by this program
     */
    public P get() {
        assert this.program != null : "ReloadableShaderProgram instance was accessed before being loaded?!?";
        return this.program;
    }

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface SetupFunction<B extends ShaderProgram.Builder<?, B>> {
        void setup(B builder);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    static final class DeferredShaderReference {
        final ShaderType type;
        final Identifier identifier;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static abstract class Builder<P extends ShaderProgram, PB extends ShaderProgram.Builder<P, PB>, B extends Builder<P, PB, B>> {
        final Object key;
        final ShaderMacros macros;
        final Function<OpenGL, ShaderProgram.Builder<P, ?>> builderFactory; //this is some truly enterprise code

        final ImmutableList.Builder<DeferredShaderReference> shaders = ImmutableList.builder();

        final ImmutableMap.Builder<Integer, String> samplerBindings = ImmutableMap.builder();
        final ImmutableMap.Builder<Integer, String> ssboBindings = ImmutableMap.builder();
        final ImmutableMap.Builder<Integer, String> uboBindings = ImmutableMap.builder();
        final ImmutableMap.Builder<Integer, String> imageBindings = ImmutableMap.builder();
        final SetupFunction<? super PB> setupFunction;

        public final B addShader(@NonNull ShaderType type, @NonNull Identifier identifier) {
            this.shaders.add(new DeferredShaderReference(type, identifier));
            return uncheckedCast(this);
        }

        public final B addSampler(@NotNegative int unit, @NonNull String name) {
            this.samplerBindings.put(unit, name);
            return uncheckedCast(this);
        }

        public final B addSSBO(@NotNegative int bindingIndex, @NonNull String name) {
            this.ssboBindings.put(bindingIndex, name);
            return uncheckedCast(this);
        }

        public final B addSSBOs(@NotNegative int bindingIndex, @NotNegative int count, @NonNull String name) {
            for (int i = 0; i < count; i++) {
                this.addSSBO(bindingIndex + i, name + '[' + i + ']');
            }
            return uncheckedCast(this);
        }

        public final B addUBO(@NotNegative int bindingIndex, @NonNull String name) {
            this.uboBindings.put(bindingIndex, name);
            return uncheckedCast(this);
        }

        public final B addUBOs(@NotNegative int bindingIndex, @NotNegative int count, @NonNull String name) {
            for (int i = 0; i < count; i++) {
                this.addUBO(bindingIndex + i, name + '[' + i + ']');
            }
            return uncheckedCast(this);
        }

        public final B addImage(@NotNegative int bindingIndex, @NonNull String name) {
            this.imageBindings.put(bindingIndex, name);
            return uncheckedCast(this);
        }

        public final B addImages(@NotNegative int bindingIndex, @NotNegative int count, @NonNull String name) {
            for (int i = 0; i < count; i++) {
                this.addImage(bindingIndex + i, name + '[' + i + ']');
            }
            return uncheckedCast(this);
        }

        final ReloadableShaderProgram<P> build() {
            return new ReloadableShaderProgram<>(this);
        }

        final SetupFunction<? super PB> buildSetupFunction() {
            val samplerBindings = this.samplerBindings.build();
            val ssboBindings = this.ssboBindings.build();
            val uboBindings = this.uboBindings.build();
            val imageBindings = this.imageBindings.build();
            SetupFunction<? super PB> setupFunction = this.setupFunction;

            if (samplerBindings.isEmpty() && ssboBindings.isEmpty() && uboBindings.isEmpty() && imageBindings.isEmpty()) {
                // There are no additional configurations, return the user-provided setup function directly
                return setupFunction;
            }

            return builder -> {
                if (setupFunction != null) {
                    setupFunction.setup(builder);
                }
                samplerBindings.forEach(builder::addSampler);
                ssboBindings.forEach(builder::addSSBO);
                uboBindings.forEach(builder::addUBO);
                imageBindings.forEach(builder::addImage);
            };
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static final class ComputeBuilder extends Builder<ComputeShaderProgram, ComputeShaderProgram.Builder, ComputeBuilder> {
        ComputeBuilder(Object key, ShaderMacros macros, SetupFunction<? super ComputeShaderProgram.Builder> setupFunction) {
            super(key, macros, ComputeShaderProgram::builder, setupFunction);
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static final class DrawBuilder extends Builder<DrawShaderProgram, DrawShaderProgram.Builder, DrawBuilder> {
        DrawBuilder(Object key, ShaderMacros macros, SetupFunction<? super DrawShaderProgram.Builder> setupFunction) {
            super(key, macros, DrawShaderProgram::builder, setupFunction);
        }
    }
}
