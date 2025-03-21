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
public enum PixelFormat {
    RED(GL_RED, PixelKind.COLOR, false, 1),
    RG(GL_RG,PixelKind.COLOR, false, 2),
    RGB(GL_RGB, PixelKind.COLOR, false, 3),
    BGR(GL_BGR, PixelKind.COLOR, false, 3),
    RGBA(GL_RGBA, PixelKind.COLOR, false, 4),
    BGRA(GL_BGRA, PixelKind.COLOR, false, 4),
    
    RED_INTEGER(GL_RED_INTEGER, PixelKind.COLOR, true, 1),
    RG_INTEGER(GL_RG_INTEGER, PixelKind.COLOR, true, 2),
    RGB_INTEGER(GL_RGB_INTEGER, PixelKind.COLOR, true, 3),
    BGR_INTEGER(GL_BGR_INTEGER, PixelKind.COLOR, true, 3),
    RGBA_INTEGER(GL_RGBA_INTEGER, PixelKind.COLOR, true, 4),
    BGRA_INTEGER(GL_BGRA_INTEGER, PixelKind.COLOR, true, 4),
    
    DEPTH_COMPONENT(GL_DEPTH_COMPONENT, PixelKind.DEPTH, false, 1),
    
    STENCIL_INDEX(GL_STENCIL_INDEX, PixelKind.STENCIL, true, 1), //requires OpenGL 4.4
    
    //DEPTH_STENCIL(GL_DEPTH_STENCIL),
    ;

    public static PixelFormat[] colorFormats() {
        return Arrays.copyOf(values(), DEPTH_COMPONENT.ordinal());
    }

    public static PixelFormat[] colorFormatsFloat() {
        return Arrays.copyOf(values(), RED_INTEGER.ordinal());
    }

    public static PixelFormat[] colorFormatsInteger() {
        return Arrays.copyOfRange(values(), RED_INTEGER.ordinal(), DEPTH_COMPONENT.ordinal());
    }

    private final int id;
    
    private final PixelKind kind;
    
    private final boolean integer;
    private final int components;
}
