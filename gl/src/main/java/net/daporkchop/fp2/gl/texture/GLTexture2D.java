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

import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.nio.ByteBuffer;

/**
 * @author DaPorkchop_
 */
public final class GLTexture2D extends GLSampledTexture {
    public static GLTexture2D create(OpenGL gl) {
        return new GLTexture2D(gl);
    }

    private GLTexture2D(OpenGL gl) {
        super(gl, TextureTarget.TEXTURE_2D);
    }

    /**
     * Uploads the given texture data to this texture at the given mipmap level.
     *
     * @param level          the mipmap level
     * @param width          the texture width
     * @param height         the texture height
     * @param internalFormat the internal format of the texture's backing data store
     * @param pixelFormat    the data format of the provided pixel data
     * @param pixelType      the data type of the provided pixel data
     * @param pixelData      the pixel data
     */
    public void texImage(@NotNegative int level, @NotNegative int width, @NotNegative int height, @NonNull TextureInternalFormat internalFormat, @NonNull PixelFormat pixelFormat, @NonNull PixelType pixelType, ByteBuffer pixelData) {
        this.checkOpen();

        this.bind(target -> {
            this.gl.glTexImage2D(target.id(), level, internalFormat.id(), width, height, pixelFormat.id(), pixelType.id(), pixelData);
        });
    }
}
