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

package net.daporkchop.fp2.gl.opengl.shader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;
import net.daporkchop.fp2.gl.shader.BaseShader;
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import net.daporkchop.fp2.gl.shader.BaseShaderProgramBuilder;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class BaseShaderProgramBuilderImpl<P extends BaseShaderProgram<L_EXTERNAL>, S extends BaseShader<L_EXTERNAL>, L extends BaseLayoutImpl, L_EXTERNAL extends BaseLayout> implements BaseShaderProgramBuilder<P, S, L_EXTERNAL> {
    @NonNull
    protected final OpenGL gl;
    @NonNull
    protected final L layout;

    protected final List<BaseShaderImpl<L_EXTERNAL>> shaders = new ArrayList<>();

    @Override
    public BaseShaderProgramBuilder<P, S, L_EXTERNAL> addShader(@NonNull S shader) {
        this.shaders.add(uncheckedCast(shader));
        return this;
    }
}
