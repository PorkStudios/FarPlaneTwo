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
public class FastLayerZoom extends FastLayer {
    public FastLayerZoom(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(int x, int z) {
        int lowX = x >> 1;
        int lowZ = z >> 1;

        if ((x & 1) == 0) {
            if ((z & 1) == 0) {
                return this.parent.getSingle(lowX, lowZ);
            } else {
                long state = start(this.seed, lowX << 1, lowZ << 1);
                return nextInt(state, 2) == 0 ? this.parent.getSingle(lowX, lowZ) : this.parent.getSingle(lowX, lowZ + 1);
            }
        } else {
            if ((z & 1) == 0) {
                long state = start(this.seed, lowX << 1, lowZ << 1);
                state = update(state, this.seed);
                return nextInt(state, 2) == 0 ? this.parent.getSingle(lowX, lowZ) : this.parent.getSingle(lowX + 1, lowZ);
            } else {
                return this.sampleXZLast(lowX, lowZ);
            }
        }
    }

    protected int sampleXZLast(int lowX, int lowZ) {
        int xz = this.parent.getSingle(lowX, lowZ);
        int Xz = this.parent.getSingle(lowX, lowZ + 1);
        int xZ = this.parent.getSingle(lowX + 1, lowZ);
        int XZ = this.parent.getSingle(lowX + 1, lowZ + 1);

        if (xZ == Xz && Xz == XZ) {
            return xZ;
        } else if (xz == xZ && xz == Xz) {
            return xz;
        } else if (xz == xZ && xz == XZ) {
            return xz;
        } else if (xz == Xz && xz == XZ) {
            return xz;
        } else if (xz == xZ && Xz != XZ) {
            return xz;
        } else if (xz == Xz && xZ != XZ) {
            return xz;
        } else if (xz == XZ && xZ != Xz) {
            return xz;
        } else if (xZ == Xz && xz != XZ) {
            return xZ;
        } else if (xZ == XZ && xz != Xz) {
            return xZ;
        } else if (Xz == XZ && xz != xZ) {
            return Xz;
        } else {
            //random
            long state = start(this.seed, lowX << 1, lowZ << 1);
            state = update(state, this.seed);
            state = update(state, this.seed);
            switch (nextInt(state, 4)) {
                case 0:
                    return xz;
                case 1:
                    return xZ;
                case 2:
                    return Xz;
                case 3:
                    return XZ;
            }
            throw new IllegalStateException();
        }
    }
}
