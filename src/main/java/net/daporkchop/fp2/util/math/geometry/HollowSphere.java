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

package net.daporkchop.fp2.util.math.geometry;

import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.util.math.Vec3d;

import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class HollowSphere extends Vec3d implements Volume {
    public final double innerRadius;
    public final double outerRadius;

    public HollowSphere(double xIn, double yIn, double zIn, double innerRadius, double outerRadius) {
        super(xIn, yIn, zIn);

        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    @Override
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double dx = this.x - clamp(this.x, minX, maxX);
        double dy = this.y - clamp(this.y, minY, maxY);
        double dz = this.z - clamp(this.z, minZ, maxZ);
        double d = sq(dx) + sq(dy) + sq(dz);
        return d >= sq(this.innerRadius) && d <= sq(this.outerRadius);
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
        double d = sq(this.x - x) + sq(this.y - y) + sq(this.z - z);
        return d > sq(this.innerRadius) && d < sq(this.outerRadius);
    }

    @Override
    public HollowSphere shrink(double d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof HollowSphere) {
            HollowSphere s = (HollowSphere) obj;
            return Double.compare(this.x, s.x) == 0 && Double.compare(this.y, s.y) == 0 && Double.compare(this.z, s.z) == 0 && Double.compare(this.innerRadius, s.innerRadius) == 0 && Double.compare(this.outerRadius, s.outerRadius) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mix32(mix64(mix64(mix64(mix64(Double.doubleToLongBits(this.x)) + Double.doubleToLongBits(this.y)) + Double.doubleToLongBits(this.z)) + Double.doubleToLongBits(this.innerRadius)) + Double.doubleToLongBits(this.outerRadius));
    }

    @Override
    public String toString() {
        return PStrings.fastFormat("hollow sphere[x=%f,y=%f,z=%f,r=%f - %f]", this.x, this.y, this.z, this.innerRadius, this.outerRadius);
    }
}
