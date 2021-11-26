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

package net.daporkchop.fp2.core.util.math.geometry;

import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.misc.string.PStrings;

import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class Sphere implements Volume {
    public final double x;
    public final double y;
    public final double z;
    public final double radius;

    @Override
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double dx = this.x - clamp(this.x, minX, maxX);
        double dy = this.y - clamp(this.y, minY, maxY);
        double dz = this.z - clamp(this.z, minZ, maxZ);
        return sq(dx) + sq(dy) + sq(dz) <= sq(this.radius);
    }

    @Override
    public boolean contains(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.contains(minX, minY, minZ)
               && this.contains(minX, minY, maxZ)
               && this.contains(minX, maxY, minZ)
               && this.contains(minX, maxY, maxZ)
               && this.contains(maxX, minY, minZ)
               && this.contains(maxX, minY, maxZ)
               && this.contains(maxX, maxY, minZ)
               && this.contains(maxX, maxY, maxZ);
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return sq(this.x - x) + sq(this.y - y) + sq(this.z - z) < sq(this.radius);
    }

    @Override
    public Sphere shrink(double d) {
        return new Sphere(this.x, this.y, this.z, this.radius - d);
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
