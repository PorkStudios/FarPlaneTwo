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

package net.daporkchop.fp2.gl.opengl.attribute.texture;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructInfo;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructLayouts;

/**
 * @author DaPorkchop_
 */
@Getter
public class TextureFormat2DImpl<S> extends BaseTextureFormatImpl<S> implements TextureFormat2D<S> {
    public TextureFormat2DImpl(@NonNull OpenGL gl, @NonNull Class<S> clazz) {
        super(gl, gl.structFormatGenerator().getTexture(StructLayouts.texture(gl, new StructInfo<>(clazz))));
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

    @Override
    public TextureTarget target() {
        return TextureTarget.TEXTURE_2D;
    }
}
