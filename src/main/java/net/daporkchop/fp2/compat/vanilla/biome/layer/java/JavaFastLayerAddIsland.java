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
import net.minecraft.world.gen.layer.GenLayerAddIsland;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerAddIsland
 */
public class JavaFastLayerAddIsland extends AbstractFastLayer implements IJavaPaddedLayer {
    public JavaFastLayerAddIsland(long seed) {
        super(seed);
    }

    @Override
    public int[] offsets(int inSizeX, int inSizeZ) {
        return IJavaPaddedLayer.offsetsCorners(inSizeX, inSizeZ);
    }

    @Override
    public int eval0(int x, int z, int center, @NonNull int[] v) {
        if (center != 0 || (v[0] | v[1] | v[2] | v[3]) == 0) {
            if (center != 0 && (v[0] == 0 || v[1] == 0 || v[2] == 0 || v[3] == 0)) {
                long state = start(this.seed, x, z);
                if (nextInt(state, 5) == 0) {
                    return center == 4 ? 4 : 0;
                }
            }
            return center;
        } else {
            long state = start(this.seed, x, z);
            int limit = 1;
            int next = 1;

            if (v[0] != 0) {
                if (nextInt(state, limit++) == 0) {
                    next = v[0];
                }
                state = update(state, this.seed);
            }
            if (v[1] != 0) {
                if (nextInt(state, limit++) == 0) {
                    next = v[1];
                }
                state = update(state, this.seed);
            }
            if (v[2] != 0) {
                if (nextInt(state, limit++) == 0) {
                    next = v[2];
                }
                state = update(state, this.seed);
            }
            if (v[3] != 0) {
                if (nextInt(state, limit) == 0) {
                    next = v[3];
                }
                state = update(state, this.seed);
            }

            if (nextInt(state, 3) == 0) {
                return next;
            } else {
                return next == 4 ? 4 : 0;
            }
        }
    }
}
