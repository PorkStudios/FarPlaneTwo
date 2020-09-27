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

package net.daporkchop.fp2.mode.voxel.server.gen;

import lombok.experimental.UtilityClass;

/**
 * Constants and helpers to be used by voxel generators.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class VoxelGeneratorConstants {
    /**
     * The maximum number of edges intersecting the surface that may be considered by the QEF.
     */
    public static final int MAX_EDGES = 6;

    /**
     * Defines all of the edges of a cube based on their point indices.
     */
    public static final int[] EDGEVMAP = {
            0, 4, 1, 5, 2, 6, 3, 7, // x-axis
            0, 2, 1, 3, 4, 6, 5, 7, // y-axis
            0, 1, 2, 3, 4, 5, 6, 7  // z-axis
    };

    /**
     * The number of vertex indices emitted on a connected edge.
     */
    public static final int CONNECTION_INDEX_COUNT = 6;

    /**
     * Defines the offsets of all connections for all vertices on the three flagged edges on a voxel.
     */
    public static final int[] CONNECTION_INDICES = {
            0, 1, 3, 0, 2, 3,
            0, 1, 5, 0, 4, 5,
            0, 2, 6, 0, 4, 6
    };

    /**
     * The number of high-resolution neighbor voxels for a low-resolution connected edge.
     */
    public static final int CONNECTION_SUB_NEIGHBOR_COUNT = 6;

    /**
     * Defines the offsets of all high-resolution neighbor voxels for all connectable edges on a low-resolution connected voxel.
     */
    public static final int[] CONNECTION_SUB_NEIGHBORS = {
            1, 2, 3, 5, 6, 7,
            2, 3, 4, 5, 6, 7,
            1, 3, 4, 5, 6, 7
    };
}
