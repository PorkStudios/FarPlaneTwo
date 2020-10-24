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

package biome;

import net.daporkchop.fp2.util.compat.vanilla.biome.layer.FastLayer;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import org.junit.Test;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static net.daporkchop.fp2.util.compat.vanilla.biome.BiomeHelper.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestFastBiomeGen {
    static {
        GET_CHILDREN.put(GenLayerRandomValues.class, layer -> new GenLayer[0]);

        FAST_MAPPERS.put(GenLayerRandomValues.class, layer -> new FastLayerRandomValues(layer.worldGenSeed));
    }

    @Test
    public void testRandom() {
        this.testLayers(new GenLayerRandomValues(0L));
    }

    @Test
    public void testZoom() {
        this.testLayers(new GenLayerZoom(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testFuzzyZoom() {
        this.testLayers(new GenLayerFuzzyZoom(1L, new GenLayerRandomValues(0L)));
    }

    private void testLayers(GenLayer vanilla) {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        vanilla.initWorldGenSeed(r.nextLong());
        FastLayer fast = makeFast(vanilla)[0];

        this.testAreas(vanilla, fast, 0, 0, 2, 2);

        for (int i = 0; i < 256; i++) {
            this.testAreas(vanilla, fast, r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(256) + 1, r.nextInt(256) + 1);
        }
    }

    private void testAreas(GenLayer vanilla, FastLayer fast, int areaX, int areaZ, int sizeX, int sizeZ) {
        int[] reference = vanilla.getInts(areaX, areaZ, sizeX, sizeZ);
        IntCache.resetIntCache();

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                int referenceValue = reference[dz * sizeX + dx];
                int fastValue = fast.getSingle(areaX + dx, areaZ + dz);
                checkState(referenceValue == fastValue, "at (%d, %d): fast: %d != expected: %d", areaX + dx, areaZ + dz, fastValue, referenceValue);
            }
        }
    }
}
