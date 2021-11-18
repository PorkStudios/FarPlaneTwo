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

package net.daporkchop.fp2.gl.compute;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLModule;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

/**
 * API for OpenGL compute shaders.
 *
 * @author DaPorkchop_
 */
public interface GLCompute extends GLModule {
    /**
     * @return the maximum size of a compute shader dispatch along each axis
     */
    ComputeGlobalSize maxGlobalSize();

    /**
     * @return the maximum total number of work groups per compute shader dispatch. Note that this may be less than the
     *         {@link ComputeGlobalSize#count()} returned by {@link #maxGlobalSize()}
     */
    long maxGlobalCount();

    /**
     * @return the maximum size of a compute shader work group along each axis
     */
    ComputeLocalSize maxLocalSize();

    /**
     * @return the maximum total number of shader invocations per work group Note that this may be less than the
     *         {@link ComputeLocalSize#count()} returned by {@link #maxLocalSize()}
     */
    long maxLocalCount();

    /**
     * @return a builder for creating a new {@link ComputeLayout}
     */
    ComputeLayoutBuilder createComputeLayout(@NonNull ComputeLocalSize localSize);

    /**
     * @return a builder for constructing a new {@link ComputeShader}
     */
    BaseShaderBuilder<ComputeShader> createComputeShader(@NonNull ComputeLayout layout);

    /**
     * Links a {@link ComputeShaderProgram} from the given {@link ComputeShader}, tuned for computing data formatted with the given {@link ComputeLayout}.
     *
     * @param layout        the {@link ComputeLayout}
     * @param computeShader the {@link ComputeShader}
     * @return the linked {@link ComputeShaderProgram}
     * @throws ShaderLinkageException if shader linkage fails
     */
    ComputeShaderProgram linkShaderProgram(@NonNull ComputeLayout layout, @NonNull ComputeShader computeShader) throws ShaderLinkageException;
}
