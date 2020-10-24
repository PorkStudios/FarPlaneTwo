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

package net.daporkchop.fp2.util.compat.vanilla.biome.layer;

import static net.daporkchop.fp2.util.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 */
public class FastLayerAddIsland extends FastLayer {
    public FastLayerAddIsland(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(int x, int z) {
        int center = this.parent.getSingle(x, z);

        int v0 = this.parent.getSingle(x - 1, z);
        int v1 = this.parent.getSingle(x, z - 1);
        int v2 = this.parent.getSingle(x + 1, z);
        int v3 = this.parent.getSingle(x, z + 1);

        if (center != 0 || (v0 | v1 | v2 | v3) == 0) {
            if (center != 0 && (v0 == 0 || v1 == 0 || v2 == 0 || v3 == 0)) {
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

            if (v0 != 0) {
                if (nextInt(state, limit++) == 0) {
                    next = v0;
                }
                state = update(state, this.seed);
            }
            if (v1 != 0) {
                if (nextInt(state, limit++) == 0) {
                    next = v1;
                }
                state = update(state, this.seed);
            }
            if (v2 != 0) {
                if (nextInt(state, limit++) == 0) {
                    next = v2;
                }
                state = update(state, this.seed);
            }
            if (v3 != 0) {
                if (nextInt(state, limit) == 0) {
                    next = v3;
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
