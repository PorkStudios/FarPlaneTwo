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

package net.daporkchop.fp2.gl.opengl.draw.binding;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.old.global.DrawGlobalBuffer;
import net.daporkchop.fp2.gl.attribute.old.local.DrawLocalBuffer;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.old.uniform.UniformArrayBuffer;
import net.daporkchop.fp2.gl.attribute.old.uniform.UniformBuffer;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.draw.index.IndexBufferImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DrawBindingBuilderImpl implements DrawBindingBuilder.OptionallyIndexedStage {
    @NonNull
    protected final DrawLayoutImpl layout;

    protected final List<BaseAttributeBufferImpl<?, ?>> uniforms = new ArrayList<>();
    protected final List<BaseAttributeBufferImpl<?, ?>> uniformArrays = new ArrayList<>();
    protected final List<BaseAttributeBufferImpl<?, ?>> globals = new ArrayList<>();
    protected final List<BaseAttributeBufferImpl<?, ?>> locals = new ArrayList<>();
    protected final List<BaseAttributeBufferImpl<?, ?>> textures = new ArrayList<>();

    protected IndexBufferImpl indices;

    //
    // OptionallyIndexedStage
    //

    @Override
    public DrawBindingBuilder<DrawBindingIndexed> withIndexes(@NonNull IndexBuffer indices) {
        this.indices = (IndexBufferImpl) indices;
        return uncheckedCast(this);
    }

    //
    // DrawBindingBuilder
    //

    @Override
    public DrawBindingBuilder<DrawBinding> withUniforms(@NonNull UniformBuffer<?> buffer) {
        this.uniforms.add((BaseAttributeBufferImpl<?, ?>) buffer);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withUniformArrays(@NonNull UniformArrayBuffer<?> buffer) {
        this.uniformArrays.add((BaseAttributeBufferImpl<?, ?>) buffer);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withGlobals(@NonNull DrawGlobalBuffer<?> buffer) {
        this.globals.add((BaseAttributeBufferImpl<?, ?>) buffer);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withLocals(@NonNull DrawLocalBuffer<?> buffer) {
        this.locals.add((BaseAttributeBufferImpl<?, ?>) buffer);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withTexture(@NonNull Texture2D<?> texture) {
        this.textures.add((BaseAttributeBufferImpl<?, ?>) texture);
        return this;
    }

    @Override
    public DrawBinding build() {
        { //uniforms
            Set<BaseAttributeFormatImpl<?>> givenFormats = this.uniforms.stream().map(BaseAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<BaseAttributeFormatImpl<?>> expectedFormats = this.layout.uniformFormats().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        { //uniform arrays
            Set<BaseAttributeFormatImpl<?>> givenFormats = this.uniformArrays.stream().map(BaseAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<BaseAttributeFormatImpl<?>> expectedFormats = this.layout.uniformArrayFormats().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        { //globals
            Set<BaseAttributeFormatImpl<?>> givenFormats = this.globals.stream().map(BaseAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<BaseAttributeFormatImpl<?>> expectedFormats = this.layout.globalFormats().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        { //locals
            Set<BaseAttributeFormatImpl<?>> givenFormats = this.locals.stream().map(BaseAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<BaseAttributeFormatImpl<?>> expectedFormats = this.layout.localFormats().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        { //textures
            Set<BaseAttributeFormatImpl<?>> givenFormats = this.textures.stream().map(BaseAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<BaseAttributeFormatImpl<?>> expectedFormats = this.layout.textureFormats().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        return this.indices != null
                ? new DrawBindingIndexedImpl(this)
                : new DrawBindingImpl(this);
    }

    public Stream<BaseAttributeBufferImpl<?, ?>> allBuffers() {
        return Stream.of(this.uniforms, this.uniformArrays, this.globals, this.locals, this.textures).flatMap(List::stream);
    }

    public Stream<BaseAttributeBufferImpl<?, ?>> allBuffersAndChildren() {
        return this.allBuffers().flatMap(BaseAttributeBufferImpl::selfAndChildren);
    }
}
