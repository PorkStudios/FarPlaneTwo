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

package net.daporkchop.fp2.gl.texture;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.nio.ByteBuffer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class GLTexture2D extends GLSampledTexture {
    public static GLTexture2D create(@NonNull OpenGL gl, @NonNull TextureInternalFormat internalFormat, @Positive int levels, @Positive int width, @Positive int height) {
        return new GLTexture2D(gl, internalFormat, levels, width, height);
    }

    private final @Positive int width;
    private final @Positive int height;

    private GLTexture2D(OpenGL gl, TextureInternalFormat internalFormat, int levels, int width, int height) {
        super(gl, TextureTarget.TEXTURE_2D, internalFormat, levels);

        try {
            this.width = positive(width, "width");
            this.height = positive(height, "height");

            if (this.immutableStorage) {
                // allocate immutable storage, using DSA or otherwise
                if (this.dsa) {
                    this.gl.glTextureStorage2D(this.id, levels, internalFormat.id(), width, height);
                } else {
                    this.bind(target -> {
                        this.gl.glTexStorage2D(target.id(), levels, internalFormat.id(), width, height);
                    });
                }
            } else {
                // allocate texture data at each mipmap level with uninitialized contents
                this.bind(target -> {
                    for (int level = 0; level < levels; level++) {
                        this.gl.glTexImage2D(target.id(), level, internalFormat.id(),
                                Math.max(width >> level, 1), Math.max(height >> level, 1),
                                internalFormat.defaultFormat().id(), GL_UNSIGNED_BYTE, 0L); //TODO: are these placeholder values for format+type valid for depth/stencil/depth+stencil internal formats?
                    }
                });
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    private void checkSubrectangle(int level, int xOffset, int yOffset, int width, int height) {
        checkIndex(this.levels, level);
        checkRangeLen(Math.max(this.width >> level, 1), xOffset, width);
        checkRangeLen(Math.max(this.height >> level, 1), yOffset, height);
    }

    /**
     * Uploads the given texture data to this texture at the given mipmap level.
     *
     * @param level       the mipmap level
     * @param xOffset     the X offset in the texture
     * @param yOffset     the Y offset in the texture
     * @param width       the texture width
     * @param height      the texture height
     * @param pixelFormat the data format of the provided pixel data
     * @param pixelType   the data type of the provided pixel data
     * @param pixelData   the pixel data
     */
    public void texSubImage(@NotNegative int level, @NotNegative int xOffset, @NotNegative int yOffset, @NotNegative int width, @NotNegative int height, @NonNull PixelFormat pixelFormat, @NonNull PixelType pixelType, @NonNull ByteBuffer pixelData) {
        this.checkOpen();
        this.checkSubrectangle(level, xOffset, yOffset, width, height);

        if (this.dsa) {
            this.gl.glTextureSubImage2D(this.id, level, xOffset, yOffset, width, height, pixelFormat.id(), pixelType.id(), pixelData);
        } else {
            this.bind(target -> {
                this.gl.glTexSubImage2D(target.id(), level, xOffset, yOffset, width, height, pixelFormat.id(), pixelType.id(), pixelData);
                this.gl.glTexImage2D(target.id(), level, this.internalFormat.id(), width, height, pixelFormat.id(), pixelType.id(), pixelData);
            });
        }
    }
}
