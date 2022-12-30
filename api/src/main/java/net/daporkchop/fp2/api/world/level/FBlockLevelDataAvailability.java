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

package net.daporkchop.fp2.api.world.level;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.query.shape.AABBsQueryShape;

import java.util.BitSet;

/**
 * All data accessibility information exposed by {@link FBlockLevel}.
 *
 * @author DaPorkchop_
 * @see FBlockLevel for more documentation on data accessibility
 */
public interface FBlockLevelDataAvailability {
    /**
     * Gets the AABB in which this level contains data.
     * <p>
     * This is not the volume in which queries may be made, but rather the volume in which valid data may be returned. Queries may be made at any valid 32-bit integer coordinates,
     * however all queries outside the data limits will return some predetermined value.
     *
     * @return the AABB in which this level contains data
     */
    IntAxisAlignedBB dataLimits();

    /**
     * Checks whether <strong>any</strong> data in the given AABB exists.
     * <p>
     * Note that this checks for data which <i>exists</i>, not merely <i>accessible</i>. Voxel positions which are out-of-bounds or whose data could be generated if requested are
     * not considered by this.
     * <p>
     * Implementations <i>may</i> choose to allow false positives to be returned, but must <i>never</i> return false negatives.
     *
     * @param minX the minimum X coordinate (inclusive)
     * @param minY the minimum Y coordinate (inclusive)
     * @param minZ the minimum Z coordinate (inclusive)
     * @param maxX the maximum X coordinate (exclusive)
     * @param maxY the maximum Y coordinate (exclusive)
     * @param maxZ the maximum Z coordinate (exclusive)
     * @return whether any block data in the given AABB is known
     */
    boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    /**
     * Checks whether <strong>any</strong> data in the given AABB is known.
     *
     * @see #containsAnyData(int, int, int, int, int, int)
     */
    default boolean containsAnyData(@NonNull IntAxisAlignedBB bb) {
        return this.containsAnyData(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
    }

    /**
     * For each of the given AABBs: checks whether <strong>any</strong> data in the given AABB is known.
     *
     * @return a {@link BitSet} with each value set to the result of calling {@link #containsAnyData(IntAxisAlignedBB)} with the given AABB at the corresponding index
     * @see #containsAnyData(int, int, int, int, int, int)
     */
    default BitSet containsAnyData(@NonNull AABBsQueryShape query) {
        query.validate();
        int count = query.count();

        BitSet out = new BitSet(count);
        query.forEach((index, minX, minY, minZ, maxX, maxY, maxZ) -> out.set(index, this.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ)));
        return out;
    }

    /**
     * Gets the AABB in which block data is guaranteed to be accessible if all the block data in the given AABB is accessible.
     * <p>
     * In other words, if generation is disallowed and a query to every point in the given AABB would succeed, it is safe to assume that querying any point in the AABB(s)
     * returned by this method would be successful.
     * <p>
     * If the level is broken up into smaller pieces (chunks/cubes/columns/shards/whatever-you-want-to-call-them), accessing block data requires these pieces to be loaded
     * into memory. Depending on the size of pieces, the size of the query, and the query's alignment relative to the pieces it would load, much of the loaded data could
     * end up unused. This method provides a way for users to retrieve the volume of data which would actually be accessible for a given query, therefore allowing users to
     * increase their query size to minimize wasted resources. Since a single block in a piece being generated means that all of the data in the piece has been generated,
     * it is obviously safe to issue a query anywhere in the piece without causing issues.
     * <p>
     * Note that the AABB(s) returned by this method may extend beyond the level's {@link #dataLimits() data limits}.
     *
     * @param minX the minimum X coordinate (inclusive)
     * @param minY the minimum Y coordinate (inclusive)
     * @param minZ the minimum Z coordinate (inclusive)
     * @param maxX the maximum X coordinate (exclusive)
     * @param maxY the maximum Y coordinate (exclusive)
     * @param maxZ the maximum Z coordinate (exclusive)
     * @return the volume intersecting the given AABB which are guaranteed to be known if all the block data in the given AABB is known
     */
    default IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        //default: make no assumptions about internal data representation, return exactly the queried bounding box
        return new IntAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Gets the AABB in which block data is guaranteed to be accessible if all the block data in the given AABB is accessible.
     *
     * @see #guaranteedDataAvailableVolume(int, int, int, int, int, int)
     */
    default IntAxisAlignedBB guaranteedDataAvailableVolume(@NonNull IntAxisAlignedBB bb) {
        return this.guaranteedDataAvailableVolume(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
    }
}
