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

import static java.lang.Math.*;

/**
 * Copypasta of LWJGL's {@code Vector3f}, but for {@code double}s.
 */
public class Vector3d {
    /**
     * Add a vector to another vector and place the result in a destination
     * vector.
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @param dest  The destination vector, or null if a new vector is to be created
     * @return the sum of left and right in dest
     */
    public static Vector3d add(Vector3d left, Vector3d right, Vector3d dest) {
        if (dest == null) {
            return new Vector3d(left.x + right.x, left.y + right.y, left.z + right.z);
        } else {
            dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
            return dest;
        }
    }

    /**
     * Subtract a vector from another vector and place the result in a destination
     * vector.
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @param dest  The destination vector, or null if a new vector is to be created
     * @return left minus right in dest
     */
    public static Vector3d sub(Vector3d left, Vector3d right, Vector3d dest) {
        if (dest == null) {
            return new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
        } else {
            dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
            return dest;
        }
    }

    /**
     * The cross product of two vectors.
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @param dest  The destination result, or null if a new vector is to be created
     * @return left cross right
     */
    public static Vector3d cross(
            Vector3d left,
            Vector3d right,
            Vector3d dest) {

        if (dest == null) {
            dest = new Vector3d();
        }

        dest.set(
                left.y * right.z - left.z * right.y,
                right.x * left.z - right.z * left.x,
                left.x * right.y - left.y * right.x
        );

        return dest;
    }

    /**
     * The dot product of two vectors is calculated as
     * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
     *
     * @param left  The LHS vector
     * @param right The RHS vector
     * @return left dot right
     */
    public static double dot(Vector3d left, Vector3d right) {
        return left.x * right.x + left.y * right.y + left.z * right.z;
    }

    /**
     * Calculate the angle between two vectors, in radians
     *
     * @param a A vector
     * @param b The other vector
     * @return the angle between the two vectors, in radians
     */
    public static double angle(Vector3d a, Vector3d b) {
        double dls = dot(a, b) / (a.length() * b.length());
        if (dls < -1d) {
            dls = -1d;
        } else if (dls > 1.0d) {
            dls = 1.0d;
        }
        return Math.acos(dls);
    }

    public double x, y, z;

    /**
     * Constructor for Vector3d.
     */
    public Vector3d() {
        super();
    }

    /**
     * Constructor
     */
    public Vector3d(double x, double y, double z) {
        set(x, y, z);
    }

    /* (non-Javadoc)
     * @see org.lwjgl.util.vector.WritableVector2d#set(double, double)
     */
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /* (non-Javadoc)
     * @see org.lwjgl.util.vector.WritableVector3d#set(double, double, double)
     */
    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @return the length squared of the vector
     */
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return sqrt(this.lengthSquared());
    }

    /**
     * Translate a vector
     *
     * @param x The translation in x
     * @param y the translation in y
     * @return this
     */
    public Vector3d translate(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    /**
     * Negate a vector
     *
     * @return this
     */
    public Vector3d negate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Negate a vector and place the result in a destination vector.
     *
     * @param dest The destination vector or null if a new vector is to be created
     * @return the negated vector
     */
    public Vector3d negate(Vector3d dest) {
        if (dest == null) {
            dest = new Vector3d();
        }
        dest.x = -x;
        dest.y = -y;
        dest.z = -z;
        return dest;
    }

    /**
     * Normalise this vector and place the result in another vector.
     *
     * @param dest The destination vector, or null if a new vector is to be created
     * @return the normalised vector
     */
    public Vector3d normalise(Vector3d dest) {
        double l = length();

        if (dest == null) {
            dest = new Vector3d(x / l, y / l, z / l);
        } else {
            dest.set(x / l, y / l, z / l);
        }

        return dest;
    }

    /* (non-Javadoc)
     * @see org.lwjgl.vector.Vector#scale(double)
     */
    public Vector3d scale(double scale) {
        x *= scale;
        y *= scale;
        z *= scale;

        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append("Vector3d[");
        sb.append(x);
        sb.append(", ");
        sb.append(y);
        sb.append(", ");
        sb.append(z);
        sb.append(']');
        return sb.toString();
    }

    /**
     * @return x
     */
    public final double getX() {
        return x;
    }

    /**
     * Set X
     *
     * @param x
     */
    public final void setX(double x) {
        this.x = x;
    }

    /**
     * @return y
     */
    public final double getY() {
        return y;
    }

    /**
     * Set Y
     *
     * @param y
     */
    public final void setY(double y) {
        this.y = y;
    }

    /* (Overrides)
     * @see org.lwjgl.vector.ReadableVector3d#getZ()
     */
    public double getZ() {
        return z;
    }

    /**
     * Set Z
     *
     * @param z
     */
    public void setZ(double z) {
        this.z = z;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Vector3d other = (Vector3d) obj;

        if (x == other.x && y == other.y && z == other.z) {
            return true;
        }

        return false;
    }
}
