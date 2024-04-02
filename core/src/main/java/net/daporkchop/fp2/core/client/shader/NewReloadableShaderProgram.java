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

import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.Shader;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.shader.source.IncludePreprocessor;
import net.daporkchop.lib.common.function.plain.TriFunction;

import java.util.function.UnaryOperator;

/**
 * A wrapper around a {@link ShaderProgram} which allows shaders to be reloaded.
 * <p>
 * Reload functionality is implemented in {@link ReloadableShaderRegistry}.
 *
 * @author DaPorkchop_
 */
public final class NewReloadableShaderProgram<P extends ShaderProgram> implements AutoCloseable {
    public static NewReloadableShaderProgram<DrawShaderProgram> draw(FP2Core fp2, ShaderMacros macros, Identifier vertexShaderSource, Identifier fragmentShaderSource) {
        return draw(fp2, macros, vertexShaderSource, fragmentShaderSource, null);
    }

    public static NewReloadableShaderProgram<DrawShaderProgram> draw(FP2Core fp2, ShaderMacros macros, Identifier vertexShaderSource, Identifier fragmentShaderSource, UnaryOperator<DrawShaderProgram.Builder> setup) {
        return new NewReloadableShaderProgram<>(fp2, macros, (gl, resourceProvider, shaderMacros) -> {
            try (Shader vertexShader = new Shader(gl, ShaderType.VERTEX, new IncludePreprocessor(resourceProvider).addVersionHeader(gl).define(shaderMacros.macros()).include(vertexShaderSource).finish());
                 Shader fragmentShader = new Shader(gl, ShaderType.FRAGMENT, new IncludePreprocessor(resourceProvider).addVersionHeader(gl).define(shaderMacros.macros()).include(fragmentShaderSource).finish())) {
                return setup.apply(DrawShaderProgram.builder(gl).vertexShader(vertexShader).fragmentShader(fragmentShader)).build();
            }
        });
    }

    final FP2Core fp2;
    final ShaderMacros macros;
    final TriFunction<OpenGL, ResourceProvider, ShaderMacros.Immutable, P> compileFunction;

    ShaderMacros.Immutable macrosSnapshot;
    P program;

    public NewReloadableShaderProgram(FP2Core fp2, ShaderMacros macros, TriFunction<OpenGL, ResourceProvider, ShaderMacros.Immutable, P> compileFunction) {
        this.fp2 = fp2;
        this.macros = macros;
        this.compileFunction = compileFunction;

        this.macrosSnapshot = macros.snapshot();
        this.program = compileFunction.apply(fp2.client().gl(), fp2.client().resourceProvider(), this.macrosSnapshot);
    }

    /**
     * @return the actual {@link ShaderProgram} referred to by this program
     */
    public P get() {
        return this.program;
    }

    @Override
    public void close() {
        this.program.close();
    }
}
