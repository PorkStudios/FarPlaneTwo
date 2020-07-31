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

package net.daporkchop.fp2.client.gl.camera;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.MatrixHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.nio.FloatBuffer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
public class Frustum implements ICamera {
    protected static void normalize(double[] arr, int off) {
        double len = sqrt(arr[off + 0] * arr[off + 0] + arr[off + 1] * arr[off + 1] + arr[off + 2] * arr[off + 2] + arr[off + 3] * arr[off + 3]);
        arr[off + 0] /= len;
        arr[off + 1] /= len;
        arr[off + 2] /= len;
        arr[off + 3] /= len;
    }

    private static double dot(double[] arr, int off, double x, double y, double z, double w) {
        return arr[off + 0] * x + arr[off + 1] * y + arr[off + 2] * z + arr[off + 3];
    }

    protected final double[] frustum = new double[6 * 4];

    protected final double[] projection = new double[MAT4_ELEMENTS];
    protected final double[] modelView = new double[MAT4_ELEMENTS];
    protected final double[] mvp = new double[MAT4_ELEMENTS];

    protected double x;
    protected double y;
    protected double z;

    public void initFromGlState() {
        FloatBuffer buffer = MatrixHelper.get(GL_PROJECTION_MATRIX);
        for (int i = 0; i < MAT4_ELEMENTS; i++) {
            this.projection[i] = buffer.get(i);
        }
        buffer = MatrixHelper.get(GL_MODELVIEW_MATRIX);
        for (int i = 0; i < MAT4_ELEMENTS; i++) {
            this.modelView[i] = buffer.get(i);
        }

        MatrixHelper.multiply(this.projection, this.modelView, this.mvp);

        this.init(this.mvp);
    }

    public void init(@NonNull double[] mvp) {
        double[] frustum = this.frustum;

        int off = 0;
        frustum[off + 0] = mvp[3] - mvp[0];
        frustum[off + 1] = mvp[7] - mvp[4];
        frustum[off + 2] = mvp[11] - mvp[8];
        frustum[off + 3] = mvp[15] - mvp[12];

        off = 4;
        frustum[off + 0] = mvp[3] + mvp[0];
        frustum[off + 1] = mvp[7] + mvp[4];
        frustum[off + 2] = mvp[11] + mvp[8];
        frustum[off + 3] = mvp[15] + mvp[12];

        off = 8;
        frustum[off + 0] = mvp[3] + mvp[1];
        frustum[off + 1] = mvp[7] + mvp[5];
        frustum[off + 2] = mvp[11] + mvp[9];
        frustum[off + 3] = mvp[15] + mvp[13];

        off = 12;
        frustum[off + 0] = mvp[3] - mvp[1];
        frustum[off + 1] = mvp[7] - mvp[5];
        frustum[off + 2] = mvp[11] - mvp[9];
        frustum[off + 3] = mvp[15] - mvp[13];

        off = 16;
        frustum[off + 0] = mvp[3] - mvp[2];
        frustum[off + 1] = mvp[7] - mvp[6];
        frustum[off + 2] = mvp[11] - mvp[10];
        frustum[off + 3] = mvp[15] - mvp[14];

        off = 20;
        frustum[off + 0] = mvp[3] + mvp[2];
        frustum[off + 1] = mvp[7] + mvp[6];
        frustum[off + 2] = mvp[11] + mvp[10];
        frustum[off + 3] = mvp[15] + mvp[14];

        for (int i = 0; i < 6; i++) {
            normalize(frustum, i * 4);
        }
    }

    @Override
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isPointInFrustum(double x, double y, double z) {
        x -= this.x;
        y -= this.y;
        z -= this.z;

        double[] frustum = this.frustum;

        for (int i = 0; i < 6; i++) {
            int off = i * 4;

            if (this.dot(frustum, off, x, y, z, 1.0d) <= 0.0d) {
                return false;
            }
        }

        return true;
    }

    public boolean isPointInFrustum(@NonNull Vec3d pos) {
        return this.isPointInFrustum(pos.x, pos.y, pos.z);
    }

    public boolean isBoundingBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        minX -= this.x;
        minY -= this.y;
        minZ -= this.z;
        maxX -= this.x;
        maxY -= this.y;
        maxZ -= this.z;

        double[] frustum = this.frustum;

        for (int i = 0; i < 6; i++) {
            int off = i * 4;

            if (this.dot(frustum, off, minX, minY, minZ, 1.0d) <= 0.0d
                && this.dot(frustum, off, maxX, minY, minZ, 1.0d) <= 0.0d
                && this.dot(frustum, off, minX, maxY, minZ, 1.0d) <= 0.0d
                && this.dot(frustum, off, maxX, maxY, minZ, 1.0d) <= 0.0d
                && this.dot(frustum, off, minX, minY, maxZ, 1.0d) <= 0.0d
                && this.dot(frustum, off, maxX, minY, maxZ, 1.0d) <= 0.0d
                && this.dot(frustum, off, minX, maxY, maxZ, 1.0d) <= 0.0d
                && this.dot(frustum, off, maxX, maxY, maxZ, 1.0d) <= 0.0d) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isBoundingBoxInFrustum(AxisAlignedBB bb) {
        return this.isBoundingBoxInFrustum(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }
}
