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
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerVoronoiZoom
 */
public class JavaFastLayerVoronoiZoom extends AbstractFastLayer {
    protected static final float DIV_1024 = 1.0f / 1024.0f;

    /**
     * @return whether or not the given grid request is properly grid-aligned, and therefore doesn't need to be padded
     */
    protected static boolean isAligned(int x, int z, int sizeX, int sizeZ) {
        return ((x | z | sizeX | sizeZ) & 3) == 0;
    }

    public JavaFastLayerVoronoiZoom(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
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
        float dxz = (ddz - rxz1) * (ddz - rxz1) + (ddx - rxz0) * (ddx - rxz0);
        float dXz = (ddz - rXz1) * (ddz - rXz1) + (ddx - rXz0) * (ddx - rXz0);
        float dxZ = (ddz - rxZ1) * (ddz - rxZ1) + (ddx - rxZ0) * (ddx - rxZ0);
        float dXZ = (ddz - rXZ1) * (ddz - rXZ1) + (ddx - rXZ0) * (ddx - rXZ0);

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
}
