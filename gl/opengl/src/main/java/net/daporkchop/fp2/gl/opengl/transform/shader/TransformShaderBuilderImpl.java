/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.transform.shader;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;
import net.daporkchop.fp2.gl.opengl.shader.source.SourceLine;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.transform.TransformLayout;
import net.daporkchop.fp2.gl.transform.shader.TransformShader;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;

import java.util.Map;

/**
 * @author DaPorkchop_
 */
public class TransformShaderBuilderImpl extends BaseShaderBuilderImpl<TransformShader, TransformLayout> implements TransformShaderBuilder {
    public TransformShaderBuilderImpl(@NonNull OpenGL gl, @NonNull TransformLayout layout) {
        super(gl, ShaderType.VERTEX, layout);
    }

    @Override
    public TransformShaderBuilder include(@NonNull Identifier id) {
        super.include(id);
        return this;
    }

    @Override
    public TransformShaderBuilder include(@NonNull Identifier... ids) {
        super.include(ids);
        return this;
    }

    @Override
    public TransformShaderBuilder define(@NonNull String key, @NonNull Object value) {
        super.define(key, value);
        return this;
    }

    @Override
    public TransformShaderBuilder defineAll(@NonNull Map<String, Object> macros) {
        super.defineAll(macros);
        return this;
    }

    @Override
    protected TransformShader compile(@NonNull SourceLine... lines) throws ShaderCompilationException {
        return new TransformShaderImpl(this, lines);
    }
}
