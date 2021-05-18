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

package net.daporkchop.fp2.compat.vanilla.biome.layer;

import lombok.NonNull;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.gen.layer.GenLayer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * Base interface for a faster alternative to {@link GenLayer}.
 * <p>
 * Once initialized, instances of this class are expected to be safely usable from multiple threads.
 *
 * @author DaPorkchop_
 */
public interface IFastLayer {
    Set<Class<? extends IFastLayer>> __HAS_LOGGED_GRID_WARNING = ConcurrentHashMap.newKeySet();
    Set<Class<? extends IFastLayer>> __HAS_LOGGED_MULTIGRID_WARNING = ConcurrentHashMap.newKeySet();

    /**
     * Initializes this layer.
     *
     * @param children the child layers that this layer should be initialized with
     */
    void init(@NonNull IFastLayer[] children);

    /**
     * Gets a single value at the given coordinates.
     *
     * @param alloc an {@link ArrayAllocator} to use for allocating {@code int[]}s
     * @param x     the X coordinate of the value to get
     * @param z     the Z coordinate of the value to get
     * @return the value
     */
    int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z);

    /**
     * Gets a grid of the given size at the given coordinates.
     *  @param alloc an {@link ArrayAllocator} to use for allocating {@code int[]}s
     * @param x     the grid's base X coordinate
     * @param z     the grid's base Z coordinate
     * @param sizeX the size of the grid along the X axis
     * @param sizeZ the size of the grid along the Z axis
     * @param out   the {@code int[]} to write to
     */
    default void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        if (__HAS_LOGGED_GRID_WARNING.add(this.getClass())) {
            FP2_LOG.warn("{} does not override getGrid(), falling back to slow implementation...", this.getClass().getCanonicalName());
        }

        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                out[i++] = this.getSingle(alloc, x + dx, z + dz);
            }
        }
    }

    /**
     * Gets a square grid of multiple square grids with the given spacing between each other.
     *  @param alloc an {@link ArrayAllocator} to use for allocating {@code int[]}s
     * @param x     the grid's base X coordinate
     * @param z     the grid's base Z coordinate
     * @param size  the size of each small grid
     * @param dist  the distance between the origin of each small grid
     * @param depth the recursion depth. Unless you are an {@link IFastLayer}, this should always be set to {@code 0}
     * @param count the number of smaller grids to generate
     * @param out   the {@code int[]} to write to
     */
    default void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        if (__HAS_LOGGED_MULTIGRID_WARNING.add(this.getClass())) {
            FP2_LOG.warn("{} does not override multiGetGrids(), falling back to slow implementation...", this.getClass().getCanonicalName());
        }

        int[] tmp = alloc.atLeast(size * size);
        try {
            for (int i = 0, gridX = 0; gridX < count; gridX++) {
                for (int gridZ = 0; gridZ < count; gridZ++, i += size * size) {
                    //get size*size grid (getGrid may have optimized implementation)
                    this.getGrid(alloc, mulAddShift(gridX, dist, x, depth), mulAddShift(gridZ, dist, z, depth), size, size, tmp);

                    //copy grid to output array
                    System.arraycopy(tmp, 0, out, i, size * size);
                }
            }
        } finally {
            alloc.release(tmp);
        }
    }
}
