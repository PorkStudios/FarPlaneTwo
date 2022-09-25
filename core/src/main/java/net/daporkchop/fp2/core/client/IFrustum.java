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

package net.daporkchop.fp2.core.client;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.annotation.AArrayType;
import net.daporkchop.fp2.gl.attribute.annotation.AAttribute;
import net.daporkchop.fp2.gl.attribute.annotation.AScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.AVectorType;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayIndex;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayLength;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;

/**
 * A view frustum which can check for intersection with objects.
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
     * @return the clipping planes which define this view frustum
     */
    ClippingPlanes clippingPlanes();

    /**
     * @author DaPorkchop_
     */
    @AAttribute(name = "clippingPlaneCount", typeScalar = @AScalarType(value = int.class, interpret = @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED)))
    @AAttribute(name = "clippingPlanes", typeArray = @AArrayType(length = ClippingPlanes.PLANES_MAX,
            componentTypeVector = @AVectorType(components = 4,
                    componentType = @AScalarType(float.class))))
    interface ClippingPlanes {
        int PLANES_MAX = 10;

        @AttributeSetter
        ClippingPlanes clippingPlaneCount(int clippingPlaneCount);

        @AttributeSetter
        ClippingPlanes clippingPlanes(float @ArrayLength(PLANES_MAX * 4) [] clippingPlanes);

        @AttributeSetter
        ClippingPlanes clippingPlanes(@ArrayIndex int planeIndex, float x, float y, float z, float w);

        @AttributeSetter
        ClippingPlanes clippingPlanes(@ArrayIndex int planeIndex, float @ArrayLength(4) [] plane);

        default ClippingPlanes put(float x, float y, float z, float w) {
            //assert this.clippingPlaneCount < PLANES_MAX : this.clippingPlaneCount;

            int clippingPlaneCount = -1; //TODO: this.clippingPlaneCount();
            if (true) {
                throw new UnsupportedOperationException();
            }
            this.clippingPlanes(clippingPlaneCount, x, y, z, w);
            this.clippingPlaneCount(clippingPlaneCount + 1);

            return this;
        }

        default ClippingPlanes put(@NonNull float[] plane) {
            //assert this.clippingPlaneCount < PLANES_MAX : this.clippingPlaneCount;

            int clippingPlaneCount = -1; //TODO: this.clippingPlaneCount();
            if (true) {
                throw new UnsupportedOperationException();
            }
            this.clippingPlanes(clippingPlaneCount, plane);
            this.clippingPlaneCount(clippingPlaneCount + 1);

            return this;
        }
    }
}
