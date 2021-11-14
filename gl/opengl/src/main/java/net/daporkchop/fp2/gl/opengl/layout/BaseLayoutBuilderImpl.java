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

package net.daporkchop.fp2.gl.opengl.layout;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeFormat;
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.layout.LayoutBuilder;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeFormatImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class BaseLayoutBuilderImpl<L extends BaseLayout> implements LayoutBuilder<L> {
    @NonNull
    protected final OpenGL gl;

    protected final List<AttributeFormatImpl> uniforms = new ArrayList<>();
    protected final List<AttributeFormatImpl> globals = new ArrayList<>();
    protected final List<LocalAttributeFormatImpl<?>> locals = new ArrayList<>();
    protected final List<AttributeFormatImpl> outputs = new ArrayList<>();

    @Override
    public LayoutBuilder<L> withUniforms(@NonNull AttributeFormat uniforms) {
        this.uniforms.add((AttributeFormatImpl) uniforms);
        return this;
    }

    @Override
    public LayoutBuilder<L> withGlobals(@NonNull AttributeFormat globals) {
        this.globals.add((AttributeFormatImpl) globals);
        return this;
    }

    @Override
    public LayoutBuilder<L> withLocals(@NonNull LocalAttributeFormat<?> locals) {
        this.locals.add((LocalAttributeFormatImpl<?>) locals);
        return this;
    }

    @Override
    public LayoutBuilder<L> withOutputs(@NonNull AttributeFormat outputs) {
        this.outputs.add((AttributeFormatImpl) outputs);
        return this;
    }
}
