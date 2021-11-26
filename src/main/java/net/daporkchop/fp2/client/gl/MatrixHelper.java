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

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.ReversedZ;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.common.util.TypeSize;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Helper methods for dealing with OpenGL matrices.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class MatrixHelper {
    private final long TEMP_MATRIX = PUnsafe.allocateMemory(16 * DOUBLE_SIZE);

    public int matrixIndex(int x, int y) {
        return x * 4 + y;
    }

    public long matrixOffset(int x, int y) {
        return (long) matrixIndex(x, y) << 2L;
    }

    public void reversedZ(float fovy, float aspect, float zNear) {
        //from http://dev.theomader.com/depth-precision/
        float radians = (float) Math.toRadians(fovy);
        float f = 1.0f / (float) Math.tan(radians * 0.5f);

        PUnsafe.setMemory(TEMP_MATRIX, MAT4_SIZE, (byte) 0);

        if (ReversedZ.REVERSED) { //we are currently rendering the world, so we need to reverse the depth buffer
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(0, 0), f / aspect);
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(1, 1), f);
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(3, 2), zNear);
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(2, 3), -1.0f);
        } else { //otherwise, use normal perspective projection - but with infinite zFar
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(0, 0), f / aspect);
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(1, 1), f);
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(2, 2), -1.0f);
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(3, 2), -zNear);
            PUnsafe.putFloat(TEMP_MATRIX + matrixOffset(2, 3), -1.0f);
        }

        glMultMatrix(DirectBufferHackery.wrapFloat(TEMP_MATRIX, MAT4_ELEMENTS));
    }

    public FloatBuffer getFloatMatrixFromGL(int id) {
        FloatBuffer buffer = DirectBufferHackery.wrapFloat(TEMP_MATRIX, MAT4_ELEMENTS);
        glGetFloat(id, buffer);
        return buffer;
    }

    public void getFloatMatrixFromGL(int id, @NonNull float[] dst) {
        getFloatMatrixFromGL(id).get(dst);
    }

    public DoubleBuffer getDoubleMatrixFromGL(int id) {
        DoubleBuffer buffer = DirectBufferHackery.wrapDouble(TEMP_MATRIX, MAT4_ELEMENTS);
        glGetDouble(id, buffer);
        return buffer;
    }

    public void getDoubleMatrixFromGL(int id, @NonNull double[] dst) {
        getDoubleMatrixFromGL(id).get(dst);
    }

    public void getModelViewProjectionMatrix(long dst) {
        ArrayAllocator<float[]> alloc = GlobalAllocators.ALLOC_FLOAT.get();

        float[] modelView = alloc.atLeast(MAT4_ELEMENTS);
        float[] projection = alloc.atLeast(MAT4_ELEMENTS);
        float[] modelViewProjection = alloc.atLeast(MAT4_ELEMENTS);
        try {
            //load modelView matrix into array
            getFloatMatrixFromGL(GL_MODELVIEW_MATRIX).get(modelView);

            //load projection matrix into array
            getFloatMatrixFromGL(GL_PROJECTION_MATRIX).get(projection);

            //multiply matrices
            multiply4x4(projection, modelView, modelViewProjection);

            float offset = ReversedZ.REVERSED ? -0.00001f : 0.00001f;
            modelViewProjection[14] += offset * abs(modelViewProjection[3] + modelViewProjection[7] + modelViewProjection[11] + modelViewProjection[15]);

            //copy result to destination address
            PUnsafe.copyMemory(modelViewProjection, PUnsafe.ARRAY_FLOAT_BASE_OFFSET, null, dst, 16 * (long) TypeSize.FLOAT_SIZE);
        } finally {
            alloc.release(modelViewProjection);
            alloc.release(projection);
            alloc.release(modelView);
        }
    }

    public void multiply4x4(@NonNull float[] a, @NonNull float[] b, @NonNull float[] dst) {
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

    public void multiply4x4(@NonNull double[] a, @NonNull double[] b, @NonNull double[] dst) {
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

    public void offsetDepth(@NonNull float[] mat, float offset) {
        //check array length at head to allow JIT to optimize array bounds checks out in method body
        checkArg(mat.length >= MAT4_ELEMENTS);

        //multiply offset by w to work around division by w on GPU
        float w = (mat[3] + mat[7]) + (mat[11] + mat[15]);
        mat[14] += offset * abs(w);
    }
}
