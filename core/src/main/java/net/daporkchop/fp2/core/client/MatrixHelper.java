/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.client;

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.FloatBuffer;

import static java.lang.Math.*;
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
    public final int MAT4_ELEMENTS = 16;

    public final int MAT4_SIZE = 16 * FLOAT_SIZE;

    public int matrixIndex(int x, int y) {
        return x * 4 + y;
    }

    private void swap(float[] arr, int i0, int i1) {
        float t = arr[i0];
        arr[i0] = arr[i1];
        arr[i1] = t;
    }

    public long matrixOffset(int x, int y) {
        return (long) matrixIndex(x, y) * FLOAT_SIZE;
    }

    public void reversedZ(float[] dst, float fovy, float aspect, float zNear) {
        reversedZ(dst, PUnsafe.ARRAY_FLOAT_BASE_OFFSET, fovy, aspect, zNear);
    }

    public void reversedZ(FloatBuffer dst, float fovy, float aspect, float zNear) {
        if (dst.hasArray()) {
            reversedZ(dst.array(), PUnsafe.arrayFloatElementOffset(dst.arrayOffset()), fovy, aspect, zNear);
        } else {
            reversedZ(null, PUnsafe.pork_directBufferAddress(dst), fovy, aspect, zNear);
        }
    }

    public void reversedZ(Object base, long offset, float fovy, float aspect, float zNear) {
        //from http://dev.theomader.com/depth-precision/
        float radians = (float) Math.toRadians(fovy);
        float f = 1.0f / (float) Math.tan(radians * 0.5f);

        PUnsafe.setMemory(base, offset, MAT4_SIZE, (byte) 0);

        if (fp2().client().isReverseZ()) { //we are currently rendering the world, so we need to reverse the depth buffer
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

    public void multiply4x4(float[] a, float[] b, float[] dst) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(a.length >= MAT4_ELEMENTS && b.length >= MAT4_ELEMENTS && dst.length >= MAT4_ELEMENTS);

        //parentheses are to allow out-of-order execution to make more of a difference due to the JVM's strict floating-point precision rules
        dst[0] = (b[0] * a[0] + b[1] * a[4]) + (b[2] * a[8] + b[3] * a[12]);
        dst[1] = (b[0] * a[1] + b[1] * a[5]) + (b[2] * a[9] + b[3] * a[13]);
        dst[2] = (b[0] * a[2] + b[1] * a[6]) + (b[2] * a[10] + b[3] * a[14]);
        dst[3] = (b[0] * a[3] + b[1] * a[7]) + (b[2] * a[11] + b[3] * a[15]);
        dst[4] = (b[4] * a[0] + b[5] * a[4]) + (b[6] * a[8] + b[7] * a[12]);
        dst[5] = (b[4] * a[1] + b[5] * a[5]) + (b[6] * a[9] + b[7] * a[13]);
        dst[6] = (b[4] * a[2] + b[5] * a[6]) + (b[6] * a[10] + b[7] * a[14]);
        dst[7] = (b[4] * a[3] + b[5] * a[7]) + (b[6] * a[11] + b[7] * a[15]);
        dst[8] = (b[8] * a[0] + b[9] * a[4]) + (b[10] * a[8] + b[11] * a[12]);
        dst[9] = (b[8] * a[1] + b[9] * a[5]) + (b[10] * a[9] + b[11] * a[13]);
        dst[10] = (b[8] * a[2] + b[9] * a[6]) + (b[10] * a[10] + b[11] * a[14]);
        dst[11] = (b[8] * a[3] + b[9] * a[7]) + (b[10] * a[11] + b[11] * a[15]);
        dst[12] = (b[12] * a[0] + b[13] * a[4]) + (b[14] * a[8] + b[15] * a[12]);
        dst[13] = (b[12] * a[1] + b[13] * a[5]) + (b[14] * a[9] + b[15] * a[13]);
        dst[14] = (b[12] * a[2] + b[13] * a[6]) + (b[14] * a[10] + b[15] * a[14]);
        dst[15] = (b[12] * a[3] + b[13] * a[7]) + (b[14] * a[11] + b[15] * a[15]);
    }

    public void multiply4x4(double[] a, double[] b, double[] dst) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(a.length >= MAT4_ELEMENTS && b.length >= MAT4_ELEMENTS && dst.length >= MAT4_ELEMENTS);

        //parentheses are to allow out-of-order execution to make more of a difference due to the JVM's strict floating-point precision rules
        dst[0] = (b[0] * a[0] + b[1] * a[4]) + (b[2] * a[8] + b[3] * a[12]);
        dst[1] = (b[0] * a[1] + b[1] * a[5]) + (b[2] * a[9] + b[3] * a[13]);
        dst[2] = (b[0] * a[2] + b[1] * a[6]) + (b[2] * a[10] + b[3] * a[14]);
        dst[3] = (b[0] * a[3] + b[1] * a[7]) + (b[2] * a[11] + b[3] * a[15]);
        dst[4] = (b[4] * a[0] + b[5] * a[4]) + (b[6] * a[8] + b[7] * a[12]);
        dst[5] = (b[4] * a[1] + b[5] * a[5]) + (b[6] * a[9] + b[7] * a[13]);
        dst[6] = (b[4] * a[2] + b[5] * a[6]) + (b[6] * a[10] + b[7] * a[14]);
        dst[7] = (b[4] * a[3] + b[5] * a[7]) + (b[6] * a[11] + b[7] * a[15]);
        dst[8] = (b[8] * a[0] + b[9] * a[4]) + (b[10] * a[8] + b[11] * a[12]);
        dst[9] = (b[8] * a[1] + b[9] * a[5]) + (b[10] * a[9] + b[11] * a[13]);
        dst[10] = (b[8] * a[2] + b[9] * a[6]) + (b[10] * a[10] + b[11] * a[14]);
        dst[11] = (b[8] * a[3] + b[9] * a[7]) + (b[10] * a[11] + b[11] * a[15]);
        dst[12] = (b[12] * a[0] + b[13] * a[4]) + (b[14] * a[8] + b[15] * a[12]);
        dst[13] = (b[12] * a[1] + b[13] * a[5]) + (b[14] * a[9] + b[15] * a[13]);
        dst[14] = (b[12] * a[2] + b[13] * a[6]) + (b[14] * a[10] + b[15] * a[14]);
        dst[15] = (b[12] * a[3] + b[13] * a[7]) + (b[14] * a[11] + b[15] * a[15]);
    }

    public void offsetDepth(float[] mat, float offset) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(mat.length >= MAT4_ELEMENTS);

        //multiply offset by w to work around division by w on GPU
        float w = (mat[3] + mat[7]) + (mat[11] + mat[15]);
        mat[14] += offset * abs(w);
    }

    public void flip(float[] mat) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(mat.length >= MAT4_ELEMENTS);

        //swap opposite pairs
        swap(mat, matrixIndex(0, 1), matrixIndex(1, 0));
        swap(mat, matrixIndex(0, 2), matrixIndex(2, 0));
        swap(mat, matrixIndex(0, 3), matrixIndex(3, 0));
        swap(mat, matrixIndex(1, 2), matrixIndex(2, 1));
        swap(mat, matrixIndex(1, 3), matrixIndex(3, 1));
        swap(mat, matrixIndex(2, 3), matrixIndex(3, 2));
    }
}
