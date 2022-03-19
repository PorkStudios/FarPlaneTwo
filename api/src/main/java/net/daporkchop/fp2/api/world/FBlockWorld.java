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
 *
 */

package net.daporkchop.fp2.api.world;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;

import java.util.stream.Stream;

/**
 * A read-only world consisting of voxels at integer coordinates, where each voxel contains the following information:<br>
 * <ul>
 *     <li>A state (represented as an {@code int}, as returned by the corresponding methods in {@link FGameRegistry})</li>
 *     <li>A biome (represented by an {@code int}, as returned by the corresponding methods in {@link FGameRegistry})</li>
 *     <li>A block light level and sky light level (represented as a {@code byte}, as returned by {@link BlockWorldConstants#packLight(int, int)}). Light levels are unsigned nibbles (4-bit integers),
 *     where {@code 0} is the darkest and {@code 15} is the brightest possible value.</li>
 * </ul>
 * <p>
 * Implementations <strong>must</strong> be thread-safe.
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
public interface FBlockWorld extends AutoCloseable {
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
     * @return whether or not this world allows generating data which is not known
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
     * Checks whether or not <strong>any</strong> data in the given AABB is known.
     * <p>
     * For worlds which allow generating unknown data, data which could be generated on-demand does not count (as it would always return {@code null}, and therefore be
     * rather useless).
     * <p>
     * Any points in the given AABB which extend outside of the world's data limits (as defined by {@link #dataLimits()}) are ignored.
     *
     * @param minX the minimum X coordinate (inclusive)
     * @param minY the minimum Y coordinate (inclusive)
     * @param minZ the minimum Z coordinate (inclusive)
     * @param maxX the maximum X coordinate (exclusive)
     * @param maxY the maximum Y coordinate (exclusive)
     * @param maxZ the maximum Z coordinate (exclusive)
     * @return whether or not any block data in the given AABB is known
     */
    boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    /**
     * Checks whether or not <strong>any</strong> data in the given AABB is known.
     *
     * @see #containsAnyData(int, int, int, int, int, int)
     */
    default boolean containsAnyData(@NonNull IntAxisAlignedBB bb) {
        return this.containsAnyData(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
    }

    /**
     * Checks whether or not <strong>any</strong> data in any of the given AABBs is known.
     *
     * @see #containsAnyData(int, int, int, int, int, int)
     */
    default boolean containsAnyData(@NonNull Stream<IntAxisAlignedBB> bbs) {
        return bbs.anyMatch(this::containsAnyData);
    }

    /**
     * Gets the AABB in which block data is guaranteed to be known if all the block data in the given AABB is known.
     * <p>
     * What this method actually does is return the volume which is guaranteed to be generated if every point in the given AABB is generated. In other words, if generation is
     * disallowed and a query to every point in the given AABB would succeed (i.e. wouldn't throw {@link GenerationNotAllowedException}), it is safe to assume that a query to
     * any point in the AABB(s) returned by this method would succeed.
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
     * Gets the AABB in which block data is guaranteed to be known if all the block data in the given AABB is known.
     *
     * @see #guaranteedDataAvailableVolume(int, int, int, int, int, int)
     */
    default IntAxisAlignedBB guaranteedDataAvailableVolume(@NonNull IntAxisAlignedBB bb) {
        return this.guaranteedDataAvailableVolume(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ());
    }

    //
    // TYPE-SPECIFIC DATA QUERIES: STATES
    //

    /**
     * Gets the state at the position described by the given coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the state
     * @throws GenerationNotAllowedException if the data at the given coordinates is not generated and this world doesn't allow generation
     */
    int getState(int x, int y, int z) throws GenerationNotAllowedException;

    default void getStates(
            @NonNull int[] dst, int dstOff, int dstStride,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) throws GenerationNotAllowedException {
        this.getData(dst, dstOff, dstStride, null, 0, 0, null, 0, 0, minX, minY, minZ, maxX, maxY, maxZ);
    }

    default void getStates(
            @NonNull int[] dst, int dstOff, int dstStride,
            int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) throws GenerationNotAllowedException {
        this.getData(dst, dstOff, dstStride, null, 0, 0, null, 0, 0, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    default void getStates(
            @NonNull int[] dst, int dstOff, int dstStride,
            @NonNull int[] xs, int xOff, int xStride,
            @NonNull int[] ys, int yOff, int yStride,
            @NonNull int[] zs, int zOff, int zStride,
            int count) throws GenerationNotAllowedException {
        this.getData(dst, dstOff, dstStride, null, 0, 0, null, 0, 0, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
    }

    //
    // TYPE-SPECIFIC DATA QUERIES: BIOMES
    //

    /**
     * Gets the biome at the position described by the given coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the biome
     * @throws GenerationNotAllowedException if the data at the given coordinates is not generated and this world doesn't allow generation
     */
    int getBiome(int x, int y, int z) throws GenerationNotAllowedException;

    default void getBiomes(
            @NonNull int[] dst, int dstOff, int dstStride,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) throws GenerationNotAllowedException {
        this.getData(null, 0, 0, dst, dstOff, dstStride, null, 0, 0, minX, minY, minZ, maxX, maxY, maxZ);
    }

    default void getBiomes(
            @NonNull int[] dst, int dstOff, int dstStride,
            int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) throws GenerationNotAllowedException {
        this.getData(null, 0, 0, dst, dstOff, dstStride, null, 0, 0, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    default void getBiomes(
            @NonNull int[] dst, int dstOff, int dstStride,
            @NonNull int[] xs, int xOff, int xStride,
            @NonNull int[] ys, int yOff, int yStride,
            @NonNull int[] zs, int zOff, int zStride,
            int count) throws GenerationNotAllowedException {
        this.getData(null, 0, 0, dst, dstOff, dstStride, null, 0, 0, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
    }

    //
    // TYPE-SPECIFIC DATA QUERIES: LIGHT
    //

    /**
     * Gets the packed block/sky light at the position described by the given coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the packed light levels
     * @throws GenerationNotAllowedException if the data at the given coordinates is not generated and this world doesn't allow generation
     */
    byte getLight(int x, int y, int z) throws GenerationNotAllowedException;

    default void getLights(
            @NonNull byte[] dst, int dstOff, int dstStride,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) throws GenerationNotAllowedException {
        this.getData(null, 0, 0, null, 0, 0, dst, dstOff, dstStride, minX, minY, minZ, maxX, maxY, maxZ);
    }

    default void getLights(
            @NonNull byte[] dst, int dstOff, int dstStride,
            int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) throws GenerationNotAllowedException {
        this.getData(null, 0, 0, null, 0, 0, dst, dstOff, dstStride, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    default void getLights(
            @NonNull byte[] dst, int dstOff, int dstStride,
            @NonNull int[] xs, int xOff, int xStride,
            @NonNull int[] ys, int yOff, int yStride,
            @NonNull int[] zs, int zOff, int zStride,
            int count) throws GenerationNotAllowedException {
        this.getData(null, 0, 0, null, 0, 0, dst, dstOff, dstStride, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
    }

    //
    // BULK DATA QUERIES
    //

    default void getData(
            int[] states, int statesOff, int statesStride,
            int[] biomes, int biomesOff, int biomesStride,
            byte[] light, int lightOff, int lightStride,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) throws GenerationNotAllowedException {
        BlockWorldConstants.validateArgsForGetData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, minX, minY, minZ, maxX, maxY, maxZ);

        for (int statesIndex = statesOff, biomesIndex = biomesOff, lightIndex = lightOff, x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++, statesIndex += statesStride, biomesIndex += biomesStride, lightIndex += lightStride) {
                    if (states != null) {
                        states[statesIndex] = this.getState(x, y, z);
                    }
                    if (biomes != null) {
                        biomes[biomesIndex] = this.getBiome(x, y, z);
                    }
                    if (light != null) {
                        light[lightIndex] = this.getLight(x, y, z);
                    }
                }
            }
        }
    }

    default void getData(
            int[] states, int statesOff, int statesStride,
            int[] biomes, int biomesOff, int biomesStride,
            byte[] light, int lightOff, int lightStride,
            int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) throws GenerationNotAllowedException {
        BlockWorldConstants.validateArgsForGetData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);

        for (int statesIndex = statesOff, biomesIndex = biomesOff, lightIndex = lightOff, dx = 0; dx < sizeX * strideX; dx++) {
            for (int dy = 0; dy < sizeY * strideY; dy++) {
                for (int dz = 0; dz < sizeZ * strideZ; dz++, statesIndex += statesStride, biomesIndex += biomesStride, lightIndex += lightStride) {
                    if (states != null) {
                        states[statesIndex] = this.getState(x + dx, y + dy, z + dz);
                    }
                    if (biomes != null) {
                        biomes[biomesIndex] = this.getBiome(x + dx, y + dy, z + dz);
                    }
                    if (light != null) {
                        light[lightIndex] = this.getLight(x + dx, y + dy, z + dz);
                    }
                }
            }
        }
    }

    default void getData(
            int[] states, int statesOff, int statesStride,
            int[] biomes, int biomesOff, int biomesStride,
            byte[] light, int lightOff, int lightStride,
            @NonNull int[] xs, int xOff, int xStride,
            @NonNull int[] ys, int yOff, int yStride,
            @NonNull int[] zs, int zOff, int zStride,
            int count) throws GenerationNotAllowedException {
        BlockWorldConstants.validateArgsForGetData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);

        for (int i = 0, statesIndex = statesOff, biomesIndex = biomesOff, lightIndex = lightOff, xIndex = xOff, yIndex = yOff, zIndex = zStride; i < count; i++, statesIndex += statesStride, biomesIndex += biomesStride, lightIndex += lightStride, xIndex += xStride, yIndex += yStride, zIndex += zStride) {
            if (states != null) {
                states[statesIndex] = this.getState(xs[xIndex], ys[yIndex], zs[zIndex]);
            }
            if (biomes != null) {
                biomes[biomesIndex] = this.getBiome(xs[xIndex], ys[yIndex], zs[zIndex]);
            }
            if (light != null) {
                light[lightIndex] = this.getLight(xs[xIndex], ys[yIndex], zs[zIndex]);
            }
        }
    }
}
