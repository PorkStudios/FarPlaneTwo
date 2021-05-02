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
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerEdge;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerEdge
 */
@UtilityClass
public class JavaFastLayerEdge {
    public static IFastLayer makeFast(@NonNull GenLayerEdge vanilla) {
        switch (vanilla.mode) {
            case COOL_WARM:
                return new CoolWarm(vanilla.worldGenSeed);
            case HEAT_ICE:
                return new HeatIce(vanilla.worldGenSeed);
            case SPECIAL:
                return new Special(vanilla.worldGenSeed);
            default:
                throw new IllegalStateException(vanilla.mode.toString());
        }
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#COOL_WARM
     */
    public static class CoolWarm extends AbstractFastLayer {
        public CoolWarm(long seed) {
            super(seed);
        }

        @Override
        public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
            int center, v0, v1, v2, v3;

            int[] arr = alloc.get(3 * 3);
            try {
                this.child.getGrid(alloc, x - 1, z - 1, 3, 3, arr);

                v0 = arr[1];
                v2 = arr[3];
                center = arr[4];
                v1 = arr[5];
                v3 = arr[7];
            } finally {
                alloc.release(arr);
            }

            return center == 1 && (v0 == 3 || v0 == 4 || v1 == 3 || v1 == 4 || v2 == 3 || v2 == 4 || v3 == 3 || v3 == 4)
                    ? 2
                    : center;
        }
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#HEAT_ICE
     */
    public static class HeatIce extends AbstractFastLayer {
        public HeatIce(long seed) {
            super(seed);
        }

        @Override
        public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
            int center, v0, v1, v2, v3;

            int[] arr = alloc.get(3 * 3);
            try {
                this.child.getGrid(alloc, x - 1, z - 1, 3, 3, arr);

                v0 = arr[1];
                v2 = arr[3];
                center = arr[4];
                v1 = arr[5];
                v3 = arr[7];
            } finally {
                alloc.release(arr);
            }

            return center == 4 && (v0 == 1 || v0 == 2 || v1 == 1 || v1 == 2 || v2 == 1 || v2 == 2 || v3 == 1 || v3 == 2)
                    ? 3
                    : center;
        }
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#SPECIAL
     */
    public static class Special extends AbstractFastLayer {
        public Special(long seed) {
            super(seed);
        }

        @Override
        public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
            int v = this.child.getSingle(alloc, x, z);

            if (v != 0) {
                long state = start(this.seed, x, z);
                if (nextInt(state, 13) == 0) {
                    state = update(state, this.seed);
                    v |= (nextInt(state, 15) + 1) << 8;
                }
            }

            return v;
        }
    }
}
