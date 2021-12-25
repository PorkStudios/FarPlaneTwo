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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.java;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.AbstractFastLayer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.IFastLayer;
import net.minecraft.world.gen.layer.GenLayerEdge;

import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.BiomeHelper.*;

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
    public static class CoolWarm extends AbstractFastLayer implements IJavaPaddedLayer {
        public CoolWarm(long seed) {
            super(seed);
        }

        @Override
        public int[] offsets(int inSizeX, int inSizeZ) {
            return IJavaPaddedLayer.offsetsSides(inSizeX, inSizeZ);
        }

        @Override
        public int eval0(int x, int z, int center, @NonNull int[] v) {
            return center == 1 && (v[0] == 3 || v[0] == 4 || v[1] == 3 || v[1] == 4 || v[2] == 3 || v[2] == 4 || v[3] == 3 || v[3] == 4)
                    ? 2
                    : center;
        }
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#HEAT_ICE
     */
    public static class HeatIce extends AbstractFastLayer implements IJavaPaddedLayer {
        public HeatIce(long seed) {
            super(seed);
        }

        @Override
        public int[] offsets(int inSizeX, int inSizeZ) {
            return IJavaPaddedLayer.offsetsSides(inSizeX, inSizeZ);
        }

        @Override
        public int eval0(int x, int z, int center, @NonNull int[] v) {
            return center == 4 && (v[0] == 1 || v[0] == 2 || v[1] == 1 || v[1] == 2 || v[2] == 1 || v[2] == 2 || v[3] == 1 || v[3] == 2)
                    ? 3
                    : center;
        }
    }

    /**
     * @author DaPorkchop_
     * @see GenLayerEdge.Mode#SPECIAL
     */
    public static class Special extends AbstractFastLayer implements IJavaTranslationLayer {
        public Special(long seed) {
            super(seed);
        }

        @Override
        public int translate0(int x, int z, int value) {
            if (value != 0) {
                long state = start(this.seed, x, z);
                if (nextInt(state, 13) == 0) {
                    state = update(state, this.seed);
                    value |= (nextInt(state, 15) + 1) << 8;
                }
            }

            return value;
        }
    }
}
