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

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
public class Cube extends Vec3d implements Volume {
    public final double size;

    public Cube(double xIn, double yIn, double zIn, double size) {
        super(xIn, yIn, zIn);

        this.size = size;
    }

    @Override
    public boolean intersects(AxisAlignedBB bb) {
        return this.intersects(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

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
    public boolean contains(AxisAlignedBB bb) {
        return this.contains(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
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
}
