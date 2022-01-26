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

package net.daporkchop.fp2.gl.opengl.layout;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.layout.binding.BaseBinding;
import net.daporkchop.fp2.gl.layout.binding.BaseBindingBuilder;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.InternalAttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.BaseTextureImpl;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class BaseBindingBuilderImpl<BUILDER extends BaseBindingBuilder<BUILDER, B>, B extends BaseBinding, L extends BaseLayoutImpl> implements BaseBindingBuilder<BUILDER, B> {
    @NonNull
    protected final L layout;

    protected final ImmutableMap.Builder<BaseAttributeBufferImpl<?, ?>, InternalAttributeUsage> buffersUsages = ImmutableMap.builder();

    protected void with(@NonNull BaseAttributeBufferImpl<?, ?> buffer, @NonNull InternalAttributeUsage usage) {
        this.buffersUsages.put(buffer, usage);
    }

    @Override
    public BUILDER with(@NonNull AttributeUsage usage, @NonNull AttributeBuffer<?> buffer) {
        this.with((AttributeBufferImpl<?, ?>) buffer, InternalAttributeUsage.fromExternal(usage));
        return uncheckedCast(this);
    }

    @Override
    public BUILDER withTexture(@NonNull Texture2D<?> texture) {
        this.with((BaseTextureImpl<?, ?>) texture, InternalAttributeUsage.TEXTURE);
        return uncheckedCast(this);
    }
}
