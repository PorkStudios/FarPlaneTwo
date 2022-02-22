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
import net.daporkchop.fp2.api.world.registry.FGameRegistry;

import static net.daporkchop.lib.common.util.PValidation.*;

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

    /**
     * @return the {@link FGameRegistry} instance used by this world
     */
    FGameRegistry registry();

    /**
     * Checks whether or not <strong>any</strong> data in the given AABB is known.
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
     * Gets the state at the position described by the given coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the state
     */
    int getState(int x, int y, int z);

    default void getStates(@NonNull int[] dst, int dstOff, int dstStride,
                           int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) {
        this.getData(dst, dstOff, dstStride, null, 0, 0, null, 0, 0, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    default void getStates(@NonNull int[] dst, int dstOff, int dstStride,
                           @NonNull int[] xs, int xOff, int xStride,
                           @NonNull int[] ys, int yOff, int yStride,
                           @NonNull int[] zs, int zOff, int zStride,
                           int count) {
        this.getData(dst, dstOff, dstStride, null, 0, 0, null, 0, 0, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
    }

    /**
     * Gets the biome at the position described by the given coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the biome
     */
    int getBiome(int x, int y, int z);

    default void getBiomes(@NonNull int[] dst, int dstOff, int dstStride,
                           int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) {
        this.getData(null, 0, 0, dst, dstOff, dstStride, null, 0, 0, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    default void getBiomes(@NonNull int[] dst, int dstOff, int dstStride,
                           @NonNull int[] xs, int xOff, int xStride,
                           @NonNull int[] ys, int yOff, int yStride,
                           @NonNull int[] zs, int zOff, int zStride,
                           int count) {
        this.getData(null, 0, 0, dst, dstOff, dstStride, null, 0, 0, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
    }

    /**
     * Gets the packed block/sky light at the position described by the given coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the packed light levels
     */
    byte getLight(int x, int y, int z);

    default void getLights(@NonNull byte[] dst, int dstOff, int dstStride,
                           int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) {
        this.getData(null, 0, 0, null, 0, 0, dst, dstOff, dstStride, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    default void getLights(@NonNull byte[] dst, int dstOff, int dstStride,
                           @NonNull int[] xs, int xOff, int xStride,
                           @NonNull int[] ys, int yOff, int yStride,
                           @NonNull int[] zs, int zOff, int zStride,
                           int count) {
        this.getData(null, 0, 0, null, 0, 0, dst, dstOff, dstStride, xs, xOff, xStride, ys, yOff, yStride, zs, zOff, zStride, count);
    }

    default void getData(int[] states, int statesOff, int statesStride,
                         int[] biomes, int biomesOff, int biomesStride,
                         byte[] light, int lightOff, int lightStride,
                         int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) {
        int count = positive(sizeX, "sizeX") * positive(sizeY, "sizeY") * positive(sizeZ, "sizeZ");
        if (states != null) {
            checkRangeLen(states.length, statesOff, positive(statesStride, "statesStride") * count);
        }
        if (biomes != null) {
            checkRangeLen(biomes.length, biomesOff, positive(biomesStride, "biomesStride") * count);
        }
        if (light != null) {
            checkRangeLen(light.length, lightOff, positive(lightStride, "lightStride") * count);
        }
        positive(strideX, "strideX");
        positive(strideY, "strideY");
        positive(strideZ, "strideZ");

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

    default void getData(int[] states, int statesOff, int statesStride,
                         int[] biomes, int biomesOff, int biomesStride,
                         byte[] light, int lightOff, int lightStride,
                         @NonNull int[] xs, int xOff, int xStride,
                         @NonNull int[] ys, int yOff, int yStride,
                         @NonNull int[] zs, int zOff, int zStride,
                         int count) {
        if (states != null) {
            checkRangeLen(states.length, statesOff, positive(statesStride, "statesStride") * count);
        }
        if (biomes != null) {
            checkRangeLen(biomes.length, biomesOff, positive(biomesStride, "biomesStride") * count);
        }
        if (light != null) {
            checkRangeLen(light.length, lightOff, positive(lightStride, "lightStride") * count);
        }
        checkRangeLen(xs.length, xOff, positive(xStride, "xStride") * count);
        checkRangeLen(ys.length, yOff, positive(yStride, "yStride") * count);
        checkRangeLen(zs.length, zOff, positive(zStride, "zStride") * count);

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
