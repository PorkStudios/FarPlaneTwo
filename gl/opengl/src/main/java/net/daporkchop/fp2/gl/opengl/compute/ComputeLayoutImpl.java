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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.compute.ComputeLayout;
import net.daporkchop.fp2.gl.compute.ComputeLocalSize;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

/**
 * @author DaPorkchop_
 */
@Getter
public class ComputeLayoutImpl extends BaseLayoutImpl implements ComputeLayout {
    protected final ComputeLocalSize localSize;

    public ComputeLayoutImpl(@NonNull ComputeLayoutBuilderImpl builder) {
        super(builder.gl);

        this.localSize = builder.localSize;
    }

    @Override
    public void prefixShaderSource(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        builder.append("layout(local_size_x = ").append(this.localSize.x())
                .append(", local_size_y = ").append(this.localSize.y())
                .append(", local_size_z = ").append(this.localSize.z())
                .append(") in;\n");
    }

    @Override
    public void configureProgramPreLink(int program) {
    }

    @Override
    public void configureProgramPostLink(int program) {
    }

    @Override
    public void close() {
    }
}
