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

package net.daporkchop.fp2.core.util.math;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
public class SMatrix3d {
    public double m00, m01, m02, m11, m12, m22;

    public void clear() {
        this.m00 = 0.0d;
        this.m01 = 0.0d;
        this.m02 = 0.0d;
        this.m11 = 0.0d;
        this.m12 = 0.0d;
        this.m22 = 0.0d;
    }

    public void set(double m00, double m01, double m02, double m11, double m12, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m11 = m11;
        this.m12 = m12;
        this.m22 = m22;
    }

    public void set(SMatrix3d matrix) {
        this.set(matrix.m00, matrix.m01, matrix.m02, matrix.m11, matrix.m12, matrix.m22);
    }

    public void vmul(Vector3d out, Vector3d v) {
        out.x = this.m00 * v.x + this.m01 * v.y + this.m02 * v.z;
        out.y = this.m01 * v.x + this.m11 * v.y + this.m12 * v.z;
        out.z = this.m02 * v.x + this.m12 * v.y + this.m22 * v.z;
    }
}
