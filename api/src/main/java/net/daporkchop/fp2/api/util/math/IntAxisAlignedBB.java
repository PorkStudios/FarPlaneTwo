/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
 */

package net.daporkchop.fp2.api.util.math;

import lombok.Data;

/**
 * A 3-dimensional axis-aligned bounding box using {@code int} coordinates.
 * <p>
 * Minimum coordinates are inclusive, maximum coordinates are exclusive.
 *
 * @author DaPorkchop_
 */
@Data
public final class IntAxisAlignedBB {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    /**
     * @return the size of this AABB along the X axis
     */
    public int sizeX() {
        return this.maxX - this.minX;
    }

    /**
     * @return the size of this AABB along the Y axis
     */
    public int sizeY() {
        return this.maxY - this.minY;
    }

    /**
     * @return the size of this AABB along the Z axis
     */
    public int sizeZ() {
        return this.maxZ - this.minZ;
    }

    /**
     * Checks whether the given point is contained by this bounding box.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @param z the point's Z coordinate
     * @return whether the given point is contained by this bounding box
     */
    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
    }

    /**
     * Checks whether the given X coordinate is contained by this bounding box. Only the X axis is taken into account.
     *
     * @param x the X coordinate
     * @return whether the given X coordinate is contained by this bounding box
     */
    public boolean containsX(int x) {
        return x >= this.minX && x < this.maxX;
    }

    /**
     * Checks whether the given Y coordinate is contained by this bounding box. Only the Y axis is taken into account.
     *
     * @param y the Y coordinate
     * @return whether the given Y coordinate is contained by this bounding box
     */
    public boolean containsY(int y) {
        return y >= this.minY && y < this.maxY;
    }

    /**
     * Checks whether the given Z coordinate is contained by this bounding box. Only the Z axis is taken into account.
     *
     * @param z the Z coordinate
     * @return whether the given Z coordinate is contained by this bounding box
     */
    public boolean containsZ(int z) {
        return z >= this.minZ && z < this.maxZ;
    }
}
