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

import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * Used to represent a circular volume for finding far terrain pieces that should be rendered at a given detail level.
 *
 * @author DaPorkchop_
 */
public class Sphere extends Vec3d {
    public static double sq(double d) {
        return d * d;
    }

    public final double radius;

    public Sphere(double xIn, double yIn, double zIn, double radius) {
        super(xIn, yIn, zIn);

        this.radius = radius;
    }

    public boolean intersects(AxisAlignedBB bb) {
        double dist = sq(this.radius);
        if (this.x < bb.minX) {
            dist -= sq(this.x - bb.minX);
        } else if (this.x > bb.maxX) {
            dist -= sq(this.x - bb.maxX);
        }
        if (this.y < bb.minY) {
            dist -= sq(this.y - bb.minY);
        } else if (this.x > bb.maxY) {
            dist -= sq(this.y - bb.maxY);
        }
        if (this.z < bb.minZ) {
            dist -= sq(this.z - bb.minZ);
        } else if (this.x > bb.maxZ) {
            dist -= sq(this.z - bb.maxZ);
        }
        return dist > 0.0d;
    }

    public boolean contains(AxisAlignedBB bb) {
        return this.contains(bb.minX, bb.minY, bb.minZ)
                && this.contains(bb.minX, bb.minY, bb.maxZ)
                && this.contains(bb.minX, bb.maxY, bb.minZ)
                && this.contains(bb.minX, bb.maxY, bb.maxZ)
                && this.contains(bb.maxX, bb.minY, bb.minZ)
                && this.contains(bb.maxX, bb.minY, bb.maxZ)
                && this.contains(bb.maxX, bb.maxY, bb.minZ)
                && this.contains(bb.maxX, bb.maxY, bb.maxZ);
    }

    public boolean contains(double x, double y, double z) {
        return sq(this.x - x) + sq(this.y - y) + sq(this.z - z) < sq(this.radius);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Sphere) {
            Sphere s = (Sphere) obj;
            return Double.compare(this.x, s.x) == 0 && Double.compare(this.y, s.y) == 0 && Double.compare(this.z, s.z) == 0 && Double.compare(this.radius, s.radius) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mix32(mix64(mix64(mix64(Double.doubleToLongBits(this.x)) + Double.doubleToLongBits(this.y)) + Double.doubleToLongBits(this.z)) + Double.doubleToLongBits(this.radius));
    }

    @Override
    public String toString() {
        return PStrings.fastFormat("sphere[x=%f,y=%f,z=%f,r=%f]", this.x, this.y, this.z, this.radius);
    }
}
