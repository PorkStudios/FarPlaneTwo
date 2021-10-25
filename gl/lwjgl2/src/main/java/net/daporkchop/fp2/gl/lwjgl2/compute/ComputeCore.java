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

import lombok.NonNull;
import net.daporkchop.fp2.gl.compute.ComputeGlobalSize;
import net.daporkchop.fp2.gl.compute.ComputeLocalSize;
import net.daporkchop.fp2.gl.compute.ComputeShader;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;

import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL30.glGetInteger;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class ComputeCore implements GLCompute {
    @Override
    public boolean supported() {
        return true;
    }

    @Override
    public ComputeGlobalSize maxGlobalSize() {
        return new ComputeGlobalSize(
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0),
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1),
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2));
    }

    @Override
    public long maxGlobalCount() {
        return Integer.MAX_VALUE; //no limit is imposed by opengl
    }

    @Override
    public ComputeLocalSize maxLocalSize() {
        return new ComputeLocalSize(
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0),
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1),
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2));
    }

    @Override
    public long maxLocalCount() {
        return glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
    }
}
