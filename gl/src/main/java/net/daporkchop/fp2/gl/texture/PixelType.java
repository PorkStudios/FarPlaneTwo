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
public enum PixelType {
    UNSIGNED_BYTE(GL_UNSIGNED_BYTE),
    BYTE(GL_BYTE),
    UNSIGNED_SHORT(GL_UNSIGNED_SHORT),
    SHORT(GL_SHORT),
    UNSIGNED_INT(GL_UNSIGNED_INT),
    INT(GL_INT),
    HALF_FLOAT(GL_HALF_FLOAT),
    FLOAT(GL_FLOAT),
    UNSIGNED_BYTE_3_3_2(GL_UNSIGNED_BYTE_3_3_2),
    UNSIGNED_BYTE_2_3_3_REV(GL_UNSIGNED_BYTE_2_3_3_REV),
    UNSIGNED_SHORT_5_6_5(GL_UNSIGNED_SHORT_5_6_5),
    UNSIGNED_SHORT_5_6_5_REV(GL_UNSIGNED_SHORT_5_6_5_REV),
    UNSIGNED_SHORT_4_4_4_4(GL_UNSIGNED_SHORT_4_4_4_4),
    UNSIGNED_SHORT_4_4_4_4_REV(GL_UNSIGNED_SHORT_4_4_4_4_REV),
    UNSIGNED_SHORT_5_5_5_1(GL_UNSIGNED_SHORT_5_5_5_1),
    UNSIGNED_SHORT_1_5_5_5_REV(GL_UNSIGNED_SHORT_1_5_5_5_REV),
    UNSIGNED_INT_8_8_8_8(GL_UNSIGNED_INT_8_8_8_8),
    UNSIGNED_INT_8_8_8_8_REV(GL_UNSIGNED_INT_8_8_8_8_REV),
    UNSIGNED_INT_10_10_10_2(GL_UNSIGNED_INT_10_10_10_2),
    UNSIGNED_INT_2_10_10_10_REV(GL_UNSIGNED_INT_2_10_10_10_REV),
    ;

    private final int id;
}
