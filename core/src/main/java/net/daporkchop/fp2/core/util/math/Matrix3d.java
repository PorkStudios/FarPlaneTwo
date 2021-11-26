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

/**
 * Copypasta of LWJGL's {@link org.lwjgl.util.vector.Matrix3d}, but for {@code double}s.
 */
public class Matrix3d {
    /**
     * Copy source matrix to destination matrix
     *
     * @param src  The source matrix
     * @param dest The destination matrix, or null of a new matrix is to be created
     * @return The copied matrix
     */
    public static Matrix3d load(Matrix3d src, Matrix3d dest) {
        if (dest == null) {
            dest = new Matrix3d();
        }

        dest.m00 = src.m00;
        dest.m10 = src.m10;
        dest.m20 = src.m20;
        dest.m01 = src.m01;
        dest.m11 = src.m11;
        dest.m21 = src.m21;
        dest.m02 = src.m02;
        dest.m12 = src.m12;
        dest.m22 = src.m22;

        return dest;
    }

    /**
     * Add two matrices together and place the result in a third matrix.
     *
     * @param left  The left source matrix
     * @param right The right source matrix
     * @param dest  The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static Matrix3d add(Matrix3d left, Matrix3d right, Matrix3d dest) {
        if (dest == null) {
            dest = new Matrix3d();
        }

        dest.m00 = left.m00 + right.m00;
        dest.m01 = left.m01 + right.m01;
        dest.m02 = left.m02 + right.m02;
        dest.m10 = left.m10 + right.m10;
        dest.m11 = left.m11 + right.m11;
        dest.m12 = left.m12 + right.m12;
        dest.m20 = left.m20 + right.m20;
        dest.m21 = left.m21 + right.m21;
        dest.m22 = left.m22 + right.m22;

        return dest;
    }

    /**
     * Subtract the right matrix from the left and place the result in a third matrix.
     *
     * @param left  The left source matrix
     * @param right The right source matrix
     * @param dest  The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static Matrix3d sub(Matrix3d left, Matrix3d right, Matrix3d dest) {
        if (dest == null) {
            dest = new Matrix3d();
        }

        dest.m00 = left.m00 - right.m00;
        dest.m01 = left.m01 - right.m01;
        dest.m02 = left.m02 - right.m02;
        dest.m10 = left.m10 - right.m10;
        dest.m11 = left.m11 - right.m11;
        dest.m12 = left.m12 - right.m12;
        dest.m20 = left.m20 - right.m20;
        dest.m21 = left.m21 - right.m21;
        dest.m22 = left.m22 - right.m22;

        return dest;
    }

    /**
     * Multiply the right matrix by the left and place the result in a third matrix.
     *
     * @param left  The left source matrix
     * @param right The right source matrix
     * @param dest  The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static Matrix3d mul(Matrix3d left, Matrix3d right, Matrix3d dest) {
        if (dest == null) {
            dest = new Matrix3d();
        }

        double m00 =
                left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
        double m01 =
                left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
        double m02 =
                left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
        double m10 =
                left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
        double m11 =
                left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
        double m12 =
                left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
        double m20 =
                left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
        double m21 =
                left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
        double m22 =
                left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;

        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;

        return dest;
    }

    /**
     * Transform a Vector by a matrix and return the result in a destination
     * vector.
     *
     * @param left  The left matrix
     * @param right The right vector
     * @param dest  The destination vector, or null if a new one is to be created
     * @return the destination vector
     */
    public static Vector3d transform(Matrix3d left, Vector3d right, Vector3d dest) {
        if (dest == null) {
            dest = new Vector3d();
        }

        double x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z;
        double y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z;
        double z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z;

        dest.x = x;
        dest.y = y;
        dest.z = z;

        return dest;
    }

    /**
     * Transpose the source matrix and place the result into the destination matrix
     *
     * @param src  The source matrix to be transposed
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the transposed matrix
     */
    public static Matrix3d transpose(Matrix3d src, Matrix3d dest) {
        if (dest == null) {
            dest = new Matrix3d();
        }
        double m00 = src.m00;
        double m01 = src.m10;
        double m02 = src.m20;
        double m10 = src.m01;
        double m11 = src.m11;
        double m12 = src.m21;
        double m20 = src.m02;
        double m21 = src.m12;
        double m22 = src.m22;

        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        return dest;
    }

    /**
     * Invert the source matrix and put the result into the destination matrix
     *
     * @param src  The source matrix to be inverted
     * @param dest The destination matrix, or null if a new one is to be created
     * @return The inverted matrix if successful, null otherwise
     */
    public static Matrix3d invert(Matrix3d src, Matrix3d dest) {
        double determinant = src.determinant();

        if (determinant != 0) {
            if (dest == null) {
                dest = new Matrix3d();
            }
            /* do it the ordinary way
             *
             * inv(A) = 1/det(A) * adj(T), where adj(T) = transpose(Conjugate Matrix)
             *
             * m00 m01 m02
             * m10 m11 m12
             * m20 m21 m22
             */
            double determinant_inv = 1d / determinant;

            // get the conjugate matrix
            double t00 = src.m11 * src.m22 - src.m12 * src.m21;
            double t01 = -src.m10 * src.m22 + src.m12 * src.m20;
            double t02 = src.m10 * src.m21 - src.m11 * src.m20;
            double t10 = -src.m01 * src.m22 + src.m02 * src.m21;
            double t11 = src.m00 * src.m22 - src.m02 * src.m20;
            double t12 = -src.m00 * src.m21 + src.m01 * src.m20;
            double t20 = src.m01 * src.m12 - src.m02 * src.m11;
            double t21 = -src.m00 * src.m12 + src.m02 * src.m10;
            double t22 = src.m00 * src.m11 - src.m01 * src.m10;

            dest.m00 = t00 * determinant_inv;
            dest.m11 = t11 * determinant_inv;
            dest.m22 = t22 * determinant_inv;
            dest.m01 = t10 * determinant_inv;
            dest.m10 = t01 * determinant_inv;
            dest.m20 = t02 * determinant_inv;
            dest.m02 = t20 * determinant_inv;
            dest.m12 = t21 * determinant_inv;
            dest.m21 = t12 * determinant_inv;
            return dest;
        } else {
            return null;
        }
    }

    /**
     * Negate the source matrix and place the result in the destination matrix.
     *
     * @param src  The source matrix
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return the negated matrix
     */
    public static Matrix3d negate(Matrix3d src, Matrix3d dest) {
        if (dest == null) {
            dest = new Matrix3d();
        }

        dest.m00 = -src.m00;
        dest.m01 = -src.m02;
        dest.m02 = -src.m01;
        dest.m10 = -src.m10;
        dest.m11 = -src.m12;
        dest.m12 = -src.m11;
        dest.m20 = -src.m20;
        dest.m21 = -src.m22;
        dest.m22 = -src.m21;
        return dest;
    }

    /**
     * Set the matrix to be the identity matrix.
     *
     * @param m The matrix to be set to the identity
     * @return m
     */
    public static Matrix3d setIdentity(Matrix3d m) {
        m.m00 = 1.0d;
        m.m01 = 0.0d;
        m.m02 = 0.0d;
        m.m10 = 0.0d;
        m.m11 = 1.0d;
        m.m12 = 0.0d;
        m.m20 = 0.0d;
        m.m21 = 0.0d;
        m.m22 = 1.0d;
        return m;
    }

    /**
     * Set the matrix matrix to 0.
     *
     * @param m The matrix to be set to 0
     * @return m
     */
    public static Matrix3d setZero(Matrix3d m) {
        m.m00 = 0.0d;
        m.m01 = 0.0d;
        m.m02 = 0.0d;
        m.m10 = 0.0d;
        m.m11 = 0.0d;
        m.m12 = 0.0d;
        m.m20 = 0.0d;
        m.m21 = 0.0d;
        m.m22 = 0.0d;
        return m;
    }

    public double m00,
            m01,
            m02,
            m10,
            m11,
            m12,
            m20,
            m21,
            m22;

    /**
     * Constructor for Matrix3d. Matrix is initialised to the identity.
     */
    public Matrix3d() {
        super();
        setIdentity();
    }

    /**
     * Load from another matrix
     *
     * @param src The source matrix
     * @return this
     */
    public Matrix3d load(Matrix3d src) {
        return load(src, this);
    }

    /**
     * Transpose this matrix
     *
     * @return this
     */
    public Matrix3d transpose() {
        return transpose(this, this);
    }

    /**
     * Transpose this matrix and place the result in another matrix
     *
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the transposed matrix
     */
    public Matrix3d transpose(Matrix3d dest) {
        return transpose(this, dest);
    }

    /**
     * @return the determinant of the matrix
     */
    public double determinant() {
        double f =
                m00 * (m11 * m22 - m12 * m21)
                + m01 * (m12 * m20 - m10 * m22)
                + m02 * (m10 * m21 - m11 * m20);
        return f;
    }

    /**
     * Returns a string representation of this matrix
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(m00).append(' ').append(m10).append(' ').append(m20).append(' ').append('\n');
        buf.append(m01).append(' ').append(m11).append(' ').append(m21).append(' ').append('\n');
        buf.append(m02).append(' ').append(m12).append(' ').append(m22).append(' ').append('\n');
        return buf.toString();
    }

    /**
     * Invert this matrix
     *
     * @return this if successful, null otherwise
     */
    public Matrix3d invert() {
        return invert(this, this);
    }

    /**
     * Negate this matrix
     *
     * @return this
     */
    public Matrix3d negate() {
        return negate(this);
    }

    /**
     * Negate this matrix and place the result in a destination matrix.
     *
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return the negated matrix
     */
    public Matrix3d negate(Matrix3d dest) {
        return negate(this, dest);
    }

    /**
     * Set this matrix to be the identity matrix.
     *
     * @return this
     */
    public Matrix3d setIdentity() {
        return setIdentity(this);
    }

    /**
     * Set this matrix to 0.
     *
     * @return this
     */
    public Matrix3d setZero() {
        return setZero(this);
    }
}
