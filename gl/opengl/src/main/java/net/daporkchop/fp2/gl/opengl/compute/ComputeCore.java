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

package net.daporkchop.fp2.gl.opengl.compute;

import lombok.NonNull;
import net.daporkchop.fp2.gl.compute.ComputeGlobalSize;
import net.daporkchop.fp2.gl.compute.ComputeLocalSize;
import net.daporkchop.fp2.gl.compute.ComputeShader;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderImpl;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class ComputeCore implements GLCompute {
    protected final OpenGL gl;
    protected final GLAPI api;

    public ComputeCore(@NonNull OpenGL gl) {
        this.gl = gl;
        this.api = gl.api();
    }

    @Override
    public boolean supported() {
        return true;
    }

    @Override
    public ComputeGlobalSize maxGlobalSize() {
        return new ComputeGlobalSize(
                this.api.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0),
                this.api.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1),
                this.api.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2));
    }

    @Override
    public long maxGlobalCount() {
        return Integer.MAX_VALUE; //no limit is imposed by opengl
    }

    @Override
    public ComputeLocalSize maxLocalSize() {
        return new ComputeLocalSize(
                this.api.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0),
                this.api.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1),
                this.api.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2));
    }

    @Override
    public long maxLocalCount() {
        return this.api.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
    }

    @Override
    public ComputeShader compileComputeShader(@NonNull ComputeLocalSize localSize, @NonNull String source) throws ShaderCompilationException {
        class UnlinkedComputeShader extends BaseShaderImpl {
            public UnlinkedComputeShader(@NonNull String... sources) throws ShaderCompilationException {
                super(ComputeCore.this.gl, GL_COMPUTE_SHADER, sources);
            }
        }

        try (UnlinkedComputeShader shader = new UnlinkedComputeShader(source)) {
            return new ComputeShaderImpl(this.gl, localSize, shader);
        } catch (ShaderLinkageException e) {
            throw new ShaderCompilationException("unable to link compute shader", e);
        }
    }
}
