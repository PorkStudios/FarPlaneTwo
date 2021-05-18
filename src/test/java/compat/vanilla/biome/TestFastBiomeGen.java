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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IPaddedLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IZoomingLayer;
import net.daporkchop.fp2.compat.vanilla.biome.layer.vanilla.GenLayerRandomValues;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerAddMushroomIsland;
import net.minecraft.world.gen.layer.GenLayerAddSnow;
import net.minecraft.world.gen.layer.GenLayerBiome;
import net.minecraft.world.gen.layer.GenLayerBiomeEdge;
import net.minecraft.world.gen.layer.GenLayerDeepOcean;
import net.minecraft.world.gen.layer.GenLayerEdge;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerHills;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerRareBiome;
import net.minecraft.world.gen.layer.GenLayerRemoveTooMuchOcean;
import net.minecraft.world.gen.layer.GenLayerRiver;
import net.minecraft.world.gen.layer.GenLayerRiverInit;
import net.minecraft.world.gen.layer.GenLayerRiverMix;
import net.minecraft.world.gen.layer.GenLayerShore;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import util.FP2Test;

import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFastBiomeGen {
    @BeforeClass
    public static void init() {
        FP2Test.init();
    }

    @BeforeClass
    public static void ensureNativeBiomeGenIsAvailable() {
        checkState(FastLayerProvider.INSTANCE.isNative(), "native biome generation must be available for testing!");
    }

    @Test
    public void testRandom() {
        this.testLayers(new GenLayerRandomValues(0L), true);
    }

    //

    @Test
    public void testAddIsland() {
        this.testLayers(new GenLayerAddIsland(1L, new GenLayerRandomValues(0L, 2)), true);
    }

    @Test
    public void testAddMushroomIsland() {
        this.testLayers(new GenLayerAddMushroomIsland(1L, new GenLayerRandomValues(0L, 2)), true);
    }

    @Test
    public void testAddSnow() {
        this.testLayers(new GenLayerAddSnow(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testBiome() {
        this.testLayers(new GenLayerBiome(1L, new GenLayerRandomValues(0L), null, null), true);
    }

    @Test
    public void testBiomeEdge() {
        this.testLayers(new GenLayerBiomeEdge(1L, new GenLayerFuzzyZoom(1L, new GenLayerRandomValues(0L))), true);
    }

    @Test
    public void testDeepOcean() {
        this.testLayers(new GenLayerDeepOcean(1L, new GenLayerRandomValues(0L, 2)), true);
    }

    @Test
    public void testEdgeCoolWarm() {
        this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L, 4), GenLayerEdge.Mode.COOL_WARM), true);
    }

    @Test
    public void testEdgeHeatIce() {
        this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.HEAT_ICE), true);
    }

    @Test
    public void testEdgeSpecial() {
        this.testLayers(new GenLayerEdge(1L, new GenLayerRandomValues(0L), GenLayerEdge.Mode.SPECIAL), true);
    }

    @Test
    public void testFuzzyZoom() {
        this.testLayers(new GenLayerFuzzyZoom(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testHills() {
        this.testLayers(new GenLayerHills(1L, new GenLayerFuzzyZoom(1L, new GenLayerRandomValues(0L)), new GenLayerRandomValues(1L)), true);
    }

    @Test
    public void testIsland() {
        this.testLayers(new GenLayerIsland(0L), true);
    }

    @Test
    public void testRareBiome() {
        this.testLayers(new GenLayerRareBiome(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testRemoveTooMuchOcean() {
        this.testLayers(new GenLayerRemoveTooMuchOcean(1L, new GenLayerRandomValues(0L, 2)), true);
    }

    @Test
    public void testRiver() {
        this.testLayers(new GenLayerRiver(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testRiverInit() {
        this.testLayers(new GenLayerRiverInit(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testRiverMix() {
        this.testLayers(new GenLayerRiverMix(1L, new GenLayerRandomValues(0L), new GenLayerRandomValues(1L)), true);
    }

    @Test
    public void testShore() {
        this.testLayers(new GenLayerShore(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testSmooth() {
        this.testLayers(new GenLayerSmooth(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testVoronoiZoom() {
        this.testLayers(new GenLayerVoronoiZoom(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testZoom() {
        this.testLayers(new GenLayerZoom(1L, new GenLayerRandomValues(0L)), true);
    }

    @Test
    public void testMultipleZooms() {
        this.testLayers(new GenLayerZoom(1L, new GenLayerZoom(1L, new GenLayerZoom(1L, new GenLayerRandomValues(0L)))), true);
    }

    @Test
    public void zzy_testEverything_default() {
        this.testLayers(GenLayer.initializeAllBiomeGenerators(1L, WorldType.DEFAULT, null)[0], false);
    }

    @Test
    public void zzz_testEverything_largeBiomes() {
        this.testLayers(GenLayer.initializeAllBiomeGenerators(1L, WorldType.LARGE_BIOMES, null)[0], false);
    }

    private void testLayers(GenLayer vanilla, boolean testSingle) {
        SplittableRandom r = new SplittableRandom(12345L);

        vanilla.initWorldGenSeed(r.nextLong());
        IFastLayer nativeFast = FastLayerProvider.INSTANCE.makeFast(vanilla)[0];
        IFastLayer javaFast = FastLayerProvider.JAVA_INSTANCE.makeFast(vanilla)[0];

        NamedLayer[] layers = {
                new NamedLayer(javaFast, "java"),
                new NamedLayer(nativeFast, "native")
        };
        if (javaFast.getClass() == nativeFast.getClass()) {
            System.err.printf("warning: no native layer implementation found for %s (fast: %s)\n", vanilla.getClass(), javaFast.getClass());
            layers = Arrays.copyOf(layers, 1);
        }

        this.testLayers(0, 0, 2, 2, testSingle, vanilla, layers);
        this.testLayers(-1, -1, 2, 2, testSingle, vanilla, layers);
        this.testLayers(-10, -10, 21, 21, testSingle, vanilla, layers);

        for (int i = 0; i < 256; i++) {
            this.testLayers(r.nextInt(-1000000, 1000000), r.nextInt(-1000000, 1000000),
                    //workaround for a vanilla bug in GenLayerVoronoiZoom
                    vanilla instanceof GenLayerVoronoiZoom ? 16 : r.nextInt(256) + 1, vanilla instanceof GenLayerVoronoiZoom ? 16 : r.nextInt(256) + 1,
                    testSingle, vanilla, layers);
        }

        this.testLayersMultiGrid(-3, -3, 5, 16, 16, vanilla, layers);
        this.testLayersMultiGrid((-5664 >> 2) - 2, (-5664 >> 2) - 2, 5, 1 << 3, 21, vanilla, layers);

        NamedLayer[] paddedLayers = Stream.of(layers).filter(l -> l.layer instanceof IPaddedLayer).toArray(NamedLayer[]::new);
        NamedLayer[] zoomingLayers = Stream.of(layers).filter(l -> l.layer instanceof IZoomingLayer).toArray(NamedLayer[]::new);
        if (paddedLayers.length > 0) { //multigrid for padded layers
            this.testLayersMultiGrid(-3, -3, 5, 7, 16, vanilla, paddedLayers);
        } else if (zoomingLayers.length > 0) { //multigrid for zooming layers
            int shift = Stream.of(zoomingLayers).mapToInt(l -> ((IZoomingLayer) l.layer).shift()).findAny().getAsInt();
            this.testLayersMultiGrid(-3, -3, 5, 5 + 2 + (1 << shift), 5, vanilla, zoomingLayers);
        }
    }

    private void testLayers(int x, int z, int sizeX, int sizeZ, boolean testSingle, GenLayer vanilla, @NonNull NamedLayer... layers) {
        CompletableFuture<int[]> futureReference = CompletableFuture.supplyAsync(() -> {
            int[] reference = vanilla.getInts(x, z, sizeX, sizeZ);
            IntCache.resetIntCache();

            int[] swapped = new int[sizeX * sizeZ];
            for (int i = 0, xx = 0; xx < sizeX; xx++) {
                for (int zz = 0; zz < sizeZ; zz++) {
                    swapped[i++] = reference[zz * sizeX + xx];
                }
            }
            return swapped;
        });

        List<CompletableFuture<int[]>> gridFutures = Stream.of(layers).map(layer -> CompletableFuture.supplyAsync(() -> {
            int[] grid = new int[sizeX * sizeZ];
            layer.layer.getGrid(ALLOC_INT.get(), x, z, sizeX, sizeZ, grid);
            return grid;
        })).collect(Collectors.toList());

        List<CompletableFuture<int[]>> singleFutures = !testSingle ? null : Stream.of(layers).map(layer -> CompletableFuture.supplyAsync(() -> {
            int[] grid = new int[sizeX * sizeZ];
            IntStream.range(0, sizeX).parallel().forEach(dx -> {
                ArrayAllocator<int[]> alloc1 = ALLOC_INT.get();
                for (int i = dx * sizeZ, dz = 0; dz < sizeZ; dz++) {
                    grid[i++] = layer.layer.getSingle(alloc1, x + dx, z + dz);
                }
            });
            return grid;
        })).collect(Collectors.toList());

        int[] reference = futureReference.join();
        for (int j = 0; j < layers.length; j++) {
            String name = layers[j].name;

            int[] grid = gridFutures.get(j).join();
            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    int referenceValue = reference[i];
                    int fastValue = grid[i];
                    if (referenceValue != fastValue) {
                        throw new IllegalStateException(PStrings.fastFormat("grid: at (%d, %d): fast (%s): %d != expected: %d", x + dx, z + dz, name, fastValue, referenceValue));
                    }
                }
            }
        }
        for (int j = 0; testSingle && j < layers.length; j++) {
            String name = layers[j].name;

            int[] grid = singleFutures.get(j).join();
            for (int i = 0, dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++, i++) {
                    int referenceValue = reference[i];
                    int fastValue = grid[i];
                    if (referenceValue != fastValue) {
                        throw new IllegalStateException(PStrings.fastFormat("single: at (%d, %d): fast (%s): %d != expected: %d", x + dx, z + dz, name, fastValue, referenceValue));
                    }
                }
            }
        }
    }

    private void testLayersMultiGrid(int x, int z, int size, int dist, int count, GenLayer vanilla, @NonNull NamedLayer... layers) {
        CompletableFuture<int[]> futureReference = CompletableFuture.supplyAsync(() -> {
            int[] out = new int[count * count * size * size];
            for (int i = 0, tileX = 0; tileX < count; tileX++) {
                for (int tileZ = 0; tileZ < count; tileZ++) {
                    int[] reference = vanilla.getInts(x + tileX * dist, z + tileZ * dist, size, size);
                    IntCache.resetIntCache();

                    for (int dx = 0; dx < size; dx++) {
                        for (int dz = 0; dz < size; dz++) {
                            out[i++] = reference[dz * size + dx];
                        }
                    }
                }
            }
            return out;
        });

        List<CompletableFuture<int[]>> fastFutures = Stream.of(layers).map(layer -> CompletableFuture.supplyAsync(() -> {
            int[] grids = new int[count * count * size * size];
            layer.layer.multiGetGrids(ALLOC_INT.get(), x, z, size, dist, 0, count, grids);
            return grids;
        })).collect(Collectors.toList());

        int[] reference = futureReference.join();
        for (int j = 0; j < layers.length; j++) {
            String name = layers[j].name;

            int[] grids = fastFutures.get(j).join();
            for (int i = 0, tileX = 0; tileX < count; tileX++) {
                for (int tileZ = 0; tileZ < count; tileZ++) {
                    for (int dx = 0; dx < size; dx++) {
                        for (int dz = 0; dz < size; dz++, i++) {
                            int referenceValue = reference[i];
                            int fastValue = grids[i];
                            if (referenceValue != fastValue) {
                                throw new IllegalStateException(PStrings.fastFormat("multigrid: at (%d, %d): fast (%s): %d != expected: %d", x + tileX * dist + dx, z + tileZ * dist + dz, name, fastValue, referenceValue));
                            }
                        }
                    }
                }
            }
        }
    }

    @RequiredArgsConstructor
    protected static class NamedLayer {
        @NonNull
        protected final IFastLayer layer;
        @NonNull
        protected final String name;
    }
}
