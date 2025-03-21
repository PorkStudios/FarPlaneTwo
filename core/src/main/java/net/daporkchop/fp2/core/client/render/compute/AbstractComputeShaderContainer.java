/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.core.client.render.compute;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.state.StatePreserver;

/**
 * @author DaPorkchop_
 */
abstract class AbstractComputeShaderContainer implements AutoCloseable {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = ComputeShaderProgram.REQUIRED_EXTENSIONS;

    protected final @NonNull OpenGL gl;
    protected final @NonNull ReloadableShaderRegistry shaderRegistry;

    public AbstractComputeShaderContainer(@NonNull OpenGL gl, @NonNull GlobalRenderer globalRenderer) {
        this.gl = gl.checkSupported(REQUIRED_EXTENSIONS);
        this.shaderRegistry = globalRenderer.shaderRegistry;
    }

    @Override
    public void close() {
        //no-op
    }

    public abstract void configureModifiedState(@NonNull StatePreserver.Builder builder);
}
