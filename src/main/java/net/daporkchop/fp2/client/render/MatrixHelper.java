/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.client.render;

import lombok.experimental.UtilityClass;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class MatrixHelper {
    private final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private final float[] EMPTY_MATRIX = new float[16];

    public FloatBuffer getMATRIX(int id, FloatBuffer buffer) {
        if (buffer == null) {
            buffer = BufferUtils.createFloatBuffer(16);
        }
        GL11.glGetFloat(id, buffer);
        return buffer;
    }

    public void perspectiveInfinite(float fovy, float aspect, float zNear) {
        //from http://dev.theomader.com/depth-precision/
        float radians = (float) Math.toRadians(fovy);
        float f = 1.0f / (float) Math.tan(radians * 0.5f);

        MATRIX.put(EMPTY_MATRIX);

        MATRIX.put(0 * 4 + 0, f / aspect);
        MATRIX.put(1 * 4 + 1, f);
        MATRIX.put(2 * 4 + 2, -1);
        MATRIX.put(3 * 4 + 2, -zNear);
        MATRIX.put(2 * 4 + 3, -1);

        glMultMatrix((FloatBuffer) MATRIX.clear());
    }
}
