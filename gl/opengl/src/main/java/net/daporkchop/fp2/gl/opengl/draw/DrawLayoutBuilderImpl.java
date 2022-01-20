/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
import net.daporkchop.fp2.gl.attribute.common.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.common.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayoutBuilder;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.InternalAttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.BaseTextureFormatImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DrawLayoutBuilderImpl implements DrawLayoutBuilder {
    @NonNull
    protected final OpenGL gl;

    protected final List<AttributeFormatImpl<?, ?>> uniforms = new ArrayList<>();
    protected final List<AttributeFormatImpl<?, ?>> uniformArrays = new ArrayList<>();
    protected final List<AttributeFormatImpl<?, ?>> globals = new ArrayList<>();
    protected final List<AttributeFormatImpl<?, ?>> locals = new ArrayList<>();
    protected final List<BaseTextureFormatImpl<?>> textures = new ArrayList<>();

    @Override
    public DrawLayoutBuilder withUniforms(@NonNull AttributeFormat<?> format) {
        checkArg(format.usage().contains(AttributeUsage.UNIFORM), "%s doesn't support %s", format, AttributeUsage.UNIFORM);
        this.uniforms.add((AttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withUniformArrays(@NonNull AttributeFormat<?> format) {
        checkArg(format.usage().contains(AttributeUsage.UNIFORM_ARRAY), "%s doesn't support %s", format, AttributeUsage.UNIFORM_ARRAY);
        this.uniformArrays.add((AttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withGlobals(@NonNull AttributeFormat<?> format) {
        checkArg(format.usage().contains(AttributeUsage.DRAW_GLOBAL), "%s doesn't support %s", format, AttributeUsage.DRAW_GLOBAL);
        this.globals.add((AttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withLocals(@NonNull AttributeFormat<?> format) {
        checkArg(format.usage().contains(AttributeUsage.DRAW_LOCAL), "%s doesn't support %s", format, AttributeUsage.DRAW_LOCAL);
        this.locals.add((AttributeFormatImpl<?, ?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder withTexture(@NonNull TextureFormat2D<?> format) {
        this.textures.add((BaseTextureFormatImpl<?>) format);
        return this;
    }

    @Override
    public DrawLayoutBuilder with(@NonNull DrawLayout _layout) {
        DrawLayoutImpl layout = (DrawLayoutImpl) _layout;
        this.uniforms.addAll(layout.uniformFormats);
        this.uniformArrays.addAll(layout.uniformArrayFormats);
        this.globals.addAll(layout.globalFormats);
        this.locals.addAll(layout.localFormats);
        this.textures.addAll(layout.textureFormats);
        return this;
    }

    @Override
    public DrawLayout build() {
        return new DrawLayoutImpl(this);
    }

    public Stream<? extends Map.Entry<InternalAttributeUsage, ? extends BaseAttributeFormatImpl<?>>> allFormatsWithUsage() {
        return Stream.of(
                this.uniforms.stream().flatMap(format -> format.actualFormatsFor(InternalAttributeUsage.UNIFORM)),
                this.uniformArrays.stream().flatMap(format -> format.actualFormatsFor(InternalAttributeUsage.UNIFORM_ARRAY)),
                this.globals.stream().flatMap(format -> format.actualFormatsFor(InternalAttributeUsage.DRAW_GLOBAL)),
                this.locals.stream().flatMap(format -> format.actualFormatsFor(InternalAttributeUsage.DRAW_LOCAL)),
                this.textures.stream().flatMap(format -> format.actualFormatsFor(InternalAttributeUsage.TEXTURE)))
                .flatMap(Function.identity());
    }
}
