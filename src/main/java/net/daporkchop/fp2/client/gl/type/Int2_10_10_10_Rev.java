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

package net.daporkchop.fp2.client.gl.type;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Methods for encoding/decoding the OpenGL {@code GL_INT_2_10_10_10_REV} vertex type.
 * <p>
 * The alpha component is not used by this implementation.
 *
 * @author DaPorkchop_
 * @see <a href="https://www.khronos.org/opengl/wiki/Vertex_Specification#Component_type">OpenGL Vertex Specification</a>
 */
@UtilityClass
public class Int2_10_10_10_Rev {
    protected static final int BITS_PER_AXIS = 10;
    protected static final int SHIFT_SIGN_EXTEND = 32 - BITS_PER_AXIS;

    public static final int MIN_AXIS_VALUE = -(1 << (BITS_PER_AXIS - 1));
    public static final int MAX_AXIS_VALUE = (1 << (BITS_PER_AXIS - 1)) - 1;

    protected static final int SHIFT_X = 32 - BITS_PER_AXIS * 1;
    protected static final int SHIFT_Y = 32 - BITS_PER_AXIS * 2;
    protected static final int SHIFT_Z = 32 - BITS_PER_AXIS * 3;

    public static int checkAxis(int val, @NonNull String name) {
        checkArg(val << SHIFT_SIGN_EXTEND >> SHIFT_SIGN_EXTEND == val, name);
        return val;
    }

    public static int packCoords(int x, int y, int z) {
        return (checkAxis(x, "x") << SHIFT_SIGN_EXTEND >>> SHIFT_X)
               | (checkAxis(y, "y") << SHIFT_SIGN_EXTEND >>> SHIFT_Y)
               | (checkAxis(z, "z") << SHIFT_SIGN_EXTEND >>> SHIFT_Z);
    }

    public static int unpackX(int point) {
        return point << SHIFT_X >> SHIFT_SIGN_EXTEND;
    }

    public static int unpackY(int point) {
        return point << SHIFT_Y >> SHIFT_SIGN_EXTEND;
    }

    public static int unpackZ(int point) {
        return point << SHIFT_Z >> SHIFT_SIGN_EXTEND;
    }
}
