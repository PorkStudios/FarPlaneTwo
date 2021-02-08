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

package net.daporkchop.fp2.client.gl;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.Constants;
import org.lwjgl.LWJGLUtil;

import java.util.StringTokenizer;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class OpenGL {
    public final int BYTE_SIZE = Byte.BYTES;
    public final int SHORT_SIZE = Short.BYTES;
    public final int MEDIUM_SIZE = SHORT_SIZE + BYTE_SIZE;
    public final int FLOAT_SIZE = Float.BYTES;
    public final int INT_SIZE = Integer.BYTES;
    public final int DOUBLE_SIZE = Double.BYTES;
    public final int LONG_SIZE = Long.BYTES;

    public final int VEC2_ELEMENTS = 2;
    public final int VEC2_SIZE = VEC2_ELEMENTS * FLOAT_SIZE;
    public final int IVEC2_SIZE = VEC2_ELEMENTS * INT_SIZE;
    public final int DVEC2_SIZE = VEC2_ELEMENTS * DOUBLE_SIZE;

    public final int VEC3_ELEMENTS = 3;
    public final int VEC3_SIZE_TIGHT = VEC3_ELEMENTS * FLOAT_SIZE;
    public final int IVEC3_SIZE_TIGHT = VEC3_ELEMENTS * INT_SIZE;
    public final int DVEC3_SIZE_TIGHT = VEC3_ELEMENTS * DOUBLE_SIZE;
    public final int VEC3_SIZE = (VEC3_ELEMENTS + 1) * FLOAT_SIZE;
    public final int IVEC3_SIZE = (VEC3_ELEMENTS + 1) * INT_SIZE;
    public final int DVEC3_SIZE = (VEC3_ELEMENTS + 1) * DOUBLE_SIZE;

    public final int VEC4_ELEMENTS = 4;
    public final int VEC4_SIZE = VEC4_ELEMENTS * FLOAT_SIZE;
    public final int IVEC4_SIZE = VEC4_ELEMENTS * INT_SIZE;
    public final int DVEC4_SIZE = VEC4_ELEMENTS * DOUBLE_SIZE;

    public final int MAT4_ELEMENTS = 4 * 4;
    public final int MAT4_SIZE = MAT4_ELEMENTS * FLOAT_SIZE;

    public static void checkGLError(String message) {
        if (FP2_DEBUG) {
            for (int error; (error = glGetError()) != GL_NO_ERROR; ) {
                Constants.LOGGER.error("########## GL ERROR ##########");
                Constants.LOGGER.error("@ {}", message);
                Constants.LOGGER.error("{}: {}", error, gluErrorString(error));
            }
        }
    }

    public static boolean isOpenGL46() {
        //ContextCapabilities only knows versions up to 4.5...
        //copypasta'd from GLContext.getSupportedExtensions()
        String version = glGetString(GL_VERSION);

        StringTokenizer version_tokenizer = new StringTokenizer(version, ". ");
        int majorVersion  = Integer.parseInt(version_tokenizer.nextToken());
        int minorVersion = Integer.parseInt(version_tokenizer.nextToken());

        if (majorVersion == 4) {
            return minorVersion >= 6;
        } else {
            return majorVersion > 4;
        }
    }
}
