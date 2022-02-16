/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.gl.opengl.shader;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.opengl.GLExtension;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;
import net.daporkchop.fp2.gl.opengl.shader.source.Preprocessor;
import net.daporkchop.fp2.gl.opengl.shader.source.SourceLine;
import net.daporkchop.fp2.gl.shader.BaseShader;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
public abstract class BaseShaderBuilderImpl<S extends BaseShader<L>, L extends BaseLayout> implements BaseShaderBuilder<S> {
    protected final OpenGL gl;
    protected final L layout;

    protected final ShaderType type;
    protected final Preprocessor preprocessor;

    public BaseShaderBuilderImpl(@NonNull OpenGL gl, @NonNull ShaderType type, @NonNull L layout) {
        this.gl = gl;
        this.type = type;
        this.layout = layout;

        this.preprocessor = new Preprocessor(this.gl.resourceProvider());

        //generate header source
        StringBuilder builder = new StringBuilder();
        builder.append("#version ").append(this.gl.version().glsl()).append('\n');
        this.gl.extensions().stream()
                .filter(GLExtension::glsl) //only include extensions which actually are valid in shaders
                .forEach(extension -> builder.append("#extension ").append(extension.name()).append(": require\n"));
        ((BaseLayoutImpl) layout).prefixShaderSource(this.type, builder);

        //convert header source to individual SourceLines
        Identifier id = Identifier.fromLenient(OpenGL.OPENGL_NAMESPACE, "<auto-generated>");
        int lineNumber = 1;

        List<SourceLine> lines = new ArrayList<>();
        for (String line : builder.toString().split("\n")) {
            lines.add(new SourceLine(line, id, lineNumber++));
        }
        this.preprocessor.appendLines(lines.toArray(new SourceLine[0]));
    }

    //
    // ShaderBuilder
    //

    @Override
    public BaseShaderBuilder<S> include(@NonNull Identifier id) {
        this.preprocessor.appendLines(id);
        return this;
    }

    @Override
    public BaseShaderBuilder<S> include(@NonNull Identifier... ids) {
        for (Identifier id : ids) {
            this.preprocessor.appendLines(id);
        }
        return this;
    }

    @Override
    public BaseShaderBuilder<S> define(@NonNull String key, @NonNull Object value) {
        this.preprocessor.define(Collections.singletonMap(key, value));
        return this;
    }

    @Override
    public BaseShaderBuilder<S> defineAll(@NonNull Map<String, Object> macros) {
        this.preprocessor.define(macros);
        return this;
    }

    @Override
    public S build() throws ShaderCompilationException {
        return this.compile(this.preprocessor.preprocess().lines());
    }

    //
    // internal
    //

    protected abstract S compile(@NonNull SourceLine... lines) throws ShaderCompilationException;
}
