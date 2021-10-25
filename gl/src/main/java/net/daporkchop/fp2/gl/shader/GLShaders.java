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

package net.daporkchop.fp2.gl.shader;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLModule;

/**
 * API for OpenGL shaders.
 *
 * @author DaPorkchop_
 */
public interface GLShaders extends GLModule {
    /**
     * Compiles a {@link VertexShader} from the given source code.
     *
     * @param source the source code
     * @return the compiled {@link VertexShader}
     * @throws ShaderCompilationException if shader compilation fails
     */
    VertexShader compileVertexShader(@NonNull String source) throws ShaderCompilationException;

    /**
     * Compiles a {@link FragmentShader} from the given source code.
     *
     * @param source the source code
     * @return the compiled {@link FragmentShader}
     * @throws ShaderCompilationException if shader compilation fails
     */
    FragmentShader compileFragmentShader(@NonNull String source) throws ShaderCompilationException;

    /**
     * Links a {@link ShaderProgram} from the given {@link VertexShader} and {@link FragmentShader}.
     *
     * @param vertexShader   the {@link VertexShader}
     * @param fragmentShader the {@link FragmentShader}
     * @return the linked {@link ShaderProgram}
     * @throws ShaderLinkageException if shader linkage fails
     */
    ShaderProgram linkShaderProgram(@NonNull VertexShader vertexShader, @NonNull FragmentShader fragmentShader) throws ShaderLinkageException;
}
