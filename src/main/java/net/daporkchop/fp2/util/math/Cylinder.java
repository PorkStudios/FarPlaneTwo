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

package net.daporkchop.fp2.util.math;

import lombok.AllArgsConstructor;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.util.math.AxisAlignedBB;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor
public class Cylinder implements Volume {
    private static double sq(double d) {
        return d * d;
    }

    public final double x;
    public final double z;
    public final double radius;

    @Override
    public boolean intersects(AxisAlignedBB bb) {
        return this.intersects(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

    @Override
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double dx = this.x - clamp(this.x, minX, maxX);
        double dz = this.z - clamp(this.z, minZ, maxZ);
        return dx * dx + dz * dz < sq(this.radius);
    }

    @Override
    public boolean contains(AxisAlignedBB bb) {
        return this.contains(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

    @Override
    public boolean contains(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.contains(minX, 0.0d, minZ)
               && this.contains(minX, 0.0d, maxZ)
               && this.contains(minX, 0.0d, minZ)
               && this.contains(minX, 0.0d, maxZ);
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return sq(this.x - x) + sq(this.z - z) < sq(this.radius);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Cylinder) {
            Cylinder s = (Cylinder) obj;
            return Double.compare(this.x, s.x) == 0 && Double.compare(this.z, s.z) == 0 && Double.compare(this.radius, s.radius) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mix32(mix64(mix64(Double.doubleToLongBits(this.x)) + Double.doubleToLongBits(this.z)) + Double.doubleToLongBits(this.radius));
    }

    @Override
    public String toString() {
        return PStrings.fastFormat("cylinder[x=%f,z=%f,r=%f]", this.x, this.z, this.radius);
    }
}
