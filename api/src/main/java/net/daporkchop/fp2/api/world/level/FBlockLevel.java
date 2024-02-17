/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.ThreadSafe;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

/**
 * A read-only world consisting of voxels at integer coordinates.
 * <p>
 * Implementations <strong>must</strong> be thread-safe.
 * <p>
 * <h2>Voxel Properties</h2>
 * <p>
 * Each voxel contains values representing the following properties:<br>
 * <ul>
 *     <li>A state (represented as an {@code int}, as returned by the corresponding methods in {@link FGameRegistry})</li>
 *     <li>A biome (represented by an {@code int}, as returned by the corresponding methods in {@link FGameRegistry})</li>
 *     <li>A block light level and sky light level (represented as a {@code byte}, as returned by {@link BlockLevelConstants#packLight(int, int)}). Light levels are unsigned nibbles (4-bit integers),
 *     where {@code 0} is the darkest and {@code 15} is the brightest possible value.</li>
 * </ul>
 * More per-voxel properties may be added in the future as necessary.
 * <p>
 * <h2>Data Availability</h2>
 * <p>
 * Data at a given voxel position is in one of three possible availability states:<br>
 * <ul>
 *     <li><strong>Exists</strong>: the data at the given position exists and is accessible.</li>
 *     <li><strong>Ungenerated</strong>: the data at the given position has not yet been generated, and does not exist. Depending on whether the world
 *     {@link #generationAllowed() allows generation}, attempting to access ungenerated data will either cause the data to be generated (and therefore transition to
 *     <i>Exists</i> for future queries), or will cause {@link GenerationNotAllowedException} to be thrown.</li>
 *     <li><strong>Out-of-Bounds</strong>: the given position is outside of the world's {@link #dataLimits() coordinate limits} and cannot exist in any case. Attempts
 *     to access out-of-bounds data will always succeed, but will return some predetermined value (possibly dependent on the position) rather than "real" data. However,
 *     even though out-of-bounds data is always accessible, it is not considered by methods such as {@link #containsAnyData}, as they report <i>existence</i> of data
 *     rather than <i>accessibility</i> of data.</li>
 * </ul>
 * <p>
 * Data availability is per-position and not specific to individual voxel properties.
 * <p>
 * <h2>Bulk Operations</h2>
 * <p>
 * All bulk operations write their output to arrays. 2-dimensional queries operate over a square area and write their output in {@code X,Z} coordinate order, while
 * 3-dimensional queries operate over a cubic volume and write their output in {@code X,Y,Z} coordinate order.
 * <p>
 * Additionally, bulk queries accept a {@code stride} parameter, which defines the spacing between sampled voxels. There are three distinct cases for this value:<br>
 * <ul>
 *     <li>All strides {@code <= 0} are invalid, and produce undefined behavior.</li>
 *     <li>A stride of exactly {@code 1} indicates that the voxels to be sampled are tightly packed. Implementations are required to return the exact information which would
 *     exist at the given position.</li>
 *     <li>All strides {@code > 1} indicate that voxels to be sampled have the given spacing between each voxel. Samples are taken every {@code stride} voxels, where each sample
 *     consists of a value which most accurately represents all of the voxels in the {@code stride²} area (for the 2-dimensional case) or {@code stride³} volume (for the 3-dimensional case).</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
@ThreadSafe
public interface FBlockLevel extends AutoCloseable {
    /**
     * Closes this world, immediately releasing any internally allocated resources.
     * <p>
     * Once closed, all of this instance's methods will produce undefined behavior when called.
     * <p>
     * If not manually closed, a world will be implicitly closed when the instance is garbage-collected.
     */
    @Override
    void close();

    //
    // STATIC WORLD INFORMATION
    //

    /**
     * @return the {@link FGameRegistry} instance used by this world
     */
    FGameRegistry registry();

    /**
     * @return whether this world allows generating data which is not known
     */
    boolean generationAllowed();

    /**
     * Gets the AABB in which this world contains data.
     * <p>
     * This is not the volume in which queries may be made, but rather the volume in which valid data may be returned. Queries may be made at any valid 32-bit integer coordinates,
     * however all queries outside of the data limits will return some predetermined value.
     *
     * @return the AABB in which this world contains data
     */
    IntAxisAlignedBB dataLimits();

    //
    // DATA AVAILABILITY
    //

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
     * Gets the AABB in which block data is guaranteed to be accessible if all the block data in the given AABB is accessible.
     * <p>
     * In other words, if generation is disallowed and a query to every point in the given AABB would succeed, it is safe to assume that querying any point in the AABB(s)
     * returned by this method would be successful.
     * <p>
     * If the world is broken up into smaller pieces (chunks/cubes/columns/shards/whatever-you-want-to-call-them), accessing block data requires these pieces to be loaded
     * into memory. Depending on the size of pieces, the size of the query, and the query's alignment relative to the pieces it would load, much of the loaded data could
     * end up unused. This method provides a way for users to retrieve the volume of data which would actually be accessible for a given query, therefore allowing users to
     * increase their query size to minimize wasted resources. Since a single block in a piece being generated means that all of the data in the piece has been generated,
     * it is obviously safe to issue a query anywhere in the piece without causing issues.
     * <p>
     * Note that the AABB(s) returned by this method may extend beyond the world's {@link #dataLimits() data limits}.
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

    //
    // INDIVIDUAL DATA QUERIES
    //

    /**
     * Gets the state at the given voxel position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the state
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this world doesn't allow generation
     */
    int getState(int x, int y, int z) throws GenerationNotAllowedException;

    /**
     * Gets the biome at the given voxel position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the biome
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this world doesn't allow generation
     */
    int getBiome(int x, int y, int z) throws GenerationNotAllowedException;

    /**
     * Gets the packed block/sky light at the given voxel position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the packed light levels
     * @throws GenerationNotAllowedException if the data at the given voxel position is not generated and this world doesn't allow generation
     */
    byte getLight(int x, int y, int z) throws GenerationNotAllowedException;

    //
    // BULK DATA QUERIES
    //

    /**
     * Gets all data values for all voxels in a cuboid defined by an origin point and a size along each axis.
     * <p>
     * This will iterate over all voxels in the given cuboid volume and write the retrieved data for the sampled voxels into the given arrays.
     * <p>
     * If any exception is thrown, the contents of the referenced ranges of the given arrays is undefined.
     *
     * @param originX   the origin X coordinate
     * @param originY   the origin Y coordinate
     * @param originZ   the origin Z coordinate
     * @param sizeX     the grid's size along the X axis
     * @param sizeY     the grid's size along the Y axis
     * @param sizeZ     the grid's size along the Z axis
     * @param states    the array for retrieved state data to be stored in, or {@code null} if state data is not requested
     * @param statesOff the offset into the {@code states} array to begin writing into
     * @param biomes    the array for retrieved biome data to be stored in, or {@code null} if biome data is not requested
     * @param biomesOff the offset into the {@code biomes} array to begin writing into
     * @param lights    the array for retrieved block/sky light data to be stored in, or {@code null} if block/sky light data is not requested
     * @param lightsOff the offset into the {@code lights} array to begin writing into
     * @throws GenerationNotAllowedException if the data at any of the given voxel positions is not generated and this world doesn't allow generation
     */
    default void multiGetDense(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ,
            int[] states, @NotNegative int statesOff,
            int[] biomes, @NotNegative int biomesOff,
            byte[] lights, @NotNegative int lightsOff) throws GenerationNotAllowedException {
        int totalSamples = BlockLevelConstants.validateDenseGridBounds(originX, originY, originZ, sizeX, sizeY, sizeZ);

        if (states == null && biomes == null && lights == null) { //nothing to do
            return;
        }

        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    int x = originX + dx;
                    int y = originY + dy;
                    int z = originZ + dz;

                    if (states != null) {
                        states[statesOff + i] = this.getState(x, y, z);
                    }
                    if (biomes != null) {
                        biomes[biomesOff + i] = this.getBiome(x, y, z);
                    }
                    if (lights != null) {
                        lights[lightsOff + i] = this.getLight(x, y, z);
                    }
                }
            }
        }
    }

    /**
     * Gets all data values for all voxels in a sparse grid defined by an origin point, a spacing between samples and a sample count along each axis.
     * <p>
     * This will iterate over all voxels in the given sampling grid and write the retrieved data for the sampled voxels into the given arrays.
     * <p>
     * If any exception is thrown, the contents of the referenced ranges of the given arrays is undefined.
     *
     * @param originX   the origin X coordinate. Must be aligned to a multiple of {@code strideX}
     * @param originY   the origin Y coordinate. Must be aligned to a multiple of {@code strideY}
     * @param originZ   the origin Z coordinate. Must be aligned to a multiple of {@code strideZ}
     * @param sizeX     the number of samples to take along the X axis
     * @param sizeY     the number of samples to take along the Y axis
     * @param sizeZ     the number of samples to take along the Z axis
     * @param zoom      {@code log2} of the distance between samples along each axis
     * @param states    the array for retrieved state data to be stored in, or {@code null} if state data is not requested
     * @param statesOff the offset into the {@code states} array to begin writing into
     * @param biomes    the array for retrieved biome data to be stored in, or {@code null} if biome data is not requested
     * @param biomesOff the offset into the {@code biomes} array to begin writing into
     * @param lights    the array for retrieved block/sky light data to be stored in, or {@code null} if block/sky light data is not requested
     * @param lightsOff the offset into the {@code lights} array to begin writing into
     * @throws GenerationNotAllowedException if the data at any of the given voxel positions is not generated and this world doesn't allow generation
     */
    default void multiGetSparse(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ,
            @NotNegative int zoom,
            int[] states, @NotNegative int statesOff,
            int[] biomes, @NotNegative int biomesOff,
            byte[] lights, @NotNegative int lightsOff) throws GenerationNotAllowedException {
        int totalSamples = BlockLevelConstants.validateSparseGridBounds(originX, originY, originZ, sizeX, sizeY, sizeZ, zoom);

        if (states == null && biomes == null && lights == null) { //nothing to do
            return;
        }

        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    int x = originX + (dx << zoom);
                    int y = originY + (dy << zoom);
                    int z = originZ + (dz << zoom);

                    if (states != null) {
                        states[statesOff + i] = this.getState(x, y, z);
                    }
                    if (biomes != null) {
                        biomes[biomesOff + i] = this.getBiome(x, y, z);
                    }
                    if (lights != null) {
                        lights[lightsOff + i] = this.getLight(x, y, z);
                    }
                }
            }
        }
    }
}
