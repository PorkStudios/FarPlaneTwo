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

package net.daporkchop.fp2.gl.lwjgl2.compute;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.compute.ComputeGlobalSize;
import net.daporkchop.fp2.gl.compute.ComputeLocalSize;
import net.daporkchop.fp2.gl.compute.ComputeShader;
import net.daporkchop.fp2.gl.lwjgl2.LWJGL2;
import net.daporkchop.fp2.gl.lwjgl2.shader.BaseShaderImpl;
import net.daporkchop.fp2.gl.lwjgl2.shader.BaseShaderProgramImpl;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class ComputeShaderImpl extends BaseShaderProgramImpl implements ComputeShader {
    protected final ComputeLocalSize localSize;

    public ComputeShaderImpl(@NonNull LWJGL2 gl, @NonNull ComputeLocalSize localSize, @NonNull BaseShaderImpl... shaders) throws ShaderLinkageException {
        super(gl, shaders);

        this.localSize = localSize;
    }

    @Override
    public void dispatch(@NonNull ComputeGlobalSize globalSize) {
        //TODO: better state management system
        glUseProgram(this.id);
        glDispatchCompute(globalSize.x(), globalSize.y(), globalSize.z());
        glUseProgram(0);
    }
}
