/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.engine;

import java.util.Arrays;

import static net.daporkchop.fp2.core.engine.VoxelConstants.*;

/**
 * Represents a single data sample contained in a voxel tile.
 *
 * @author DaPorkchop_
 */
public class VoxelData {
    //vertex position and mesh intersection data
    public int x;
    public int y;
    public int z;
    public int edges;

    //block data (for texturing and shading)
    public final int[] states = new int[EDGE_COUNT];
    public int biome;
    public byte light;

    /**
     * Resets this instance.
     *
     * @return this instance
     */
    public VoxelData reset() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.edges = 0;
        Arrays.fill(this.states, 0);
        this.biome = 0;
        this.light = 0;
        return this;
    }
}
