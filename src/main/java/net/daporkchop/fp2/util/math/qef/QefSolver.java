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

package net.daporkchop.fp2.util.math.qef;

import lombok.Getter;
import net.daporkchop.fp2.util.math.SMatrix3f;
import net.daporkchop.fp2.util.threading.DefaultFastThreadLocal;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector3f;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class QefSolver {
    protected static final DefaultFastThreadLocal<Ctx> CTX_TL = new DefaultFastThreadLocal<>(Ctx::new);

    protected static float dot(Vector3f a, Vector3f b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    protected static void sub(Vector3f c, Vector3f a, Vector3f b) {
        c.x = a.x - b.x;
        c.y = a.y - b.y;
        c.z = a.z - b.z;
    }

    protected static float fnorm(SMatrix3f a) {
        return (float) sqrt(a.m00 * a.m00 + a.m01 * a.m01 + a.m02 * a.m02
                            + a.m01 * a.m01 + a.m11 * a.m11 + a.m12 * a.m12
                            + a.m02 * a.m02 + a.m12 * a.m12 + a.m22 * a.m22);
    }

    protected static float off(SMatrix3f a) {
        return (float) sqrt(2.0f * (a.m01 * a.m01 + a.m02 * a.m02 + a.m12 * a.m12));
    }

    protected static void calcSymmetricGivensCoefficients(float a_pp, float a_pq, float a_qq, float c, float s) {
        if (a_pq == 0.0f) {
            c = 1.0f;
            s = 0.0f;
            return;
        }

        float tau = (a_qq - a_pp) / (2.0f * a_pq);
        float stt = (float) sqrt(1.0f + tau * tau);
        float tan = 1.0f / (tau >= 0 ? tau + stt : tau - stt);
        c = 1.0f / (float) sqrt(1.0f + tan * tan);
        s = tan * c;
    }

    protected static void rotate01(SMatrix3f vtav, Matrix3f v) {
        if (vtav.m01 == 0) {
            return;
        }

        float c, s;
        { //rot01
            SMatrix3f m = vtav;
            { //calcSymmetricGivensCoefficients
                float a_pp = m.m00;
                float a_pq = m.m01;
                float a_qq = m.m11;
                if (a_pq == 0.0f) {
                    c = 1.0f;
                    s = 0.0f;
                } else {
                    float tau = (a_qq - a_pp) / (2.0f * a_pq);
                    float stt = (float) sqrt(1.0f + tau * tau);
                    float tan = 1.0f / (tau >= 0 ? tau + stt : tau - stt);
                    c = 1.0f / (float) sqrt(1.0f + tan * tan);
                    s = tan * c;
                }
            }

            float cc = c * c;
            float ss = s * s;
            float mix = 2.0f * c * s * m.m01;
            m.set(cc * m.m00 - mix + ss * m.m11, 0, c * m.m02 - s * m.m12,
                    ss * m.m00 + mix + cc * m.m11, s * m.m02 + c * m.m12, m.m22);
        }
        { //rot01_post
            Matrix3f m = v;
            float m00 = m.m00, m01 = m.m01, m10 = m.m10, m11 = m.m11, m20 = m.m20, m21 = m.m21;
            m.m00 = c * m00 - s * m01;
            m.m01 = s * m00 + c * m01;
            //m02
            m.m10 = c * m10 - s * m11;
            m.m11 = s * m10 + c * m11;
            //m12
            m.m20 = c * m20 - s * m21;
            m.m21 = s * m20 + c * m21;
            //m22
        }
    }

    protected static void rotate02(SMatrix3f vtav, Matrix3f v) {
        if (vtav.m02 == 0) {
            return;
        }

        float c, s;
        { //rot02
            SMatrix3f m = vtav;
            { //calcSymmetricGivensCoefficients
                float a_pp = m.m00;
                float a_pq = m.m01;
                float a_qq = m.m11;
                if (a_pq == 0.0f) {
                    c = 1.0f;
                    s = 0.0f;
                } else {
                    float tau = (a_qq - a_pp) / (2.0f * a_pq);
                    float stt = (float) sqrt(1.0f + tau * tau);
                    float tan = 1.0f / (tau >= 0 ? tau + stt : tau - stt);
                    c = 1.0f / (float) sqrt(1.0f + tan * tan);
                    s = tan * c;
                }
            }

            float cc = c * c;
            float ss = s * s;
            float mix = 2.0f * c * s * m.m02;
            m.set(cc * m.m00 - mix + ss * m.m22, c * m.m01 - s * m.m12, 0,
                    m.m11, s * m.m01 + c * m.m12, ss * m.m00 + mix + cc * m.m22);
        }
        { //rot02_post
            Matrix3f m = v;
            float m00 = m.m00, m02 = m.m02, m10 = m.m10, m12 = m.m12, m20 = m.m20, m22 = m.m22;
            m.m00 = c * m00 - s * m02;
            //m01
            m.m02 = s * m00 + c * m02;
            m.m10 = c * m10 - s * m12;
            //m11
            m.m12 = s * m10 + c * m12;
            m.m20 = c * m20 - s * m22;
            //m21
            m.m22 = s * m20 + c * m22;
        }
    }

    protected static void rotate12(SMatrix3f vtav, Matrix3f v) {
        if (vtav.m12 == 0) {
            return;
        }

        float c, s;
        { //rot12
            SMatrix3f m = vtav;
            { //calcSymmetricGivensCoefficients
                float a_pp = m.m00;
                float a_pq = m.m01;
                float a_qq = m.m11;
                if (a_pq == 0.0f) {
                    c = 1.0f;
                    s = 0.0f;
                } else {
                    float tau = (a_qq - a_pp) / (2.0f * a_pq);
                    float stt = (float) sqrt(1.0f + tau * tau);
                    float tan = 1.0f / (tau >= 0 ? tau + stt : tau - stt);
                    c = 1.0f / (float) sqrt(1.0f + tan * tan);
                    s = tan * c;
                }
            }

            float cc = c * c;
            float ss = s * s;
            float mix = 2.0f * c * s * m.m12;
            m.set(m.m00, c * m.m01 - s * m.m02, s * m.m01 + c * m.m02,
                    cc * m.m11 - mix + ss * m.m22, 0, ss * m.m11 + mix + cc * m.m22);
        }
        { //rot12_post
            Matrix3f m = v;
            float m01 = m.m01, m02 = m.m02, m11 = m.m11, m12 = m.m12, m21 = m.m21, m22 = m.m22;
            //m00
            m.m01 = c * m01 - s * m02;
            m.m02 = s * m01 + c * m02;
            //m10
            m.m11 = c * m11 - s * m12;
            m.m12 = s * m11 + c * m12;
            //m20
            m.m21 = c * m21 - s * m22;
            m.m22 = s * m21 + c * m22;
        }
    }

    protected static void getSymmetricSvd(SMatrix3f a, SMatrix3f vtav, Matrix3f v, float tol, int max_sweeps) {
        vtav.set(a);
        v.setIdentity();
        float delta = tol * fnorm(vtav);

        for (int i = 0; i < max_sweeps && off(vtav) > delta; i++) {
            rotate01(vtav, v);
            rotate02(vtav, v);
            rotate12(vtav, v);
        }
    }

    protected static float pinv(float x, float tol) {
        return abs(x) < tol || abs(1 / x) < tol ? 0 : 1 / x;
    }

    protected static void pseudoinverse(Matrix3f out, SMatrix3f d, Matrix3f v, float tol) {
        float d0 = pinv(d.m00, tol);
        float d1 = pinv(d.m11, tol);
        float d2 = pinv(d.m22, tol);

        out.m00 = v.m00 * d0 * v.m00 + v.m01 * d1 * v.m01 + v.m02 * d2 * v.m02;
        out.m01 = v.m00 * d0 * v.m10 + v.m01 * d1 * v.m11 + v.m02 * d2 * v.m12;
        out.m02 = v.m00 * d0 * v.m20 + v.m01 * d1 * v.m21 + v.m02 * d2 * v.m22;
        out.m10 = v.m10 * d0 * v.m00 + v.m11 * d1 * v.m01 + v.m12 * d2 * v.m02;
        out.m11 = v.m10 * d0 * v.m10 + v.m11 * d1 * v.m11 + v.m12 * d2 * v.m12;
        out.m12 = v.m10 * d0 * v.m20 + v.m11 * d1 * v.m21 + v.m12 * d2 * v.m22;
        out.m20 =  v.m20 * d0 * v.m00 + v.m21 * d1 * v.m01 + v.m22 * d2 * v.m02;
        out.m21 =  v.m20 * d0 * v.m10 + v.m21 * d1 * v.m11 + v.m22 * d2 * v.m12;
        out.m22 =  v.m20 * d0 * v.m20 + v.m21 * d1 * v.m21 + v.m22 * d2 * v.m22;
    }

    protected static void vmul(Vector3f out, Matrix3f a, Vector3f v)    {
        out.x = a.m00 * v.x + a.m01 * v.y + a.m02 * v.z;
        out.y = a.m10 * v.x + a.m11 * v.y + a.m12 * v.z;
        out.z = a.m20 * v.x + a.m21 * v.y + a.m22 * v.z;
    }

    protected static float calcError(SMatrix3f A, Vector3f x, Vector3f b, Vector3f vtmp)    {
        A.vmul(vtmp, x);
        sub(vtmp, b, vtmp);
        return dot(vtmp, vtmp);
    }

    protected static float solveSymmetric(SMatrix3f A, Vector3f b, Vector3f x, float svd_tol, int svd_sweeps, float pinv_tol) {
        Ctx ctx = CTX_TL.get();
        getSymmetricSvd(A, ctx.VTAV, ctx.V, svd_tol, svd_sweeps);
        pseudoinverse(ctx.pinv, ctx.VTAV, ctx.V, pinv_tol);
        vmul(x, ctx.pinv, b);
        return calcError(A, x, b, ctx.vtmp);
    }

    @Getter
    protected final QefData data = new QefData();
    protected final SMatrix3f ata = new SMatrix3f();
    protected final Vector3f tmpv = new Vector3f();
    protected final Vector3f atb = new Vector3f();
    @Getter
    protected final Vector3f massPoint = new Vector3f();
    protected final Vector3f x = new Vector3f();
    protected boolean hasSolution;

    public void add(float px, float py, float pz, float nx, float ny, float nz) {
        this.hasSolution = false;

        float length = (float) sqrt(nx * nx + ny * ny + nz * nz);
        nx /= length;
        ny /= length;
        nz /= length;

        float dot = nx * px + ny * py + nz * pz;

        this.data.ata_00 += nx * nx;
        this.data.ata_01 += nx * ny;
        this.data.ata_02 += nx * nz;
        this.data.ata_11 += ny * ny;
        this.data.ata_12 += ny * nz;
        this.data.ata_22 += nz * nz;
        this.data.atb_x += dot * nx;
        this.data.atb_y += dot * ny;
        this.data.atb_z += dot * nz;
        this.data.btb += dot * dot;
        this.data.massPoint_x += px;
        this.data.massPoint_y += py;
        this.data.massPoint_z += pz;
        this.data.numPoints++;
    }

    public void add(Vector3f p, Vector3f n) {
        this.add(p.x, p.y, p.z, n.x, n.y, n.z);
    }

    public void add(QefData data) {
        this.hasSolution = false;
        this.data.add(data);
    }

    public float getError() {
        checkState(this.hasSolution);
        return this.getError(this.x);
    }

    public float getError(Vector3f pos) {
        if (!this.hasSolution) {
            this.setAta();
            this.setAtb();
        }

        this.ata.vmul(this.tmpv, pos);
        return dot(pos, this.tmpv) - 2.0f * dot(pos, this.atb) + this.data.btb;
    }

    public void reset() {
        this.hasSolution = false;
        this.data.clear();
    }

    private void setAta() {
        this.ata.set(this.data.ata_00, this.data.ata_01, this.data.ata_02, this.data.ata_11, this.data.ata_12, this.data.ata_22);
    }

    private void setAtb() {
        this.atb.set(this.data.atb_x, this.data.atb_y, this.data.atb_z);
    }

    public float solve(Vector3f outx, float svd_tol, int svd_sweeps, float pinv_tol) {
        checkArg(this.data.numPoints != 0);

        this.massPoint.set(this.data.massPoint_x, this.data.massPoint_y, this.data.massPoint_z);
        this.massPoint.scale(1.0f / this.data.numPoints);

        this.setAta();
        this.setAtb();

        this.ata.vmul(this.tmpv, this.massPoint);
        sub(this.atb, this.atb, this.tmpv);
        this.x.set(0.0f, 0.0f, 0.0f);

        float result = solveSymmetric(this.ata, this.atb, this.x, svd_tol, svd_sweeps, pinv_tol);

        outx.x = this.x.x += this.massPoint.x;
        outx.y = this.x.y += this.massPoint.y;
        outx.z = this.x.z += this.massPoint.z;
        this.setAtb();
        this.hasSolution = true;

        return result;
    }

    private static final class Ctx {
        public final Matrix3f pinv = new Matrix3f();
        public final Matrix3f V = new Matrix3f();
        public final SMatrix3f VTAV = new SMatrix3f();
        public final Vector3f vtmp = new Vector3f();
    }
}
