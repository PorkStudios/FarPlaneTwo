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

package net.daporkchop.fp2.gl;

import lombok.Getter;
import net.daporkchop.lib.common.misc.string.PUnsafeStrings;

/**
 * All known OpenGL versions.
 *
 * @author DaPorkchop_
 */
@Getter
public enum GLVersion {
    GL_1_0(1, 0),
    GL_1_1(1, 1),
    GL_1_2(1, 2),
    GL_1_3(1, 3),
    GL_1_4(1, 4),
    GL_1_5(1, 5),
    GL_2_0(2, 0),
    GL_2_1(2, 1),
    GL_3_0(3, 0),
    GL_3_1(3, 1),
    GL_3_2(3, 2),
    GL_3_3(3, 3),
    GL_4_0(4, 0),
    GL_4_1(4, 1),
    GL_4_2(4, 2),
    GL_4_3(4, 3),
    GL_4_4(4, 4),
    GL_4_5(4, 5),
    GL_4_6(4, 6);

    private final int major;
    private final int minor;

    GLVersion(int major, int minor) {
        PUnsafeStrings.setEnumName(this, "OpenGL " + major + '.' + minor);

        this.major = major;
        this.minor = minor;
    }
}
