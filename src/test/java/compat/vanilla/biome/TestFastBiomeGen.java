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

package compat.vanilla.biome;

import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerIsland;
import net.daporkchop.fp2.compat.vanilla.biome.nativelayer.NativeFastLayerIsland;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerAddMushroomIsland;
import net.minecraft.world.gen.layer.GenLayerAddSnow;
import net.minecraft.world.gen.layer.GenLayerDeepOcean;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerRareBiome;
import net.minecraft.world.gen.layer.GenLayerRiver;
import net.minecraft.world.gen.layer.GenLayerRiverInit;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import org.junit.Test;

import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongFunction;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestFastBiomeGen {
    static {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }

        GET_CHILDREN.put(GenLayerRandomValues.class, layer -> new GenLayer[0]);

        FAST_MAPPERS.put(GenLayerRandomValues.class, layer -> new FastLayerRandomValues(layer.worldGenSeed, ((GenLayerRandomValues) layer).limit));
    }

    @Test
    public void testRandom() {
        this.testLayers(new GenLayerRandomValues(0L));
    }

    //

    @Test
    public void testAddIsland() {
        this.testLayers(new GenLayerAddIsland(1L, new GenLayerRandomValues(0L, 2)));
    }

    @Test
    public void testAddMushroomIsland() {
        this.testLayers(new GenLayerAddMushroomIsland(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testAddSnow() {
        this.testLayers(new GenLayerAddSnow(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testBiome() {
        //this.testLayers(new GenLayerBiome(1L, new GenLayerRandomValues(0L), null, null));
    }

    @Test
    public void testBiomeEdge() {
        //this.testLayers(new GenLayerBiomeEdge(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testDeepOcean() {
        this.testLayers(new GenLayerDeepOcean(1L, new GenLayerRandomValues(0L, 2)));
    }

    @Test
    public void testEdgeCoolWarm() {
        //this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.COOL_WARM));
    }

    @Test
    public void testEdgeHeatIce() {
        //this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.HEAT_ICE));
    }

    @Test
    public void testEdgeSpecial() {
        //this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.SPECIAL));
    }

    @Test
    public void testFuzzyZoom() {
        this.testLayers(new GenLayerFuzzyZoom(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testHills() {
        //this.testLayers(new GenLayerHills(1L, new GenLayerRandomValues(0L), new GenLayerRandomValues(1L)));
    }

    @Test
    public void testIsland() {
        this.testLayers(new GenLayerIsland(0L));
    }

    @Test
    public void testIslandNative() {
        this.testFastLayers(FastLayerIsland::new, NativeFastLayerIsland::new);
    }

    @Test
    public void testRareBiome() {
        this.testLayers(new GenLayerRareBiome(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testRemoveTooMuchOcean() {
        this.testLayers(new GenLayerFuzzyZoom(1L, new GenLayerRandomValues(0L, 2)));
    }

    @Test
    public void testRiver() {
        this.testLayers(new GenLayerRiver(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testRiverInit() {
        this.testLayers(new GenLayerRiverInit(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testRiverMix() {
        //this.testLayers(new GenLayerRiverMix(1L, new GenLayerRandomValues(0L), new GenLayerRandomValues(1L)));
    }

    @Test
    public void testShore() {
        //this.testLayers(new GenLayerShore(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testSmooth() {
        this.testLayers(new GenLayerSmooth(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testVoronoiZoom() {
        //this.testLayers(new GenLayerVoronoiZoom(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testZoom() {
        this.testLayers(new GenLayerZoom(1L, new GenLayerRandomValues(0L)));
    }

    private void testLayers(GenLayer vanilla) {
        if (true || !(vanilla instanceof GenLayerIsland)) { //TODO: remove this
            return;
        }

        SplittableRandom r = new SplittableRandom(12345L);

        vanilla.initWorldGenSeed(r.nextLong());
        FastLayer fast = makeFast(vanilla)[0];

        this.testAreas(vanilla, fast, 0, 0, 2, 2);
        this.testAreas(vanilla, fast, -1, -1, 2, 2);
        this.testAreas(vanilla, fast, -10, -10, 21, 21);

        for (int i = 0; i < 256; i++) {
            this.testAreas(vanilla, fast, r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(256) + 1, r.nextInt(256) + 1);
        }
    }

    private void testAreas(GenLayer vanilla, FastLayer fast, int areaX, int areaZ, int sizeX, int sizeZ) {
        int[] reference = vanilla.getInts(areaX, areaZ, sizeX, sizeZ);
        IntCache.resetIntCache();

        IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
        int[] fastGrid = alloc.get(sizeX * sizeZ);
        try {
            fast.getGrid(alloc, areaX, areaZ, sizeX, sizeZ, fastGrid);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    int referenceValue = reference[dz * sizeX + dx];
                    int fastValue = fastGrid[i];
                    checkState(referenceValue == fastValue, "at (%d, %d): fast: %d != expected: %d", areaX + dx, areaZ + dz, fastValue, referenceValue);
                }
            }
        } finally {
            alloc.release(fastGrid);
        }
    }

    private void testFastLayers(LongFunction<FastLayer> fs0, LongFunction<FastLayer> fs1) {
        SplittableRandom r = new SplittableRandom(12345L);

        long seed = r.nextLong();
        FastLayer f0 = fs0.apply(seed);
        FastLayer f1 = fs1.apply(seed);

        this.testAreas(f0, f1, 0, 0, 2, 2);
        this.testAreas(f0, f1, -1, -1, 2, 2);
        this.testAreas(f0, f1, 0, 0, 20, 10);
        this.testAreas(f0, f1, 10, 10, 21, 21);
        this.testAreas(f0, f1, -10, -10, 21, 21);

        for (int i = 0; i < 256; i++) {
            this.testAreas(f0, f1, r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(256) + 1, r.nextInt(256) + 1);
        }
    }

    private void testAreas(FastLayer f0, FastLayer f1, int areaX, int areaZ, int sizeX, int sizeZ) {
        IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
        int[] g0 = alloc.get(sizeX * sizeZ);
        int[] g1 = alloc.get(sizeX * sizeZ);
        try {
            f0.getGrid(alloc, areaX, areaZ, sizeX, sizeZ, g0);
            f1.getGrid(alloc, areaX, areaZ, sizeX, sizeZ, g1);

            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    int referenceValue = g0[i];
                    int fastValue = g1[i];
                    checkState(referenceValue == fastValue, "at (%d, %d): fast: %d != expected: %d", areaX + dx, areaZ + dz, fastValue, referenceValue);
                }
            }
        } finally {
            alloc.release(g1);
            alloc.release(g0);
        }
    }
}
