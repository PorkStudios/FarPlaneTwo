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
    //
    // COLOR FORMATS
    //
    R8(GL_R8, PixelFormat.RED),
    R16(GL_R16, PixelFormat.RED),
    R16F(GL_R16F, PixelFormat.RED),
    R32F(GL_R32F, PixelFormat.RED),
    R8I(GL_R8I, PixelFormat.RED_INTEGER),
    R16I(GL_R16I, PixelFormat.RED_INTEGER),
    R32I(GL_R32I, PixelFormat.RED_INTEGER),
    R8UI(GL_R8UI, PixelFormat.RED_INTEGER),
    R16UI(GL_R16UI, PixelFormat.RED_INTEGER),
    R32UI(GL_R32UI, PixelFormat.RED_INTEGER),
    RG8(GL_RG8, PixelFormat.RG),
    RG16(GL_RG16, PixelFormat.RG),
    RG16F(GL_RG16F, PixelFormat.RG),
    RG32F(GL_RG32F, PixelFormat.RG),
    RG8I(GL_RG8I, PixelFormat.RG_INTEGER),
    RG16I(GL_RG16I, PixelFormat.RG_INTEGER),
    RG32I(GL_RG32I, PixelFormat.RG_INTEGER),
    RG8UI(GL_RG8UI, PixelFormat.RG_INTEGER),
    RG16UI(GL_RG16UI, PixelFormat.RG_INTEGER),
    RG32UI(GL_RG32UI, PixelFormat.RG_INTEGER),
    RGB32F(GL_RGB32F, PixelFormat.RGB),
    RGB32I(GL_RGB32I, PixelFormat.RGB_INTEGER),
    RGB32UI(GL_RGB32UI, PixelFormat.RGB_INTEGER),
    RGBA8(GL_RGBA8, PixelFormat.RGBA),
    RGBA16(GL_RGBA16, PixelFormat.RGBA),
    RGBA16F(GL_RGBA16F, PixelFormat.RGBA),
    RGBA32F(GL_RGBA32F, PixelFormat.RGBA),
    RGBA8I(GL_RGBA8I, PixelFormat.RGBA_INTEGER),
    RGBA16I(GL_RGBA16I, PixelFormat.RGBA_INTEGER),
    RGBA32I(GL_RGBA32I, PixelFormat.RGBA_INTEGER),
    RGBA8UI(GL_RGBA8UI, PixelFormat.RGBA_INTEGER),
    RGBA16UI(GL_RGBA16UI, PixelFormat.RGBA_INTEGER),
    RGBA32UI(GL_RGBA32UI, PixelFormat.RGBA_INTEGER),

    //
    // DEPTH FORMATS
    //
    DEPTH_COMPONENT_16(GL_DEPTH_COMPONENT16, PixelFormat.DEPTH_COMPONENT),
    DEPTH_COMPONENT_24(GL_DEPTH_COMPONENT24, PixelFormat.DEPTH_COMPONENT),
    DEPTH_COMPONENT_32(GL_DEPTH_COMPONENT32, PixelFormat.DEPTH_COMPONENT),
    DEPTH_COMPONENT_32F(GL_DEPTH_COMPONENT32F, PixelFormat.DEPTH_COMPONENT),

    //
    // STENCIL FORMATS
    //
    STENCIL_INDEX1(GL_STENCIL_INDEX1, PixelFormat.STENCIL_INDEX),
    STENCIL_INDEX4(GL_STENCIL_INDEX4, PixelFormat.STENCIL_INDEX),
    STENCIL_INDEX8(GL_STENCIL_INDEX8, PixelFormat.STENCIL_INDEX),
    STENCIL_INDEX16(GL_STENCIL_INDEX16, PixelFormat.STENCIL_INDEX),

    //
    // DEPTH+STENCIL FORMATS
    //
    DEPTH24_STENCIL8(GL_DEPTH24_STENCIL8, PixelFormat.DEPTH_STENCIL),
    DEPTH32F_STENCIL8(GL_DEPTH32F_STENCIL8, PixelFormat.DEPTH_STENCIL),
    ;

    private final int id;
    private final PixelFormat defaultFormat;
}
