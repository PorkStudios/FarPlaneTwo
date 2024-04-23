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

package net.daporkchop.fp2.core.client.shader;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.Shader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.shader.source.IncludePreprocessor;
import net.daporkchop.lib.common.util.PorkUtil;

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
public final class NewReloadableShaderProgram<P extends ShaderProgram> implements AutoCloseable {
    final ReloadableShaderRegistry registry;
    final FP2Core fp2;
    final ShaderMacros macros;

    final List<DeferredShaderReference> shaders;
    final Function<OpenGL, ShaderProgram.Builder<P, ?>> builderFactory; //this is some truly enterprise code
    final SetupFunction<?> setupFunction;

    ShaderMacros.Immutable macrosSnapshot;
    P program;

    NewReloadableShaderProgram(NewReloadableShaderProgram.Builder<P> builder) {
        this.registry = builder.registry;
        this.fp2 = builder.fp2;
        this.macros = builder.macros;

        this.shaders = ImmutableList.copyOf(builder.shaders);
        this.builderFactory = builder.builderFactory;
        this.setupFunction = builder.setupFunction;

        this.macrosSnapshot = this.macros.snapshot();
        this.program = this.compile(this.fp2.client().gl(), this.fp2.client().resourceProvider(), this.macrosSnapshot);

        this.registry.register(this);
    }

    P compile(OpenGL gl, ResourceProvider resourceProvider, ShaderMacros.Immutable macrosSnapshot) throws ShaderCompilationException, ShaderLinkageException {
        List<Shader> compiledShaders = new ArrayList<>(this.shaders.size());
        try {
            //compile each of the referenced shaders
            for (val shader : this.shaders) {
                compiledShaders.add(new Shader(gl, shader.type,
                        new IncludePreprocessor(resourceProvider)
                                .addVersionHeader(gl)
                                .define(macrosSnapshot.macros())
                                .include(shader.identifier)
                                .finish()));
            }

            val builder = this.builderFactory.apply(gl);
            this.setupFunction.setup(uncheckedCast(builder));
            for (val shader : compiledShaders) { //add all the compiled shaders to the program for linking
                builder.addShader(shader);
            }
            return builder.build();
        } finally {
            PorkUtil.closeAll(compiledShaders);
        }
    }

    /**
     * @return the actual {@link ShaderProgram} referred to by this program
     */
    public P get() {
        return this.program;
    }

    @Override
    public void close() {
        this.registry.unregister(this);
        if (this.program != null) {
            this.program.close();
        }
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
    public static final class Builder<P extends ShaderProgram> {
        final ReloadableShaderRegistry registry;
        final FP2Core fp2;
        final ShaderMacros macros;
        final List<DeferredShaderReference> shaders = new ArrayList<>();

        final Function<OpenGL, ShaderProgram.Builder<P, ?>> builderFactory; //this is some truly enterprise code
        final SetupFunction<?> setupFunction;

        public Builder<P> addShader(@NonNull ShaderType type, @NonNull Identifier identifier) {
            this.shaders.add(new DeferredShaderReference(type, identifier));
            return uncheckedCast(this);
        }

        public NewReloadableShaderProgram<P> build() {
            return new NewReloadableShaderProgram<>(this);
        }
    }
}
