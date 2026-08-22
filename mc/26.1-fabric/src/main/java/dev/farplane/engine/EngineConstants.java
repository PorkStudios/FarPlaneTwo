/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2026 DaPorkchop_
 * Portions Copyright (c) 2026 FarPlane contributors
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

package dev.farplane.engine;

/**
 * Constants used throughout the voxel code.
 * Adapted from FarPlaneTwo {@code net.daporkchop.fp2.core.engine.EngineConstants}.
 *
 * @author DaPorkchop_ (original)
 */
public final class EngineConstants {
    private EngineConstants() {}

    // --- Tile dimensions ---
    public static final int T_SHIFT = 4;
    public static final int T_MASK = (1 << T_SHIFT) - 1;
    public static final int T_VOXELS = 1 << T_SHIFT;       // 16
    public static final int T_VERTS = T_VOXELS + 1;         // 17

    public static final int MAX_LODS = 32 - Integer.numberOfLeadingZeros(60_000_000 >> T_SHIFT);

    // --- Voxel vertex position fractional bits ---
    public static final int POS_FRACT_SHIFT = 3;
    public static final int POS_ONE = 1 << POS_FRACT_SHIFT; // 8

    // --- Edge definitions (dual-contouring) ---
    public static final int EDGE_COUNT = 3;

    /**
     * Defines the three edges from each voxel corner relevant to the renderer.
     * Pairs of corner indices: (from, to) for +X, +Y, +Z edges from the ++++ corner.
     */
    public static final int[] EDGE_VERTEX_MAP = {
            3, 7, // x-axis edge
            5, 7, // y-axis edge
            6, 7  // z-axis edge
    };

    public static final int EDGE_DIR_NONE = 0;
    public static final int EDGE_DIR_POSITIVE = 1;
    public static final int EDGE_DIR_NEGATIVE = 2;
    public static final int EDGE_DIR_BOTH = 3;
    public static final int EDGE_DIR_MASK = 3;

    // --- QEF (Quadratic Error Function) edges ---
    public static final int QEF_EDGE_COUNT = 12;
    public static final int[] QEF_EDGE_VERTEX_MAP = {
            0b000, 0b100, 0b001, 0b101, 0b001, 0b110, 0b011, 0b111, // x-axis
            0b000, 0b010, 0b001, 0b011, 0b100, 0b110, 0b101, 0b111, // y-axis
            0b000, 0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111  // z-axis
    };

    // --- Connection indices for neighbor stitching ---
    public static final int CONNECTION_INDEX_COUNT = 4;

    public static final int[] CONNECTION_INDICES = {
            0, 1, 2, 3,
            0, 1, 4, 5,
            0, 2, 4, 6
    };

    public static final int CONNECTION_SUB_NEIGHBOR_COUNT = 6;
    public static final int[] CONNECTION_SUB_NEIGHBORS = {
            1, 2, 3, 5, 6, 7,
            2, 3, 4, 5, 6, 7,
            1, 3, 4, 5, 6, 7
    };

    public static final int[] CONNECTION_INTERSECTION_VOLUMES = {
            T_VOXELS, T_VOXELS, T_VOXELS,
            T_VOXELS, T_VOXELS, 1,
            T_VOXELS, 1, T_VOXELS,
            T_VOXELS, 1, 1,
            1, T_VOXELS, T_VOXELS,
            1, T_VOXELS, 1,
            1, 1, T_VOXELS,
            1, 1, 1
    };

    // --- Block type classification ---
    public static final int BLOCK_TYPE_INVISIBLE = 0;
    public static final int BLOCK_TYPE_TRANSPARENT = 1;
    public static final int BLOCK_TYPE_CUTOUT = 2;
    public static final int BLOCK_TYPE_OPAQUE = 3;

    // --- Light packing ---
    public static int packLight(int skyLight, int blockLight) {
        return ((skyLight & 0xF) << 4) | (blockLight & 0xF);
    }

    public static int unpackSkyLight(byte packedLight) {
        return (packedLight >> 4) & 0xF;
    }

    public static int unpackBlockLight(byte packedLight) {
        return packedLight & 0xF;
    }
}
