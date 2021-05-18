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

package net.daporkchop.fp2.compat.vanilla.biome.layer.java;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * @author DaPorkchop_
 * @see GenLayerVoronoiZoom
 */
public class JavaFastLayerVoronoiZoom extends AbstractFastLayer implements IJavaZoomingLayer {
    protected static final float DIV_1024 = 1.0f / 1024.0f;

    public JavaFastLayerVoronoiZoom(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        x -= 2;
        z -= 2;

        int rndX = x & ~3;
        int rndZ = z & ~3;

        long state = start(this.seed, rndX + 0, rndZ + 0);
        float rxz0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = update(state, this.seed);
        float rxz1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = start(this.seed, rndX + 0, rndZ + 4);
        float rxZ0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = update(state, this.seed);
        float rxZ1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;
        state = start(this.seed, rndX + 4, rndZ + 0);
        float rXz0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;
        state = update(state, this.seed);
        float rXz1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = start(this.seed, rndX + 4, rndZ + 4);
        float rXZ0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;
        state = update(state, this.seed);
        float rXZ1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;

        float ddx = x & 3;
        float ddz = z & 3;
        float dxz = sq(ddz - rxz1) + sq(ddx - rxz0);
        float dXz = sq(ddz - rXz1) + sq(ddx - rXz0);
        float dxZ = sq(ddz - rxZ1) + sq(ddx - rxZ0);
        float dXZ = sq(ddz - rXZ1) + sq(ddx - rXZ0);

        if (dxz < dXz && dxz < dxZ && dxz < dXZ) {
            return this.child.getSingle(alloc, (x >> 2), (z >> 2));
        } else if (dXz < dxz && dXz < dxZ && dXz < dXZ) {
            return this.child.getSingle(alloc, (x >> 2) + 1, (z >> 2));
        } else if (dxZ < dxz && dxZ < dXz && dxZ < dXZ) {
            return this.child.getSingle(alloc, (x >> 2), (z >> 2) + 1);
        } else {
            return this.child.getSingle(alloc, (x >> 2) + 1, (z >> 2) + 1);
        }
    }

    @Override
    public void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        IJavaZoomingLayer.super.getGrid(alloc, x - 2, z - 2, sizeX, sizeZ, out);
    }

    @Override
    public void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        IJavaZoomingLayer.super.multiGetGrids(alloc, x - (2 << depth), z - (2 << depth), size, dist, depth, count, out);
    }

    @Override
    public int shift() {
        return 2;
    }

    @Override
    public void zoomTile0(int x, int z, @NonNull int[] v, @NonNull int[] out, int off, int size) {
        long state = start(this.seed, x + 0, z + 0);
        float rxz0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = update(state, this.seed);
        float rxz1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = start(this.seed, x + 0, z + 4);
        float rxZ0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = update(state, this.seed);
        float rxZ1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;
        state = start(this.seed, x + 4, z + 0);
        float rXz0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;
        state = update(state, this.seed);
        float rXz1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f;
        state = start(this.seed, x + 4, z + 4);
        float rXZ0 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;
        state = update(state, this.seed);
        float rXZ1 = (nextInt(state, 1024) * DIV_1024 - 0.5f) * 3.6f + 4.0f;

        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 4; dz++) {
                float dxz = sq(dz - rxz1) + sq(dx - rxz0);
                float dXz = sq(dz - rXz1) + sq(dx - rXz0);
                float dxZ = sq(dz - rxZ1) + sq(dx - rxZ0);
                float dXZ = sq(dz - rXZ1) + sq(dx - rXZ0);

                int val;
                if (dxz < dXz && dxz < dxZ && dxz < dXZ) {
                    val = v[0];
                } else if (dXz < dxz && dXz < dxZ && dXz < dXZ) {
                    val = v[1];
                } else if (dxZ < dxz && dxZ < dXz && dxZ < dXZ) {
                    val = v[2];
                } else {
                    val = v[3];
                }
                out[off + dx * size + dz] = val;
            }
        }
    }
}
