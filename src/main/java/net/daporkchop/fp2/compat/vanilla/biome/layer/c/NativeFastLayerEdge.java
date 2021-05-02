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

package net.daporkchop.fp2.compat.vanilla.biome.layer.c;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.JavaFastLayerEdge;
import net.minecraft.world.gen.layer.GenLayerEdge;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class NativeFastLayerEdge {
    public static IFastLayer makeFast(@NonNull GenLayerEdge vanilla) {
        switch (vanilla.mode) {
            case COOL_WARM:
                return new CoolWarm(vanilla.worldGenSeed);
            case HEAT_ICE:
                return new HeatIce(vanilla.worldGenSeed);
            case SPECIAL:
                return new Special(vanilla.worldGenSeed);
            default:
                return JavaFastLayerEdge.makeFast(vanilla);
        }
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#COOL_WARM
     */
    public static class CoolWarm extends JavaFastLayerEdge.CoolWarm implements INativePaddedLayer {
        public CoolWarm(long seed) {
            super(seed);
        }

        @Override
        public native void getGrid0(long seed, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in);

        @Override
        public native void multiGetGridsCombined0(long seed, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);

        @Override
        public native void multiGetGridsIndividual0(long seed, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#HEAT_ICE
     */
    public static class HeatIce extends JavaFastLayerEdge.HeatIce implements INativePaddedLayer {
        public HeatIce(long seed) {
            super(seed);
        }

        @Override
        public native void getGrid0(long seed, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in);

        @Override
        public native void multiGetGridsCombined0(long seed, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);

        @Override
        public native void multiGetGridsIndividual0(long seed, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in);
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#SPECIAL
     */
    public static class Special extends JavaFastLayerEdge.Special implements INativeTranslationLayer {
        public Special(long seed) {
            super(seed);
        }

        @Override
        public native void getGrid0(long seed, int x, int z, int sizeX, int sizeZ, @NonNull int[] inout);

        @Override
        public native void multiGetGrids0(long seed, int x, int z, int size, int dist, int depth, int count, @NonNull int[] inout);
    }
}
