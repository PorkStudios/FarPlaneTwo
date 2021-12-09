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

package net.daporkchop.fp2.gl.opengl.draw;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalFormat;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalFormat;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;
import net.daporkchop.fp2.gl.attribute.uniform.UniformFormat;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayoutBuilder;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DrawLayoutBuilderImpl implements DrawLayoutBuilder {
    @NonNull
    protected final OpenGL gl;

    protected final List<BaseAttributeFormatImpl<?, ?>> uniforms = new ArrayList<>();
    protected final List<BaseAttributeFormatImpl<?, ?>> uniformArrays = new ArrayList<>();
    protected final List<BaseAttributeFormatImpl<?, ?>> globals = new ArrayList<>();
    protected final List<BaseAttributeFormatImpl<?, ?>> locals = new ArrayList<>();
    protected final List<BaseAttributeFormatImpl<?, ?>> textures = new ArrayList<>();

    protected boolean selectionEnabled = false;

    @Override
    public DrawLayoutBuilder withUniforms(@NonNull UniformFormat<?> format) {
        this.uniforms.add((BaseAttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withUniformArrays(@NonNull UniformArrayFormat<?> format) {
        this.uniformArrays.add((BaseAttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withGlobals(@NonNull DrawGlobalFormat<?> format) {
        this.globals.add((BaseAttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withLocals(@NonNull DrawLocalFormat<?> format) {
        this.locals.add((BaseAttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withTexture(@NonNull TextureFormat2D<?> format) {
        this.textures.add((BaseAttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder enableSelection() {
        this.selectionEnabled = true;
        return this;
    }

    @Override
    public DrawLayout build() {
        return new DrawLayoutImpl(this);
    }

    public Stream<BaseAttributeFormatImpl<?, ?>> allFormats() {
        return Stream.of(this.uniforms, this.uniformArrays, this.globals, this.locals, this.textures).flatMap(List::stream);
    }

    public Stream<BaseAttributeFormatImpl<?, ?>> allFormatsAndChildren() {
        return this.allFormats().flatMap(BaseAttributeFormatImpl::selfAndChildren);
    }
}
