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
import lombok.RequiredArgsConstructor;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum TextureInternalFormat {
    R8(GL_R8),
    R16(GL_R16),
    R16F(GL_R16F),
    R32F(GL_R32F),
    R8I(GL_R8I),
    R16I(GL_R16I),
    R32I(GL_R32I),
    R8UI(GL_R8UI),
    R16UI(GL_R16UI),
    R32UI(GL_R32UI),
    RG8(GL_RG8),
    RG16(GL_RG16),
    RG16F(GL_RG16F),
    RG32F(GL_RG32F),
    RG8I(GL_RG8I),
    RG16I(GL_RG16I),
    RG32I(GL_RG32I),
    RG8UI(GL_RG8UI),
    RG16UI(GL_RG16UI),
    RG32UI(GL_RG32UI),
    RGB32F(GL_RGB32F),
    RGB32I(GL_RGB32I),
    RGB32UI(GL_RGB32UI),
    RGBA8(GL_RGBA8),
    RGBA16(GL_RGBA16),
    RGBA16F(GL_RGBA16F),
    RGBA32F(GL_RGBA32F),
    RGBA8I(GL_RGBA8I),
    RGBA16I(GL_RGBA16I),
    RGBA32I(GL_RGBA32I),
    RGBA8UI(GL_RGBA8UI),
    RGBA16UI(GL_RGBA16UI),
    RGBA32UI(GL_RGBA32UI),
    ;

    private final int id;
}
