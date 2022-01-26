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

package net.daporkchop.fp2.gl.opengl.attribute.texture;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructLayouts;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class TextureFormat2DImpl<S> extends BaseTextureFormatImpl<S> implements TextureFormat2D<S> {
    public TextureFormat2DImpl(@NonNull TextureFormatBuilderImpl<S, TextureFormat2D<S>> builder) {
        super(builder.gl(), builder.gl().structFormatGenerator().getTexture(StructLayouts.texture(builder.gl(), builder.structInfo())));
    }

    @Override
    public BindingLocation<?> bindingLocation(@NonNull AttributeUsage usage, @NonNull BindingLocationAssigner assigner) {
        checkArg(usage == AttributeUsage.TEXTURE, "unsupported usage: %s", usage);

        return new TextureBindingLocation<S, Texture2DImpl<S>>(this.structFormat(), TextureTarget.TEXTURE_2D, assigner);
    }

    @Override
    public TextureWriter2D<S> createWriter(int width, int height) {
        return new TextureWriter2DImpl<>(this, width, height);
    }

    @Override
    public Texture2D<S> createTexture(int width, int height, int levels) {
        return new Texture2DImpl<>(this, width, height, levels);
    }

    @Override
    public Texture2D<S> wrapExternalTexture(@NonNull Object id) throws UnsupportedOperationException {
        return new WrappedTexture2DImpl<>(this, (Integer) id);
    }
}
