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

package net.daporkchop.fp2.util.math;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.lwjgl.util.vector.Vector3f;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
public class SMatrix3f {
    public float m00, m01, m02, m11, m12, m22;

    public void clear() {
        this.m00 = 0.0f;
        this.m01 = 0.0f;
        this.m02 = 0.0f;
        this.m11 = 0.0f;
        this.m12 = 0.0f;
        this.m22 = 0.0f;
    }

    public void set(float m00, float m01, float m02, float m11, float m12, float m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m11 = m11;
        this.m12 = m12;
        this.m22 = m22;
    }

    public void set(SMatrix3f matrix) {
        this.set(matrix.m00, matrix.m01, matrix.m02, matrix.m11, matrix.m12, matrix.m22);
    }

    public void vmul(Vector3f out, Vector3f v) {
        out.x = this.m00 * v.x + this.m01 * v.y + this.m02 * v.z;
        out.y = this.m01 * v.x + this.m11 * v.y + this.m12 * v.z;
        out.z = this.m02 * v.x + this.m12 * v.y + this.m22 * v.z;
    }
}
