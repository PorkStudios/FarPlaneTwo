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

package net.daporkchop.fp2.client.gl.camera;

import lombok.NonNull;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/**
 * Basically the same as {@link ICamera}, but not shitty and bad.
 * <p>
 * (also: why did microjang decide to make ICamera an interface, when they only use one implementation? not that i'm complaining, but it's very unlike notch
 * to abstract things away nicely, ESPECIALLY when there seems to be no valid reason to do so...)
 *
 * @author DaPorkchop_
 */
public interface IFrustum {
    /**
     * Checks whether or not the given point is contained in this frustum.
     *
     * @param x the X coordinate of the point
     * @param y the Y coordinate of the point
     * @param z the Z coordinate of the point
     * @return whether or not the given point is contained in this frustum
     */
    boolean containsPoint(double x, double y, double z);

    /**
     * Checks whether or not the given point is contained in this frustum.
     *
     * @param pos the point
     * @return whether or not the given point is contained in this frustum
     */
    default boolean containsPoint(@NonNull Vec3d pos) {
        return this.containsPoint(pos.x, pos.y, pos.z);
    }

    /**
     * Checks whether or not the given axis-aligned bounding box intersects with this frustum.
     *
     * @param minX the minimum X coordinate of the bounding box
     * @param minY the minimum Y coordinate of the bounding box
     * @param minZ the minimum Z coordinate of the bounding box
     * @param maxX the maximum X coordinate of the bounding box
     * @param maxY the maximum Y coordinate of the bounding box
     * @param maxZ the maximum Z coordinate of the bounding box
     * @return whether or not the given axis-aligned bounding box intersects with this frustum
     */
    boolean intersectsBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    /**
     * Checks whether or not the given axis-aligned bounding box intersects with this frustum.
     *
     * @param bb the bounding box
     * @return whether or not the given axis-aligned bounding box intersects with this frustum
     */
    default boolean intersectsBB(AxisAlignedBB bb) {
        return this.intersectsBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }
}
