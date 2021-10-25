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

package net.daporkchop.fp2.gl.lwjgl2.shader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.lwjgl2.LWJGL2;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.GLShaders;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.VertexShader;

import static org.lwjgl.opengl.GL20.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class ShadersCore implements GLShaders {
    @NonNull
    protected final LWJGL2 gl;

    @Override
    public boolean supported() {
        return true;
    }

    @Override
    public VertexShader compileVertexShader(@NonNull String source) throws ShaderCompilationException {
        return new VertexShaderImpl(this.gl, GL_VERTEX_SHADER, source);
    }

    @Override
    public FragmentShader compileFragmentShader(@NonNull String source) throws ShaderCompilationException {
        return new FragmentShaderImpl(this.gl, GL_FRAGMENT_SHADER, source);
    }

    @Override
    public ShaderProgram linkShaderProgram(@NonNull VertexShader vertexShader, @NonNull FragmentShader fragmentShader) throws ShaderLinkageException {
        return new ShaderProgramImpl(this.gl, (VertexShaderImpl) vertexShader, (FragmentShaderImpl) fragmentShader);
    }
}
