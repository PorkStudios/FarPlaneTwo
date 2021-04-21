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

package net.daporkchop.fp2.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Helper methods for working with off-heap {@link AxisAlignedBB}s.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class DirectAABB {
    public static final int AABB_MINX_OFFSET = 0;
    public static final int AABB_MINY_OFFSET = AABB_MINX_OFFSET + Double.BYTES;
    public static final int AABB_MINZ_OFFSET = AABB_MINY_OFFSET + Double.BYTES;
    public static final int AABB_MAXX_OFFSET = AABB_MINZ_OFFSET + Double.BYTES;
    public static final int AABB_MAXY_OFFSET = AABB_MAXX_OFFSET + Double.BYTES;
    public static final int AABB_MAXZ_OFFSET = AABB_MAXY_OFFSET + Double.BYTES;

    public static final int AABB_SIZE = AABB_MAXZ_OFFSET + Double.BYTES;

    /**
     * Reads the given {@link AxisAlignedBB} from the given off-heap memroy address.
     *
     * @param addr the memory address to read from
     * @param aabb the {@link AxisAlignedBB} write to
     */
    public void load(long addr, @NonNull AxisAlignedBB aabb) {
        aabb.minX = PUnsafe.getDouble(addr + AABB_MINX_OFFSET);
        aabb.minY = PUnsafe.getDouble(addr + AABB_MINY_OFFSET);
        aabb.minZ = PUnsafe.getDouble(addr + AABB_MINZ_OFFSET);
        aabb.maxX = PUnsafe.getDouble(addr + AABB_MAXX_OFFSET);
        aabb.maxY = PUnsafe.getDouble(addr + AABB_MAXY_OFFSET);
        aabb.maxZ = PUnsafe.getDouble(addr + AABB_MAXZ_OFFSET);
    }

    /**
     * Writes the given {@link AxisAlignedBB} to the given off-heap memory address.
     *
     * @param aabb the {@link AxisAlignedBB} read from
     * @param addr the memory address to write to
     */
    public void store(@NonNull AxisAlignedBB aabb, long addr) {
        PUnsafe.putDouble(addr + AABB_MINX_OFFSET, aabb.minX);
        PUnsafe.putDouble(addr + AABB_MINY_OFFSET, aabb.minY);
        PUnsafe.putDouble(addr + AABB_MINZ_OFFSET, aabb.minZ);
        PUnsafe.putDouble(addr + AABB_MAXX_OFFSET, aabb.maxX);
        PUnsafe.putDouble(addr + AABB_MAXY_OFFSET, aabb.maxY);
        PUnsafe.putDouble(addr + AABB_MAXZ_OFFSET, aabb.maxZ);
    }

    /**
     * Checks whether or not the given off-heap AABB intersects the given {@link Volume}.
     *
     * @param addr   the address of the AABB
     * @param volume the {@link Volume}
     * @return whether or not the AABB intersects the {@link Volume}
     */
    public boolean intersects(long addr, @NonNull Volume volume) {
        return volume.intersects(
                PUnsafe.getDouble(addr + AABB_MINX_OFFSET), PUnsafe.getDouble(addr + AABB_MINY_OFFSET), PUnsafe.getDouble(addr + AABB_MINZ_OFFSET),
                PUnsafe.getDouble(addr + AABB_MAXX_OFFSET), PUnsafe.getDouble(addr + AABB_MAXY_OFFSET), PUnsafe.getDouble(addr + AABB_MAXZ_OFFSET));
    }

    /**
     * Checks whether or not the given off-heap AABB is contained by the given {@link Volume}.
     *
     * @param addr   the address of the AABB
     * @param volume the {@link Volume}
     * @return whether or not the AABB is contained by the {@link Volume}
     */
    public boolean containedBy(long addr, @NonNull Volume volume) {
        return volume.contains(
                PUnsafe.getDouble(addr + AABB_MINX_OFFSET), PUnsafe.getDouble(addr + AABB_MINY_OFFSET), PUnsafe.getDouble(addr + AABB_MINZ_OFFSET),
                PUnsafe.getDouble(addr + AABB_MAXX_OFFSET), PUnsafe.getDouble(addr + AABB_MAXY_OFFSET), PUnsafe.getDouble(addr + AABB_MAXZ_OFFSET));
    }

    /**
     * Checks whether or not the given off-heap AABB intersects the given {@link IFrustum}.
     *
     * @param addr    the address of the AABB
     * @param frustum the {@link IFrustum}
     * @return whether or not the AABB intersects the {@link IFrustum}
     */
    public boolean inFrustum(long addr, @NonNull IFrustum frustum) {
        return frustum.intersectsBB(
                PUnsafe.getDouble(addr + AABB_MINX_OFFSET), PUnsafe.getDouble(addr + AABB_MINY_OFFSET), PUnsafe.getDouble(addr + AABB_MINZ_OFFSET),
                PUnsafe.getDouble(addr + AABB_MAXX_OFFSET), PUnsafe.getDouble(addr + AABB_MAXY_OFFSET), PUnsafe.getDouble(addr + AABB_MAXZ_OFFSET));
    }
}
