/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.gl.opengl.attribute.texture;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureTarget;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLSamplerType;
import net.daporkchop.fp2.gl.opengl.layout.LayoutEntry;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class TextureFormat2DImpl extends BaseTextureFormatImpl<TextureFormat2DImpl> implements TextureFormat2D {
    public TextureFormat2DImpl(@NonNull OpenGL gl, @NonNull String name, @NonNull GLSLSamplerType glslType) {
        super(gl, name, glslType);
    }

    @Override
    public BindingLocation<?> bindingLocation(@NonNull LayoutEntry<TextureFormat2DImpl> layout, @NonNull BindingLocationAssigner assigner) {
        checkArg(layout.usage() == AttributeUsage.TEXTURE, "unsupported usage: %s", layout.usage());

        return new TextureBindingLocation<Texture2DImpl>(layout, TextureTarget.TEXTURE_2D, assigner);
    }

    @Override
    public abstract TextureWriter2D createWriter(int width, int height); //implemented in generated code

    @Override
    public abstract Texture2D createTexture(int width, int height, int levels); //implemented in generated code

    @Override
    public Texture2D wrapExternalTexture(@NonNull Object id) throws UnsupportedOperationException {
        return new WrappedTexture2DImpl(this, (Integer) id);
    }
}
