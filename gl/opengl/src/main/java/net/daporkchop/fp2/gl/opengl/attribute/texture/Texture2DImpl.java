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
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class Texture2DImpl extends BaseTextureImpl<TextureFormat2DImpl> implements Texture2D {
    protected final int width;
    protected final int height;
    protected final int levels;

    public Texture2DImpl(@NonNull TextureFormat2DImpl formatIn, int width, int height, int levels) {
        super(formatIn);

        this.width = positive(width, "width");
        this.height = positive(height, "height");
        this.levels = positive(levels, "levels");

        this.bindAnyUnit(TextureTarget.TEXTURE_2D, target -> {
            int internalFormat = this.gl_internalFormat();
            int format = this.gl_format();
            int type = this.gl_type();

            for (int level = 0; level < levels; level++) {
                this.gl.api().glTexImage2D(target, level, internalFormat, width >> level, height >> level, format, type, 0L);
            }

            this.gl.api().glTexParameter(target, GL_TEXTURE_BASE_LEVEL, 0);
            this.gl.api().glTexParameter(target, GL_TEXTURE_MAX_LEVEL, levels - 1);
        });
    }

    @Override
    public void set(int level, int xOffset, int yOffset, @NonNull TextureWriter2D _writer) {
        TextureWriter2DImpl writer = (TextureWriter2DImpl) _writer;
        checkIndex(this.levels, level);
        checkRangeLen(this.width >> level, xOffset, writer.width);
        checkRangeLen(this.height >> level, yOffset, writer.height);

        this.bindAnyUnit(TextureTarget.TEXTURE_2D, target -> {
            this.gl.api().glTexSubImage2D(target, level, xOffset, yOffset, writer.width, writer.height, this.gl_format(), this.gl_type(), writer.addr);
        });
    }
}
