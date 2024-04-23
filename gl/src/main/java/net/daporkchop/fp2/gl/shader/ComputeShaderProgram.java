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

import lombok.Getter;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.compute.ComputeWorkGroupCount;
import net.daporkchop.fp2.gl.compute.ComputeWorkGroupSize;

import java.util.EnumSet;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class ComputeShaderProgram extends ShaderProgram {
    public static boolean supported(OpenGL gl) {
        return gl.supports(GLExtension.GL_ARB_compute_shader);
    }

    public static Builder builder(OpenGL gl) {
        return new Builder(gl);
    }

    private final ComputeWorkGroupSize workGroupSize;

    ComputeShaderProgram(Builder builder) throws ShaderLinkageException {
        super(builder);

        int[] workGroupSize = this.gl.glGetProgramiv(this.id, GL_COMPUTE_WORK_GROUP_SIZE, 3);
        this.workGroupSize = new ComputeWorkGroupSize(workGroupSize[0], workGroupSize[1], workGroupSize[2]);
    }

    /**
     * Dispatches this compute shader with the given number of compute work groups.
     *
     * @param workGroupCount the number of compute work groups to dispatch
     */
    public void dispatch(ComputeWorkGroupCount workGroupCount) {
        checkArg(workGroupCount.isValid(this.gl), "compute work group count %s exceeds context limits", workGroupCount);
        this.bind(() -> this.gl.glDispatchCompute(workGroupCount.x(), workGroupCount.y(), workGroupCount.z()));
    }

    /**
     * @author DaPorkchop_
     */
    public static final class Builder extends ShaderProgram.Builder<ComputeShaderProgram, Builder> {
        Builder(OpenGL gl) {
            super(gl, EnumSet.of(ShaderType.COMPUTE), EnumSet.of(ShaderType.COMPUTE));
            checkState(supported(gl), gl);
        }

        public Builder computeShader(Shader computeShader) {
            checkArg(computeShader.type() == ShaderType.COMPUTE, "not a compute shader: %s", computeShader);
            return this.addShader(computeShader);
        }

        @Override
        protected ComputeShaderProgram build0() throws ShaderLinkageException {
            return new ComputeShaderProgram(this);
        }
    }
}
