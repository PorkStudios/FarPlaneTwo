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
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL33;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Methods for encoding/decoding the OpenGL {@link GL33#GL_INT_2_10_10_10_REV} and {@link GL12#GL_UNSIGNED_INT_2_10_10_10_REV} vertex type.
 *
 * @author DaPorkchop_
 * @see <a href="https://www.khronos.org/opengl/wiki/Vertex_Specification#Component_type">OpenGL Vertex Specification</a>
 */
@UtilityClass
public class Int2_10_10_10_Rev {
    protected static final int BITS_XYZ = 10;
    protected static final int MASK_XYZ = (1 << BITS_XYZ) - 1;
    protected static final int SHIFT_XYZ_SIGN_EXTEND = 32 - BITS_XYZ;

    protected static final int BITS_W = 2;
    protected static final int MASK_W = (1 << BITS_W) - 1;
    protected static final int SHIFT_W_SIGN_EXTEND = 32 - BITS_W;

    protected static final int DIRECT_SHIFT_X = BITS_XYZ * 0;
    protected static final int DIRECT_SHIFT_Y = BITS_XYZ * 1;
    protected static final int DIRECT_SHIFT_Z = BITS_XYZ * 2;
    protected static final int DIRECT_SHIFT_W = BITS_XYZ * 3;

    protected static final int EXTEND_SHIFT_X = 32 - BITS_XYZ * 1;
    protected static final int EXTEND_SHIFT_Y = 32 - BITS_XYZ * 2;
    protected static final int EXTEND_SHIFT_Z = 32 - BITS_XYZ * 3;
    protected static final int EXTEND_SHIFT_W = 0;

    public static final int MIN_XYZ_VALUE = -(1 << (BITS_XYZ - 1));
    public static final int MAX_XYZ_VALUE = (1 << (BITS_XYZ - 1)) - 1;

    public static int checkXYZ(int val, @NonNull String name) {
        checkArg(val << SHIFT_XYZ_SIGN_EXTEND >> SHIFT_XYZ_SIGN_EXTEND == val, name);
        return val;
    }

    public static int checkUnsignedXYZ(int val, @NonNull String name) {
        int masked = val & MASK_XYZ;
        checkArg(masked == val, name);
        return masked;
    }

    public static int checkW(int val) {
        checkArg(val << SHIFT_W_SIGN_EXTEND >> SHIFT_W_SIGN_EXTEND == val, "w");
        return val;
    }

    public static int checkUnsignedW(int val) {
        int masked = val & MASK_W;
        checkArg(masked == val, "w");
        return masked;
    }

    public static int packXYZ(int x, int y, int z) {
        return (checkXYZ(x, "x") << SHIFT_XYZ_SIGN_EXTEND >>> EXTEND_SHIFT_X)
               | (checkXYZ(y, "y") << SHIFT_XYZ_SIGN_EXTEND >>> EXTEND_SHIFT_Y)
               | (checkXYZ(z, "z") << SHIFT_XYZ_SIGN_EXTEND >>> EXTEND_SHIFT_Z);
    }

    public static int packXYZW(int x, int y, int z, int w) {
        return packXYZ(x, y, z) | (checkW(w) << SHIFT_W_SIGN_EXTEND >> EXTEND_SHIFT_W);
    }

    public static int packUnsignedXYZ(int x, int y, int z) {
        return (checkUnsignedXYZ(x, "x") << DIRECT_SHIFT_X)
               | (checkUnsignedXYZ(y, "y") << DIRECT_SHIFT_Y)
               | (checkUnsignedXYZ(z, "z") << DIRECT_SHIFT_Z);
    }

    public static int packUnsignedXYZW(int x, int y, int z, int w) {
        return packUnsignedXYZ(x, y, z) | (checkUnsignedW(w) << DIRECT_SHIFT_W);
    }

    public static int packUnsignedXYZUnsafe(int x, int y, int z) {
        return ((x & MASK_XYZ) << DIRECT_SHIFT_X)
               | ((y & MASK_XYZ) << DIRECT_SHIFT_Y)
               | ((z & MASK_XYZ) << DIRECT_SHIFT_Z);
    }

    public static int packUnsignedXYZWUnsafe(int x, int y, int z, int w) {
        return packUnsignedXYZUnsafe(x, y, z) | ((w & MASK_W) << DIRECT_SHIFT_W);
    }

    public static int unpackX(int point) {
        return point << EXTEND_SHIFT_X >> SHIFT_XYZ_SIGN_EXTEND;
    }

    public static int unpackY(int point) {
        return point << EXTEND_SHIFT_Y >> SHIFT_XYZ_SIGN_EXTEND;
    }

    public static int unpackZ(int point) {
        return point << EXTEND_SHIFT_Z >> SHIFT_XYZ_SIGN_EXTEND;
    }

    public static int unpackW(int point) {
        return point << EXTEND_SHIFT_W >> SHIFT_W_SIGN_EXTEND;
    }

    public static int unpackUnsignedX(int point) {
        return (point >>> DIRECT_SHIFT_X) & MASK_XYZ;
    }

    public static int unpackUnsignedY(int point) {
        return (point >>> DIRECT_SHIFT_Y) & MASK_XYZ;
    }

    public static int unpackUnsignedZ(int point) {
        return (point >>> DIRECT_SHIFT_Z) & MASK_XYZ;
    }

    public static int unpackUnsignedW(int point) {
        return (point >>> DIRECT_SHIFT_W) & MASK_W;
    }
}
