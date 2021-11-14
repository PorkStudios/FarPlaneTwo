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

package net.daporkchop.fp2.gl.opengl.binding;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeBuffer;
import net.daporkchop.fp2.gl.binding.DrawBinding;
import net.daporkchop.fp2.gl.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.index.IndexBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.global.GlobalAttributeBufferVertexAttribute;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.uniform.UniformAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.index.IndexBufferImpl;
import net.daporkchop.fp2.gl.opengl.layout.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DrawBindingBuilderImpl implements DrawBindingBuilder.OptionallyIndexedStage {
    @NonNull
    protected final DrawLayoutImpl layout;

    protected final List<UniformAttributeBufferImpl> uniforms = new ArrayList<>();
    protected final List<BaseAttributeBufferImpl> globals = new ArrayList<>();
    protected final List<LocalAttributeBufferImpl<?>> locals = new ArrayList<>();

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
    public DrawBindingBuilder<DrawBinding> withUniforms(@NonNull UniformAttributeBuffer uniforms) {
        this.uniforms.add((UniformAttributeBufferImpl) uniforms);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withGlobals(@NonNull GlobalAttributeBuffer globals) {
        this.globals.add((BaseAttributeBufferImpl) globals);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withLocals(@NonNull LocalAttributeBuffer<?> locals) {
        this.locals.add((LocalAttributeBufferImpl) locals);
        return this;
    }

    @Override
    public DrawBinding build() {
        { //uniforms
            Set<AttributeFormatImpl> givenFormats = this.uniforms.stream().map(UniformAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<AttributeFormatImpl> expectedFormats = this.layout.uniformFormatsByName().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        { //globals
            Set<AttributeFormatImpl> givenFormats = this.globals.stream().map(BaseAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<AttributeFormatImpl> expectedFormats = this.layout.globalFormatsByName().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        { //locals
            Set<LocalAttributeFormatImpl<?>> givenFormats = this.locals.stream().map(LocalAttributeBufferImpl::format).collect(Collectors.toSet());
            Set<LocalAttributeFormatImpl<?>> expectedFormats = this.layout.localFormatsByName().values();
            checkArg(expectedFormats.equals(givenFormats), "attribute format mismatch: %s (given) != %s (expected)", givenFormats, expectedFormats);
        }

        return this.indices != null
                ? new DrawBindingIndexedImpl(this)
                : new DrawBindingImpl(this);
    }
}
