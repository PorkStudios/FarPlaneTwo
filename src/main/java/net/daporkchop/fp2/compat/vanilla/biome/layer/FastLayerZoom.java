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
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerZoom;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerZoom
 */
public class FastLayerZoom extends FastLayer {
    public FastLayerZoom(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        int lowX = x >> 1;
        int lowZ = z >> 1;

        if ((x & 1) == 0) {
            if ((z & 1) == 0) {
                return this.parent.getSingle(alloc, lowX, lowZ);
            } else {
                long state = start(this.seed, lowX << 1, lowZ << 1);
                return this.parent.getSingle(alloc, lowX, lowZ + nextInt(state, 2));
            }
        } else {
            if ((z & 1) == 0) {
                long state = start(this.seed, lowX << 1, lowZ << 1);
                state = update(state, this.seed);
                return this.parent.getSingle(alloc, lowX + nextInt(state, 2), lowZ);
            } else {
                return this.sampleXZLast(alloc, lowX, lowZ);
            }
        }
    }

    protected int sampleXZLast(IntArrayAllocator alloc, int lowX, int lowZ) {
        int xz, xZ, Xz, XZ;

        int[] arr = this.parent.getGrid(alloc, lowX, lowZ, 2, 2);
        try {
            xz = arr[0];
            xZ = arr[1];
            Xz = arr[2];
            XZ = arr[3];
        } finally {
            alloc.release(arr);
        }

        //here be branch predictor hell
        if (Xz == xZ && xZ == XZ) {
            return Xz;
        } else if (xz == Xz && xz == xZ) {
            return xz;
        } else if (xz == Xz && xz == XZ) {
            return xz;
        } else if (xz == xZ && xz == XZ) {
            return xz;
        } else if (xz == Xz && xZ != XZ) {
            return xz;
        } else if (xz == xZ && Xz != XZ) {
            return xz;
        } else if (xz == XZ && Xz != xZ) {
            return xz;
        } else if (Xz == xZ && xz != XZ) {
            return Xz;
        } else if (Xz == XZ && xz != xZ) {
            return Xz;
        } else if (xZ == XZ && xz != Xz) {
            return xZ;
        } else {
            //random
            long state = start(this.seed, lowX << 1, lowZ << 1);
            state = update(state, this.seed);
            state = update(state, this.seed);
            switch (nextInt(state, 4)) {
                case 0:
                    return xz;
                case 1:
                    return Xz;
                case 2:
                    return xZ;
                case 3:
                    return XZ;
            }
            throw new IllegalStateException();
        }
    }
}
