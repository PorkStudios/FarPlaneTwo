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

import static java.lang.Math.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class Cube implements Volume {
    public final double x;
    public final double y;
    public final double z;
    public final double size;

    @Override
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.x - this.size < minX
               && this.x + this.size > maxX
               && this.y - this.size < minY
               && this.y + this.size > maxY
               && this.z - this.size < minZ
               && this.z + this.size > maxZ;
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
        return abs(x - this.x) < this.size
               && abs(y - this.y) < this.size
               && abs(z - this.z) < this.size;
    }

    @Override
    public Cube shrink(double d) {
        return new Cube(this.x, this.y, this.z, this.size - d);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Cube) {
            Cube c = (Cube) obj;
            return Double.compare(this.x, c.x) == 0 && Double.compare(this.y, c.y) == 0 && Double.compare(this.z, c.z) == 0 && Double.compare(this.size, c.size) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mix32(mix64(mix64(mix64(Double.doubleToLongBits(this.x)) + Double.doubleToLongBits(this.y)) + Double.doubleToLongBits(this.z)) + Double.doubleToLongBits(this.size));
    }

    @Override
    public String toString() {
        return PStrings.fastFormat("cube[x=%f,y=%f,z=%f,size=%f]", this.x, this.y, this.z, this.size);
    }
}
