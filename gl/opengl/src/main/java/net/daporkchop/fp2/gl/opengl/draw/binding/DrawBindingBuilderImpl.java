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

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.InternalAttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.BaseTextureImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.draw.index.IndexBufferImpl;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DrawBindingBuilderImpl implements DrawBindingBuilder.OptionallyIndexedStage {
    @NonNull
    protected final DrawLayoutImpl layout;

    protected final ImmutableMap.Builder<BaseAttributeBufferImpl<?, ?>, InternalAttributeUsage> buffersUsages = ImmutableMap.builder();

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

    protected void with(@NonNull BaseAttributeBufferImpl<?, ?> buffer, @NonNull InternalAttributeUsage usage) {
        this.buffersUsages.put(buffer, usage);
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withUniforms(@NonNull AttributeBuffer<?> buffer) {
        this.with((AttributeBufferImpl<?, ?>) buffer, InternalAttributeUsage.UNIFORM);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withUniformArrays(@NonNull AttributeBuffer<?> buffer) {
        this.with((AttributeBufferImpl<?, ?>) buffer, InternalAttributeUsage.UNIFORM_ARRAY);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withGlobals(@NonNull AttributeBuffer<?> buffer) {
        this.with((AttributeBufferImpl<?, ?>) buffer, InternalAttributeUsage.DRAW_GLOBAL);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withLocals(@NonNull AttributeBuffer<?> buffer) {
        this.with((AttributeBufferImpl<?, ?>) buffer, InternalAttributeUsage.DRAW_LOCAL);
        return this;
    }

    @Override
    public DrawBindingBuilder<DrawBinding> withTexture(@NonNull Texture2D<?> texture) {
        this.with((BaseTextureImpl<?, ?>) texture, InternalAttributeUsage.TEXTURE);
        return this;
    }

    @Override
    public DrawBinding build() {
        return this.indices != null
                ? new DrawBindingIndexedImpl(this)
                : new DrawBindingImpl(this);
    }
}
