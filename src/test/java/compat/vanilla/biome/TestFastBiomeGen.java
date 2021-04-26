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
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.java.JavaLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.vanilla.GenLayerRandomValues;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.daporkchop.fp2.util.threading.fj.ThreadSafeForkJoinSupplier;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerAddSnow;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerRemoveTooMuchOcean;
import net.minecraft.world.gen.layer.GenLayerRiverInit;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.IntCache;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.SplittableRandom;
import java.util.concurrent.ForkJoinTask;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestFastBiomeGen {
    @BeforeClass
    public static void registerBootstrap() {
        Bootstrap.register();
    }

    @BeforeClass
    public static void ensureNativeBiomeGenIsAvailable() {
        checkState(FastLayerProvider.INSTANCE.isNative(), "native biome generation must be available for testing!");
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
        //this.testLayers(new GenLayerAddMushroomIsland(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testAddSnow() {
        this.testLayers(new GenLayerAddSnow(1L, new GenLayerRandomValues(0L)));
    }

    /*@Test
    public void testBiome() {
        this.testLayers(new GenLayerBiome(1L, new GenLayerRandomValues(0L), null, null));
    }*/

    /*@Test
    public void testBiomeEdge() {
        this.testLayers(new GenLayerBiomeEdge(1L, new GenLayerRandomValues(0L)));
    }*/

    @Test
    public void testDeepOcean() {
        //this.testLayers(new GenLayerDeepOcean(1L, new GenLayerRandomValues(0L, 2)));
    }

    /*@Test
    public void testEdgeCoolWarm() {
        this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.COOL_WARM));
    }*/

    /*@Test
    public void testEdgeHeatIce() {
        this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.HEAT_ICE));
    }*/

    /*@Test
    public void testEdgeSpecial() {
        this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.SPECIAL));
    }*/

    @Test
    public void testFuzzyZoom() {
        //this.testLayers(new GenLayerFuzzyZoom(1L, new GenLayerRandomValues(0L)));
    }

    /*@Test
    public void testHills() {
        this.testLayers(new GenLayerHills(1L, new GenLayerRandomValues(0L), new GenLayerRandomValues(1L)));
    }*/

    @Test
    public void testIsland() {
        this.testLayers(new GenLayerIsland(0L));
    }

    @Test
    public void testRareBiome() {
        //this.testLayers(new GenLayerRareBiome(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testRemoveTooMuchOcean() {
        this.testLayers(new GenLayerRemoveTooMuchOcean(1L, new GenLayerRandomValues(0L, 2)));
    }

    @Test
    public void testRiver() {
        //this.testLayers(new GenLayerRiver(1L, new GenLayerRandomValues(0L)));
    }

    @Test
    public void testRiverInit() {
        this.testLayers(new GenLayerRiverInit(1L, new GenLayerRandomValues(0L)));
    }

    /*@Test
    public void testRiverMix() {
        this.testLayers(new GenLayerRiverMix(1L, new GenLayerRandomValues(0L), new GenLayerRandomValues(1L)));
    }*/

    /*@Test
    public void testShore() {
        this.testLayers(new GenLayerShore(1L, new GenLayerRandomValues(0L)));
    }*/

    @Test
    public void testSmooth() {
        this.testLayers(new GenLayerSmooth(1L, new GenLayerRandomValues(0L)));
    }

    /*@Test
    public void testVoronoiZoom() {
        this.testLayers(new GenLayerVoronoiZoom(1L, new GenLayerRandomValues(0L)));
    }*/

    @Test
    public void testZoom() {
        //this.testLayers(new GenLayerZoom(1L, new GenLayerRandomValues(0L)));
    }

    private void testLayers(GenLayer vanilla) {
        SplittableRandom r = new SplittableRandom(12345L);

        vanilla.initWorldGenSeed(r.nextLong());
        FastLayer nativeFast = FastLayerProvider.INSTANCE.makeFast(vanilla)[0];
        FastLayer javaFast = JavaLayerProvider.INSTANCE.makeFast(vanilla)[0];

        if (javaFast.getClass() == nativeFast.getClass()) {
            System.err.printf("warning: no native layer implementation found for %s (fast: %s)\n", vanilla.getClass(), javaFast.getClass());

            this.testAreas(vanilla, javaFast, 0, 0, 2, 2);
            this.testAreas(vanilla, javaFast, -1, -1, 2, 2);
            this.testAreas(vanilla, javaFast, -10, -10, 21, 21);

            for (int i = 0; i < 256; i++) {
                this.testAreas(vanilla, javaFast, r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(256) + 1, r.nextInt(256) + 1);
            }
        } else {
            this.testAreasAndNative(vanilla, javaFast, nativeFast, 0, 0, 2, 2);
            this.testAreasAndNative(vanilla, javaFast, nativeFast, -1, -1, 2, 2);
            this.testAreasAndNative(vanilla, javaFast, nativeFast, -10, -10, 21, 21);

            for (int i = 0; i < 256; i++) {
                this.testAreasAndNative(vanilla, javaFast, nativeFast, r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000), r.nextInt(256) + 1, r.nextInt(256) + 1);
            }
        }
    }

    private void testAreas(GenLayer vanilla, FastLayer javaFast, int areaX, int areaZ, int sizeX, int sizeZ) {
        ForkJoinTask<int[]> futureReference = new ThreadSafeForkJoinSupplier<int[]>() {
            @Override
            protected int[] compute() {
                int[] reference = vanilla.getInts(areaX, areaZ, sizeX, sizeZ);
                IntCache.resetIntCache();
                return reference;
            }
        }.fork();

        //this is technically unsafe, but there's guaranteed to be nobody using biome code other than us since this is a unit test
        ForkJoinTask<int[]> futureJava = new ThreadSafeForkJoinSupplier<int[]>() {
            @Override
            protected int[] compute() {
                IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
                int[] fastGrid = alloc.get(sizeX * sizeZ);
                try {
                    javaFast.getGrid(alloc, areaX, areaZ, sizeX, sizeZ, fastGrid);
                    return fastGrid;
                } finally {
                    alloc.release(fastGrid);
                }
            }
        }.fork();

        int[] reference = futureReference.join();
        int[] fastGrid = futureJava.join();
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, i++) {
                int referenceValue = reference[dz * sizeX + dx];
                int fastValue = fastGrid[i];
                checkState(referenceValue == fastValue, "at (%d, %d): fast: %d != expected: %d", areaX + dx, areaZ + dz, fastValue, referenceValue);
            }
        }
    }

    private void testAreasAndNative(GenLayer vanilla, FastLayer javaFast, FastLayer nativeFast, int areaX, int areaZ, int sizeX, int sizeZ) {
        ForkJoinTask<int[]> futureReference = new ThreadSafeForkJoinSupplier<int[]>() {
            @Override
            protected int[] compute() {
                int[] reference = vanilla.getInts(areaX, areaZ, sizeX, sizeZ);
                IntCache.resetIntCache();
                return reference;
            }
        }.fork();

        ForkJoinTask<int[]> futureJava = new ThreadSafeForkJoinSupplier<int[]>() {
            @Override
            protected int[] compute() {
                IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
                int[] fastGrid = alloc.get(sizeX * sizeZ);
                try {
                    javaFast.getGrid(alloc, areaX, areaZ, sizeX, sizeZ, fastGrid);
                    return fastGrid.clone();
                } finally {
                    alloc.release(fastGrid);
                }
            }
        }.fork();

        ForkJoinTask<int[]> futureNative = new ThreadSafeForkJoinSupplier<int[]>() {
            @Override
            protected int[] compute() {
                IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
                int[] fastGrid = alloc.get(sizeX * sizeZ);
                try {
                    nativeFast.getGrid(alloc, areaX, areaZ, sizeX, sizeZ, fastGrid);
                    return fastGrid.clone();
                } finally {
                    alloc.release(fastGrid);
                }
            }
        }.fork();

        int[] reference = futureReference.join();
        int[] fastGrid = futureJava.join();
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, i++) {
                int referenceValue = reference[dz * sizeX + dx];
                int fastValue = fastGrid[i];
                checkState(referenceValue == fastValue, "at (%d, %d): fast (java): %d != expected: %d", areaX + dx, areaZ + dz, fastValue, referenceValue);
            }
        }

        fastGrid = futureNative.join();
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, i++) {
                int referenceValue = reference[dz * sizeX + dx];
                int fastValue = fastGrid[i];
                checkState(referenceValue == fastValue, "at (%d, %d): fast (native): %d != expected: %d", areaX + dx, areaZ + dz, fastValue, referenceValue);
            }
        }
    }
}
