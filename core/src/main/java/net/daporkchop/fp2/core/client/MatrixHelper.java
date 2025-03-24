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

package net.daporkchop.fp2.core.client;

import lombok.experimental.UtilityClass;
import lombok.val;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper methods for dealing with OpenGL matrices.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class MatrixHelper {
    public static final int MAT4_ELEMENTS = 16;

    public static final int MAT4_SIZE = 16 * FLOAT_SIZE;

    static {
        //float array must be tightly packed so that we can write to them using unsafe
        PUnsafe.requireTightlyPackedFloatArrays();
    }

    private static int matrixIndex(int x, int y) {
        assert x >= 0 && x < 4 && y >= 0 && y < 4 : "x=" + x + ", y=" + y;
        return x * 4 + y;
    }

    private static long matrixOffset(int x, int y) {
        return (long) matrixIndex(x, y) * FLOAT_SIZE;
    }

    public static void reversedZ(float[] dst, float fovy, float aspect, float zNear) {
        reversedZ(dst, PUnsafe.arrayFloatBaseOffset(), fovy, aspect, zNear);
    }

    public static void reversedZ(FloatBuffer dst, float fovy, float aspect, float zNear) {
        if (dst.hasArray()) {
            reversedZ(dst.array(), PUnsafe.arrayFloatElementOffset(dst.arrayOffset()), fovy, aspect, zNear);
        } else {
            reversedZ(null, PUnsafe.pork_directBufferAddress(dst), fovy, aspect, zNear);
        }
    }

    private static void reversedZ(Object base, long offset, float fovy, float aspect, float zNear) {
        //from http://dev.theomader.com/depth-precision/
        float f = (float) (1.0d / Math.tan(Math.toRadians(fovy) * 0.5d));

        PUnsafe.setMemory(base, offset, MAT4_SIZE, (byte) 0);

        val reversedZ = fp2().client().renderManager().reversedZ();
        if (reversedZ != null && reversedZ.isActive()) { //if reversed-Z projection is enabled, we need to reverse the depth buffer
            PUnsafe.putFloat(base, offset + matrixOffset(0, 0), f / aspect);
            PUnsafe.putFloat(base, offset + matrixOffset(1, 1), f);
            PUnsafe.putFloat(base, offset + matrixOffset(3, 2), zNear);
            PUnsafe.putFloat(base, offset + matrixOffset(2, 3), -1.0f);
        } else { //otherwise, use normal perspective projection - but with infinite zFar
            PUnsafe.putFloat(base, offset + matrixOffset(0, 0), f / aspect);
            PUnsafe.putFloat(base, offset + matrixOffset(1, 1), f);
            PUnsafe.putFloat(base, offset + matrixOffset(2, 2), -1.0f);
            PUnsafe.putFloat(base, offset + matrixOffset(3, 2), -zNear);
            PUnsafe.putFloat(base, offset + matrixOffset(2, 3), -1.0f);
        }
    }

    public static void multiply4x4(float[] a, float[] b, float[] dst) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(a.length >= MAT4_ELEMENTS && b.length >= MAT4_ELEMENTS && dst.length >= MAT4_ELEMENTS);

        float a00 = a[matrixIndex(0, 0)], a01 = a[matrixIndex(0, 1)], a02 = a[matrixIndex(0, 2)], a03 = a[matrixIndex(0, 3)];
        float a10 = a[matrixIndex(1, 0)], a11 = a[matrixIndex(1, 1)], a12 = a[matrixIndex(1, 2)], a13 = a[matrixIndex(1, 3)];
        float a20 = a[matrixIndex(2, 0)], a21 = a[matrixIndex(2, 1)], a22 = a[matrixIndex(2, 2)], a23 = a[matrixIndex(2, 3)];
        float a30 = a[matrixIndex(3, 0)], a31 = a[matrixIndex(3, 1)], a32 = a[matrixIndex(3, 2)], a33 = a[matrixIndex(3, 3)];

        float b00 = b[matrixIndex(0, 0)], b01 = b[matrixIndex(0, 1)], b02 = b[matrixIndex(0, 2)], b03 = b[matrixIndex(0, 3)];
        float b10 = b[matrixIndex(1, 0)], b11 = b[matrixIndex(1, 1)], b12 = b[matrixIndex(1, 2)], b13 = b[matrixIndex(1, 3)];
        float b20 = b[matrixIndex(2, 0)], b21 = b[matrixIndex(2, 1)], b22 = b[matrixIndex(2, 2)], b23 = b[matrixIndex(2, 3)];
        float b30 = b[matrixIndex(3, 0)], b31 = b[matrixIndex(3, 1)], b32 = b[matrixIndex(3, 2)], b33 = b[matrixIndex(3, 3)];

        dst[matrixIndex(0, 0)] = b00 * a00 + b01 * a10 + b02 * a20 + b03 * a30;
        dst[matrixIndex(0, 1)] = b00 * a01 + b01 * a11 + b02 * a21 + b03 * a31;
        dst[matrixIndex(0, 2)] = b00 * a02 + b01 * a12 + b02 * a22 + b03 * a32;
        dst[matrixIndex(0, 3)] = b00 * a03 + b01 * a13 + b02 * a23 + b03 * a33;
        dst[matrixIndex(1, 0)] = b10 * a00 + b11 * a10 + b12 * a20 + b13 * a30;
        dst[matrixIndex(1, 1)] = b10 * a01 + b11 * a11 + b12 * a21 + b13 * a31;
        dst[matrixIndex(1, 2)] = b10 * a02 + b11 * a12 + b12 * a22 + b13 * a32;
        dst[matrixIndex(1, 3)] = b10 * a03 + b11 * a13 + b12 * a23 + b13 * a33;
        dst[matrixIndex(2, 0)] = b20 * a00 + b21 * a10 + b22 * a20 + b23 * a30;
        dst[matrixIndex(2, 1)] = b20 * a01 + b21 * a11 + b22 * a21 + b23 * a31;
        dst[matrixIndex(2, 2)] = b20 * a02 + b21 * a12 + b22 * a22 + b23 * a32;
        dst[matrixIndex(2, 3)] = b20 * a03 + b21 * a13 + b22 * a23 + b23 * a33;
        dst[matrixIndex(3, 0)] = b30 * a00 + b31 * a10 + b32 * a20 + b33 * a30;
        dst[matrixIndex(3, 1)] = b30 * a01 + b31 * a11 + b32 * a21 + b33 * a31;
        dst[matrixIndex(3, 2)] = b30 * a02 + b31 * a12 + b32 * a22 + b33 * a32;
        dst[matrixIndex(3, 3)] = b30 * a03 + b31 * a13 + b32 * a23 + b33 * a33;
    }

    public static void offsetDepth(float[] mat, float offset) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(mat.length >= MAT4_ELEMENTS);

        //multiply offset by w to work around division by w on GPU
        float w = mat[matrixIndex(0, 3)] + mat[matrixIndex(1, 3)] + mat[matrixIndex(2, 3)] + mat[matrixIndex(3, 3)];
        mat[matrixIndex(3, 2)] += offset * Math.abs(w);
    }

    public static void flip(float[] mat) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(mat.length >= MAT4_ELEMENTS);

        //swap opposite pairs
        PArrays.swap(mat, matrixIndex(0, 1), matrixIndex(1, 0));
        PArrays.swap(mat, matrixIndex(0, 2), matrixIndex(2, 0));
        PArrays.swap(mat, matrixIndex(0, 3), matrixIndex(3, 0));
        PArrays.swap(mat, matrixIndex(1, 2), matrixIndex(2, 1));
        PArrays.swap(mat, matrixIndex(1, 3), matrixIndex(3, 1));
        PArrays.swap(mat, matrixIndex(2, 3), matrixIndex(3, 2));
    }

    /**
     * Copies a 4x4 matrix from the given source array into the given destination array.
     *
     * @param src the source array
     * @param dst the destination array
     */
    public static void copy4x4(float[] src, float[] dst) {
        //this should be able to handle all bounds checks and whatnot better than i could
        System.arraycopy(src, 0, dst, 0, MAT4_ELEMENTS);
    }
}
