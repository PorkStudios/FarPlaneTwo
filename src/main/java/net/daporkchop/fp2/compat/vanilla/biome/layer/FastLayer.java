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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class FastLayer {
    public static final long PARENT_OFFSET = PUnsafe.pork_getOffset(FastLayer.class, "parent");

    protected final long seed;
    protected final FastLayer parent = null;

    public void init(@NonNull FastLayer[] children) {
        PUnsafe.putObject(this, PARENT_OFFSET, children[0]);
    }

    /**
     * Gets a single value at the given coordinates.
     *
     * @param alloc an {@link IntArrayAllocator} to use for allocating {@code int[]}s
     * @param x     the X coordinate of the value to get
     * @param z     the Z coordinate of the value to get
     * @return the value
     */
    public abstract int getSingle(@NonNull IntArrayAllocator alloc, int x, int z);

    /**
     * Gets a grid of the given size at the given coordinates.
     *
     * @param alloc an {@link IntArrayAllocator} to use for allocating {@code int[]}s
     * @param x     the grid's base X coordinate
     * @param z     the grid's base Z coordinate
     * @param sizeX the size of the grid along the X axis
     * @param sizeZ the size of the grid along the Z axis
     * @param out   the {@code int[]} to write to
     */
    public void getGrid(@NonNull IntArrayAllocator alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                out[i++] = this.getSingle(alloc, x + dx, z + dz);
            }
        }
    }
}
