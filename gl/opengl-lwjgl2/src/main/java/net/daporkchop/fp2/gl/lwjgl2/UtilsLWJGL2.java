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

package net.daporkchop.fp2.gl.lwjgl2;

import lombok.experimental.UtilityClass;

import static org.lwjgl.opengl.EXTAbgr.*;
import static org.lwjgl.opengl.EXTBgra.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class UtilsLWJGL2 {
    //
    // copied from GLChecks
    //

    /**
     * Calculate the storage required for an image in bytes.
     *
     * @param format The format of the image (example: GL_RGBA)
     * @param type   The type of the image elements (example: GL_UNSIGNED_BYTE)
     * @param width  The width of the image
     * @param height The height of the image (1 for 1D images)
     * @param depth  The depth of the image (1 for 2D images)
     * @return the size, in bytes, of the image
     */
    public static int calculateImageStorage(int format, int type, int width, int height, int depth) {
        return calculateBytesPerPixel(format, type) * width * height * depth;
    }

    public static int calculateTexImage1DStorage(int format, int type, int width) {
        return calculateBytesPerPixel(format, type) * width;
    }

    public static int calculateTexImage2DStorage(int format, int type, int width, int height) {
        return calculateTexImage1DStorage(format, type, width) * height;
    }

    public static int calculateTexImage3DStorage(int format, int type, int width, int height, int depth) {
        return calculateTexImage2DStorage(format, type, width, height) * depth;
    }

    public static int calculateBytesPerPixel(int format, int type) {
        int bpe;
        switch (type) {
            case GL_UNSIGNED_BYTE:
            case GL_BYTE:
                bpe = 1;
                break;
            case GL_UNSIGNED_SHORT:
            case GL_SHORT:
                bpe = 2;
                break;
            case GL_UNSIGNED_INT:
            case GL_INT:
            case GL_FLOAT:
                bpe = 4;
                break;
            default:
                // TODO: Add more types (like the GL12 types GL_UNSIGNED_INT_8_8_8_8
                return 0;
            //		throw new IllegalArgumentException("Unknown type " + type);
        }
        int epp;
        switch (format) {
            case GL_LUMINANCE:
            case GL_ALPHA:
                epp = 1;
                break;

            case GL_LUMINANCE_ALPHA:
                epp = 2;
                break;
            case GL_RGB:
            case GL_BGR_EXT:
                epp = 3;
                break;
            case GL_RGBA:
            case GL_ABGR_EXT:
            case GL_BGRA_EXT:
                epp = 4;
                break;
            default:
                // TODO: Add more formats. Assuming 4 is too wasteful on buffer sizes where e.g. 1 is enough (like GL_DEPTH_COMPONENT)
                return 0;
/*				// Assume 4 elements per pixel
				epp = 4;*/
        }

        return bpe * epp;
    }
}
