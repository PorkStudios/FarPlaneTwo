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
import net.daporkchop.fp2.gl.compute.ComputeLayout;
import net.daporkchop.fp2.gl.compute.ComputeLayoutBuilder;
import net.daporkchop.fp2.gl.compute.ComputeLocalSize;
import net.daporkchop.fp2.gl.compute.ComputeShader;
import net.daporkchop.fp2.gl.compute.ComputeShaderProgram;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderProgramBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;
import net.daporkchop.fp2.gl.opengl.shader.source.SourceLine;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.BaseShaderProgramBuilder;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class ComputeImpl implements GLCompute {
    protected final OpenGL gl;
    protected final GLAPI api;

    public ComputeImpl(@NonNull OpenGL gl) {
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
    public ComputeLayoutBuilder createComputeLayout(@NonNull ComputeLocalSize localSize) {
        ComputeLocalSize maxLocalSize = this.maxLocalSize();
        checkArg(localSize.x() <= maxLocalSize.x() && localSize.y() <= maxLocalSize.y() && localSize.z() <= maxLocalSize.z(),
                "local size %s max not be greater than maximum local size %s on any axis", localSize, maxLocalSize);

        long maxLocalCount = this.maxLocalCount();
        checkArg(localSize.count() <= maxLocalCount, "local count %d may not be greater than maximum local count %d", localSize.count(), maxLocalCount);

        return new ComputeLayoutBuilderImpl(this.gl, localSize);
    }

    @Override
    public BaseShaderBuilder<ComputeShader> createComputeShader(@NonNull ComputeLayout layout) {
        return new BaseShaderBuilderImpl<ComputeShader, ComputeLayout>(this.gl, ShaderType.COMPUTE, layout) {
            @Override
            protected ComputeShader compile(@NonNull SourceLine... lines) throws ShaderCompilationException {
                return new ComputeShaderImpl(this, lines);
            }
        };
    }

    @Override
    public BaseShaderProgramBuilder<ComputeShaderProgram, ComputeShader, ComputeLayout> createComputeShaderProgram(@NonNull ComputeLayout layout) {
        return new BaseShaderProgramBuilderImpl<ComputeShaderProgram, ComputeShader, ComputeLayoutImpl, ComputeLayout>(this.gl, (ComputeLayoutImpl) layout) {
            @Override
            public ComputeShaderProgram build() throws ShaderLinkageException {
                return new ComputeShaderProgramImpl(this);
            }
        };
    }
}
