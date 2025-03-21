/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

import java.util.Arrays;

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
    R8(GL_R8, PixelComponentType.FLOAT, PixelFormat.RED),
    R16(GL_R16, PixelComponentType.FLOAT, PixelFormat.RED),
    RG8(GL_RG8, PixelComponentType.FLOAT, PixelFormat.RG),
    RG16(GL_RG16, PixelComponentType.FLOAT, PixelFormat.RG),
    RGBA8(GL_RGBA8, PixelComponentType.FLOAT, PixelFormat.RGBA),
    RGBA16(GL_RGBA16, PixelComponentType.FLOAT, PixelFormat.RGBA),

    R16F(GL_R16F, PixelComponentType.FLOAT, PixelFormat.RED),
    R32F(GL_R32F, PixelComponentType.FLOAT, PixelFormat.RED),
    RG16F(GL_RG16F, PixelComponentType.FLOAT, PixelFormat.RG),
    RG32F(GL_RG32F, PixelComponentType.FLOAT, PixelFormat.RG),
    RGB32F(GL_RGB32F, PixelComponentType.FLOAT, PixelFormat.RGB),
    RGBA16F(GL_RGBA16F, PixelComponentType.FLOAT, PixelFormat.RGBA),
    RGBA32F(GL_RGBA32F, PixelComponentType.FLOAT, PixelFormat.RGBA),

    R8I(GL_R8I, PixelComponentType.INT, PixelFormat.RED_INTEGER),
    R16I(GL_R16I, PixelComponentType.INT, PixelFormat.RED_INTEGER),
    R32I(GL_R32I, PixelComponentType.INT, PixelFormat.RED_INTEGER),
    RG8I(GL_RG8I, PixelComponentType.INT, PixelFormat.RG_INTEGER),
    RG16I(GL_RG16I, PixelComponentType.INT, PixelFormat.RG_INTEGER),
    RG32I(GL_RG32I, PixelComponentType.INT, PixelFormat.RG_INTEGER),
    RGB32I(GL_RGB32I, PixelComponentType.INT, PixelFormat.RGB_INTEGER),
    RGBA8I(GL_RGBA8I, PixelComponentType.INT, PixelFormat.RGBA_INTEGER),
    RGBA16I(GL_RGBA16I, PixelComponentType.INT, PixelFormat.RGBA_INTEGER),
    RGBA32I(GL_RGBA32I, PixelComponentType.INT, PixelFormat.RGBA_INTEGER),

    R8UI(GL_R8UI, PixelComponentType.UINT, PixelFormat.RED_INTEGER),
    R16UI(GL_R16UI, PixelComponentType.UINT, PixelFormat.RED_INTEGER),
    R32UI(GL_R32UI, PixelComponentType.UINT, PixelFormat.RED_INTEGER),
    RG8UI(GL_RG8UI, PixelComponentType.UINT, PixelFormat.RG_INTEGER),
    RG16UI(GL_RG16UI, PixelComponentType.UINT, PixelFormat.RG_INTEGER),
    RG32UI(GL_RG32UI, PixelComponentType.UINT, PixelFormat.RG_INTEGER),
    RGB32UI(GL_RGB32UI, PixelComponentType.UINT, PixelFormat.RGB_INTEGER),
    RGBA8UI(GL_RGBA8UI, PixelComponentType.UINT, PixelFormat.RGBA_INTEGER),
    RGBA16UI(GL_RGBA16UI, PixelComponentType.UINT, PixelFormat.RGBA_INTEGER),
    RGBA32UI(GL_RGBA32UI, PixelComponentType.UINT, PixelFormat.RGBA_INTEGER),

    //
    // DEPTH FORMATS
    //
    DEPTH_COMPONENT_16(GL_DEPTH_COMPONENT16, PixelComponentType.FLOAT, PixelFormat.DEPTH_COMPONENT),
    DEPTH_COMPONENT_24(GL_DEPTH_COMPONENT24, PixelComponentType.FLOAT, PixelFormat.DEPTH_COMPONENT),
    DEPTH_COMPONENT_32(GL_DEPTH_COMPONENT32, PixelComponentType.FLOAT, PixelFormat.DEPTH_COMPONENT),
    DEPTH_COMPONENT_32F(GL_DEPTH_COMPONENT32F, PixelComponentType.FLOAT, PixelFormat.DEPTH_COMPONENT),

    //
    // STENCIL FORMATS
    //
    STENCIL_INDEX1(GL_STENCIL_INDEX1, PixelComponentType.UINT, PixelFormat.STENCIL_INDEX),
    STENCIL_INDEX4(GL_STENCIL_INDEX4, PixelComponentType.UINT, PixelFormat.STENCIL_INDEX),
    STENCIL_INDEX8(GL_STENCIL_INDEX8, PixelComponentType.UINT, PixelFormat.STENCIL_INDEX),
    STENCIL_INDEX16(GL_STENCIL_INDEX16, PixelComponentType.UINT, PixelFormat.STENCIL_INDEX),

    //
    // DEPTH+STENCIL FORMATS
    //
    //DEPTH24_STENCIL8(GL_DEPTH24_STENCIL8, PixelFormat.DEPTH_STENCIL),
    //DEPTH32F_STENCIL8(GL_DEPTH32F_STENCIL8, PixelFormat.DEPTH_STENCIL),
    ;

    public static TextureInternalFormat[] colorFormats() {
        return Arrays.copyOf(values(), DEPTH_COMPONENT_16.ordinal());
    }

    public static TextureInternalFormat[] colorFormatsFloat() {
        return Arrays.copyOf(values(), R8I.ordinal());
    }

    public static TextureInternalFormat[] colorFormatsInteger() {
        return Arrays.copyOfRange(values(), R8I.ordinal(), DEPTH_COMPONENT_16.ordinal());
    }

    private final int id;

    /**
     * The component type for samplers/images accessing a texture with this internal format.
     */
    private final PixelComponentType sampledType;

    private final PixelFormat defaultFormat;
}
